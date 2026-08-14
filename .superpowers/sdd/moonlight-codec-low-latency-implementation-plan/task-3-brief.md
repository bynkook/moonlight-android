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

