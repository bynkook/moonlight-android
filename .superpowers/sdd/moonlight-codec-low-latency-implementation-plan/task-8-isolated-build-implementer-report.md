# Task 8 isolated build implementer report

## Scope and commit

- Branch: `fix-codec-low-latency-compatibility`
- Required base/pre-commit HEAD: `6ff9b36d3c508f58351b4dfa78bb1c8bcb62ead8`
- Commit: `132eea33f510ee94fe5c9bb3a200d152233aff5b`
- Subject: `Add isolated low-latency debug build`
- Commit contents: only `app/build.gradle` (`10 insertions`)
- Implemented one `lowLatencyDebug` build type immediately after `debug`; no existing build type or product flavor was edited.

## RED / GREEN

RED before editing:

- `:app:assembleNonRoot_gameLowLatencyDebug --console=plain` exited 1.
- Exact cause: `task 'assembleNonRoot_gameLowLatencyDebug' not found in project ':app'`.
- There was no Java, Android SDK, or native-build failure in the RED run.

GREEN model and builds:

- Gradle exposed these exact tasks:
  - `assembleNonRoot_gameLowLatencyDebug`
  - `compileNonRoot_gameLowLatencyDebugJavaWithJavac`
  - `externalNativeBuildNonRoot_gameLowLatencyDebug`
- Fresh `compileNonRoot_gameLowLatencyDebugJavaWithJavac --rerun-tasks`: success, 18 tasks executed.
- Fresh `externalNativeBuildNonRoot_gameLowLatencyDebug --rerun-tasks`: success, 9 tasks executed.
- Fresh existing `assembleNonRoot_gameDebug --rerun-tasks`: success, 44 tasks executed.
- Fresh new `assembleNonRoot_gameLowLatencyDebug --rerun-tasks`: success, 44 tasks executed.

The inherited debuggable/minify and overridden `resValue` configuration warnings are Gradle configuration warnings, not build failures. The same pre-existing resource formatting/default-value warnings also appeared in the existing debug assemble.

## APK outputs

Output directory: `app/build/outputs/apk/nonRoot_game/lowLatencyDebug/`

- `app-nonRoot_game-arm64-v8a-lowLatencyDebug.apk` — 41,436,944 bytes
- `app-nonRoot_game-armeabi-v7a-lowLatencyDebug.apk` — 38,941,404 bytes
- `app-nonRoot_game-x86-lowLatencyDebug.apk` — 51,106,513 bytes
- `app-nonRoot_game-x86_64-lowLatencyDebug.apk` — 57,711,116 bytes

Exact ARM64 path:

`C:\Users\BgKing\dev\android\moonlight-android\app\build\outputs\apk\nonRoot_game\lowLatencyDebug\app-nonRoot_game-arm64-v8a-lowLatencyDebug.apk`

ARM64 SHA-256:

`48D2CFE363810D78A2DC0175E8C86D6168E437DB3D6D5077CA6538CFCBDF6C2B`

## Unit test baseline

Fresh command: `:app:testNonRoot_gameLowLatencyDebugUnitTest --rerun-tasks`

- Gradle exit: 1 because the frozen failures remain.
- XML directory: `app/build/test-results/testNonRoot_gameLowLatencyDebugUnitTest/`
- XML suites: 28
- Tests: 193
- Failures: 5
- Errors: 0
- Skipped: 0

The five failure identities exactly match the frozen baseline:

1. `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
2. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
3. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
4. `com.limelight.SimpleStartupTest.testApplicationOnCreate`
5. `com.limelight.StartupTest.testApplicationStartup`

No new test failure identity was introduced.

## Lint baseline comparison

Fresh command: `:app:lintNonRoot_gameLowLatencyDebug --rerun-tasks`

- Gradle exit: 1 because the frozen lint errors remain.
- Task 7 baseline XML: `app/build/reports/lint-results-nonRoot_gameDebug.xml`
- Baseline XML SHA-256: `8C9FA6B984E48CCC36FBCDCDFF6908694AF1C8B7AE9C6801B701D4813C770476`
- Baseline: 32 errors total (`20 Error` + `12 Fatal`) and 477 warnings.
- New variant: 32 errors total and 473 warnings.
- Error identity/location diff: 0.
- New normalized source/config identity/location occurrences: 0.
- Lint locations on the inserted `app/build.gradle` lines 124-133: 0.

The four-warning reduction is fully classified and contains no new actionable item:

- `UnknownIssueId` / `MissingThemeAttr` at existing `app/lint.xml:6`: minus 2 duplicate occurrences.
- `UnusedResources` / existing `R.string.app_label_game` at `app/build.gradle` with no line: minus 1 duplicate occurrence.
- `UnusedResources` / existing `R.string.app_label_root` at `app/build.gradle` with no line: minus 1 duplicate occurrence.

All other severity/id/message counts match the Task 7 report. In particular, the new build type added no error, warning fingerprint, or new source/config location.

## ARM64 APK identity and signing

Discovered tools:

- `C:\Users\BgKing\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\apkanalyzer.bat`
- `C:\Users\BgKing\AppData\Local\Android\Sdk\build-tools\37.0.0\aapt2.exe`
- `C:\Users\BgKing\AppData\Local\Android\Sdk\build-tools\37.0.0\apksigner.bat`

Verified before commit:

- Application ID: `com.limelight.artemis.lowlatency.debug`
- Application label: `Artemis Low Latency (Debug)`
- Debuggable: `true`
- Launcher: `com.limelight.PcView`
- APK native-code declaration: only `arm64-v8a`
- APK `lib/` ABI directories: only `arm64-v8a`
- `apksigner verify`: v1 true, v2 true, one signer, exit 0
- Certificate DN: `C=US, O=Android, CN=Android Debug`
- APK certificate SHA-256: `5C8C3AE408E30A0C4FB26E3B0CC6C4EB44AAB418BAA5863A568EF482A282EE14`
- Default Android debug keystore certificate SHA-256: same value

`apksigner` emitted standard v1/JAR warnings for unprotected `META-INF` entries; APK Signature Scheme v2 verification succeeded and the certificate matched the local Android debug keystore.

## Scope checks and boundaries

- Pre-commit `git diff --check`: exit 0.
- Pre-commit `git diff --cached --check`: exit 0.
- Staged/committed path: only `app/build.gradle`.
- Before editing, visible untracked state contained only the three existing `.codex/` plan/design documents; they were not staged or committed.
- `AGENTS.md` is ignored and was not staged or committed.
- No source, resource, manifest, native, codec, preference, external-display, input/mouse, or frame-pacing file was changed.
- Build artifacts were not staged or committed.
- This report is ignored by `.superpowers/sdd/.gitignore` and is not part of the commit.

Per the review gate, no ADB command, device install, package launch, Task 2, or Task 9 was performed. Apollo pairing, PIN approval, streaming, and real-device latency behavior were not tested.
