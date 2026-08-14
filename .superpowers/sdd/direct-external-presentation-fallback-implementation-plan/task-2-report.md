# Task 2 integration report

## Starting state and integration

- Workspace: `C:\Users\BgKing\dev\android\moonlight-android`
- Starting branch and SHA: `fix-codec-low-latency-compatibility` at `132eea33f510ee94fe5c9bb3a200d152233aff5b`.
- The tracked worktree and index were clean before integration. Local-only untracked `.codex/` was preserved.
- Reviewed source commits applied with `git cherry-pick --no-commit`, in this order:
  1. `54551a36036c307482476db0d67ce0da222e1cf6`
  2. `9ec2c68de773be4da8717334481977f3351f2f70`
  3. `3ff2f22dccd77bdabd20728fdfd9ee540f4a9873`
- All three applied without conflicts. `Game.java` was auto-merged without a conflict on each applicable commit.
- The staged scope contains only the seven allowed files: three production external-display files and four external-display test files. No low-latency, MTK, QTI, decoder, preference, manifest, native, or build-identity files are staged.

## Verification

- Focused external-display tests:
  `./gradlew.bat :app:testNonRoot_gameDebugUnitTest --tests com.limelight.ServerHelperStartIntentTest --tests com.limelight.ExternalDisplayControlActivityLaunchTest --tests com.limelight.ExternalDisplayControlActivityLifecycleTest --tests com.limelight.GameExternalDisplayLaunchResolverTest --tests com.limelight.utils.ExternalDisplayDirectLaunchTest --tests com.limelight.PresentationServiceBindingTest --tests com.limelight.PresentationDisplaySelectionTest --tests com.limelight.ExternalPresentationLifecycleTest --tests com.limelight.PresentationInputMappingTest --tests com.limelight.PresentationControllerFocusForwardingTest --tests com.limelight.ExternalDisplayControllerKeyForwardingTest --tests com.limelight.utils.GamePresentationDisplayModeTest --console=plain`
  - Passed: 55 tests, 0 failures, 0 errors, 0 skipped.
- Focused low-latency tests:
  `./gradlew.bat :app:testNonRoot_gameDebugUnitTest --tests com.limelight.binding.video.LowLatencyProfilePlannerTest --tests com.limelight.binding.video.MediaCodecLowLatencyCapabilityTest --tests com.limelight.binding.video.MediaCodecDecoderRecoveryProfileTest --tests com.limelight.binding.video.DecoderLatencySamplerTest --tests com.limelight.profiles.ProfilesOverlayTest --console=plain`
  - Passed: 89 tests, 0 failures, 0 errors, 0 skipped.
- Java compilation:
  `./gradlew.bat :app:compileNonRoot_gameDebugJavaWithJavac --console=plain`
  - Passed.
- Full JVM suite:
  `./gradlew.bat :app:testNonRoot_gameDebugUnitTest --console=plain`
  - 205 tests, 5 failures, 0 errors, 0 skipped. The failures exactly match the frozen baseline:
    - `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
    - `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
    - `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
    - `com.limelight.SimpleStartupTest.testApplicationOnCreate`
    - `com.limelight.StartupTest.testApplicationStartup`
- Native build:
  `./gradlew.bat :app:externalNativeBuildNonRoot_gameLowLatencyDebug --console=plain`
  - Passed for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` build tasks.
- Low-latency ARM64 assembly:
  `./gradlew.bat :app:assembleNonRoot_gameLowLatencyDebug --console=plain`
  - Passed.

## APK identity

- APK: `app\build\outputs\apk\nonRoot_game\lowLatencyDebug\app-nonRoot_game-arm64-v8a-lowLatencyDebug.apk`
- Package: `com.limelight.artemis.lowlatency.debug`
- Application label: `Artemis Low Latency (Debug)`
- Debuggable: yes.
- Launchable activity: `com.limelight.PcView`.
- Native-code ABI in this split: `arm64-v8a` only.

## Final commit and scope

- Final SHA: `70b1c1f6` — `Try direct external display before Presentation fallback`.
- `git -c core.whitespace=cr-at-eol diff --cached --check` passed before committing.
- The final staged scope before commit was the seven authorized external-display files listed above; the index is empty after commit.
- No ADB, install, launch, push, or PR action was performed.
