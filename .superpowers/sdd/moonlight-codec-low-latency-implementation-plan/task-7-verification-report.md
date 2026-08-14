# Task 7 final verification report

Date: 2026-08-10 (Asia/Seoul)

Verdict: **APPROVED / PASS** at `6ff9b36d3c508f58351b4dfa78bb1c8bcb62ead8`.

Every Task 7 gate meets its stated acceptance boundary. Fresh lint retains only the frozen baseline findings, and the fresh full unit suite retains exactly the frozen five failures. No new blocker was found.

No source file was edited, staged, or committed during this verification. No checkout, ADB, install, or launch action was performed. The only edited file is this ignored verification report; Gradle generated ignored build outputs/reports.

## Pin, ancestry, and scope

- Branch: `fix-codec-low-latency-compatibility`
- HEAD: `6ff9b36d3c508f58351b4dfa78bb1c8bcb62ead8`
- Baseline: `877c5c22a11ee783fc2b33bc4c151587ee36cfc6`
- Previous Task 7 verification HEAD: `a126f0f3223bc3e6977af6fb86e1f3c1343a8da3`
- `git merge-base --is-ancestor 877c5c22... HEAD`: exit 0.
- `git merge-base --is-ancestor a126f0f3... HEAD`: exit 0.
- Tip metadata is exact:
  - Parent: `a126f0f3223bc3e6977af6fb86e1f3c1343a8da3`
  - Subject: `Annotate API 31 vendor parameter probe`
  - Tracked scope: only `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
  - CR-at-EOL/ignore-EOL semantic change: one method-local `@androidx.annotation.RequiresApi(api = Build.VERSION_CODES.S)` annotation on the existing API-31 vendor probe.
- The cumulative range is a linear eleven-commit chain. The original ten ledger commits remain unchanged and the lint-fix commit is the eleventh direct child.
- Cumulative file allowlist: 16 actual, 16 authorized, zero unexpected, zero missing.
- Protected audit: 54 external-display/Presentation/input/mouse/controller candidate files, zero changed.
- Renderer protected changed-line audit: zero Surface/Presentation/display-ownership hits and zero `doFrame()`, `releaseWithPolicy()`, frame-pacing/drop-threshold/EWMA/latest-only/balanced-policy declaration hits.
- `Game.java` still only removes the dead ultra-low-latency transport setter call; `StreamConfiguration.java` still only removes the dead field/setter/getter/default.

## Static source guards

| Check | Fresh result |
|---|---|
| Unbounded `for (int tryNumber = 0;;` | `rg` exit 1, pass/no match |
| MTK `nvop.skip|skip.mode|drop.nonref.frame|frame-drop.policy` keys | `rg` exit 1, pass/no match |
| MTK ultra-low-latency written as zero | `rg` exit 1, pass/no match |
| `vdec-lowlatency` source search | Exit 0, exactly two hits: constant plus existing Fire TV comment |

- `vdec-lowlatency` insertion remains gated by `isLegacyVdecLowLatencyAllowed()`: Qualcomm is rejected; only MediaTek, Amlogic, or Amazon are eligible, with the existing Xiaomi/API <=23 exclusion.
- Output dequeue structure: exactly one raw `videoDecoder.dequeueOutputBuffer(...)`, located inside the latency wrapper; exactly six wrapper occurrences (one definition plus five production calls); zero delayed `updateDecodeLatencyStats(...)` calls.
- Exact metric: `decoder_input_queue_call_start_to_first_output_dequeue_us`.
- Exact production prefixes each occur once: `CodecLLCapability`, `CodecLLAttempt`, `CodecLLFallback`, `CodecLLSelected`, `CodecLatencySummary`.
- Capability output uses `advertisedLowLatencyParameters`; attempt/failure output uses `requestedOptions`. Misleading requested-as-supported searches return exit 1/no match.
- `DecoderLatencyState`: zero `videoDecoder` references and zero `MediaCodec` method calls. Its only `MediaCodec` references are the CSD flag constant. Queue and codec-invalidation calls remain outside the latency metadata lock.
- Configuration attempts remain bounded to eleven option profiles plus immediate base.

## Fresh command ledger

All Gradle commands used:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
```

| Command | Exit | Fresh evidence |
|---|---:|---|
| `:app:testNonRoot_gameDebugUnitTest --tests 'com.limelight.binding.video.*LowLatency*' --tests com.limelight.binding.video.DecoderLatencySamplerTest --tests com.limelight.profiles.ProfilesOverlayTest --console=plain --rerun-tasks` | 0 | XML: 4 suites, 75 tests, 0 failures/errors/skips (31 planner, 14 capability, 25 sampler, 5 overlay). |
| `:app:compileNonRoot_gameDebugJavaWithJavac --console=plain --rerun-tasks` | 0 | `BUILD SUCCESSFUL`; 18 tasks executed. |
| `:app:lintNonRoot_gameDebug --console=plain --rerun-tasks` | 1, accepted frozen baseline | 32 errors, 477 warnings; zero range-new blocker and zero lint-fix warning location. |
| `:app:externalNativeBuildNonRoot_gameDebug --console=plain --rerun-tasks` | 0 | All four ABIs configured/built; `BUILD SUCCESSFUL`. |
| `:app:assembleNonRoot_gameDebug --console=plain --rerun-tasks` | 0 | `BUILD SUCCESSFUL`; 44 tasks executed. |
| `:app:testNonRoot_gameDebugUnitTest --console=plain --rerun-tasks` | 1, accepted frozen baseline | XML: 28 suites, 193 tests, exactly five frozen failures, 0 errors, 0 skipped. |
| Required working-tree `git diff --check` | 0 | Clean. |
| Required `git diff --cached --check` | 0 | Clean; index empty. |
| Final `git status --short` | 0 | Only intentional `?? .codex/`. |

Resource processing continues to emit the existing non-positional substitution and missing-default localized-resource warnings. They do not block compile, native build, or APK assembly.

## Lint XML and blame classification

Fresh reports:

- `app/build/reports/lint-results-nonRoot_gameDebug.xml`
- `app/build/reports/lint-results-nonRoot_gameDebug.html`
- `app/build/reports/lint-results-nonRoot_gameDebug.txt`

Acceptance result:

- Blocking issue nodes/locations: 32.
- All 32 blocking locations blame to commits at or before baseline `877c5c22`.
- Range-new blocking locations: 0.
- Blocking breakdown: 18 `NewApi`, 12 fatal `ExtraTranslation`, one `MissingSuperCall`, one `StringFormatInvalid`.
- Warnings: exactly 477, the same frozen count as pre-fix.
- Warning primary-location classification against pre-fix `a126f0f3`: 463 frozen source locations plus 14 issues with no source line; tip-new/touched warning locations: 0.
- `MediaCodecHelper` `NewApi`: 0.
- `MediaCodecHelper` `UseRequiresApi`: 0.
- Other pre-existing `UseRequiresApi` warnings elsewhere in the repository remain part of the frozen warning set.

The lint task's nonzero exit is therefore solely the accepted frozen project baseline, not a new Task 1-7 defect.

## Full unit-suite acceptance

Fresh XML totals: 28 suites, 193 tests, 5 failures, 0 errors, 0 skipped.

The failure identities match the frozen set exactly:

1. `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
2. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
3. `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
4. `com.limelight.SimpleStartupTest.testApplicationOnCreate`
5. `com.limelight.StartupTest.testApplicationStartup`

Unexpected failures: 0. Missing frozen identities: 0.

## Fresh APK/native-library audit

`output-metadata.json` declares exactly four `ONE_OF_MANY` ABI splits and no universal APK. Each split contains only its declared ABI directory and five native libraries: `libc++_shared.so`, `libmoonlight-core.so`, `libopencv_java4.so`, `libtensorflowlite_gpu_jni.so`, and `libtensorflowlite_jni.so`.

| APK | Size (bytes) | SHA-256 | Native ABI |
|---|---:|---|---|
| `app-nonRoot_game-arm64-v8a-debug.apk` | 41,436,847 | `b9b6bea628887456044fa7252020e5132719c558e2ff10a86243ffe9dda28ad0` | `arm64-v8a` |
| `app-nonRoot_game-armeabi-v7a-debug.apk` | 38,941,310 | `3eef7d2d6de94cf51eeac2a6d2f80c189f4b4d1d2d0cd5d1c7aa09d0b6cc0ece` | `armeabi-v7a` |
| `app-nonRoot_game-x86-debug.apk` | 51,106,412 | `e62331a35eb507b6645745ae3a68c7be7fcea41e3fc88109c9407a02e069b38b` | `x86` |
| `app-nonRoot_game-x86_64-debug.apk` | 57,711,016 | `97cfad56db72cd6f4847781a0c8ba257894fba1cca68e7422dedf865eb1ec499` | `x86_64` |

The required ARM64 target exists at `app/build/outputs/apk/nonRoot_game/debug/app-nonRoot_game-arm64-v8a-debug.apk`. It was not installed.

## Diff, EOL, index, and tracking audit

- Required working-tree `git diff --check`: exit 0.
- Required cached `git diff --cached --check`: exit 0.
- Tip-only plain and CR-at-EOL diff checks (`a126f0f3..HEAD`): both exit 0.
- Plain cumulative `git diff --check 877c5c22..HEAD`: exit 2 with 84 diagnostics, all on added CRLF lines in committed `MediaCodecHelper.java`.
- CRLF-aware cumulative `git -c core.whitespace=cr-at-eol diff --check 877c5c22..HEAD`: exit 0.
- `git ls-files --eol MediaCodecHelper.java`: `i/mixed w/mixed`; no normalization was performed.
- A full-file scan finds 26 actual trailing space/tab lines among cumulative files, but blame proves all 26 are at-or-before baseline; range-new actual trailing space/tab lines: 0. The CR-at-EOL cumulative pass independently confirms no added semantic whitespace error.
- Final branch/HEAD remain pinned; index is empty; visible status is only `?? .codex/`.
- `AGENTS.md`, `.superpowers/`, repo-local `docs/`, and `app/build/` are not tracked or staged. Positive ignore evidence exists for `AGENTS.md`, this report, and build reports.

## Final result

Task 7 is complete and **APPROVED / PASS** at `6ff9b36d3c508f58351b4dfa78bb1c8bcb62ead8`. The verification created no commit and performed no device action.
