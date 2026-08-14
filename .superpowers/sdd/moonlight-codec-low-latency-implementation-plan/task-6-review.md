APPROVED

# Task 6 Fix Round 2 review

No Critical or Important correctness, lifecycle, concurrency, performance, compatibility, scope, or test-behavior finding remains for the complete range `945a70f59657ef4dea5f9b57bea05b666b004cec..a126f0f3223bc3e6977af6fb86e1f3c1343a8da3`. Fix Round 2 closes the retained-link blocker. The two Minor test-quality notes below are non-blocking.

## Findings

### Critical

- None.

### Important

- None.

### Minor

1. `app/src/test/java/com/limelight/binding/video/DecoderLatencySamplerTest.java:173-203` behaviorally covers failed-tail preservation and FIFO output, while the production identity-removal code was inspected for single, head, middle, and tail cases. There is still no explicit middle-token removal test. Likewise, the invalidation tests at lines 354-495 cover blocking, overlap, and CSD behavior but not nested or throwing invalidation, although the production depth/`finally` logic is correct by inspection. Adding those cases later would strengthen edge regression coverage without blocking this task.
2. The raw-source assertion at `DecoderLatencySamplerTest.java:510-522` remains a structural count that could theoretically pass for dead or semantically misplaced calls. All five live production call sites were independently inspected and are correctly placed immediately after successful dequeue and before release/drop/pacing, so this does not block approval. A future injected codec adapter exercised through the production entry points would be stronger.

## Fix Round 2 closure

- `clearPendingLocked()` iterates the `LongSparseArray` of current heads, so each loop entry represents one distinct PTS chain. It saves `next`, nulls the current link, and advances until that chain is fully detached before clearing both head and tail maps (`MediaCodecDecoderRenderer.java:319-332`). There is no double-head path: a token is created once, belongs to one PTS, and is appended to only that chain.
- All append, exact removal, output consume, and lifecycle-clear mutations use the same metadata lock. The clear walk therefore cannot race with an append/removal, skip a concurrently added node, or observe partial tail state. New tokens start with `next == null`; append only links the prior tail to the new token; output/failure removal severs the removed token. These invariants do not create cycles, so the null-terminated lifecycle walk cannot loop indefinitely.
- Recovery clear, new-session reset, and stop snapshot all call this same detach routine. The three focused tests retain A, B, and C references across each lifecycle path and prove every retained head/middle/tail `next` is null and no pending output remains (`DecoderLatencySamplerTest.java:229-323`).
- The new work is O(number of pending inputs) and runs only during codec invalidation, session reset, or stop summary. Per-frame token append, exact output consume, failure cancellation, and sampler record are unchanged. The latency lock still never spans `queueInputBuffer()`, `flush()`, or any other vendor codec call, including CSD.

## Complete Task 6 contract audit

- `DecoderLatencySampler` remains a fixed `int[16384]` circular buffer with O(1), allocation/sort/log-free record; defensive snapshots; sort outside the shared lock; nearest-rank p50/p95/p99; negative rejection; and microsecond clamp to `Integer.MAX_VALUE`. Empty summaries use deterministic `NA` fields and the exact metric name `decoder_input_queue_call_start_to_first_output_dequeue_us`.
- Unique operation tokens plus per-PTS head/tail FIFO preserve exact cancellation identity. Failed B leaves A; two same-PTS successes produce A then B; successful first output atomically removes and records exactly once. CSD is never tracked.
- Non-CSD timestamp insertion occurs in the short metadata critical section directly before the vendor queue call. Generation/depth is established and pending state is cleared before invalidation, vendor work runs outside the diagnostics lock, nested/exceptional depth is balanced in `finally`, overlapping queues run untracked, and stale generations cannot delete or resurrect current entries.
- All five production output dequeue paths use the immediate wrapper; the six delayed update calls and old method are absent. Timeout/release/drop/EWMA/latest-only/balanced/pacing behavior is preserved. No renderer stutter counter was introduced.
- Setup resets pending state, sampler, and the one-shot guard. Flush/restart/reset/recreate clear pending state before invalidation while retaining session samples. `prepareForStop()` does not snapshot/reset. Stop joins Choreographer and renderer threads to termination with interrupt restoration, then logs/resets once; cleanup is idempotent. Crash snapshot is copy-only and reports the active successful profile plus the exact metric.
- Each real configuration sequence builds one immutable plan and emits one deterministic capability snapshot. API < 31 is `not_supported`; successful-empty differs from failure; failures retry only on a later sequence; advertised parameters are the sorted known-key intersection; requested options are never called supported. Attempt/fallback/selected/capability/summary prefixes and field order remain stable with no full vendor dump.
- Task 2/3 QTI/MTK profile and success-cache behavior, Task 4 bounded attempts/base fallback/initial `-5`/runtime exception/successful-state commit, and all external-display/Surface/input/mouse/frame-pacing boundaries remain intact.

## Commit and scope

- Initial commit `7b34ad61b85a3e67e93033da85f662b2bebc6deb` has parent `945a70f59657ef4dea5f9b57bea05b666b004cec`; Fix Round 1 `29e834218bc452b7ae9bebd767a36fc4e3e02447` has parent `7b34ad61b85a3e67e93033da85f662b2bebc6deb`; Fix Round 2 `a126f0f3223bc3e6977af6fb86e1f3c1343a8da3` has parent `29e834218bc452b7ae9bebd767a36fc4e3e02447`.
- The complete range contains exactly the original five authorized files. Fix Round 2 changes only `MediaCodecDecoderRenderer.java` and `DecoderLatencySamplerTest.java`.
- Current source contains one direct `videoDecoder.dequeueOutputBuffer()` inside the wrapper, exactly five wrapper calls, zero delayed `updateDecodeLatencyStats()` calls, and zero `videoDecoder` references inside `DecoderLatencyState`.
- No protected external-display/Presentation/Surface/input/mouse/controller, resource/preference/manifest, native/network, Task 7, stutter-counter, unbounded-loop, install, launch, or ADB change appears in the range.

## Independent verification

- The updated implementer report, complete original implementation, both fix deltas, and cumulative base-to-head state were reviewed directly.
- Focused command: `./gradlew.bat :app:testNonRoot_gameDebugUnitTest --tests com.limelight.binding.video.LowLatencyProfilePlannerTest --tests com.limelight.binding.video.MediaCodecLowLatencyCapabilityTest --tests com.limelight.binding.video.MediaCodecDecoderRecoveryProfileTest --tests com.limelight.binding.video.DecoderLatencySamplerTest --console=plain --rerun-tasks` — `BUILD SUCCESSFUL`. Parsed XML: 4 suites, 84 tests, 0 failures, 0 errors, 0 skipped (31 planner, 14 capability, 14 recovery, 25 sampler/renderer).
- Fresh compile: `./gradlew.bat :app:compileNonRoot_gameDebugJavaWithJavac --console=plain --rerun-tasks` — `BUILD SUCCESSFUL`; 18 tasks executed.
- Full command: `./gradlew.bat :app:testNonRoot_gameDebugUnitTest --console=plain --rerun-tasks` — expected nonzero Gradle result. Parsed XML: 28 suites, 193 tests, 5 failures, 0 errors, 0 skipped. Failures are exactly `LayoutInflationTest.allLayoutsInflateSuccessfully`, `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`, `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`, `SimpleStartupTest.testApplicationOnCreate`, and `StartupTest.testApplicationStartup`.
- `git diff --check` passed for the complete range and Fix Round 2; `git diff --cached --check` passed. Scope audit reports five authorized cumulative files, two authorized fix files, zero unauthorized/protected paths, zero stutter-counter hits, and zero unbounded-loop additions.
- `AGENTS.md` and this review remain ignored. The visible worktree remains only the pre-existing untracked `.codex/` directory.

No source was edited, staged, committed, checked out, installed, or exercised through ADB. Task 7 was not started.
