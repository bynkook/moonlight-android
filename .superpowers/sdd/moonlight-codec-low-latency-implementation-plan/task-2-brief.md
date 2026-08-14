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

