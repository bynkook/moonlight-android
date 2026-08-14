# Direct External Display with Presentation Fallback Implementation Plan

## Global Constraints

- Base implementation commit: `5dfa15aec9f93bd2c9ecf20f70b547c915c5937c` on `pr/samsung-fully-external-display`.
- Work only on `stack/direct-external-presentation-fallback` in the isolated linked worktree.
- Restore direct external-display Activity launch as the primary path on API 30+ when supported and allowed.
- Never restore `createDisplayContext()` or `Intent.FLAG_ACTIVITY_NEW_TASK` for `Game` launch.
- Presentation fallback triggers are limited to unsupported/disallowed direct launch, launch exception, actual-display mismatch, or target-display loss before streaming.
- Do not add a Surface timeout, manufacturer/model allowlist, duplicate `Game` launch, or a second `NvConnection`.
- Preserve the existing Presentation service, controller takeover, input, display-removal, and cleanup implementation unless a narrowly required controller state change is covered by a failing test.
- Do not change MTK/QTI codec planning, JNI, networking, preferences, manifest launch modes, or low-latency code in this stacked branch.
- `AGENTS.md`, `.codex/`, and `.superpowers/` are local-only and must not be staged or committed.
- Use TDD: every behavior change must first fail for the expected reason, then pass after the minimal production change.

## Task 1: Implement direct-first external display launch with in-place Presentation fallback

### Launch contract

- Update `ServerHelper.createStartIntent()` so the wrapped `Game` intent is created from the parent Activity context, carries `Game.EXTRA_DISPLAY_ID` equal to the selected secondary display ID, and initially has no `Game.EXTRA_PRESENTATION_DISPLAY_ID`.
- Keep the wrapper `ExternalDisplayControlActivity` and preserve all common stream extras.
- Keep `FLAG_ACTIVITY_NEW_TASK` absent.
- In `ExternalDisplayControlActivity`, attempt direct placement only when all are true:
  - SDK is API 30 or newer.
  - `PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS` is present.
  - the secondary display still exists.
  - `ActivityManager.isActivityStartAllowedOnDisplay(this, displayId, gameIntent)` returns true.
- Direct placement must use `ActivityOptions.makeBasic().setLaunchDisplayId(displayId)`.
- If eligibility fails or direct `startActivity()` throws a runtime exception, launch one cloned fallback intent on the default display with `EXTRA_PRESENTATION_DISPLAY_ID` set to the secondary display ID.
- Do not mutate the wrapped source intent in a way that can leak stale launch-mode extras into `onNewIntent()` reuse.

### Actual placement and in-place fallback

- In `Game.onCreate()`, distinguish requested display from the Activity's actual display.
- On API 30+, use the Activity context's `getDisplay()` as the actual placement source of truth; direct launch is not attempted below API 30.
- Preserve explicit `EXTRA_PRESENTATION_DISPLAY_ID` behavior.
- Add a small package-private pure resolver with literal-testable results:
  - requested secondary ID equals actual ID and target exists: direct external mode.
  - requested secondary ID differs from actual ID and target exists: Presentation fallback.
  - requested target no longer exists: abort before connection.
- For mismatch fallback, keep the same `Game` instance, set the existing Presentation fields, hide the Activity stream surface, and bind the existing `StreamPresentationService` before any stream connection starts.
- Do not finish and relaunch `Game`.

### Controller lifecycle and diagnostics

- When `initViews()` observes a direct-mode `Game`, set the Presentation state to `IDLE` and restore the existing direct-mode focus request to `Game`.
- When it observes Presentation mode, preserve the current `LAUNCHING_GAME -> WAITING_FOR_CONTROLLER_FOCUS -> CONTROLLER_ACTIVE` flow.
- Add deterministic logs containing requested display ID, actual display ID, selected path, and fallback reason (`not-supported`, `not-allowed`, `launch-exception`, `display-mismatch`, or `target-unavailable`).
- No persistent log file may be added.

### Required tests and verification

- Update `ServerHelperStartIntentTest` for requested-secondary direct intent, absent Presentation extra, preserved common extras, and absent NEW_TASK.
- Update/add `ExternalDisplayControlActivityLaunchTest` coverage for ActivityOptions target ID, direct eligibility, disallowed/unsupported fallback, exception fallback, source-intent isolation, and exactly one successful launch.
- Add pure Game resolver tests for direct, mismatch fallback, target-unavailable abort, and explicit Presentation precedence.
- Add controller lifecycle tests proving direct mode returns to IDLE without Presentation cleanup and Presentation mode retains the existing takeover states.
- Re-run all existing Presentation display, service binding, input, focus, lifecycle, and removal tests.
- Run Java compile, focused external-display tests, full `testNonRoot_gameDebugUnitTest`, and `assembleNonRoot_gameDebug`.
- Full-suite acceptance: only the frozen five baseline failures may remain; no new failure/error/skip identity is allowed.
- Commit only tracked production/test changes with message `Try direct external display before Presentation fallback`.

## Task 2: Integrate and verify the combined low-latency debug build

- Apply only the reviewed Task 1 commit to `fix-codec-low-latency-compatibility` after confirming its worktree has no tracked local changes.
- Resolve conflicts by preserving the combined branch's low-latency code and Task 1's external-display behavior; do not rewrite either feature's unrelated code.
- Run the focused external-display tests plus low-latency planner/capability/recovery tests.
- Run Java compile, full unit suite, native build, and `assembleNonRoot_gameLowLatencyDebug`.
- Verify the ARM64 APK remains a separate debuggable package and does not replace the standard installed application.
- Do not push or open a PR. The new upstream PR is deferred until PR #589 merges.

