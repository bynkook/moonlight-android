# Task 6 implementer report

## Identity

- Branch: `fix-codec-low-latency-compatibility`
- Base: `945a70f59657ef4dea5f9b57bea05b666b004cec`
- Initial commit: `7b34ad61b85a3e67e93033da85f662b2bebc6deb`
- Initial commit message: `Add deterministic codec low-latency diagnostics`
- Fix Round 1 commit: `29e834218bc452b7ae9bebd767a36fc4e3e02447`
- Fix Round 1 commit message: `Fix decoder latency tracking concurrency`
- Fix Round 2 commit: `a126f0f3223bc3e6977af6fb86e1f3c1343a8da3`
- Fix Round 2 commit message: `Clear decoder latency token links on reset`

## Implemented contract

- Added `DecoderLatencySampler`, a fixed `int[16384]` ring with O(1), allocation/sort/log-free recording, defensive snapshots, nearest-rank p50/p95/p99, negative-delta rejection, and microsecond clamping to `Integer.MAX_VALUE`.
- Defined the exact metric as `decoder_input_queue_call_start_to_first_output_dequeue_us`. Non-CSD input timestamps are inserted immediately before `queueInputBuffer()` and a unique operation token is removed if the queue call throws. Duplicate PTS values use FIFO order, so cancellation cannot overwrite or remove another operation.
- Routed all five output dequeue sites through one wrapper. It atomically consumes the first matching PTS and records before release/drop/pacing branches; all six delayed stats calls were removed while the existing aggregate millisecond semantics were retained at dequeue.
- Used the renderer's single `decoderLatencyLock` only for pending metadata and sampler operations. Vendor input queue and codec invalidation calls run outside the diagnostics lock. A generation plus nested invalidation depth clears and invalidates pending tokens before codec invalidation, suppresses tracking for overlapping queue operations without suppressing the codec call, and prevents stale failures from removing a later generation's token. CSD bypasses latency metadata locking entirely.
- Reset pending entries, samples, and the one-shot guard at setup; retained samples across recovery while clearing pending entries before codec invalidation; left `prepareForStop()` snapshot-free; and made stop/cleanup summary handling one-shot and idempotent.
- Stop now joins renderer and Choreographer threads to actual termination before snapshotting, while restoring an interrupt observed during join.
- Added one immutable capability/profile plan per configuration sequence with tri-state vendor enumeration, sorted known-key filtering, successful-result caching, failure retry only on a later sequence, and one deterministic capability log.
- Added exact deterministic `CodecLLCapability`, `CodecLLAttempt`, `CodecLLFallback`, `CodecLLSelected`, and `CodecLatencySummary` formatting. Crash text copies the sampler without reset and reports only the active successfully configured profile index/name.
- Preserved Task 2 capability-cache contracts and Task 4 profile ordering, fallback/recovery disposition, and active-state commit semantics. No renderer stutter/drop counter was added.

## TDD evidence

### RED

The first focused test run failed at test compilation with 68 missing-symbol errors for the not-yet-created sampler, latency state, immutable plan, tri-state status, and deterministic formatter seams.

Additional narrow RED checks were captured before their production changes:

- OTHER-family runtime plan enumeration: expected one provider open, observed zero.
- NVIDIA advertised-key filtering: expected `vendor.nvidia.disable-output-reorder`, observed it omitted.
- Interrupted join and atomic queue/invalidation regression tests: test compilation failed with three missing seams (`joinThreadPreservingInterrupt`, `runInputQueueCall`, and `clearPendingAndRunCodecInvalidation`).

### Initial GREEN

Focused command:

`./gradlew.bat :app:testNonRoot_gameDebugUnitTest --tests com.limelight.binding.video.LowLatencyProfilePlannerTest --tests com.limelight.binding.video.MediaCodecLowLatencyCapabilityTest --tests com.limelight.binding.video.MediaCodecDecoderRecoveryProfileTest --tests com.limelight.binding.video.DecoderLatencySamplerTest --console=plain`

Result: `BUILD SUCCESSFUL`; 76 tests, 0 failures, 0 errors, 0 skipped.

- `LowLatencyProfilePlannerTest`: 31
- `MediaCodecLowLatencyCapabilityTest`: 14
- `MediaCodecDecoderRecoveryProfileTest`: 14
- `DecoderLatencySamplerTest`: 17

Fresh Java compile command:

`./gradlew.bat :app:compileNonRoot_gameDebugJavaWithJavac --console=plain --rerun-tasks`

Result: `BUILD SUCCESSFUL`.

## Full-suite comparison

Baseline at the required base: 164 tests, 5 failures, 0 errors, 0 skipped.

Initial Task 6 commit after a fresh `--rerun-tasks`: 185 tests, 5 failures, 0 errors, 0 skipped. Fix Round 1 adds five focused sampler/renderer tests, bringing that tree to 190 tests. Fix Round 2 adds three lifecycle-link tests, bringing the fresh final tree to 193 tests, 5 failures, 0 errors, 0 skipped.

Failure identities are unchanged:

1. `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
2. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
3. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
4. `com.limelight.SimpleStartupTest.testApplicationOnCreate`
5. `com.limelight.StartupTest.testApplicationStartup`

No new failure or error was introduced.

## Scope and audits

- Initial commit parent is exactly `945a70f59657ef4dea5f9b57bea05b666b004cec`; the Fix Round 1 commit parent is exactly `7b34ad61b85a3e67e93033da85f662b2bebc6deb`; the Fix Round 2 commit parent is exactly `29e834218bc452b7ae9bebd767a36fc4e3e02447`.
- The full base-to-head range contains exactly the five Task 6 authorized files; each fix commit modifies only the same authorized renderer and sampler test files.
- `git diff --check` and `git diff --cached --check` passed.
- Source audit found one direct `videoDecoder.dequeueOutputBuffer()` call inside the wrapper, five wrapper call sites, and zero delayed `updateDecodeLatencyStats` calls.
- No `releaseWithPolicy()` or `doFrame()`/Choreographer scheduling declaration was modified. No unbounded `while(true)`, `for(;;)`, or `do` loop was introduced; the new join loop is bounded by `Thread.isAlive()` and preserves the pre-existing blocking-stop contract.
- No Game/preferences/resources, display/Presentation/Surface ownership, input/mouse/controller, release/drop/EWMA/latest-only/balanced policy, JNI/native/network, `AGENTS.md`, `.codex/`, docs, build artifact, Task 7, ADB, install, or launch file/action was committed.
- `AGENTS.md` and this report are ignored. The pre-existing untracked `.codex/` directory remains untouched.

## Caveats

- The full Gradle task exits nonzero solely because of the accepted frozen five-test baseline above.
- Existing Gradle/resource/deprecation and Mockito dynamic-agent warnings remain non-blocking.
- The raw-source assertion is limited to the binding requirement that exactly five dequeue sites use the wrapper; all sampler, lifecycle, queue-cancellation, concurrency, capability, formatting, and crash contracts are otherwise covered behaviorally.
- No device, ADB, install, streaming, or hardware validation was performed; those belong to later tasks.

## Fix Round 1

### Review findings addressed

- Replaced the one-entry-per-PTS map with unique pending tokens and per-PTS head/tail FIFO queues. Each tracked non-CSD queue attempt allocates one token object in place of the previous boxed `Long` timestamp; the tail sparse array stores the same reference and does not add a per-input object. Append and output consume remain O(1). Exact failed-token removal is O(k) only within the duplicate-PTS chain on the exceptional path.
- Removed all vendor codec calls from `decoderLatencyLock`. Non-CSD tracking metadata is inserted under the lock immediately before the vendor queue call, then the call runs outside the lock. Invalidation increments the generation/depth and clears pending metadata under the lock, invokes the codec outside it, and decrements depth in `finally`. Queue operations that overlap invalidation still reach the codec but are not tracked. The coordinated recovery path retains its pre-existing all-thread quiescence.
- Retained the interrupted-join fix: `stop()` waits for actual renderer/Choreographer termination, restores interrupt status, and only then takes the one-shot latency summary.

### RED

The duplicate-PTS focused run executed 19 tests and failed exactly two new tests against the reviewed implementation:

1. `duplicatePtsFailureRemovesOnlyTheFailedOperationToken`: expected 100, got -1.
2. `duplicatePtsOutputsConsumeSuccessfulOperationsInFifoOrder`: expected first 100, got 90.

After the token/FIFO change, all 19 tests passed. The lock-boundary focused run then executed 22 tests and failed exactly four new concurrency tests by timeout/assertion against vendor calls held under the diagnostics lock:

1. `blockedInputQueueCallDoesNotBlockUnrelatedOutputConsumption`
2. `invalidationClearsPendingBeforeCodecCallWithoutHoldingLatencyLock`
3. `inputQueueCallOverlappingInvalidationRunsButIsNotTracked`
4. `codecConfigQueueCallDoesNotAcquireLatencyLockDuringInvalidation`

### GREEN

- `DecoderLatencySamplerTest`: 22 tests, 0 failures, 0 errors, 0 skipped, including a second fresh stability run after the full suite.
- Focused Task 1-6 set: 81 tests, 0 failures, 0 errors, 0 skipped (`LowLatencyProfilePlannerTest` 31, `MediaCodecLowLatencyCapabilityTest` 14, `MediaCodecDecoderRecoveryProfileTest` 14, `DecoderLatencySamplerTest` 22).
- Fresh Java compilation with `--rerun-tasks`: `BUILD SUCCESSFUL`.
- Fresh full unit suite with `--rerun-tasks`: 190 tests, 5 failures, 0 errors, 0 skipped. The five failure identities remain exactly the frozen baseline listed above.

### Fix-round audits

- The follow-up commit changes only `MediaCodecDecoderRenderer.java` and `DecoderLatencySamplerTest.java`, both within the original five-file Task 6 authorization. The full base-to-head range remains exactly the authorized five files.
- The renderer still has one raw output dequeue inside the latency wrapper and five wrapper call sites. No delayed stats call was restored.
- No protected pacing/drop/display/input path, unbounded loop, Task 7, ADB, install, or launch action was added.

## Fix Round 2

### Review finding addressed

- `clearPendingLocked()` now walks every distinct pending head chain under `decoderLatencyLock`, saves each successor, and nulls the current token's `next` before clearing the head/tail maps. An in-flight vendor queue call retaining an old head therefore cannot retain its cleared same-PTS successors after recovery, session reset, or stop.
- The walk is O(number of pending inputs) only on lifecycle invalidation/reset/stop. Chains are append-only singly linked lists whose mutations all occur under the same lock, so traversal cannot observe a cycle or concurrent append. Per-frame append, output consume, and sampler recording remain unchanged.
- Generation and invalidation-depth behavior is unchanged. Vendor queue and codec invalidation calls remain outside `decoderLatencyLock`.

### RED

Against Fix Round 1, the focused sampler suite executed 25 tests and failed exactly the three new retained-token tests, each because the old head's `next` remained non-null:

1. `recoveryClearSeversEveryRetainedDuplicatePtsLink`
2. `newSessionResetSeversEveryRetainedDuplicatePtsLink`
3. `stopSnapshotSeversEveryRetainedDuplicatePtsLink`

Each test retains all three tokens in an A-to-B-to-C same-PTS chain, verifies the chain exists, invokes its lifecycle clear path, then requires every retained token's `next` to be null and the pending output lookup to be absent.

### GREEN

- `DecoderLatencySamplerTest`: 25 tests, 0 failures, 0 errors, 0 skipped.
- Focused Task 1-6 set: 84 tests, 0 failures, 0 errors, 0 skipped (`LowLatencyProfilePlannerTest` 31, `MediaCodecLowLatencyCapabilityTest` 14, `MediaCodecDecoderRecoveryProfileTest` 14, `DecoderLatencySamplerTest` 25).
- Fresh Java compilation with `--rerun-tasks`: `BUILD SUCCESSFUL`; 18 tasks executed.
- Fresh full unit suite with `--rerun-tasks`: 193 tests, 5 failures, 0 errors, 0 skipped. The five failure identities remain exactly the frozen baseline listed above.

### Fix-round audits

- Fix Round 2 changes only `MediaCodecDecoderRenderer.java` and `DecoderLatencySamplerTest.java`; the full base-to-head range remains exactly the original five authorized files.
- Source remains at one raw output dequeue inside the wrapper, five wrapper call sites, zero delayed stats calls, and zero `videoDecoder` references inside `DecoderLatencyState`.
- The only new loops are a finite sparse-array head iteration and a null-terminated chain walk under the metadata lock. No unbounded loop or protected pacing/drop/display/input change was added.
