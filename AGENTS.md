# Repository Guidelines

## Local-Only Status

`AGENTS.md` is a local contributor/agent guide for this workspace. It is ignored by `.gitignore` and must not be staged, committed, or included in a PR. Use `git check-ignore -v AGENTS.md` to confirm this before preparing any patch.

## Project Structure & Module Organization

This repository is a single-module Gradle Android app. Java sources are under `app/src/main/java/com/limelight`, Android resources under `app/src/main/res`, native code and vendored native libraries under `app/src/main/jni`, and JVM/Robolectric tests under `app/src/test/java/com/limelight`. Flavor-specific manifests live in `app/src/root`, `app/src/nonRoot_game`, and `app/src/game`.

## Local Environment

This Windows workspace needs explicit environment values when running Gradle from PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
```

The system-level `JAVA_HOME` may point to an invalid path such as `C:\Program Files\Java\jdk-25\bin\server\`; do not rely on it for this repo. Prefer setting the variables inline for each Gradle command instead of writing machine-specific paths to tracked files. `local.properties` is also ignored and may be used locally if needed.

## Build, Test, and Install Commands

Run commands from the repository root. On Windows, use `.\gradlew.bat`.

- `git submodule update --init --recursive` initializes native submodules.
- `.\gradlew.bat :app:compileNonRoot_gameDebugJavaWithJavac --console=plain` checks Java compilation quickly.
- `.\gradlew.bat :app:assembleNonRoot_gameDebug --console=plain` builds debug APKs.
- `.\gradlew.bat :app:testNonRoot_gameDebugUnitTest` runs the default flavor JVM tests.
- `.\gradlew.bat :app:lint` runs Android lint.

For the current debug device flow, install the ARM64 debug APK:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\nonRoot_game\debug\app-nonRoot_game-arm64-v8a-debug.apk
```

The debug package is `com.limelight.noirdebug` and can coexist with the original `com.limelight.noir`.

## Fully External Display Work Log

This workspace carries the original Presentation implementation plus the later direct-first fallback update for the `Fully external` display path. These notes are the current codebase intent, not a general project overview.

### `ba1eb1c8` - `Fix fully external display launch contract`

Why:

- The earlier PoC launched `Game` through a secondary-display `Context` with `FLAG_ACTIVITY_NEW_TASK`. On Samsung external-display flows this made launch/window ownership ambiguous and led to fragile activity/display behavior.
- That Presentation-only contract remains the reliable fallback, but it should not be selected before Android has had a supported direct external-display launch attempt.

What changed:

- `ServerHelper.createStartIntent()` now always builds the `Game` intent from the parent Activity context.
- When `enableFullExDisplay` is true and a secondary display exists, `ServerHelper` puts only the external display id into `Game.EXTRA_DISPLAY_ID`, wraps the `Game` intent in `ExternalDisplayControlActivity.EXTRA_LAUNCH_INTENT`, and returns the controller wrapper intent.
- `ServerHelper.getSecondaryDisplay()` prefers `DisplayManager.DISPLAY_CATEGORY_PRESENTATION` displays and falls back to the first non-default display.
- `ExternalDisplayControlActivity` owns the direct-first decision. On API 30+, it uses `ActivityOptions.setLaunchDisplayId()` only after checking the secondary-activity feature, target-display existence, and `ActivityManager.isActivityStartAllowedOnDisplay()`.
- Unsupported, disallowed, and launch-exception cases start exactly one cloned default-display `Game` intent carrying `EXTRA_PRESENTATION_DISPLAY_ID`.
- `Game` verifies its actual display on API 30+. A real direct placement uses the Activity window; a placement mismatch keeps the same `Game` instance and switches to the existing `Presentation`/service route before streaming; target loss aborts before connecting.
- `Game` writes requested display, actual display, selected path, and the truthful fallback reason to logcat. Controller request-path logs are not confirmation of actual placement.
- `Game` now guards Presentation creation/removal with display-resolution checks, `InvalidDisplayException` handling, Presentation window null handling, and an external-display removal listener.

### `e94a474b` - `Fix fully external controller lifecycle`

Why:

- A later foregrounding attempt that reused `StartExternalDisplayControlReceiver.requestFocusToExternalDisplayControl()` caused unstable relaunch/focus behavior.
- The clean fix was to avoid global focus-path relaunches and make the controller lifecycle state explicit enough for `Game.onStop()` and controller background teardown.

What changed:

- `ExternalDisplayControlActivity` tracks only real foreground state with `controllerForeground`, set in `onResume()` and cleared in `onStop()`/`onDestroy()`.
- `Game.onStop()` keeps an external Presentation session alive only when `onExternelDisplay`, `gamePresentation != null`, and `ExternalDisplayControlActivity.isControllerForeground()` are all true, and the Activity is not finishing or changing configuration.
- `ExternalDisplayControlActivity.onStop()` stops the external Presentation session only when the controller was actually foreground, there is a `Game` instance, launch is not still pending, and the stop is not a configuration change.
- `Game.stopExternalPresentationSessionFromControllerBackground()` is intentionally narrow: it only handles active external Presentation sessions, stops the stream if connected/connecting, dismisses the Presentation, and finishes `Game`.

### Do Not Reintroduce or Broaden

- Do not use `new Intent(displayContext, Game.class)` or `FLAG_ACTIVITY_NEW_TASK` for the Fully external launch contract.
- Do not call `ActivityOptions.setLaunchDisplayId()` outside the documented API-30+, feature, target-existence, and `isActivityStartAllowedOnDisplay()` gate. Do not treat a successful `startActivity()` call as proof of direct placement.
- Do not replace the one in-place Presentation fallback with a duplicate `Game`/`NvConnection` launch, a Surface timeout, or a manufacturer/model allowlist.
- Do not call `StartExternalDisplayControlReceiver.requestFocusToExternalDisplayControl()` from `Game` startup or Presentation startup. That global focus path caused the failed foreground relaunch behavior.
- Do not add a pending/assumed controller-foreground state. Treat only actual `ExternalDisplayControlActivity.onResume()` as foreground.
- Do not use `ExternalDisplayControlActivity.instance != null` as proof that the controller is foreground.
- Do not pass `Window`, `View`, or capture targets through static registries or Intent extras. `Presentation` owns its own window/context.
- Do not refactor mouse capture, `InputCaptureManager`, `OnCapturedPointerListener`, `onGenericMotionEvent()`, or `handleMotionEvent()` as part of launch/window lifecycle fixes.
- Do not broaden this into general activity/task rewrites, manifest launch-mode changes, or receiver changes unless new logs prove those paths are the root cause.
- Do not add repo-local `docs/` files or stage `AGENTS.md`; both are local-only workflow material.
