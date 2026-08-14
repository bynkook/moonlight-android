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

