# SDD ledger — plan: .codex/moonlight-codec-low-latency-implementation-plan.md

## Preflight

- Branch: `fix-codec-low-latency-compatibility`
- Baseline HEAD: `877c5c22a11ee783fc2b33bc4c151587ee36cfc6`
- Parent/default reference: `3397ec7750969466ad8983364ee1a33182bbffa1`
- Workspace: existing dedicated feature-branch checkout; `.superpowers/sdd/` is ignored.
- `AGENTS.md`: ignored and excluded from all commits.
- Task 0: complete.
- Baseline rerun: 106 tests, 5 failures, 0 errors; the failures exactly match the documented baseline.
- Codec overlap from `3397ec77..877c5c22`: only `MediaCodecDecoderRenderer.java`; preserve its external-display-related edits and keep codec changes surgical.

## Plan conflicts awaiting resolution

- Task 1 mixes core-model tests with QTI and MTK behaviors owned by Tasks 2 and 3.
- Task 3 asserts renderer-owned base-attempt behavior before Task 4 implements it.
- Task commits precede Task 7's first full-suite no-new-failures gate.

## Reviewed and not conflicts

- Runtime recovery is separately and explicitly constrained to standard then base in Task 4.
- The MTK literal key/value matrix, `.value` alias preference, and API 31 capability gate are specified in section 3.4.
- `vdec-lowlatency` is explicitly documented as an all-supported-API compatibility exception, independent of the ULL toggle.
- API 26–30 speculative MTK vendor options remain disabled until a validated device/BSP allowlist exists.

## Tasks

- Task 1: complete; BASE=`877c5c22a11ee783fc2b33bc4c151587ee36cfc6`; HEAD=`087afa8300fdcb5a4131cfadeddd9af04f853391`; review=`APPROVED`.
  - Controller resolution: Task 1 follows its explicit minimal-implementation clause. It owns only the pure immutable `Profile` model, insertion-ordered options, and ordered-option deduplication. QTI tests move to Task 2 and MTK tests to Task 3.
  - Per-task commit gate: targeted test, Java compile, and full-suite comparison against the exact 106/5 baseline.
  - Acceptance correction: Task 1 adds two tests, so the verified suite is 108 tests with the exact same 5 failures, 0 errors, and no new failures.
- Task 2: complete; BASE=`087afa8300fdcb5a4131cfadeddd9af04f853391`; HEAD=`ee2910ca2a6233fbfe736a162bea47b5a183e54d`; re-review=`APPROVED`.
  - API 31+ QTI vendor keys, including picture-order, are independently gated by the normalized advertised snapshot. API 26-30 retains only legacy core/picture and never fence keys.
  - Probe success and success-empty are immutable/cacheable; failure is unavailable and retryable. Any opened codec/session is released in `finally`.
  - The helper applies planner profiles only for Qualcomm. Renderer-owned base and bounded retry remain Task 4 scope.
  - Implementation commit `a60aa3d700c682a8c8474ee20c982d2880c1ea67`; first review=`CHANGES_REQUESTED` for active-helper integration coverage and deterministic concurrent cache coverage.
  - Fix commit `ee2910ca2a6233fbfe736a162bea47b5a183e54d` closes both findings. Verified targeted 24/24, Java compile, and full suite 130 tests with the exact same 5 failures and 0 errors.
- Task 3: complete; BASE=`ee2910ca2a6233fbfe736a162bea47b5a183e54d`; HEAD=`1d85dc3eac630bcdd43b6447c286b482106ea8b7`; review=`APPROVED`.
  - Decoder family and legacy-vdec eligibility are classified in `MediaCodecHelper` with case-insensitive pure seams; planner tests receive an already-classified family.
  - `mtk-experimental-full` exists only when at least one recognized advertised MTK vendor key is inserted.
  - `OTHER + legacyVdecLowLatencyAllowed` uses generic `android-standard -> legacy-vdec`; no MTK label/vendor/scheduler. Other decoders remain on their existing path.
  - Task 3 asserts planner profile count `<=11`; renderer-owned base and total 12-attempt limit remain Task 4.
  - Verified targeted 38/38, Java compile, and full suite 144 tests with the exact frozen 5 failures and 0 errors.
- Task 4: complete; BASE=`1d85dc3eac630bcdd43b6447c286b482106ea8b7`; HEAD=`4d37bb1e179ceeb3307c4ed5bb38907f9b05bf71`; re-review=`APPROVED`.
  - Helper returns a non-null immutable named option result only for real planner profiles; exhaustion is null and `base` remains renderer-owned.
  - Attempt selection caps at 11 option profiles and appends exactly one immediate base, for at most 12 attempts. `startTry` is an option-list index; recovery always starts its separate plan at 0.
  - Initial base failure returns `-5`; runtime-recovery base failure rethrows the original exception to preserve Surface-loss handling.
  - Active format/profile state commits only after configure+start succeeds. Transient/restart/reset retain current state; failed full recreation downgrades NON_STANDARD -> standard -> base, standard/base -> base.
  - Implementation commit `c6a731678ab0a499e5427f2fc2843536dd754184`; first review=`CHANGES_REQUESTED` because finite legacy conversion dropped established standard/vendor/scheduler fallback combinations and the public boolean entry-point argument was ignored.
  - Fix commit `4d37bb1e179ceeb3307c4ed5bb38907f9b05bf71` restores finite deduplicated legacy standard/vendor/scheduler fallback combinations and maps the public initialization boolean to the initial/runtime recovery policies at real call sites.
  - Verified targeted 55/55, Java compile, and full suite 161 tests with the exact frozen 5 failures and 0 errors; protected renderer display/input/frame-pacing paths remained untouched.
- Task 5: complete; BASE=`4d37bb1e179ceeb3307c4ed5bb38907f9b05bf71`; HEAD=`945a70f59657ef4dea5f9b57bea05b666b004cec`; review=`APPROVED`.
  - Preserved `checkbox_ultra_low_latency` and default false; clarified five locale resources; removed only the dead `StreamConfiguration` copy and `Game` setter call.
  - Independent evidence: focused 5/5, resource/Java compile rerun success, full suite 164 tests with the exact frozen 5 failures and 0 errors; diff/scope/ABI audits clean.
  - Non-blocking review notes: two newly edited locale lines have mixed working-tree EOL style; exact localized-string and reflection tests intentionally lock the current compatibility contract and may require maintenance if translations or internal names change.
- Task 6: complete; BASE=`945a70f59657ef4dea5f9b57bea05b666b004cec`; HEAD=`a126f0f3223bc3e6977af6fb86e1f3c1343a8da3`; re-review=`APPROVED`.
  - Added exact queue-call-start to first-output-dequeue PTS correlation, fixed 16,384-sample ring/nearest-rank percentiles, lifecycle-safe one-shot summary, tri-state capability snapshot, and deterministic profile/fallback/selection/crash diagnostics.
  - Early concurrency review found interrupted joins could snapshot before thread termination and a teardown queue/flush race; implementer added RED tests, joins-to-termination with interrupt restoration, and serialized queue/invalidation seams before commit.
  - Implementer evidence: focused 76/76, fresh Java compile, full suite 185 tests with the exact frozen 5 failures and 0 errors; five authorized files only.
  - Formal review Important findings: single-value pending PTS storage overwrites duplicate operations and cannot cancel/consume exact FIFO identity; `decoderLatencyLock` spans vendor `queueInputBuffer()`/teardown `flush()`, creating diagnostic-induced release jitter and stop-hang risk.
  - Controller disposition: unique operation token plus FIFO-per-PTS tracking; latency lock restricted to metadata/sampler; codec calls outside the lock with two-phase generation/invalidation handling for overlapping teardown. Preserve join-to-termination and protected codec/pacing semantics.
  - Fix Round 1 commit `29e834218bc452b7ae9bebd767a36fc4e3e02447` closed both original Important findings; focused 81/81 and full suite 190/frozen-five passed.
  - Re-review found one residual Important cleanup issue: lifecycle clear drops sparse-array roots but does not sever FIFO token links retained by an in-flight vendor call. Fix Round 2 must detach all links under the metadata lock before clearing maps.
  - Fix Round 2 commit `a126f0f3223bc3e6977af6fb86e1f3c1343a8da3` severs all pending FIFO links during lifecycle clear; focused 84/84, fresh compile, full suite 193 tests with exact frozen five/0 errors.
  - Final re-review verified all Critical/Important findings closed. Non-blocking minors: no explicit middle-token or nested/throwing invalidation test despite inspected correctness; five-wrapper guard remains a supplemental raw-source assertion.
- Task 7: complete; initial verification HEAD=`a126f0f3223bc3e6977af6fb86e1f3c1343a8da3`; final verification HEAD=`6ff9b36d3c508f58351b4dfa78bb1c8bcb62ead8`; verdict=`APPROVED/PASS`.
  - Initial verification passed static guards, focused 75/75, Java compile, native build, four-ABI assemble/APK audit, and full suite 193/frozen-five, but found one range-new API31 `NewApi` lint error at the vendor-parameter probe. The other 32 lint errors and 477 warnings are frozen baseline findings.
  - Lint fix commit `6ff9b36d3c508f58351b4dfa78bb1c8bcb62ead8` adds a method-local `@RequiresApi(S)` at the already runtime-gated probe boundary. Fresh lint returns only the frozen 32 errors/477 warnings; focused 45/45, Java compile, and full suite 193/frozen-five pass.
  - Plain cumulative diff-check reports CRLF terminators as trailing whitespace; byte audit found zero actual trailing spaces/tabs. Working/index checks pass, and the cumulative audit passes with Git's `cr-at-eol` whitespace interpretation. No EOL normalization is authorized because baseline uses CRLF and normalization would create unrelated churn.
  - Final fresh evidence: focused 75/75; Java compile/native build/assemble success; four ABI split/native-lib audit success; ARM64 APK SHA-256 `b9b6bea628887456044fa7252020e5132719c558e2ff10a86243ffe9dda28ad0`; full suite 193 with exact frozen five/0 errors; lint has only frozen 32 errors/477 warnings and zero range-new locations.
  - Final index empty and visible status only the intentional untracked `.codex/` plan; protected/static/commit/file allowlist audits clean.
- Task 8: automated build/install phase complete; implementation HEAD=`132eea33f510ee94fe5c9bb3a200d152233aff5b`; isolated-build review=`APPROVED`; manual stream phase in progress.
  - Added only the reusable `lowLatencyDebug` build type in `app/build.gradle`; application ID is `com.limelight.artemis.lowlatency.debug` and label is `Artemis Low Latency (Debug)`.
  - New Java/native/assemble tasks and the existing debug assemble passed. New-variant unit XML contains 193 tests with the exact frozen five failures and 0 errors/skips. Lint has no new source/config finding.
  - ARM64 APK SHA-256 is `48D2CFE363810D78A2DC0175E8C86D6168E437DB3D6D5077CA6538CFCBDF6C2B`; APK package, label, launcher, debuggable flag, ARM64 contents, and Android Debug signature were independently verified.
  - Installed successfully on the single connected Galaxy S25+ `R3CY20LMGDF`. Device package metadata reports version `20.2.6` (57), `arm64-v8a`, and `DEBUGGABLE`.
  - Coexistence proved after installation: `com.limelight.noir`, `com.limelight.noirdebug`, and `com.limelight.artemis.lowlatency.debug` are all installed. No existing package was removed or updated.
  - Per user instruction, no stream was launched. The user will disconnect USB, pair the isolated package with Apollo, and perform the manual stream test. Task 9 must not begin until the user reconnects USB and explicitly reports completion.
- Task 9: pending USB reconnection after the user's manual Apollo/stream test.
