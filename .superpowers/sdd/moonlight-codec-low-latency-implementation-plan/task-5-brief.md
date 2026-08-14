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

