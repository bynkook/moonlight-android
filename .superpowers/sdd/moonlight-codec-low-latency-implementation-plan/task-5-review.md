APPROVED

# Task 5 review

No Critical or Important correctness, specification, scope, API, or test-behavior findings were found for `4d37bb1e179ceeb3307c4ed5bb38907f9b05bf71..945a70f59657ef4dea5f9b57bea05b666b004cec`. The two Minor notes below are non-blocking.

## Findings

### Critical

- None.

### Important

- None.

### Minor

- `app/src/main/res/values-ru/strings.xml:503-504` and `app/src/main/res/values-zh-rCN/strings.xml:525-526` use LF endings inside otherwise CRLF files. A byte census found exactly 2 LF-only lines in each file, versus 676 and 716 CRLF lines respectively. This has no Android resource/runtime impact and `git diff --check` is clean, but normalizing the edited lines would avoid mixed-EOL churn.
- `app/src/test/java/com/limelight/profiles/ProfilesOverlayTest.java:62-92` intentionally locks exact localized prose and exact removed member names. The tests are deterministic and valid for this task's copy/removal contract, but legitimate future translation edits must update duplicated expectations, and the reflection test alone would not detect a renamed duplicate transport field. This is not blocking because the behavioral key/default and overlay/persistence tests remain meaningful, and the independent whole-tree reference audit establishes the sole-source/dead-field invariant.

## Commit and scope

- Commit `945a70f59657ef4dea5f9b57bea05b666b004cec` has the single required parent `4d37bb1e179ceeb3307c4ed5bb38907f9b05bf71` and exact subject `Clarify experimental vendor low-latency setting`.
- The range contains exactly eight authorized files: `Game.java`, `StreamConfiguration.java`, the default/French/Russian/Simplified Chinese/Traditional Chinese string resources, and `ProfilesOverlayTest.java`. No file mode changed.
- `preferences.xml` is byte-for-byte preserved: both endpoints resolve it to blob `5b009c891fc3ef1ca371de9ce957e97d7fbf1171`. Its sole matching preference remains `android:key="checkbox_ultra_low_latency"` with `android:defaultValue="false"` at `app/src/main/res/xml/preferences.xml:68-73`.
- No `AGENTS.md`, `.codex/`, `.superpowers/`, `docs/`, build output, APK/native binary, Fully External Display, Presentation, input/mouse, JNI, network, or HTTP file appears in the commit range. `AGENTS.md` and this review path are confirmed ignored. The worktree's only visible untracked item before and after verification is the pre-existing `.codex/moonlight-codec-low-latency-implementation-plan.md`; it is not in the commit.

## Preference and resource contract

- `PreferenceConfiguration` retains the same key constant, `DEFAULT_ENABLE_ULTRA_LOW_LATENCY = false`, live field, and preference read at `app/src/main/java/com/limelight/preferences/PreferenceConfiguration.java:47,143,234,882`.
- The renderer retains that same `PreferenceConfiguration` instance at `app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java:347,533-541` and passes `prefs.enableUltraLowLatency` directly to `MediaCodecHelper.getDecoderLowLatencyOptions()` at lines 941-949. No second production `enableUltraLowLatency` state remains.
- Android standard low latency is capability-gated independently of the option: `LowLatencyProfilePlanner.standardOptions()` adds `MediaFormat.KEY_LOW_LATENCY` only when `androidLowLatencySupported` at `app/src/main/java/com/limelight/binding/video/LowLatencyProfilePlanner.java:324-329`. Advertised vendor parameters require SDK 31+, successful enumeration, and membership in the supported-parameter set at lines 420-423; older bounded family/manufacturer gates remain separate compatibility paths.
- The English title and summary at `app/src/main/res/values/strings.xml:561-562` match the brief exactly. French (`values-fr/strings.xml:496-497`), Russian (`values-ru/strings.xml:503-504`), Simplified Chinese (`values-zh-rCN/strings.xml:525-526`), and Traditional Chinese (`values-zh-rTW/strings.xml:607-608`) accurately convey automatic supported Android low latency plus optional capability-checked vendor decoder optimization. None of the five ULL entries retains an SD8Gen/Snapdragon/8Elite/MTK generation restriction.

## Dead transport-field and API audit

- The production diff removes exactly the private `StreamConfiguration.enableUltraLowLatency` field, its public builder setter, constructor default assignment, public getter, and the single `Game` builder call. `Game` otherwise retains the stream builder unchanged at `app/src/main/java/com/limelight/Game.java:843-865`; the current `StreamConfiguration` builder/default boundary is intact at `app/src/main/java/com/limelight/nvstream/StreamConfiguration.java:20-35,128-165`.
- Independent `rg`/`git grep` checks found no remaining production `setEnableUltraLowLatency`, `getEnableUltraLowLatency`, or `StreamConfiguration` ULL-field reference. Exact-name searches under JNI, `nvstream`, and `nvstream/http` returned no matches. `NvConnection` consumes `StreamConfiguration` through explicit getters and passes explicit values to `MoonBridge.startConnection()`; there is no reflective serialization dependency.
- API risk is appropriately bounded: the removed setter/getter were public Java methods, so an unknown out-of-tree binary/source caller would break. This repository is a single `com.android.application` module, the only tracked builder caller was updated, there is no published library ABI in scope, and a forced fresh Java compile passed. The removal is therefore appropriate for the explicit dead-field requirement.

## Test-quality assessment

- `ultraLowLatencyPreference_keepsCompatibleKeyAndDefaultsOff()` at `ProfilesOverlayTest.java:51-59` clears shared state, loads the real preference XML with `readAgain=true`, and verifies both key presence and false default.
- The existing overlay and restart-persistence behaviors remain exercised at lines 95-135; replacing repeated literals with one constant does not weaken their assertions.
- Locale contexts are created explicitly at lines 137-144, so the five copy checks are deterministic under Robolectric. The reflection helpers at lines 146-161 are a narrow structural regression guard for the explicitly forbidden dead members; they supplement rather than replace the repository-wide dependency audit.

## Independent verification

- Focused command: `:app:processNonRoot_gameDebugResources :app:compileNonRoot_gameDebugJavaWithJavac :app:testNonRoot_gameDebugUnitTest --tests 'com.limelight.profiles.ProfilesOverlayTest' --console=plain` — `BUILD SUCCESSFUL`; all 5 profile tests passed.
- Forced resource/compile command: `:app:processNonRoot_gameDebugResources :app:compileNonRoot_gameDebugJavaWithJavac --rerun-tasks --console=plain` — `BUILD SUCCESSFUL`; all 18 tasks executed. Existing unrelated resource/deprecation warnings remain.
- Full command: `:app:testNonRoot_gameDebugUnitTest --console=plain` — expected nonzero task result. Parsed JUnit XML: 27 suites, 164 tests, 5 failures, 0 errors, 0 skipped. The failures are exactly the frozen set: `LayoutInflationTest.allLayoutsInflateSuccessfully`, both named `ProfilesNavigationTest` launch tests, `SimpleStartupTest.testApplicationOnCreate`, and `StartupTest.testApplicationStartup`. All five `ProfilesOverlayTest` cases passed in the full run.
- `git diff --check 4d37bb1e..945a70f5` exited 0. Protected-scope, mode, exact-symbol, stale-copy, and commit-tree audits passed.

No source was edited, staged, committed, installed, or exercised through ADB during this review. Task 6 was not started.
