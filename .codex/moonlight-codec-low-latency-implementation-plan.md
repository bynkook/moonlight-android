# Moonlight Android Codec Low-Latency Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android 표준 저지연 디코딩을 기본 안전 경로로 유지하면서, Qualcomm QTI와 MediaTek 확장을 capability 기반의 명시적·유한 fallback 프로필로 재구성하여 Galaxy S25+ 호환성을 검증하고 MTK 경로의 안정성·성능·테스트 가능성을 높인다.

**Architecture:** `MediaCodecHelper`가 디코더 capability를 한 번 수집하고, 순수 Java `LowLatencyProfilePlanner`가 위험도가 높은 프로필부터 안전한 프로필까지 결정론적으로 생성한다. `MediaCodecDecoderRenderer`는 프로필 이름을 기록하며 유한 횟수만 구성하고, 마지막에는 반드시 옵션 없는 base format을 시도한다. Android 표준 `MediaFormat.KEY_LOW_LATENCY`는 지원되는 모든 디코더의 기본 경로이며, 실험 옵션은 광고된 vendor parameter 또는 명시적으로 제한된 legacy 경로에서만 추가한다.

**Tech Stack:** Java 11, Android MediaCodec/MediaFormat, Gradle Android Plugin, JUnit 4, Robolectric 4.16, Mockito 5.19, ADB/logcat, 실제 Sunshine/Moonlight 스트리밍.

---

## 1. 현재 기준점과 이미 확인된 사실

### 1.1 Git 기준점

- 작업 브랜치: `fix-codec-low-latency-compatibility`
- 현재 HEAD: `877c5c22a11ee783fc2b33bc4c151587ee36cfc6`
- 부모 커밋: `3397ec7750969466ad8983364ee1a33182bbffa1` (`moonlight-noir`)
- `877c5c22` 제목: `Samsung One UI Fully External Display 기능 이식`
- 기본 브랜치와의 차이: 앞선 커밋 1개, 뒤처진 커밋 0개
- 의미: 저지연 작업은 기본 브랜치 HEAD가 아니라 사용자의 마지막 최종 수정 커밋 다음에 쌓인다.
- 보호 범위: Fully External Display, Presentation, 외부 입력, 마우스 캡처 및 controller lifecycle 코드는 이번 작업에서 구조 변경하지 않는다.
- 계획서 위치: `.codex/moonlight-codec-low-latency-implementation-plan.md`; 현재 요청에 따라 저장만 했으며 stage/commit하지 않는다.

### 1.2 현재 브랜치 테스트 기준선

2026-08-09에 다음 명령을 현재 HEAD에서 실행했다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat :app:testNonRoot_gameDebugUnitTest --console=plain
```

결과는 컴파일 성공, 106 tests / 5 existing failures / 0 errors / 0 skipped이다. 기존 실패는 다음 다섯 개이며 저지연 변경과 무관한 기준선으로 고정한다.

1. `LayoutInflationTest.allLayoutsInflateSuccessfully`: MaterialComponents theme 문제
2. `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivityFromAppView`: `ExtendedFloatingActionButton`을 `ImageButton`으로 cast
3. `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity`: 동일 cast 문제
4. `SimpleStartupTest.testApplicationOnCreate`: base context가 없는 startup test
5. `StartupTest.testApplicationStartup`: 동일 base context 문제

이번 작업의 완료 조건은 새 targeted test가 모두 통과하고, 전체 suite에서 위 다섯 실패 외 새 실패가 0개인 것이다. 이 다섯 실패를 이번 저지연 변경에 섞어 수정하지 않는다.

### 1.3 Galaxy S25+ 실기기 증거

연결 기기에서 확인된 값은 다음과 같다.

| 항목 | 값 |
|---|---|
| ADB serial | `R3CY20LMGDF` |
| 모델 | `SM-S936N` |
| SoC 제조사 | `QTI` |
| SoC 모델 | `SM8750` |
| 제품명 | Snapdragon 8 Elite for Galaxy |
| board / hardware | `sun` / `qcom` |
| ABI | `arm64-v8a` |
| Android | 16, API 36 |
| 보안 패치 | 2026-07-05 |

실기기 MediaCodec probe에서 다음 hardware decoder가 존재하고 모두 `FEATURE_LowLatency=true`였다.

- `c2.qti.avc.decoder.low_latency`
- `c2.qti.hevc.decoder.low_latency`
- `c2.qti.av1.decoder.low_latency`

S25+ codec이 광고한 관련 vendor parameter는 다음 세 개다.

- `vendor.qti-ext-dec-picture-order.enable`
- `vendor.qti-ext-output-fence.enable`
- `vendor.qti-ext-output-fence.fence_type`

다음 두 개는 광고되지 않았다.

- `vendor.qti-ext-dec-low-latency.enable`
- `vendor.qti-ext-output-sw-fence-enable.value`

AVC/HEVC/AV1에 대해 standard-only, QTI-only, standard+광고된 QTI, 현재 전체 key 조합의 최소 configure/start는 모두 성공했다. 다만 이는 조합이 configure 단계에서 거부되지 않았다는 뜻일 뿐, 광고되지 않은 key가 실제 적용되었거나 지연이 개선됐다는 증거는 아니다. 실제 stream A/B와 input-to-photon 측정은 새 브랜치 빌드로 다시 수행해야 한다.

이 실기기 증거는 브랜치 기준점을 옮기기 전 `3397ec77`의 app/probe로 수집했다. `MediaCodecHelper.java` blob은 `3397ec77`과 현재 `877c5c22`에서 동일하지만, `MediaCodecDecoderRenderer.java`는 외부-display 커밋에서 변경됐으므로 현재 브랜치 APK의 실제 stream 검증을 생략하지 않는다.

## 2. 기술 평가와 설계 결정

### 2.1 Android 표준 저지연은 이 streaming 용도에 적합한가

적합하다. Android는 저지연 decode를 game 및 real-time application 용도로 명시하고, codec 표준이 허용하는 범위에서 출력 buffer를 가능한 빨리 반환하도록 정의한다.

현재 repo는 이미 다음 표준 경로를 사용한다.

- `MediaCodecHelper.java:470-483`: `CodecCapabilities.FEATURE_LowLatency` 확인
- `MediaCodecHelper.java:548-551`: `"low-latency"=1` 설정
- `MediaCodecDecoderRenderer.java:577-609`: 해당 `MediaFormat`을 `MediaCodec.configure()`에 전달
- `MediaCodecHelper.java:1010-1066`: FEATURE를 광고하는 low-latency decoder를 먼저 선택

문자열 `"low-latency"`는 `MediaFormat.KEY_LOW_LATENCY`와 값이 정확히 같으므로 성능 차이는 없다. 다만 상수로 바꾸면 API 의도와 정적 검토 품질이 좋아진다. configure 시점에 넣는 구현은 올바르며, runtime `MediaCodec.PARAMETER_KEY_LOW_LATENCY`를 쓰지 않는다고 해서 구식이 아니다.

표준 API의 한계도 명확하다. Surface fence, compositor, frame pacing, network jitter, thermal throttling을 해결하거나 특정 millisecond를 보장하지 않는다. 따라서 “지원됨”과 “실측 성능 개선”을 분리한다.

참조:

- Android 저지연 media: https://source.android.com/docs/core/media/low-latency-media
- `MediaFormat.KEY_LOW_LATENCY`: https://developer.android.com/reference/android/media/MediaFormat#KEY_LOW_LATENCY
- `CodecCapabilities.FEATURE_LowLatency`: https://developer.android.com/reference/android/media/MediaCodecInfo.CodecCapabilities#FEATURE_LowLatency
- vendor parameter 열거: https://developer.android.com/reference/android/media/MediaCodec#getSupportedVendorParameters%28%29

### 2.2 표준 + QTI 조합의 장점

두 경로는 같은 기능의 중복이 아니라 서로 다른 층을 보완할 수 있다.

- 표준 key: decoder가 frame을 내부에 불필요하게 보류하지 않도록 하는 Android-level 계약
- QTI picture-order / decoder low-latency: Qualcomm decoder 내부 reorder 및 decode path 힌트
- QTI output fence: decoder output과 Surface consumer 사이의 동기화 경로 힌트
- operating-rate / priority: resource allocation 및 scheduler 힌트

따라서 S25+에서는 표준 key를 유지하면서 실제 광고된 picture-order와 output-fence pair를 실험적으로 조합하는 것이 타당하다. 단, `KEY_OPERATING_RATE=32767`은 latency 보장이 아니라 공격적인 resource hint이고 발열·전력 비용이 있으므로 가장 공격적인 프로필에만 둔다.

### 2.3 현재 QTI fallback이 QTI-only를 배제하는 이유

설계 의도가 아니라 회귀다.

- 원 upstream은 마지막 vendor fallback에 `vendor.qti-ext-dec-low-latency.enable` 단독 단계를 가졌다.
- 2025-04-12의 `c5f203b6`가 세 fence key를 기존 `tryNumber < 5` 블록 안에 추가했다.
- 그 결과 try 4가 “QTI LL 단독”이 아니라 “QTI LL + 모든 fence”가 됐고, try 5에서는 QTI LL과 fence가 동시에 제거된다.
- 주석은 QTI-only fallback을 말하지만 실제 코드는 그 단계를 더 이상 만들지 않는다.
- ULL을 켜면 aggregate try 0 실패 뒤 standard-only도 다시 시도하지 않는다.

개선안은 QTI core, picture-order, fence, scheduler, Android standard를 별도 축으로 분리하고 각각 제거하는 명시적 프로필이다. 광고된 QTI core가 있는 기기에는 QTI-only 단계가 반드시 존재해야 한다. S25+는 QTI core key를 광고하지 않으므로 API 36에서 그 key를 억지로 쓰지 않고 standard-only와 광고된 output-fence 경로를 사용한다.

### 2.4 현재 구현 품질 평가

좋은 부분:

- 표준 API와 FEATURE-aware decoder selection은 현대적이고 streaming에 적절하다.
- codec configure 실패 시 옵션을 줄여 base format까지 가는 기본 발상은 옳다.
- Qualcomm/MTK prefix 구분과 기존 vendor compatibility 지식이 축적되어 있다.

개선이 필요한 부분:

- `tryNumber < N` 조건이 하나의 긴 메서드에 흩어져 실제 프로필을 읽거나 테스트하기 어렵다.
- 지원 vendor parameter 열거 결과를 log heuristic에만 쓰고 실제 key gating에는 쓰지 않는다.
- 서로 다른 세대의 QTI fence family를 한 format에 동시에 넣는다.
- `vdec-lowlatency`가 QTI에도 무차별 적용된다.
- ULL off/on 의미가 FEATURE 존재 여부에 따라 달라져 UI 설명과 맞지 않는다.
- `safeSet()`은 `MediaFormat` map에 값을 넣을 뿐 codec 지원을 검증하지 않는다.
- MTK는 `tryNumber >= 4`에서도 `setNewOption=true`가 되어 configure 실패 시 무한 반복할 수 있다.
- MTK ULL key를 항상 `0`으로 써서 체크박스를 켜도 ULL을 켜지 않는다.
- exact key matrix와 fallback 순서를 검증하는 unit test가 없다.
- renderer recovery가 동일한 aggressive format을 재사용하거나 다시 try 0부터 시작할 수 있다.

평가 결론: 표준 core가 구식이거나 성능이 낮은 것이 아니다. 품질 문제는 표준 API가 아니라 그 위에 쌓인 실험 vendor option의 무검증·암묵적 조합과 fallback 회귀다.

### 2.5 DerFlacco repo 적용 판단

전체 merge/cherry-pick은 부적절하다.

- local repo에는 ClassicOldSong PR #361의 MTK/Qualcomm 기반 작업(`d0205ab`)이 이미 들어 있다.
- DerFlacco 기본 브랜치는 `8157555`로 오래됐고, 최신 released branch는 `Artemide_Experimental_Async_0.5` / `0b8dc86`이다.
- 최신 Async branch는 공통 base 이후 341개 커밋, 43개 파일의 unrelated 변경을 포함한다.
- async decode, FSR/OpenGL, CPU affinity/boost, overlay, input/pacing, native build, preference redesign까지 같이 가져오면 검증 범위가 과도하게 커진다.
- experimental MTK key의 공식 문서와 unit test가 부족하고, hardware 결과도 제한적이며 mixed report가 있다.

선별 재구현할 가치가 있는 부분:

- `bb54ded`: MTK 시도 순서를 유한하게 만들고 마지막 base attempt에서 종료
- ULL 체크 시에만 `vendor.mtk.vdec.ultra-low-latency=1`
- standard + legacy + performance를 단계적으로 축소하는 fallback 발상
- `359f83c`: skip/drop 관련 네 key 제거

그 외 DerFlacco 변경은 이번 범위에서 제외한다.

## 3. 목표 동작

### 3.1 공통 원칙

1. Android standard low latency는 지원 시 자동 적용한다. 실험 체크박스와 무관하다.
2. 체크박스는 “표준 저지연 on/off”가 아니라 “추가 experimental vendor ultra-low-latency 최적화 on/off”다. 기존 호환성 baseline인 `vdec-lowlatency`와 capability가 확인된 QTI core fallback은 체크박스와 독립적으로 유지한다.
3. Android 12/API 31 이상에서는 vendor key를 `getSupportedVendorParameters()` 결과로 gate한다.
4. 열거를 지원하지 않는 API 26-30에서는 decoder family prefix와 기존 검증 범위로 legacy option을 좁힌다. QTI fence는 열거 없이는 추가하지 않는다.
5. QTI output-fence pair와 software-fence를 같은 format에 동시에 넣지 않는다.
6. 모든 프로필은 결정론적 이름과 ordered key map을 갖는다.
7. 중복 프로필은 제거한다.
8. planner는 option이 하나 이상 있는 profile만 반환한다. option 없는 base format은 renderer가 정확히 한 번만 소유·시도한다.
9. option profile은 최대 11개, base를 포함한 전체 configure 횟수는 최대 12회로 제한한다.
10. configure 성공은 “호환” 증거이고 latency 개선은 별도 A/B 실측으로 판단한다.

`vdec-lowlatency`는 vendor-parameter 열거 대상이 아닌 기존 framework/ACodec 호환성 key이므로 모든 지원 API에서 별도 예외로 허용한다. 조건은 MTK prefix, Amlogic prefix 또는 `Build.MANUFACTURER=Amazon` 중 하나이며, 기존 Xiaomi Android M 이하 제외 규칙을 그대로 적용한다. Qualcomm prefix에는 이 key를 절대 적용하지 않는다. API 31+ vendor 열거가 success-empty 또는 failure여도 이 baseline key만은 prefix 규칙으로 유지한다.

### 3.2 계획할 순수 Java 모델

새 파일 `app/src/main/java/com/limelight/binding/video/LowLatencyProfilePlanner.java`에 Android runtime object를 보관하지 않는 작은 모델을 둔다.

```java
final class LowLatencyProfilePlanner {
    enum DecoderFamily { QUALCOMM, MEDIATEK, OTHER }
    enum PlanPurpose { INITIAL_CONFIGURATION, RUNTIME_RECOVERY }

    static final class Capabilities {
        final DecoderFamily family;
        final String decoderName;
        final int sdkInt;
        final boolean androidLowLatencySupported;
        final boolean maxOperatingRateSupported;
        final boolean legacyVdecLowLatencyAllowed;
        final boolean vendorParameterEnumerationAvailable;
        final Set<String> supportedVendorParameters;
    }

    static final class Profile {
        final String name;
        final Map<String, Integer> integerOptions;
    }

    static List<Profile> plan(Capabilities capabilities,
                              boolean ultraLowLatency,
                              PlanPurpose purpose) {
        // INITIAL_CONFIGURATION: compatible alternatives, no empty profile
        // RUNTIME_RECOVERY: standard-only when safe, otherwise no profile
    }
}
```

`LinkedHashMap`으로 key 순서를 고정하고, 생성자에서 defensive copy 후 unmodifiable view를 노출한다. profile equality는 ordered option 내용 기준으로 처리해 동일한 단계가 자동 제거되게 한다.

### 3.3 Qualcomm 프로필 순서

`qtiCore`는 `vendor.qti-ext-dec-low-latency.enable`이 API 31+에서 광고됐거나, vendor enumeration이 없는 API 26-30의 Qualcomm legacy decoder일 때만 존재한다.

`preferredFence` 선택 규칙:

1. output-fence enable과 fence_type 두 key가 모두 광고되면 그 pair 사용
2. 그렇지 않고 software-fence key가 광고되면 software-fence 사용
3. 둘 다 완전하지 않으면 fence 없음
4. 두 family가 모두 광고돼도 같은 format에 넣지 않고 별도 fallback profile로만 시도

picture-order 값은 기존 codec contract를 유지해 `OMX.qcom`은 `0`, `c2.qti`/`c2.qcom`은 `1`로 한다.

ULL off의 option profile:

1. `android-standard`: FEATURE가 있을 때 `MediaFormat.KEY_LOW_LATENCY=1`
2. `qti-core-picture`: qtiCore + codec별 picture-order
3. `qti-core-only`: qtiCore 단독

그 뒤 renderer가 base를 정확히 한 번 시도한다.

ULL on의 option profile:

1. `qti-experimental-full`: standard + qtiCore(지원 시) + picture-order + preferredFence + operating-rate/priority
2. `qti-experimental-alt-fence`: 양쪽 fence family가 광고된 경우에만 alternate family로 별도 시도
3. `qti-experimental-no-scheduler`: standard + qtiCore + picture-order + preferredFence
4. `qti-no-fence`: standard + qtiCore + picture-order
5. `android-standard`: standard-only
6. `qti-core-picture`: qtiCore + picture-order
7. `qti-core-only`: qtiCore 단독

그 뒤 renderer가 base를 정확히 한 번 시도한다.

빈 key 조합과 중복 프로필은 제거한다. 따라서 현재 S25+의 예상 순서는 다음처럼 짧아진다.

1. standard + picture-order + output-fence pair + scheduler
2. standard + picture-order + output-fence pair
3. standard + picture-order
4. standard-only
5. renderer-owned base

S25+에서 광고되지 않은 QTI core와 software-fence는 들어가지 않는다. `vdec-lowlatency`도 Qualcomm profile에는 들어가지 않는다.

### 3.4 MediaTek 프로필 순서

ULL off의 option profile:

1. `android-standard`: FEATURE가 있으면 standard-only
2. `mtk-legacy`: `vdec-lowlatency=1`만 적용

그 뒤 renderer가 base를 정확히 한 번 시도한다. `mtk-legacy`는 실험 toggle의 일부가 아니라 Android standard를 광고하지 않거나 standard configure가 실패한 MTK BSP를 위한 기존 호환성 baseline이다.

ULL on의 option profile:

1. `mtk-experimental-full`: standard + legacy + 광고된 MTK vendor keys + scheduler
2. `mtk-standard-legacy-performance`: standard + legacy + scheduler
3. `mtk-standard-performance`: standard + scheduler
4. `android-standard`: standard-only
5. `mtk-legacy-performance`: legacy + scheduler
6. `mtk-legacy`: legacy-only
7. `performance-only`: operating-rate 또는 priority

그 뒤 renderer가 base를 정확히 한 번 시도한다.

`mtk-experimental-full`의 exact vendor key/value matrix는 다음과 같다.

| 의미 | primary key | alternate alias | 값 |
|---|---|---|---:|
| low-latency path | `vendor.mtk.vdec.low-latency.mode` | 없음 | 1 |
| disable idle | `vendor.mtk.vdec.disable-idle` | 없음 | 1 |
| vsync adjust | `vendor.mtk.vdec.vsync.adjust.enable` | 없음 | 0 |
| explicit ULL | `vendor.mtk.vdec.ultra-low-latency` | 없음 | 1 |
| preload frames | `vendor.mtk.vdec.preload.frame.count` | 없음 | 0 |
| input queue depth | `vendor.mtk.vdec.input.max.queue.depth` | 없음 | 2 |
| output queue depth | `vendor.mtk.vdec.output.max.queue.depth` | 없음 | 2 |
| fetch timeout | `vendor.mtk.vdec.buffer.fetch.timeout.ms` | `vendor.mtk.vdec.buffer.fetch.timeout.ms.value` | 2 |
| guard interval | `vendor.mtk.vdec.bq.guard.interval.time` | `vendor.mtk.vdec.bq.guard.interval.time.value` | 2 |
| CPU boost | `vendor.mtk.vdec.cpu.boost.mode` | `vendor.mtk.vdec.cpu.boost.mode.value` | 1 |
| DVFS mode | `vendor.mtk.vdec.dvfs.mode` | 없음 | 1 |
| DVFS level | `vendor.mtk.vdec.dvfs.level` | 없음 | 1 |

API 31+에서는 광고된 exact key만 넣는다. primary와 alternate가 모두 광고되면 `.value` alternate를 우선하고 한 alias만 설정한다. explicit ULL은 ULL on일 때만 값 1로 넣고 off일 때 key 자체를 생략한다. DerFlacco의 generic `media.low-latency.enable`/`vendor.low-latency.enable`은 source와 효과가 불명확하므로 가져오지 않는다.

API 26-30 또는 API 31+ enumeration failure에서는 위 speculative `vendor.mtk.*` bundle을 기본 활성화하지 않는다. standard, `vdec-lowlatency`, scheduler만 사용한다. 별도의 OMX.MTK 실기기 검증으로 exact decoder/BSP가 확인된 경우에만 후속 allowlist 변경으로 vendor bundle을 연다.

다음 네 key는 제거하고 다시 추가하지 않는다.

- `vendor.mtk.vdec.nvop.skip`
- `vendor.mtk.vdec.skip.mode`
- `vendor.mtk.vdec.drop.nonref.frame`
- `vendor.mtk.vdec.frame-drop.policy`

`KEY_OPERATING_RATE`와 `KEY_PRIORITY`를 동시에 넣지 않는다. 기존 helper 판단상 max operating rate가 안전하면 operating-rate 하나만, 아니면 API 23+에서 priority 하나만 사용한다.

## 4. 범위와 비범위

### 포함

- Android standard/QTI/MTK low-latency option 계획과 fallback
- vendor parameter capability snapshot/cache
- exact option matrix unit test
- bounded configure/recovery downgrade
- ULL preference 설명 정정
- deterministic diagnostics
- S25+ 실기기 AVC/HEVC/AV1 검증
- 접근 가능한 MTK C2/OMX 기기 검증

### 제외

- DerFlacco Async decoder 구조 이식
- FSR/OpenGL, CPU affinity, overlay, native build 변경
- network/jitter/frame pacing 전면 개편
- Fully External Display, Presentation, controller/input/mouse lifecycle 변경
- unrelated 5개 baseline unit-test 실패 수정
- 새 production debug UI 또는 임시 우회 버튼
- 지원 증거 없는 SoC 모델 문자열 hardcode

## 5. 구현 작업

### Task 0: 기준선 고정과 변경 경계 검사

**Files:**

- Inspect: `AGENTS.md`
- Inspect: `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
- Inspect: `app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java`
- Test report: `app/build/test-results/testNonRoot_gameDebugUnitTest/`

- [ ] `git status --short --untracked-files=all`, `git branch --show-current`, `git rev-parse HEAD`를 실행해 branch=`fix-codec-low-latency-compatibility`, HEAD=`877c5c22...`를 확인한다. 구현 전 허용되는 유일한 상태 항목은 사용자가 요청한 `?? .codex/moonlight-codec-low-latency-implementation-plan.md`이며, 그 밖의 source change는 없어야 한다.
- [ ] `git check-ignore -v AGENTS.md`로 local-only 파일임을 재확인한다.
- [ ] 위 106/5 baseline을 테스트 결과 XML에서 다시 추출해 구현 시작 로그에 남긴다.
- [ ] `git diff 3397ec77...HEAD -- app/src/main/java/com/limelight/binding/video`로 외부-display commit과 codec 파일의 겹치는 변경을 확인하고 최소 line edit 경계를 정한다.
- [ ] 이 Task는 production 변경이나 commit을 만들지 않는다.

### Task 1: 실패하는 profile-planner contract test 작성

**Files:**

- Create: `app/src/main/java/com/limelight/binding/video/LowLatencyProfilePlanner.java`
- Create: `app/src/test/java/com/limelight/binding/video/LowLatencyProfilePlannerTest.java`

- [ ] 먼저 테스트만 작성한다. 최소 test method는 다음과 같다.

```java
@Test public void standardOffReturnsAndroidStandardOptionProfileOnly() { }
@Test public void qtiS25UsesOnlyAdvertisedOutputFenceFamily() { }
@Test public void qtiAdvertisedCoreIncludesTrueCoreOnlyFallback() { }
@Test public void qtiNeverMixesSoftwareAndOutputFenceFamilies() { }
@Test public void qtiNeverUsesVdecLowLatency() { }
@Test public void mtkOffNeverSetsExperimentalUltraLowLatency() { }
@Test public void mtkOnSetsUltraLowLatencyToOneWhenAdvertised() { }
@Test public void mtkProfilesRemoveSkipAndDropKeys() { }
@Test public void profilesAreDeduplicatedAndBounded() { }
```

- [ ] S25 fixture는 실제 probe 결과를 그대로 사용한다.

```java
Set<String> s25VendorParameters = Set.of(
    "vendor.qti-ext-dec-picture-order.enable",
    "vendor.qti-ext-output-fence.enable",
    "vendor.qti-ext-output-fence.fence_type"
);
```

- [ ] MTK fixture는 advertised/unadvertised 두 경우와 `c2.mtk`, `OMX.MTK` 대소문자 두 prefix를 포함한다.
- [ ] 다음 명령으로 class/method 부재 때문에 실패하는 것을 확인한다.

```powershell
.\gradlew.bat :app:testNonRoot_gameDebugUnitTest `
  --tests com.limelight.binding.video.LowLatencyProfilePlannerTest `
  --console=plain
```

- [ ] 최소 immutable model, ordered map, duplicate 제거만 구현한다. 아직 `MediaCodecHelper`에 연결하지 않는다.
- [ ] 다시 targeted test를 실행해 model-level test를 통과시킨다.
- [ ] Commit: `git commit -m "Add testable codec low-latency profile planner"`

### Task 2: capability snapshot과 Qualcomm profile 구현

**Files:**

- Modify: `app/src/main/java/com/limelight/binding/video/LowLatencyProfilePlanner.java`
- Modify: `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
- Modify: `app/src/test/java/com/limelight/binding/video/LowLatencyProfilePlannerTest.java`
- Create: `app/src/test/java/com/limelight/binding/video/MediaCodecLowLatencyCapabilityTest.java`

- [ ] API 31+ vendor parameter snapshot test를 먼저 작성한다. 이름은 `Locale.US` lowercase로 normalize하며 결과 set은 immutable이어야 한다.
- [ ] probe 성공 결과는 decoder name별로 cache하고, codec instance는 `finally`에서 반드시 release한다.
- [ ] probe 실패는 `enumerationAvailable=false`로 구분한다. “성공했지만 empty”와 “probe 실패”를 같은 상태로 처리하지 않는다.
- [ ] package-private `VendorParameterProvider`를 주입 가능하게 두고 production provider만 `MediaCodec.createByCodecName()`을 호출한다. unit test는 fake provider로 success/empty/failure/release contract를 재현해 실제 codec이나 static mocking에 의존하지 않는다.
- [ ] `knownVendorLowLatencyOptions`의 boolean-only 판단을 snapshot 기반 판단으로 교체하거나, 기존 RFI 판단용 목록에 picture/fence constants를 추가하되 source of truth는 planner constants 하나로 유지한다.
- [ ] QTI test를 먼저 확장해 다음 exact contract를 실패시키고 확인한다.

```java
assertFalse(full.options.containsKey("vdec-lowlatency"));
assertFalse(full.options.containsKey("vendor.qti-ext-dec-low-latency.enable"));
assertFalse(full.options.containsKey("vendor.qti-ext-output-sw-fence-enable.value"));
assertEquals(1, full.options.get("vendor.qti-ext-output-fence.enable").intValue());
assertEquals(1, full.options.get("vendor.qti-ext-output-fence.fence_type").intValue());
```

- [ ] planner에 3.3의 QTI 순서를 구현한다. `MediaFormat.KEY_LOW_LATENCY` 상수를 사용하고, output-fence pair는 둘 다 광고될 때만 넣는다.
- [ ] API 31+에서 광고되지 않은 QTI key를 profile map에 넣는 경로가 없음을 테스트한다.
- [ ] API 26-30 legacy QTI에서는 core/picture fallback을 유지하되 fence는 넣지 않는 테스트를 추가한다.
- [ ] 원래 회귀의 핵심인 `qtiAdvertisedCoreIncludesTrueCoreOnlyFallback`가 QTI core 외 key가 없는 map을 검사하도록 한다.
- [ ] targeted test를 실행한다.

```powershell
.\gradlew.bat :app:testNonRoot_gameDebugUnitTest `
  --tests com.limelight.binding.video.LowLatencyProfilePlannerTest `
  --tests com.limelight.binding.video.MediaCodecLowLatencyCapabilityTest `
  --console=plain
```

- [ ] Commit: `git commit -m "Gate Qualcomm low-latency profiles by codec capabilities"`

### Task 3: MTK selective port와 유한 fallback 구현

**Files:**

- Modify: `app/src/main/java/com/limelight/binding/video/LowLatencyProfilePlanner.java`
- Modify: `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
- Modify: `app/src/test/java/com/limelight/binding/video/LowLatencyProfilePlannerTest.java`

- [ ] 먼저 attempt 0-7의 profile name/key contract를 table-driven test로 작성한다.
- [ ] ULL off에서 `vendor.mtk.*` experimental key와 ULL key가 전혀 나오지 않는 테스트를 추가한다.
- [ ] ULL on + advertised key에서 ULL value가 정확히 `1`이고 `0`인 profile이 하나도 없는 테스트를 추가한다.
- [ ] primary alias만, `.value` alias만, 두 alias 모두, enumeration success-empty, enumeration failure fixture를 각각 만든다. 두 alias가 모두 광고되면 `.value` 하나만 선택하고, failure/empty에서는 speculative `vendor.mtk.*`가 0개인지 검사한다.
- [ ] API 26과 API 30 fixture에서는 ULL on이어도 speculative `vendor.mtk.*`가 없고 standard/`vdec-lowlatency`/scheduler만 남는지 검사한다.
- [ ] `media.low-latency.enable`과 `vendor.low-latency.enable`을 전체 MTK profile에서 제외하는 테스트를 추가한다.
- [ ] skip/drop 네 key가 전체 profile list 어디에도 없는 테스트를 추가한다.
- [ ] 모든 capability 조합에서 option `profiles.size() <= 11`이며 마지막 profile 뒤 renderer가 base를 한 번만 시도해 총 configure 횟수가 12회를 넘지 않음을 검사한다.
- [ ] DerFlacco `bb54ded`를 cherry-pick하지 않고 3.4의 단계만 현재 planner 구조에 재구현한다.
- [ ] current `MediaCodecHelper.java:636-673`의 MTK block을 planner 적용으로 교체해 `setNewOption=true` 무한 반복 가능성을 제거한다.
- [ ] `vdec-lowlatency`를 모든 지원 API의 MTK/Amlogic/Amazon compatibility baseline으로 제한하고 Qualcomm path에서는 제거한다. API 31+ enumeration success-empty/failure fixture에서도 이 key만 유지되고 speculative vendor key는 없는지 테스트한다.
- [ ] targeted test를 실행한다.
- [ ] Commit: `git commit -m "Bound and strengthen MediaTek low-latency fallbacks"`

### Task 4: renderer에 명시적 profile 이름, hard limit, recovery downgrade 연결

**Files:**

- Modify: `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
- Modify: `app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java`
- Create: `app/src/test/java/com/limelight/binding/video/MediaCodecDecoderRecoveryProfileTest.java`

- [ ] helper의 현재 boolean 반환을 다음 의미가 분명한 result로 바꾸는 test/compile contract를 먼저 만든다.

```java
static final class AppliedLowLatencyOptions {
    final String profileName; // e.g. qti-experimental-full, android-standard
    final boolean hasOptions;
}
```

- [ ] renderer log가 attempt number, profile name, exact applied option keys를 함께 기록하도록 한다.
- [ ] 무한 loop를 다음 유한 구조로 바꾼다.

```java
private static final int MAX_DECODER_CONFIGURATION_ATTEMPTS = 12;

int cappedStartTry = Math.min(startTry, MAX_DECODER_CONFIGURATION_ATTEMPTS - 1);
for (int tryNumber = cappedStartTry;
     tryNumber < MAX_DECODER_CONFIGURATION_ATTEMPTS;
     tryNumber++) {
    boolean forceBase = tryNumber == MAX_DECODER_CONFIGURATION_ATTEMPTS - 1;
    // The renderer labels and owns base; the planner never returns an empty profile.
}
```

- [ ] planner option list가 소진되면 renderer가 즉시 base를 한 번 시도하고 종료한다. helper bug로 option이 계속 반환돼도 마지막 예약 attempt는 helper를 호출하지 않고 강제로 base를 구성한다.
- [ ] `base` configure 실패 또는 hard limit 도달 시 `-5`로 종료하고 severe log에 decoder/profile/capability를 남긴다.
- [ ] successful profile index/name을 renderer field에 저장한다.
- [ ] recoverable restart/reset은 동일 format을 먼저 유지한다. non-transient failure가 full recreation까지 승격될 때는 initial configure list의 다음 index를 사용하지 않고 별도 `RUNTIME_RECOVERY` plan을 만든다.
- [ ] active profile이 experimental/vendor/performance profile이면 recovery plan은 `android-standard`(FEATURE 지원 시) 다음 renderer-owned base만 허용한다. active profile이 이미 `android-standard`이면 즉시 base만 허용한다. alternate fence 또는 vendor-only profile을 runtime downgrade로 다시 도입하지 않는다.
- [ ] 다음 pure behavior test를 작성한다.

```java
@Test public void runtimeRecoveryFromExperimentalUsesStandardOnlyThenBase() { }
@Test public void runtimeRecoveryFromStandardUsesBaseOnly() { }
@Test public void runtimeRecoveryNeverIntroducesVendorOptions() { }
@Test public void transientRecoveryKeepsCurrentProfile() { }
@Test public void configurationAttemptsStopAtHardLimit() { }
```

- [ ] 외부-display 관련 renderer field/method와 frame pacing loop는 건드리지 않았는지 diff를 검토한다.
- [ ] targeted tests와 Java compile을 실행한다.

```powershell
.\gradlew.bat :app:testNonRoot_gameDebugUnitTest `
  --tests 'com.limelight.binding.video.*LowLatency*' `
  --tests com.limelight.binding.video.MediaCodecDecoderRecoveryProfileTest `
  --console=plain
.\gradlew.bat :app:compileNonRoot_gameDebugJavaWithJavac --console=plain
```

- [ ] Commit: `git commit -m "Bound codec configuration and downgrade unsafe profiles"`

### Task 5: preference 의미와 dead transport field 정리

**Files:**

- Modify: `app/src/main/res/xml/preferences.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `app/src/main/res/values-ru/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`
- Modify: `app/src/main/java/com/limelight/Game.java`
- Modify: `app/src/main/java/com/limelight/nvstream/StreamConfiguration.java`
- Modify: `app/src/test/java/com/limelight/profiles/ProfilesOverlayTest.java`

- [ ] preference key `checkbox_ultra_low_latency`와 default false는 유지해 사용자 설정 호환성을 보존한다.
- [ ] 표시 title을 `Vendor ultra-low-latency optimizations (experimental)`로 바꾼다.
- [ ] English summary를 다음 의미로 바꾼다: `Android standard low latency is enabled automatically when supported. This option adds capability-checked vendor decoder optimizations.`
- [ ] 다섯 locale resource가 “Snapdragon 세대 제한”이라는 오래된 설명을 유지하지 않도록 같은 의미로 갱신한다.
- [ ] Profiles overlay persistence test를 유지하고 title/summary 변경이 preference key를 바꾸지 않았음을 확인한다.
- [ ] `StreamConfiguration.enableUltraLowLatency`, builder setter/getter 및 `Game.java`의 setter call은 runtime consumer가 없음을 `rg`로 재확인한 뒤 제거한다. renderer의 실제 source of truth는 `PreferenceConfiguration.enableUltraLowLatency` 하나만 남긴다.
- [ ] resource compile과 targeted profile test를 실행한다.
- [ ] Commit: `git commit -m "Clarify experimental vendor low-latency setting"`

### Task 6: 진단 품질 강화

**Files:**

- Modify: `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
- Modify: `app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java`
- Modify: `app/src/test/java/com/limelight/binding/video/LowLatencyProfilePlannerTest.java`
- Create: `app/src/main/java/com/limelight/binding/video/DecoderLatencySampler.java`
- Create: `app/src/test/java/com/limelight/binding/video/DecoderLatencySamplerTest.java`

- [ ] 다음을 한 번씩 deterministic하게 log한다.

  - decoder name과 MIME
  - `FEATURE_LowLatency`
  - vendor enumeration 성공/실패
  - low-latency 관련 광고 parameter만 filtering한 set
  - ULL preference state
  - 각 attempt의 profile name과 option map
  - 성공 profile과 fallback 이유

- [ ] crash report에는 SOC 값과 함께 active profile name/index를 추가한다. 전체 vendor parameter dump는 다시 켜지 않고 관련 key만 남긴다.
- [ ] `safeSet()` 성공 log를 “codec이 key를 지원함”으로 표현하지 않는다. `requested`와 `advertised`를 명시적으로 구분한다.
- [ ] log formatting test가 동일 capability에서 동일 순서를 내는지 확인한다.
- [ ] 현재 aggregate average만으로 p50/p95/p99를 만들 수 없으므로, `updateDecodeLatencyStats()`에서 enqueue-to-dequeue microseconds를 기록하는 고정 크기 sampler를 추가한다. `int[16384]` circular buffer를 사용하고 per-frame allocation·sorting·log를 하지 않는다.
- [ ] stream stop 또는 crash-report snapshot에서만 복사·정렬해 nearest-rank p50/p95/p99를 계산한다. 이 값이 input-to-photon이 아니라 decoder enqueue-to-dequeue임을 label에 명시한다.
- [ ] 다음 sampler test를 먼저 작성한다.

```java
@Test public void percentilesUseMicrosecondsAndNearestRank() { }
@Test public void circularBufferKeepsMostRecentSamples() { }
@Test public void emptySamplerReturnsNoMeasurement() { }
@Test public void recordingBeyondCapacityKeepsFixedSampleCount() { }
```

- [ ] Commit: `git commit -m "Add deterministic codec low-latency diagnostics"`

### Task 7: 정적 검증과 전체 build/test

**Files:**

- Verify only: all modified files
- Reports: `app/build/reports/tests/testNonRoot_gameDebugUnitTest/`

- [ ] 금지 패턴을 검색한다.

```powershell
rg -n 'for \(int tryNumber = 0;;' app/src/main/java/com/limelight/binding/video
rg -n 'vendor\.mtk\.vdec\.(nvop\.skip|skip\.mode|drop\.nonref\.frame|frame-drop\.policy)' app/src/main/java
rg -n 'vendor\.mtk\.vdec\.ultra-low-latency"\s*,\s*0' app/src/main/java
rg -n 'vdec-lowlatency' app/src/main/java/com/limelight/binding/video
```

첫 세 검색은 no match가 성공이다. 마지막 검색은 MTK/Amlogic/Amazon scoped path만 나와야 한다.

- [ ] 다음 검증을 순서대로 실행한다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'

.\gradlew.bat :app:testNonRoot_gameDebugUnitTest `
  --tests 'com.limelight.binding.video.*LowLatency*' `
  --tests com.limelight.binding.video.DecoderLatencySamplerTest `
  --tests com.limelight.profiles.ProfilesOverlayTest `
  --console=plain

.\gradlew.bat :app:compileNonRoot_gameDebugJavaWithJavac --console=plain
.\gradlew.bat :app:assembleNonRoot_gameDebug --console=plain
.\gradlew.bat :app:testNonRoot_gameDebugUnitTest --console=plain
git diff --check
git status --short
```

- [ ] 전체 test 결과는 기존 5 failures만 허용한다. 새 failure가 있으면 commit하지 않는다.
- [ ] `AGENTS.md`, repo-local `docs/`, build output이 staged되지 않았는지 확인한다.
- [ ] Commit이 task별로 분리돼 있고 외부-display code에 unrelated diff가 없는지 `git log --stat 877c5c22..HEAD`로 검토한다.

### Task 8: Galaxy S25+ 새 브랜치 실기기 검증

**Files:**

- APK: `app/build/outputs/apk/nonRoot_game/debug/app-nonRoot_game-arm64-v8a-debug.apk`
- Temporary logs only: `$env:TEMP\moonlight-low-latency\`
- No tracked source changes unless a reproducible defect is found

- [ ] 기기와 APK를 다시 확인한다.

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$deviceSerial = 'R3CY20LMGDF'
$packageName = 'com.limelight.noirdebug'

& $adb devices -l
& $adb -s $deviceSerial shell getprop ro.product.model
& $adb -s $deviceSerial shell getprop ro.soc.manufacturer
& $adb -s $deviceSerial shell getprop ro.soc.model
& $adb -s $deviceSerial install -r `
  'app\build\outputs\apk\nonRoot_game\debug\app-nonRoot_game-arm64-v8a-debug.apk'
& $adb -s $deviceSerial shell dumpsys package $packageName |
  Select-String 'versionCode=|versionName=|debuggable'
```

- [ ] app을 cold start하고 FATAL exception이 없는지 확인한다.
- [ ] 실제 paired host에서 AVC, HEVC, AV1 각각 ULL off/on을 1080p60으로 120초씩 5회 교차 실행한다: OFF-A, ON-A, OFF-B, ON-B 순서를 반복해 thermal/order bias를 줄인다.
- [ ] 가능하면 1440p120 또는 기기/host가 안정적으로 제공하는 최고 120 FPS mode로 15분 thermal soak를 추가한다.
- [ ] 각 run에서 다음을 수집한다.

  - 선택된 decoder name
  - advertised standard/vendor capability
  - 요청 profile과 exact key
  - configure/start 성공 attempt
  - fallback/recreation/watchdog/CodecException
  - sampler가 보고한 decoder enqueue-to-dequeue latency p50/p95/p99
  - frame drop과 stutter count
  - thermal status와 sustained performance 변화

- [ ] 예상 contract를 확인한다.

  - ULL off: `android-standard`, 실패 시 `base`
  - ULL on: 광고된 output-fence pair 사용
  - unadvertised QTI core와 software-fence 없음
  - AVC/HEVC/AV1 모두 frame output 성공
  - 무한 retry, codec reset loop, watchdog 없음

- [ ] 성능 판정은 configure 성공과 분리한다. 다섯 run 중앙값 기준으로 ON의 p95 decoder latency가 OFF보다 `max(0.5 ms, 10%)` 이상 악화되거나 frame-drop rate가 0.1 percentage point 이상 악화되면 해당 experimental profile을 채택하지 않는다. 개선을 주장하려면 p50 또는 p95가 5% 이상 개선되고 다른 안정성 지표가 악화되지 않아야 한다.
- [ ] 실제 input-to-photon 개선을 주장할 때는 app timestamp가 아니라 240/960 FPS high-speed camera 또는 동등한 외부 측정을 사용한다.
- [ ] raw logs와 측정표는 repo 밖에 보존한다. 측정 없이 “지연 개선 완료”라고 결론내리지 않는다.

### Task 9: MTK 실기기 검증과 release gate

**Files:**

- Temporary logs only: `$env:TEMP\moonlight-low-latency\mtk\`
- No tracked source changes unless a reproducible defect is found

- [ ] capability-gated MTK 성능 주장을 위해 최소 한 대의 `c2.mtk` 기기를 확보한다. API 26-30 speculative vendor bundle을 후속 allowlist로 활성화하려면 `OMX.MTK` legacy 기기 검증도 필수이며, 확보하지 못하면 해당 bundle은 비활성 상태로 남긴다.
- [ ] 각 기기에서 model, SoC, Android API, decoder name, vendor parameter set을 먼저 기록한다.
- [ ] H.264/HEVC를 60 FPS와 지원 시 120 FPS로 ULL off/on 검증한다. AV1 hardware decoder가 있는 기기에서만 AV1을 추가한다.
- [ ] attempt 0-7 중 성공 profile, fallback 종료, reconnect, stream stop/start, codec recovery를 검증한다.
- [ ] ULL off에서 experimental MTK vendor key가 요청되지 않고, ULL on에서 광고된 경우 ULL=1이 요청되는지 확인한다.
- [ ] skip/drop key가 log에 없고 화면 손상, non-reference frame 손실, stutter가 증가하지 않는지 확인한다.
- [ ] 15분 thermal soak와 S25+와 동일한 성능 판정 규칙을 적용한다.
- [ ] MTK 실기기가 없으면 code-path/unit-test 완료로만 보고하고 “MTK 성능 검증 완료” 또는 “획기적 개선”이라고 표시하지 않는다. release note에서 experimental 상태를 유지한다.

## 6. 최종 승인 기준

다음 항목을 모두 만족해야 구현 완료로 판정한다.

- [ ] 작업 commit은 `877c5c22`의 자손이다.
- [ ] Android standard `KEY_LOW_LATENCY`가 FEATURE 지원 decoder의 기본 경로다.
- [ ] ULL preference off에서도 standard low latency는 유지된다.
- [ ] API 31+에서 unadvertised QTI/MTK vendor key를 요청하지 않는다.
- [ ] QTI fence family를 같은 format에 혼합하지 않는다.
- [ ] 광고된 QTI core가 있을 때 진짜 QTI-only fallback이 존재한다.
- [ ] S25+에서 unadvertised QTI core/software-fence를 요청하지 않는다.
- [ ] Qualcomm path에 `vdec-lowlatency`가 없다.
- [ ] MTK ULL off는 vendor ULL key를 생략하고, on은 지원될 때 값 1을 쓴다.
- [ ] MTK skip/drop 네 key가 제거됐다.
- [ ] option profile은 최대 11개이고, renderer-owned base를 포함한 configure attempt가 최대 12회 안에 종료한다.
- [ ] 모든 새 targeted unit test가 통과한다.
- [ ] 전체 suite는 기존 다섯 실패 외 새 실패가 없다.
- [ ] `assembleNonRoot_gameDebug`가 성공한다.
- [ ] S25+ AVC/HEVC/AV1 실제 stream이 configure/start/render되고 recovery loop가 없다.
- [ ] MTK 성능 주장은 실제 MTK hardware A/B 증거가 있을 때만 한다.
- [ ] Fully External Display와 input/mouse lifecycle에 unrelated diff가 없다.
- [ ] `AGENTS.md`, repo-local `docs/`, build artifact가 commit에 없다.

## 7. 구현 후 보고 형식

최종 보고는 다음 네 범주를 분리한다.

1. **코드 계약:** 어떤 profile/key/fallback을 구현했는가
2. **자동 검증:** targeted test, compile, assemble, 전체 suite baseline 비교
3. **기기 호환성:** configure/start/render 성공 여부와 실제 decoder/profile
4. **성능 증거:** A/B 측정값과 thermal/stability 결과

“key가 format log에 있음”, “configure 성공”, “실제 지연 개선”을 같은 증거로 취급하지 않는다.
