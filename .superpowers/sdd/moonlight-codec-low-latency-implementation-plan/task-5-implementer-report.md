# Task 5 implementer report

## Identity

- Branch: `fix-codec-low-latency-compatibility`
- Base: `4d37bb1e179ceeb3307c4ed5bb38907f9b05bf71`
- Commit: `945a70f59657ef4dea5f9b57bea05b666b004cec`
- Commit message: `Clarify experimental vendor low-latency setting`

## Implemented contract

- Preserved preference key `checkbox_ultra_low_latency` and XML/default runtime value `false`.
- Updated English, French, Russian, Simplified Chinese, and Traditional Chinese title/summary resources to distinguish automatic Android standard low latency from the optional capability-checked vendor decoder optimizations.
- Removed only the dead `StreamConfiguration.enableUltraLowLatency` field, builder setter, constructor initialization, getter, and the corresponding `Game` builder call.
- Kept `PreferenceConfiguration.enableUltraLowLatency` unchanged as the live runtime source read directly by `MediaCodecDecoderRenderer`.
- Added three focused `ProfilesOverlayTest` cases for key/default compatibility, localized semantic resources, and absence of duplicate stream transport state; retained and reused the existing profile overlay/persistence checks.

## TDD evidence

### RED

Command:

`./gradlew.bat :app:testNonRoot_gameDebugUnitTest --tests 'com.limelight.profiles.ProfilesOverlayTest' --console=plain`

Result before production edits: 5 tests completed, 2 failed.

- `ultraLowLatencyPreference_explainsAutomaticStandardAndOptionalVendorPaths`: expected the new vendor-optimization title but resolved the stale `Ultra Low Latency (experimental)` resource.
- `ultraLowLatencyPreference_isNotDuplicatedInStreamTransportConfiguration`: found the stale private `StreamConfiguration.enableUltraLowLatency` field.
- The key/default and both existing overlay/persistence tests passed during RED.

### GREEN

Final focused command:

`./gradlew.bat :app:processNonRoot_gameDebugResources :app:compileNonRoot_gameDebugJavaWithJavac :app:testNonRoot_gameDebugUnitTest --tests 'com.limelight.profiles.ProfilesOverlayTest' --console=plain`

Result: `BUILD SUCCESSFUL`; all 5 focused tests passed. Resource processing and Java compilation completed successfully in the same Gradle invocation.

## Full-suite comparison

Baseline at the required base: 161 tests, 5 failures, 0 errors, 0 skipped.

Final tree: 164 tests, 5 failures, 0 errors, 0 skipped. The +3 total is exactly the three Task 5 tests added.

Failure identities were unchanged:

1. `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
2. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
3. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
4. `com.limelight.SimpleStartupTest.testApplicationOnCreate`
5. `com.limelight.StartupTest.testApplicationStartup`

No new failure or error was introduced.

## Scope and audits

- `preferences.xml` is byte-for-byte unchanged from the base; its preference block still has `android:key="checkbox_ultra_low_latency"` and `android:defaultValue="false"`.
- Final production-reference audit finds the key/default/value only in `PreferenceConfiguration` and the direct renderer read at `MediaCodecDecoderRenderer`; no ultra-low-latency reference remains in `Game` or `com.limelight.nvstream`.
- `git diff --cached --check` passed before commit.
- Commit contains exactly 8 authorized files: `Game.java`, `StreamConfiguration.java`, five locale/default string resources, and `ProfilesOverlayTest.java`.
- No JNI/native/network/HTTP ABI, Fully External Display/Presentation, input, frame-pacing, `AGENTS.md`, `.codex/`, `.superpowers/`, docs, or build artifact was committed.

## Caveats

- The full Gradle task exits nonzero solely because of the accepted frozen five-test baseline above.
- Existing Gradle/resource/deprecation warnings remain; the focused resource/compile/test command succeeds and no warning was introduced as a Task 5 contract failure.
