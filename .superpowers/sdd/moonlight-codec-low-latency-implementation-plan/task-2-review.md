APPROVED

# Task 2 re-review — Fix Round 1

No blocking findings remain.

## Prior findings closed

1. The active Qualcomm helper bridge is now exercised rather than only its planner/store components. The public production entry point performs Qualcomm classification and supplies the private production capability store, real SDK, real `FEATURE_LowLatency` result, and real scheduler capability (`app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java:626-641`). Its package-private delegate owns the same snapshot-to-plan-to-single-profile application path used in production and rejects negative, exhausted, and non-Qualcomm calls before mutating the format (`app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java:784-827`).

   `qualcommHelperAppliesOneS25ProfilePerAttemptAndTerminates` verifies the exact four S25 option maps on fresh formats, one profile per try, success caching/release, and finite exhaustion (`app/src/test/java/com/limelight/binding/video/MediaCodecLowLatencyCapabilityTest.java:237-292`). `qualcommHelperUllOffStartsWithStandardAndRejectsInvalidAttempts` proves Android-standard remains first with ULL off and that negative/out-of-range attempts return false without format mutation (`app/src/test/java/com/limelight/binding/video/MediaCodecLowLatencyCapabilityTest.java:295-328`). `injectableHelperRejectsNonQualcommWithoutOpeningProvider` proves the injected QTI path cannot probe a non-Qualcomm decoder (`app/src/test/java/com/limelight/binding/video/MediaCodecLowLatencyCapabilityTest.java:332-355`).

2. `concurrentCaseVariantLookupsShareOneCompleteSnapshot` uses a barrier and latches, not sleeps, to overlap two normalized-name callers. It proves one open, one enumeration, one release, the same fully populated snapshot object for both callers, immutability, and clean executor termination (`app/src/test/java/com/limelight/binding/video/MediaCodecLowLatencyCapabilityTest.java:145-230`). The synchronized store publishes the cached immutable snapshot only after normalization is complete and keeps the second caller out through the `finally` release (`app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java:96-138`).

## Seam and scope assessment

- The injectable overload and provider/store types are package-private; no public or cross-package injection surface was added. Only the existing public production wrapper and tests call the overload.
- `MediaCodec.createByCodecName()` remains confined to the private production store. Tests use fake sessions and cannot replace the application-wide production provider.
- The overload accepts already-derived booleans and SDK only to avoid static Android probing in unit tests; production derives and supplies those values immediately before delegation. The additional surface is proportional to the required integration coverage and does not duplicate profile logic.
- The fix commit `ee2910ca` changes only `MediaCodecHelper.java` and `MediaCodecLowLatencyCapabilityTest.java`. Across `087afa83..ee2910ca`, no `AGENTS.md`, `.codex`, `docs`, renderer, external-display, input, or mouse file is committed. `git -c core.whitespace=cr-at-eol diff --check` is clean.

## Reviewer verification

- Targeted rerun: 24 tests passed, 0 failed.
- Full XML rerun: 130 tests, 5 failures, 0 errors, 0 skipped. The failures are exactly the frozen baseline: `LayoutInflationTest.allLayoutsInflateSuccessfully`, both `ProfilesNavigationTest` button-launch cases, `SimpleStartupTest.testApplicationOnCreate`, and `StartupTest.testApplicationStartup`.
