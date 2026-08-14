# Task 2 implementer report

## Summary

- Added immutable `DecoderFamily`, `PlanPurpose`, `Capabilities`, and deterministic Qualcomm profile planning to `LowLatencyProfilePlanner`.
- Added a package-private, fake-friendly vendor-parameter provider/session seam and per-normalized-decoder capability cache to `MediaCodecHelper`.
- Routed the active Qualcomm `setDecoderLowLatencyOptions()` path through advertised API 31+ capabilities while retaining API 26-30 core/picture compatibility and returning `false` after planner exhaustion for the renderer-owned base attempt.
- Kept the existing MTK, NVIDIA, Kirin, Exynos, Amlogic, renderer, external-display, input, and mouse paths outside this task's implementation scope.

## RED evidence

- Command: `:app:testNonRoot_gameDebugUnitTest --tests com.limelight.binding.video.LowLatencyProfilePlannerTest --tests com.limelight.binding.video.MediaCodecLowLatencyCapabilityTest`
- Result: failed during test compilation as intended with 79 missing-symbol errors for the new planner constants/model/entrypoint and the vendor-parameter provider, session, snapshot, and store seam.

## GREEN and verification evidence

- Targeted tests: 20 passed, 0 failed.
- Java compile: `:app:compileNonRoot_gameDebugJavaWithJavac --console=plain` succeeded.
- Full suite: 126 tests, 5 failures, 0 errors, 0 skipped.
- The five failures exactly match the frozen baseline:
  - `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
  - `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
  - `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
  - `com.limelight.SimpleStartupTest.testApplicationOnCreate`
  - `com.limelight.StartupTest.testApplicationStartup`
- `git diff --check` passed with transient `core.whitespace=cr-at-eol`, required because the tracked `MediaCodecHelper.java` blob intentionally uses CRLF.
- Only the four Task 2 source/test files were staged. `.codex`, `AGENTS.md`, and ignored SDD files were not staged.

## Files

- `app/src/main/java/com/limelight/binding/video/LowLatencyProfilePlanner.java`
- `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
- `app/src/test/java/com/limelight/binding/video/LowLatencyProfilePlannerTest.java`
- `app/src/test/java/com/limelight/binding/video/MediaCodecLowLatencyCapabilityTest.java`

## Commit

- `a60aa3d700c682a8c8474ee20c982d2880c1ea67 Gate Qualcomm low-latency profiles by codec capabilities`

## Self-review

- API 31+ QTI keys are independently gated; an output-fence profile requires both pair keys.
- Output-fence and software-fence families are never mixed. When both are advertised, software-fence is a separate alternate profile.
- S25 advertised capabilities produce exactly full, no-scheduler, no-fence, and Android-standard option profiles, with no QTI core, software-fence, or `vdec-lowlatency` key.
- API 26-30 Qualcomm uses only legacy core/picture and no fence.
- OMX picture-order is 0; Codec2 picture-order is 1.
- Scheduler options are mutually exclusive.
- Successful empty enumeration is distinguished from failure, successful results are cached, failures are retried, and every opened probe session is released in `finally`.

## Deferred concerns

- Renderer-owned profile names, hard attempt limits, and runtime recovery downgrade remain Task 4 work.
- MTK planning and removal of its current unbounded fallback behavior remain Task 3 work.
- The pre-existing unused `applyExtraVendorOptions()` method still contains legacy Qualcomm setters, but no production or test caller exists; this task changed only the active `setDecoderLowLatencyOptions()` Qualcomm path as directed.
- Hardware configure/render and latency measurements remain Tasks 8 and 9.

## Fix Round 1

### Review findings addressed

- Added a package-private active-helper overload that accepts the fake-capable store, SDK, and already-probed standard/scheduler capability booleans. The public overload still supplies the production store, `Build.VERSION.SDK_INT`, real `FEATURE_LowLatency`, and real max-operating-rate result.
- Added a Robolectric/Mockito active-bridge test using a Qualcomm `MediaCodecInfo` and fake S25 snapshot. Fresh formats prove exact try 0-3 profile key/value sets, try 4 finite exhaustion, ULL-off standard-first behavior, negative/out-of-range format immutability, and non-Qualcomm no-probe rejection.
- Added a latch/barrier concurrent case-variant lookup test proving one open, one enumeration, one release, the same complete immutable snapshot object, and no caller exception.

### RED evidence

- Targeted command failed during test compilation with four overload-resolution errors because the injectable eight-argument `setDecoderLowLatencyOptions()` bridge did not yet exist.
- The first GREEN attempt then exposed a test-lifecycle setup error (`RuntimeEnvironment` application unavailable in `@BeforeClass`); moving initialization to Robolectric's per-test `@Before` lifecycle fixed the test harness without changing production behavior.

### GREEN and verification evidence

- Targeted tests: 24 passed, 0 failed.
- Java compile: `:app:compileNonRoot_gameDebugJavaWithJavac --console=plain` succeeded.
- Full XML suite: 130 tests, 5 failures, 0 errors, 0 skipped.
- Failure identities exactly match the frozen five-test baseline; no new failure or error was introduced.
- Transient `core.whitespace=cr-at-eol` diff check passed for the tracked CRLF helper file.
- Scope audit contains only `MediaCodecHelper.java` and `MediaCodecLowLatencyCapabilityTest.java`; `.codex`, `AGENTS.md`, renderer, MTK, external display, input, and mouse files were not staged.

### Fix commit

- `ee2910ca2a6233fbfe736a162bea47b5a183e54d Test Qualcomm low-latency helper integration`
