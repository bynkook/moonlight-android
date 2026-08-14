# Task 4 implementer report

## Scope and base

- Branch: `fix-codec-low-latency-compatibility`
- BASE: `1d85dc3eac630bcdd43b6447c286b482106ea8b7`
- Protected paths: no changes to `Game`, render target/Surface ownership, Presentation/controller lifecycle, frame pacing/output scheduling, input/mouse, preferences, native code, `.codex`, or `AGENTS.md`.

## RED evidence

- Added the named/indexed immutable helper-result tests and the pure renderer recovery/attempt suite before production edits.
- Command: `:app:testNonRoot_gameDebugUnitTest --tests 'com.limelight.binding.video.*LowLatency*' --tests com.limelight.binding.video.MediaCodecDecoderRecoveryProfileTest`.
- Expected failure: unit-test compilation failed with 63 missing-symbol errors for `AppliedLowLatencyOptions`, `DecoderConfigurationProfile`, selection/recovery/state seams, and failure policy.
- Added final-failure diagnostic contract separately; its RED run failed because `finalConfigurationFailureMessage(...)` did not exist.

## Implementation

- Replaced the boolean helper result with `AppliedLowLatencyOptions(profileIndex, profileName, ordered immutable integerOptions)`; empty option maps are rejected and exhaustion returns `null` without touching the format.
- Preserved the existing QTI/MTK planner and injected capability-store seams. Remaining NVIDIA/Kirin/Exynos paths now return one finite, deterministic named nonstandard profile; NVIDIA can no longer return options forever.
- Added pure profile-attempt selection: option indices are limited to `0..10`, early exhaustion immediately appends one renderer-owned `base`, a broken endless source is not called after index 10, and `startTry >= 11` is base-only.
- Renderer logs the real attempt ordinal, profile index/name, and exact ordered requested map. Base is always renderer-owned with index `-1` and empty options.
- Replaced the premature `configuredFormat` assignment with one immutable `ConfiguredDecoderState`. It is committed only after both `configure()` and `start()` succeed; failed candidates leave the previous state unchanged.
- Flush/restart/reset retain and reuse the active format/profile. Only full recreation uses the distinct runtime-recovery selection: NON_STANDARD -> `android-standard` when available -> base; ANDROID_STANDARD/BASE -> base only.
- Initial base failure severe-logs decoder/profile/options/FEATURE capability and returns `-5`. Runtime recovery base failure severe-logs and rethrows the original `IllegalArgumentException`/`IllegalStateException`, or the existing `RuntimeException(IOException)` wrapping.

## GREEN and regression evidence

- Targeted command: 52 tests passed, exit 0.
- Java compile: `:app:compileNonRoot_gameDebugJavaWithJavac --console=plain`, exit 0.
- Full suite: 159 tests, 5 failures, 0 errors. The only failures are the frozen baseline:
  - `LayoutInflationTest.allLayoutsInflateSuccessfully`
  - `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
  - `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
  - `SimpleStartupTest.testApplicationOnCreate`
  - `StartupTest.testApplicationStartup`
- `git diff --check`: clean.
- Unbounded legacy renderer loop search: no match.
- Protected renderer diff search: no changed renderTarget, Surface/frame-rate/pacing, input/mouse, Presentation, or display logic; the only textual match was the unchanged class declaration containing `Choreographer.FrameCallback` due line-ending context.

## Self-review

- Result metadata makes requested options explicit; it does not infer codec support from `MediaFormat.setInteger()` success.
- The planner never represents base and non-null helper results cannot be empty.
- Actual configuration is capped at 11 option attempts plus exactly one base attempt.
- Runtime recovery never reintroduces vendor, fence, legacy, or performance options.
- The `.codex` plan remains untracked and was not staged. `AGENTS.md` and this report are ignored.

## Commit

- `c6a73167` — `Bound codec configuration and downgrade unsafe profiles`
- The commit contains exactly the two production files and two Task 4 test files listed in the brief/adjudication.

## Fix Round 1

### Review findings addressed

- Restored the finite historical fallback dimensions for NVIDIA, Kirin, and Exynos without restoring cumulative format mutation or an unbounded NVIDIA loop. The ordered risk-to-safe sequence is assembled from independent `standard`, family `vendor`, and `scheduler` maps; impossible, empty, and ordered-duplicate profiles are omitted.
- Android standard is included only when `FEATURE_LowLatency` is advertised. Vendor keys are returned only while Ultra Low Latency is enabled. The scheduler selects maximum operating rate when allowed, otherwise API 23+ priority.
- Made `initializeDecoder(boolean throwOnCodecError)` authoritative: `false` maps to `INITIAL`, `true` maps to `RUNTIME_RECOVERY`. Setup calls the public entry point with `false`; full codec recreation calls it with `true`; the redundant private bypass was removed.

### RED evidence

- Added the entry-point mapping contract first. Its RED compile failed on the missing `configurationModeForInitializeDecoder(boolean)` seam.
- After adding only that mapping, the legacy capability test remained RED in two expected ways: ULL-off generic fallback returned no scheduler profile, and NVIDIA's first profile lacked the required standard/vendor/scheduler combination.

### GREEN and regression evidence

- Targeted command (`*LowLatency*` plus `MediaCodecDecoderRecoveryProfileTest`): 55 tests, 0 failures, 0 errors; exit 0.
- Java compile (`:app:compileNonRoot_gameDebugJavaWithJavac --console=plain`): exit 0.
- Full suite: 161 tests, the same frozen 5 failures, 0 errors. The failure identities remain exactly:
  - `LayoutInflationTest.allLayoutsInflateSuccessfully`
  - `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
  - `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
  - `SimpleStartupTest.testApplicationOnCreate`
  - `StartupTest.testApplicationStartup`
- `git diff --check`: clean.
- Review diff introduces no infinite loop. Renderer review diff contains no render target, Surface/frame-rate/pacing, input/mouse, Presentation, or display change.

### Fix self-review

- Table-driven tests assert exact deterministic profile names, exact ordered key/value maps, and finite null exhaustion for NVIDIA, Kirin, and Exynos across advertised/unadvertised standard support and operating-rate/priority scheduler choices.
- ULL-off coverage proves no vendor options are returned. FEATURE-unsupported OTHER coverage proves scheduler-only fallback remains available.
- The public initialize entry-point mapping test ties `false` to the initial renderer-owned base `-5` contract and `true` to runtime-recovery base rethrow, rather than testing failure disposition in isolation.
- `.codex`, `AGENTS.md`, and this ignored report were not staged.

### Fix commit

- `4d37bb1e` — `Preserve bounded legacy codec fallbacks`
- The fix commit contains only the same two Task 4 production files and two Task 4 test files.
