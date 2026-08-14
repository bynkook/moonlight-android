APPROVED

# Task 3 review

## Findings

No blocking, major, or minor findings.

## Spec and behavior verification

- `LowLatencyProfilePlanner.java:158-184` performs initialization-independent, case-insensitive family classification. It recognizes `omx.qcom`, `omx.qti`, `c2.qti`, and `c2.qcom` as Qualcomm and `omx.mtk`/`c2.mtk` as MediaTek. Legacy `vdec-lowlatency` eligibility is limited to MTK, Amlogic, or Amazon, excludes Xiaomi API 23 and below, and excludes Qualcomm before manufacturer handling.
- `LowLatencyProfilePlanner.java:238-275` emits the specified MTK risk order. ULL off is `android-standard` then `mtk-legacy`; ULL on is experimental full, standard+legacy+performance, standard+performance, standard, legacy+performance, legacy, then performance-only, with absent or duplicate option sets removed.
- `LowLatencyProfilePlanner.java:357-396` implements the exact twelve-key MTK value matrix. API 31+ support is gated by normalized advertised parameters, `.value` aliases win when both aliases are advertised, and the ULL key can only be inserted with value `1`. API 26-30 and unavailable/success-empty enumeration cannot insert speculative MTK vendor keys.
- `LowLatencyProfilePlanner.java:251-257` only emits `mtk-experimental-full` after at least one recognized advertised MTK key was actually inserted. Scheduler insertion alone cannot create that profile.
- `LowLatencyProfilePlanner.java:277-281` keeps eligible generic OTHER behavior independent of ULL and ordered as Android standard then legacy vdec, without MTK/QTI vendor options or scheduler options.
- `LowLatencyProfilePlanner.java:410-418` makes scheduler choices exclusive: maximum operating rate when the existing safety seam permits it, otherwise priority on API 23+.
- Profile construction remains deterministic, ordered, immutable, de-duplicated by ordered option contents, empty-profile free, and bounded to seven MTK option profiles, below the Task 3 limit of eleven.
- `MediaCodecHelper.java:626-650` routes Qualcomm, MTK, and eligible legacy decoders through the planner before the old cumulative path. `MediaCodecHelper.java:749-802` derives family/legacy eligibility from injectable inputs, avoids vendor enumeration for generic legacy OTHER, applies one planner profile to the supplied fresh format, returns false for negative or exhausted attempts, and preserves the Task 2 capability-store seam.
- The unscoped cumulative `vdec-lowlatency` write and the old MTK cumulative branch were removed. Exact production-source search found none of the four forbidden MTK skip/drop keys. The remaining NVIDIA, Kirin, Exynos, and Xiaomi API<=23 noneligible Amlogic paths continue through the unchanged cumulative control flow; Exynos and NVIDIA also have direct production-path regression assertions.
- Task 4 boundaries are respected: no renderer-owned base attempt, total-12 configure bound, or recovery implementation was added here, and no renderer/protected path was changed.

## Additional Qualcomm-prefix scrutiny

- Keeping `omx.qti` classified as Qualcomm is the safe and requested result: it prevents Amazon/manufacturer fallback from ever applying `vdec-lowlatency` to a QTI decoder. Removing it would violate the absolute Qualcomm exclusion.
- The existing max-operating-rate list remains narrower (`omx.qcom`, `c2.qti`, `c2.qcom`) by design; family recognition does not itself prove that the aggressive operating-rate hint is safe. An `omx.qti` codec therefore receives the priority fallback unless another existing safety list qualifies it.
- The written codec contract assigns picture-order `0` specifically to `OMX.qcom` and `1` to Codec2 QTI/QCOM. The implementation preserves that rule for all named deployed prefixes. `OMX.qti` is classified for safety but has no separately documented device contract; changing its picture-order value speculatively is not warranted without codec/device evidence.

## Test quality and independently verified evidence

- The planner tests use duplicated expected constants/maps rather than reading production structures back into expected values. They cover exact full MTK maps/order, ULL off/on, primary/alternate/both alias fixtures, API 31 success-empty/failure, API 26/30 no-speculation, forbidden generic and skip/drop keys, scheduler exclusivity, capability combinations, uniqueness, non-empty profiles, and the profile bound.
- Integration tests exercise fresh `MediaFormat` instances, exact applied key sets/values, cached provider acquisition/release, finite exhaustion without mutation, generic legacy no-enumeration behavior, and preserved noneligible production paths.
- Independent targeted run: 38/38 tests passed for `LowLatencyProfilePlannerTest` and `MediaCodecLowLatencyCapabilityTest` (`BUILD SUCCESSFUL`).
- Independent Java compile: `:app:compileNonRoot_gameDebugJavaWithJavac` succeeded.
- Independent full-suite XML audit: 144 tests, 5 failures, 0 errors, 0 skipped. The failures exactly match the frozen baseline: `LayoutInflationTest.allLayoutsInflateSuccessfully`, both named `ProfilesNavigationTest` failures, `SimpleStartupTest.testApplicationOnCreate`, and `StartupTest.testApplicationStartup`.
- `git diff --check ee2910ca..1d85dc3e` is clean. Commit `1d85dc3e` changes only the four authorized Task 3 files (814 insertions, 98 deletions); renderer, external-display, input/mouse, preferences, native, and resource paths are absent. `AGENTS.md` remains ignored and the only worktree status entry is the pre-existing untracked `.codex/` plan directory.
