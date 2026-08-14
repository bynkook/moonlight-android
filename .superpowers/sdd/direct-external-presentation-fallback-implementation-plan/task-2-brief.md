# Task 2 Brief: Integrate reviewed direct-first external display fallback

## Exact workspace and base

- Work only in `C:\Users\BgKing\dev\android\moonlight-android`.
- Branch: `fix-codec-low-latency-compatibility`.
- Required starting HEAD: `132eea33f510ee94fe5c9bb3a200d152233aff5b`.
- Tracked worktree and index must be clean before integration. Existing untracked `.codex/` and ignored `.superpowers/` are local-only and must remain unstaged.

## Reviewed source range

- Reviewed aggregate is the ordered range on `stack/direct-external-presentation-fallback`:
  1. `54551a36036c307482476db0d67ce0da222e1cf6`
  2. `9ec2c68de773be4da8717334481977f3351f2f70`
  3. `3ff2f22dccd77bdabd20728fdfd9ee540f4a9873`
- Integrate the aggregate as one new commit on the combined branch. Preserve its reviewed behavior and tests.
- Commit message: `Try direct external display before Presentation fallback`.

## Required integration behavior

- Preserve every existing low-latency, QTI, MTK, decoder recovery, latency sampler, preference, and isolated debug-build change already present at the base.
- Preserve the reviewed direct-first behavior: API 30+ eligibility gate, `ActivityOptions` requested placement, exactly one cloned Presentation fallback for unsupported/disallowed/exception/mismatch cases, target-loss abort, truthful fallback reasons, and existing Presentation lifecycle/input cleanup.
- If a conflict occurs, keep current low-latency code plus the reviewed external-display behavior. Do not rewrite either feature.
- Do not change manifests, JNI/native code, networking, codec planning, preferences, build-type identity, input capture, or unrelated display/frame-pacing code.
- Do not create or push a PR and do not install or launch through ADB.

## Allowed tracked files

- `app/src/main/java/com/limelight/Game.java`
- `app/src/main/java/com/limelight/utils/ExternalDisplayControlActivity.java`
- `app/src/main/java/com/limelight/utils/ServerHelper.java`
- `app/src/test/java/com/limelight/ExternalDisplayControlActivityLaunchTest.java`
- `app/src/test/java/com/limelight/GameExternalDisplayLaunchResolverTest.java`
- `app/src/test/java/com/limelight/ServerHelperStartIntentTest.java`
- `app/src/test/java/com/limelight/utils/ExternalDisplayDirectLaunchTest.java`

## Required verification

1. Verify starting SHA, status, index, and the reviewed aggregate file list.
2. Apply the three commits without committing, resolve only real conflicts, then inspect the complete staged/working diff against the base.
3. Run focused external-display tests from Task 1.
4. Run focused low-latency tests covering planner, capability, recovery, decoder sampler, and profile preference transport.
5. Run fresh Java compile.
6. Run full `testNonRoot_gameDebugUnitTest`. Acceptance is the exact frozen five failures only, with zero errors and zero skips; total count may increase only by the newly integrated external-display tests.
7. Run native build and `assembleNonRoot_gameLowLatencyDebug`.
8. Verify the ARM64 APK exists and retains package `com.limelight.artemis.lowlatency.debug`, is debuggable, uses the expected launcher, and is arm64-only for the ARM64 split.
9. Run `git -c core.whitespace=cr-at-eol diff --check` against the base, confirm the allowed-file scope, empty index after commit, and no tracked worktree changes.
10. Write an ignored report at `.superpowers/sdd/direct-external-presentation-fallback-implementation-plan/task-2-report.md` with exact commands, counts, frozen failure identities, APK path/identity, conflict resolution, and final SHA.

## Commit gate

- Commit only after the focused checks, compile, full-suite identity comparison, native build, APK assembly/identity, scope, and CRLF-aware diff checks meet the contract.
- If a new failure, compile error, conflict ambiguity, package identity change, or unauthorized file appears, stop without committing and report the blocker.
