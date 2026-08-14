# Artemis Low Latency 독립 Debug 빌드 설계

## 문서 성격

- 유형: 구현 설계 및 검증 기준
- 대상: 이 저장소에서 Android APK를 빌드·설치하는 개발자
- 목표: 기존 Artemis 정식 앱과 현재 Diana debug 앱을 덮어쓰지 않는 저지연 검증용 debug 앱을 만든다.

## 결정

기존 `nonRoot_game` flavor를 그대로 사용하고, 별도의 `lowLatencyDebug` build type을 추가한다.

| 항목 | 값 |
|---|---|
| 표시 이름 | `Artemis Low Latency (Debug)` |
| application ID | `com.limelight.artemis.lowlatency.debug` |
| 서명 | 기존 Android debug signing config |
| debuggable | `true` |
| 기반 설정 | 현재 `debug` build type을 `initWith`로 상속 |
| 대상 variant | `nonRoot_gameLowLatencyDebug` |

이 package ID는 현재 정식 앱 `com.limelight.noir` 및 기존 debug 앱 `com.limelight.noirdebug`와 다르므로 세 앱이 동시에 설치될 수 있다. Java namespace와 Activity class는 기존 `com.limelight`를 유지한다.

## 구현 범위

`app/build.gradle`에 한 개의 build type만 추가한다.

- `initWith debug`
- `applicationIdSuffix ".artemis.lowlatency.debug"`
- `signingConfig signingConfigs.debug`
- `debuggable true`
- `app_label`, `app_label_root`, `app_label_game`을 `Artemis Low Latency (Debug)` 계열로 재정의
- `matchingFallbacks = ['debug']`

codec, preference, manifest component, native code, external-display, input/mouse 및 streaming 동작은 변경하지 않는다. 기존 `externalDisplayStandard` flavor와 `adbTest` build type도 변경하지 않는다.

## 데이터와 페어링

별도 application ID이므로 SharedPreferences, profile, certificate 및 Apollo pairing 상태도 기존 앱과 공유하지 않는다. 실제 스트리밍을 하려면 새 앱에서 Apollo PIN 승인이 다시 필요하다. 이번 설치 단계는 pairing을 우회하거나 기존 앱의 private data를 복사하지 않는다.

## 검증 기준

1. 기존 `assembleNonRoot_gameDebug`가 계속 성공한다.
2. 새 variant의 Java compile, native build 및 ARM64 APK assemble이 성공한다.
3. APK manifest에서 다음을 확인한다.
   - package: `com.limelight.artemis.lowlatency.debug`
   - label: `Artemis Low Latency (Debug)`
   - debuggable: `true`
   - launcher Activity: `com.limelight.PcView`
4. Galaxy S25+에 기존 두 package를 삭제하지 않고 설치한다.
5. 세 package가 동시에 존재하는지 확인한다.
6. 새 앱을 cold start하고 FATAL, ANR, `UnsatisfiedLinkError`, `VerifyError`가 없는지 확인한다.
7. 소스 변경은 build identity 한 파일로 한정하고 전체 unit baseline 및 diff/scope 검사를 다시 실행한다.

## 제외 범위

- Apollo PIN 등록 또는 우회
- paired-host AVC/HEVC/AV1 스트림 실행
- MTK 실기기 성능 주장
- input-to-photon 또는 latency 개선 주장
- 기존 앱 데이터 마이그레이션

## 대안과 기각 이유

- 표시 이름만 변경: package가 같아 기존 debug 앱을 교체하므로 충돌 방지 요구를 만족하지 못한다.
- 새 product flavor 추가: 별도 manifest/source set과 variant 조합이 늘어나 이 용도에는 과도하다.
- APK 사후 재패키징: manifest, provider authority, signature 및 재현성을 훼손할 수 있어 사용하지 않는다.
