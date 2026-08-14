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

