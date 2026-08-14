# Task 3 implementer report

## Identity

- Task: MTK selective profiles and finite fallback integration
- Branch: `fix-codec-low-latency-compatibility`
- Base: `ee2910ca2a6233fbfe736a162bea47b5a183e54d`
- Commit: `1d85dc3e` (`Bound and strengthen MediaTek low-latency fallbacks`)

## Scope implemented

- Added initialization-independent, case-insensitive decoder-family classification and scoped legacy `vdec-lowlatency` eligibility.
- Added deterministic MediaTek profiles with exact advertised-key gating, `.value` alias preference, ULL value `1`, scheduler exclusivity, ordered deduplication, and a maximum of seven option profiles.
- Added standard/legacy-only generic Amlogic and Amazon planner handling while excluding Qualcomm and Xiaomi API 23 or lower.
- Routed Qualcomm, MediaTek, and eligible generic legacy decoders through the planner before the old cumulative helper path.
- Removed the active cumulative MTK block, the unscoped `vdec-lowlatency` write, and all four MTK skip/drop writes.
- Preserved the existing non-eligible NVIDIA, Kirin, and Exynos paths; Exynos and NVIDIA are covered by production-path regression assertions.

## TDD evidence

### RED 1

Command:

`.\gradlew.bat :app:testNonRoot_gameDebugUnitTest --tests com.limelight.binding.video.LowLatencyProfilePlannerTest --tests com.limelight.binding.video.MediaCodecLowLatencyCapabilityTest --console=plain`

Observed expected compilation failure with 18 errors because the new pure classification/eligibility seams and manufacturer-injectable helper overload did not yet exist. No production implementation was present for the new tests.

### RED 2

During self-review, `OMX.QTI` was found missing from the `qcom/qti` family contract. Two focused tests were added and observed failing with `expected:<QUALCOMM> but was:<OTHER>` and an erroneous Amazon legacy-eligibility result before the prefix implementation was corrected.

### GREEN

Final targeted command:

`.\gradlew.bat :app:testNonRoot_gameDebugUnitTest --tests com.limelight.binding.video.LowLatencyProfilePlannerTest --tests com.limelight.binding.video.MediaCodecLowLatencyCapabilityTest --console=plain`

Result: `BUILD SUCCESSFUL`; all 38 targeted tests passed.

Coverage includes exact MTK profile names and ordered maps, all documented values, primary/alternate/both alias selection, ULL off/on, API 31 success-empty/failure, API 26/30 no-speculation gates, prefix and manufacturer eligibility, generic legacy behavior, full capability combinations, uniqueness/empty/bound checks, finite helper exhaustion, fresh-format application, and non-eligible Exynos/NVIDIA preservation.

## Compilation and full-suite evidence

- `.\gradlew.bat :app:compileNonRoot_gameDebugJavaWithJavac --console=plain`: `BUILD SUCCESSFUL`.
- `.\gradlew.bat :app:testNonRoot_gameDebugUnitTest --console=plain`: 144 tests, 5 failures, 0 errors.
- The five failures exactly match the frozen baseline identities:
  - `com.limelight.LayoutInflationTest.allLayoutsInflateSuccessfully`
  - `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`
  - `com.limelight.profiles.ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`
  - `com.limelight.SimpleStartupTest.testApplicationOnCreate`
  - `com.limelight.StartupTest.testApplicationStartup`
- No new full-suite failure or error was introduced.

## Static and scope verification

- `git diff --check`: clean (only Git line-ending normalization warnings were printed before staging).
- Active production source contains none of:
  - `vendor.mtk.vdec.nvop.skip`
  - `vendor.mtk.vdec.skip.mode`
  - `vendor.mtk.vdec.drop.nonref.frame`
  - `vendor.mtk.vdec.frame-drop.policy`
- `LowLatencyProfilePlanner` contains neither `media.low-latency.enable` nor `vendor.low-latency.enable`.
- Only the four authorized Task 3 files were committed.
- `.codex`, `AGENTS.md`, renderer, external-display, input/mouse, preferences, native code, and other protected paths were not staged or committed.

## Self-review

- Qualcomm planner ordering and profile construction were not changed.
- API 31+ MTK options are emitted only for normalized, advertised, recognized keys.
- API 26-30 and enumeration unavailable/empty paths emit no speculative `vendor.mtk.*` keys.
- MTK ULL off emits only standard and scoped legacy profiles; ULL on never writes ULL `0`.
- Scheduler profiles choose operating rate or priority, never both.
- Generic legacy profiles are independent of ULL and never contain MTK/QTI keys or scheduler settings.
- The helper applies exactly one profile to each fresh `MediaFormat` and returns false after finite exhaustion.

## Deferred Task 4 boundary

- Renderer-owned empty base format, exact total configure-attempt limit of 12, hard-loop bounding, and runtime recovery ordering remain intentionally deferred to Task 4.
- No renderer or decoder-recovery code was modified in this task.
