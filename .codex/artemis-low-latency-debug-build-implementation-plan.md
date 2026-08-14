# Artemis Low Latency 독립 Debug 빌드 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 Artemis 정식 앱과 Diana debug 앱을 덮어쓰지 않는 `Artemis Low Latency (Debug)` ARM64 APK를 빌드하고 Galaxy S25+에 공존 설치한다.

**Architecture:** `nonRoot_game` product flavor와 모든 streaming 코드는 그대로 두고, 기존 `debug` 설정을 상속하는 `lowLatencyDebug` build type만 추가한다. 새 build type은 application ID와 label만 격리하며, 별도 package data를 사용하므로 Apollo pairing을 공유하거나 우회하지 않는다.

**Tech Stack:** Gradle Android application plugin, Groovy build DSL, Android SDK build-tools, ADB, PowerShell 7.

## Global Constraints

- 표시 이름은 `Artemis Low Latency (Debug)`이다.
- 최종 application ID는 `com.limelight.artemis.lowlatency.debug`이다.
- 새 variant는 Android debug key로 서명되고 `debuggable=true`여야 한다.
- `nonRoot_game`의 native build와 manifest를 재사용한다.
- 기존 `debug`, `adbTest`, `release`, `externalDisplayStandard` 설정은 변경하지 않는다.
- codec, preference, native source, external-display, input/mouse 및 frame-pacing 코드는 변경하지 않는다.
- `.codex/`, `.superpowers/`, `AGENTS.md`, `app/build/`은 stage/commit하지 않는다.
- Apollo PIN, paired-host private data 및 기존 app data를 복사하거나 우회하지 않는다.

---

### Task 1: 독립 lowLatencyDebug build type 추가

**Files:**

- Modify: `app/build.gradle:114-173`
- Verify: `app/build/outputs/apk/nonRoot_game/lowLatencyDebug/`
- Test reports: `app/build/test-results/testNonRoot_gameLowLatencyDebugUnitTest/`

**Interfaces:**

- Consumes: 기존 `debug` build type, `nonRoot_game` flavor, debug signing config.
- Produces: Gradle variant `nonRoot_gameLowLatencyDebug`, package `com.limelight.artemis.lowlatency.debug`, label `Artemis Low Latency (Debug)`.

- [ ] **Step 1: 기준 상태와 기존 variant를 고정한다**

```powershell
git branch --show-current
git rev-parse HEAD
git status --short
git diff --cached --name-only
.\gradlew.bat tasks --all --console=plain |
  Select-String 'assembleNonRoot_game(Debug|AdbTest)|compileNonRoot_gameDebugJavaWithJavac'
```

Expected: branch `fix-codec-low-latency-compatibility`, HEAD `6ff9b36d3c508f58351b4dfa78bb1c8bcb62ead8`, index empty, visible untracked state only `.codex/`; low-latency variant task는 아직 없다.

- [ ] **Step 2: 새 variant가 없어서 RED임을 확인한다**

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat :app:assembleNonRoot_gameLowLatencyDebug --console=plain
```

Expected: Gradle이 `assembleNonRoot_gameLowLatencyDebug` task를 찾지 못해 실패한다. Java compile, SDK 또는 native 오류를 RED 근거로 사용하지 않는다.

- [ ] **Step 3: 최소 build type을 구현한다**

`app/build.gradle`의 `debug` 다음에 아래 block을 추가한다.

```groovy
        lowLatencyDebug {
            initWith debug
            applicationIdSuffix = ".artemis.lowlatency.debug"
            signingConfig signingConfigs.debug
            debuggable true
            resValue "string", "app_label", "Artemis Low Latency (Debug)"
            resValue "string", "app_label_root", "Artemis Low Latency (Debug Root)"
            resValue "string", "app_label_game", "Artemis Low Latency (Debug Game)"
            matchingFallbacks = ['debug']
        }
```

`initWith debug` 뒤의 suffix와 label은 복제된 Diana 값보다 새 값을 우선하도록 명시한다.

- [ ] **Step 4: Gradle model과 새 variant compile을 GREEN으로 만든다**

```powershell
.\gradlew.bat tasks --all --console=plain |
  Select-String 'assembleNonRoot_gameLowLatencyDebug|compileNonRoot_gameLowLatencyDebugJavaWithJavac|externalNativeBuildNonRoot_gameLowLatencyDebug'
.\gradlew.bat :app:compileNonRoot_gameLowLatencyDebugJavaWithJavac --console=plain --rerun-tasks
.\gradlew.bat :app:externalNativeBuildNonRoot_gameLowLatencyDebug --console=plain --rerun-tasks
```

Expected: 세 task가 존재하고 compile/native build가 성공한다.

- [ ] **Step 5: 기존 variant와 새 ARM64 APK를 모두 assemble한다**

```powershell
.\gradlew.bat :app:assembleNonRoot_gameDebug --console=plain --rerun-tasks
.\gradlew.bat :app:assembleNonRoot_gameLowLatencyDebug --console=plain --rerun-tasks
Get-ChildItem -LiteralPath 'app/build/outputs/apk/nonRoot_game/lowLatencyDebug' -Filter '*.apk' |
  Select-Object Name, Length, LastWriteTimeUtc
```

Expected: 기존 debug assemble이 계속 성공하고, 새 variant가 x86/x86_64/armeabi-v7a/arm64-v8a split APK를 만든다.

- [ ] **Step 6: 새 variant test와 lint 결과를 기준선과 비교한다**

```powershell
.\gradlew.bat :app:testNonRoot_gameLowLatencyDebugUnitTest --console=plain --rerun-tasks
.\gradlew.bat :app:lintNonRoot_gameLowLatencyDebug --console=plain --rerun-tasks
```

Expected: unit XML은 193 tests, 기존 frozen 5 failures, 0 errors/skips이다. Lint는 기존 32 errors/477 warnings만 허용하며 `app/build.gradle` 또는 lowLatencyDebug variant가 새 위치를 추가하면 실패다.

- [ ] **Step 7: scope와 whitespace를 검사한다**

```powershell
git diff --check
git diff -- app/build.gradle
git status --short
```

Expected: tracked 변경은 `app/build.gradle` 하나뿐이고 `.codex/` 외 새 untracked source가 없다.

- [ ] **Step 8: build identity 변경을 commit한다**

```powershell
git add -- app/build.gradle
git diff --cached --check
git diff --cached --name-only
git commit -m "Add isolated low-latency debug build"
```

Expected: commit에는 `app/build.gradle` 한 파일만 포함된다.

---

### Task 2: APK identity, 공존 설치 및 cold start 검증

**Files:**

- Inspect: `app/build/outputs/apk/nonRoot_game/lowLatencyDebug/*arm64-v8a*.apk`
- Temporary evidence: `$env:TEMP\moonlight-low-latency\artemis-isolated\<timestamp>\`
- No tracked source changes

**Interfaces:**

- Consumes: Task 1의 ARM64 APK와 package/application label.
- Produces: APK hash, manifest identity, three-package coexistence 결과, installed package identity 및 cold-start crash evidence.

- [ ] **Step 1: ARM64 APK를 단일 경로로 확정하고 hash를 기록한다**

```powershell
$apkCandidates = Get-ChildItem -LiteralPath 'app/build/outputs/apk/nonRoot_game/lowLatencyDebug' -Filter '*arm64-v8a*.apk'
if ($apkCandidates.Count -ne 1) { throw "Expected one ARM64 APK, found $($apkCandidates.Count)" }
$apk = $apkCandidates[0].FullName
Get-FileHash -Algorithm SHA256 -LiteralPath $apk
```

Expected: 정확히 한 개의 ARM64 APK가 선택된다.

- [ ] **Step 2: APK manifest와 서명을 검사한다**

Android SDK의 `apkanalyzer.bat`, `aapt2.exe`, `apksigner.bat` 위치를 실제 설치된 build-tools/cmdline-tools에서 발견한 뒤 다음을 확인한다.

```text
applicationId = com.limelight.artemis.lowlatency.debug
application-label = Artemis Low Latency (Debug)
debuggable = true
launcher activity class = com.limelight.PcView
signature = Android debug certificate
native ABI = arm64-v8a
```

Expected: 여섯 identity가 모두 일치한다. APK 사후 변조나 재서명은 하지 않는다.

- [ ] **Step 3: 한 대의 대상 S25+만 선택한다**

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$devices = & $adb devices -l
$devices
```

Expected: authorized device가 정확히 하나이고 serial=`R3CY20LMGDF`, model=`SM_S936N`이다. 이후 모든 ADB 명령에 `-s R3CY20LMGDF`를 사용한다.

- [ ] **Step 4: 기존 package를 보존한 채 새 APK를 설치한다**

```powershell
& $adb -s R3CY20LMGDF shell pm list packages |
  Select-String '^package:(com\.limelight\.noir|com\.limelight\.noirdebug)$'
& $adb -s R3CY20LMGDF install -r $apk
& $adb -s R3CY20LMGDF shell pm list packages |
  Select-String '^package:(com\.limelight\.noir|com\.limelight\.noirdebug|com\.limelight\.artemis\.lowlatency\.debug)$'
```

Expected: install은 성공하고 기존 package를 uninstall하지 않는다. 기기에 원래 존재하던 기존 package들과 새 package가 함께 보인다.

- [ ] **Step 5: installed identity를 검증한다**

```powershell
$packageName = 'com.limelight.artemis.lowlatency.debug'
& $adb -s R3CY20LMGDF shell pm path $packageName
& $adb -s R3CY20LMGDF shell dumpsys package $packageName |
  Select-String 'versionCode=|versionName=|DEBUGGABLE|primaryCpuAbi=|lastUpdateTime=|codePath='
& $adb -s R3CY20LMGDF shell cmd package resolve-activity --brief `
  -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $packageName
```

Expected: installed path가 존재하고 debuggable/arm64 identity 및 launcher component가 APK 검사 결과와 일치한다.

- [ ] **Step 6: 새 앱을 cold start하고 crash를 검사한다**

```powershell
& $adb -s R3CY20LMGDF shell am force-stop $packageName
& $adb -s R3CY20LMGDF logcat -b all -c
& $adb -s R3CY20LMGDF shell am start -W `
  -n "$packageName/com.limelight.PcView" `
  -a android.intent.action.MAIN `
  -c android.intent.category.LAUNCHER
Start-Sleep -Seconds 4
$pid = ((& $adb -s R3CY20LMGDF shell pidof $packageName) | Select-Object -First 1).Trim()
& $adb -s R3CY20LMGDF logcat -d --pid $pid -v threadtime |
  Select-String 'FATAL EXCEPTION|ANR in|UnsatisfiedLinkError|VerifyError|Fatal signal'
```

Expected: cold start status가 성공하고 process가 실행되며 fatal signature가 없다.

- [ ] **Step 7: 최종 Git/device 경계를 기록한다**

```powershell
git status --short
git diff --cached --name-only
```

Expected: index는 비어 있고 visible repository status는 `.codex/` local documents뿐이다. Apollo PIN과 실제 stream은 수행하지 않았다고 기록한다.
