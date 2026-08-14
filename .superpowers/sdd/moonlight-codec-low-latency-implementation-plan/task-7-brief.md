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

#### Controller verification addendum

- Verification is pinned to `a126f0f3223bc3e6977af6fb86e1f3c1343a8da3`; Task 7 creates no source commit.
- Run all commands with `--rerun-tasks` where a stale Gradle result could hide a regression. Parse JUnit XML rather than relying only on Gradle exit status; expected final suite is 193 tests, exactly the frozen five failures, zero errors/skips.
- In addition to the listed sequence, run `:app:lintNonRoot_gameDebug` and `:app:externalNativeBuildNonRoot_gameDebug`. Record existing warnings separately from new blocking errors.
- Inspect `assembleNonRoot_gameDebug` outputs and verify the ARM64 install target exists. Audit every generated split/universal APK's native-library ABI directories and ensure the expected `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` variants are represented by the build outputs; do not install in Task 7.
- Static guards also verify: capability/requested wording, exact deterministic log prefixes, one raw dequeue plus five wrappers and no delayed stats calls, no broad codec calls under `decoderLatencyLock`, no unbounded configuration loop, and no MTK forbidden keys/value zero.
- Cumulative diff/commit audit is `877c5c22a11ee783fc2b33bc4c151587ee36cfc6..HEAD`. Confirm each task commit ancestry/message, full range contains only authorized low-latency/preference files, and no protected external-display/Presentation/Surface/input/mouse/frame-pacing policy diff beyond the explicitly authorized renderer latency/configuration sites.
- Confirm index is empty and visible status contains only the intentional untracked `.codex/` plan. `AGENTS.md`, `.superpowers/`, repo-local `docs/`, and `app/build/` must not be tracked/staged.
