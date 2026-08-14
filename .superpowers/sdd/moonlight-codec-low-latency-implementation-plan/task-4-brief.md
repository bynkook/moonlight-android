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

