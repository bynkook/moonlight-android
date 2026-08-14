# Task 7 Lint Fix Report

## Summary

- Branch: `fix-codec-low-latency-compatibility`
- Starting HEAD: `a126f0f3223bc3e6977af6fb86e1f3c1343a8da3`
- Fix commit: `6ff9b36d` (`Annotate API 31 vendor parameter probe`)
- Tracked scope: only `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
- Change: add a method-local `@androidx.annotation.RequiresApi(api = Build.VERSION_CODES.S)` contract to the production vendor-parameter enumeration method. Runtime guards, cache behavior, provider lifecycle, and capability semantics are unchanged.

## RED Evidence and Root Cause

Fresh pre-fix lint:

- `:app:lintNonRoot_gameDebug --console=plain --rerun-tasks`
- Result: 33 errors, 477 warnings.
- Sole range-introduced blocking issue: `NewApi` at `MediaCodecHelper.java:171`, on `MediaCodec.getSupportedVendorParameters()`.
- The provider is opened only after `VendorParameterCapabilityStore.getSnapshot()` returns early below API 31, but lint cannot propagate that injected `sdkInt` guard into the anonymous `VendorParameterSession` method.

The first local `@TargetApi(S)` candidate removed the blocking `NewApi` issue but introduced exactly one new warning:

- Identity/location: `UseRequiresApi`, `MediaCodecHelper.java:171`.
- Classification: introduced solely by the uncommitted `@TargetApi` candidate; warning total changed from 477 to 478.
- Resolution: replaced the candidate with the narrower AndroidX `@RequiresApi(S)` contract. The `UseRequiresApi` warning is absent from the committed result.

## GREEN Verification

### Focused tests and Java compile

Final command included both the explicit Java compile task and the two focused suites with `--rerun-tasks`:

```text
:app:compileNonRoot_gameDebugJavaWithJavac
:app:testNonRoot_gameDebugUnitTest
  --tests com.limelight.binding.video.MediaCodecLowLatencyCapabilityTest
  --tests com.limelight.binding.video.LowLatencyProfilePlannerTest
```

- Gradle result: `BUILD SUCCESSFUL`; compile task executed.
- Fresh XML: 45 tests, 0 failures, 0 errors, 0 skipped.
  - `LowLatencyProfilePlannerTest`: 31/0/0/0.
  - `MediaCodecLowLatencyCapabilityTest`: 14/0/0/0.

### Lint

Fresh final `:app:lintNonRoot_gameDebug --console=plain --rerun-tasks`:

- 32 blocking errors and 477 warnings.
- `MediaCodecHelper` `NewApi`: 0.
- `MediaCodecHelper` `UseRequiresApi`: 0.
- Git blame plus `merge-base --is-ancestor` against baseline `877c5c22a11ee783fc2b33bc4c151587ee36cfc6`: all 32 blocking locations are frozen at/before baseline; range-introduced blockers: 0.
- The lint Gradle task retains exit 1 only because the 32 frozen blocking issues remain.

### Full unit suite

Fresh `:app:testNonRoot_gameDebugUnitTest --console=plain --rerun-tasks` XML:

- 28 suites, 193 tests, 5 failures, 0 errors, 0 skipped.
- The five failures exactly match the frozen set:
  1. `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
  2. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
  3. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
  4. `com.limelight.SimpleStartupTest.testApplicationOnCreate`
  5. `com.limelight.StartupTest.testApplicationStartup`

## Scope, Whitespace, and EOL Audit

- `git diff --check`: exit 0 before commit.
- `git -c core.whitespace=cr-at-eol diff --check`: exit 0 before commit.
- Staged file list before commit contained only `MediaCodecHelper.java`.
- `git ls-files --eol` remains `i/mixed w/mixed`; no file-wide EOL normalization was performed.
- CR-at-EOL/ignore-EOL semantic diff is exactly one added annotation at the API-31-only boundary.
- `AGENTS.md` and this report are ignored; `.codex/` remains untracked and unstaged.

## Deferred / Not Performed

- No runtime capability/cache/probe behavior was changed.
- No renderer, MTK, external-display, input, or mouse code was changed.
- No Task 8 work, ADB command, APK build/install, or device verification was performed.
