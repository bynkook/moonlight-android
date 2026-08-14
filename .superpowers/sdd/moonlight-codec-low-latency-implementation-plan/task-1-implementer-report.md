# Task 1 Implementer Report

## Summary

- Added a package-private pure Java `LowLatencyProfilePlanner` shell containing an immutable `Profile` model.
- Preserved integer option insertion order with a defensive `LinkedHashMap` copy and an unmodifiable view.
- Defined profile equality and hash codes from the ordered key/value entry sequence, excluding the profile name.
- Added deterministic first-wins deduplication with `LinkedHashSet`.
- Did not implement QTI, MTK, Android capability, renderer, or `MediaCodec` behavior; those remain assigned to later tasks.

## RED Evidence

Command:

```powershell
.\gradlew.bat :app:testNonRoot_gameDebugUnitTest `
  --tests com.limelight.binding.video.LowLatencyProfilePlannerTest `
  --console=plain
```

Result: exit code 1 at `:app:compileNonRoot_gameDebugUnitTestJavaWithJavac`, before any production file existed.

Expected failure evidence:

- Nine compiler errors reported `package LowLatencyProfilePlanner does not exist`.
- The deduplication call also reported `cannot find symbol: variable LowLatencyProfilePlanner`.
- This proved both tests failed because the requested production class/API was absent rather than because of a test typo or unrelated runtime failure.

## GREEN Evidence

Targeted test command: same command as RED.

Result: exit code 0, `BUILD SUCCESSFUL`.

- `profileOptionsAreOrderedAndImmutable PASSED`
- `duplicateOrderedOptionsAreRemoved PASSED`

Compile command:

```powershell
.\gradlew.bat :app:compileNonRoot_gameDebugJavaWithJavac --console=plain
```

Result: exit code 0, `BUILD SUCCESSFUL`.

Full-suite command:

```powershell
.\gradlew.bat :app:testNonRoot_gameDebugUnitTest --console=plain
```

Gradle result: exit code 1 because the documented baseline failures remain. XML aggregation from `app/build/test-results/testNonRoot_gameDebugUnitTest/TEST-*.xml` reported:

- XML suites: 25
- Tests: 108
- Failures: 5
- Errors: 0
- Skipped: 0
- Task 1 tests: 2 tests, 0 failures, 0 errors, 0 skipped

The accepted total-count correction is baseline 106 plus the two new Task 1 tests, yielding 108. The failure set is unchanged and contains no new failure:

1. `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
2. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
3. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
4. `com.limelight.SimpleStartupTest.testApplicationOnCreate`
5. `com.limelight.StartupTest.testApplicationStartup`

`git diff --cached --check` also completed with exit code 0 before commit.

## Files

- `app/src/main/java/com/limelight/binding/video/LowLatencyProfilePlanner.java`
- `app/src/test/java/com/limelight/binding/video/LowLatencyProfilePlannerTest.java`

No `MediaCodecHelper`, renderer, external-display, input, mouse, `AGENTS.md`, `.codex`, or tracked documentation file was changed by Task 1.

## Commit

- `087afa8300fdcb5a4131cfadeddd9af04f853391` — `Add testable codec low-latency profile planner`
- Commit contains exactly the two Task 1 source/test files.
- Post-commit status contains only the pre-existing untracked `.codex/moonlight-codec-low-latency-implementation-plan.md`.

## Self-review

- The defensive copy prevents later mutation of the caller-owned map from changing a profile.
- The public package-local view cannot be mutated.
- Ordered entry-list equality distinguishes reversed option order even though ordinary `Map.equals()` would not.
- Deduplication preserves the first profile and its name while removing later identical ordered option sequences.
- Implementation is deliberately limited to the model/deduplication contract required by Task 1.

## Concerns and deferred work

- Empty option profiles are not filtered by this Task 1 helper; the later planner implementation must enforce the plan's “no empty option profile” invariant before renderer integration.
- Capability fields, QTI/MTK constants and profile construction, bounds, and runtime recovery are intentionally absent and require their own failing tests in Tasks 2–4.
- The full Gradle suite remains red only because of the five documented pre-existing failures listed above.
