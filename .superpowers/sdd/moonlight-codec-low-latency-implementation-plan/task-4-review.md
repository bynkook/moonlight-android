APPROVED

# Task 4 re-review — Fix Round 1

No blocking correctness, specification, scope, or test-quality findings remain for Task 4 at `4d37bb1e`.

## Prior findings resolved

- `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java:784-860` now derives independent ordered `standard`, legacy-family `vendor`, and exclusive `scheduler` maps, then emits a finite deduplicated risk-to-safe sequence. ULL-on Nvidia/Kirin/Exynos paths preserve standard+vendor+scheduler, standard+vendor, standard, vendor+scheduler, vendor, and scheduler-only combinations when their component maps exist. ULL-off emits no vendor key and retains standard and/or scheduler-only fallback. Empty maps are skipped, duplicate maps are removed, and no cumulative `MediaFormat` mutation or unbounded Nvidia source remains.
- `app/src/test/java/com/limelight/binding/video/MediaCodecLowLatencyCapabilityTest.java:536-775` verifies exact profile names, ordered key/value maps, operating-rate versus priority selection, FEATURE-supported/unsupported fixtures, ULL-off vendor omission, scheduler-only generic fallback, and finite `null` exhaustion for Nvidia, Kirin, and Exynos.
- `app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java:197-202,865-870` makes the public boolean authoritative: `false` selects `INITIAL`, `true` selects `RUNTIME_RECOVERY`. `setup()` passes false at line 1034 and full recreation passes true at line 1134. The composed entry-point policy test at `MediaCodecDecoderRecoveryProfileTest.java:244-261` proves false leads to renderer-owned base `-5`, while true leads to base rethrow.

## Original Task 4 contract rechecked

- `AppliedLowLatencyOptions` remains defensive, ordered, immutable, named/indexed, rejects empty profiles, returns `null` on exhaustion, and never represents base.
- Renderer selection is capped at 11 option profiles plus exactly one fresh renderer-owned base; early exhaustion attempts base immediately and a broken endless source cannot cause a 13th attempt.
- State is committed only after configure+start succeeds. Restart/reset retains the active configuration; only full recreation uses the safe runtime downgrade plan. Initial base failure returns `-5`; runtime base failure preserves the original exception/recovery behavior.
- QTI/MTK capability profiles and noneligible Amlogic behavior remain covered by the focused suite. No render-target/Surface ownership, Presentation/display, frame-pacing/Choreographer, or input/mouse code changed.

## Independent verification

- Targeted low-latency/recovery suite: 55 tests passed.
- `:app:compileNonRoot_gameDebugJavaWithJavac`: passed.
- Full suite: 161 tests, 5 failures, 0 errors. Failure identities exactly match the frozen baseline: `LayoutInflationTest`, both specified `ProfilesNavigationTest` methods, `SimpleStartupTest`, and `StartupTest`.
- `git diff --check 1d85dc3e..4d37bb1e`: clean.
- Full range and Fix Round 1 each touch only the two Task 4 production codec files and two Task 4 codec test files. Worktree status contains only the pre-existing untracked `.codex/` plan.
