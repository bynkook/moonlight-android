# Task 8 isolated install report

## Result

PASS for isolated APK installation and package coexistence. Real stream validation remains pending by explicit user-controlled sequence.

## Source and artifact

- Branch: `fix-codec-low-latency-compatibility`
- HEAD: `132eea33f510ee94fe5c9bb3a200d152233aff5b`
- APK: `app/build/outputs/apk/nonRoot_game/lowLatencyDebug/app-nonRoot_game-arm64-v8a-lowLatencyDebug.apk`
- SHA-256: `48D2CFE363810D78A2DC0175E8C86D6168E437DB3D6D5077CA6538CFCBDF6C2B`
- Package: `com.limelight.artemis.lowlatency.debug`
- Launcher: `com.limelight.PcView`
- App label: `Artemis Low Latency (Debug)`
- Version: `20.2.6` (`versionCode=57`)
- ABI: `arm64-v8a`
- APK review: `APPROVED`; v1/v2 Android Debug signature verified before installation.

## Device and installation

- Authorized device serial: `R3CY20LMGDF`
- Device model previously revalidated in Task 8: Galaxy S25+ `SM-S936N`, Qualcomm `SM8750`.
- Command boundary: the ARM64 APK was installed with `adb -s R3CY20LMGDF install -r`.
- Installer result: `Success`.
- Installed metadata: `DEBUGGABLE`, `primaryCpuAbi=arm64-v8a`, `versionName=20.2.6`, `versionCode=57`.
- Resolved launcher: `com.limelight.artemis.lowlatency.debug/com.limelight.PcView`.

## Coexistence proof

The following packages were simultaneously present after installation:

- `com.limelight.noir`
- `com.limelight.noirdebug`
- `com.limelight.artemis.lowlatency.debug`

No existing package was uninstalled, replaced, launched, or otherwise modified as part of the isolated install.

## Explicit stop boundary

Per the user's instruction, the next phase is performed with USB disconnected:

1. The user opens the isolated Artemis app.
2. The isolated package completes its own Apollo PIN pairing.
3. The user performs the real streaming test.
4. The user reconnects USB and explicitly reports reconnection.

Task 9 log collection, evidence analysis, and final reporting must begin only after step 4. No stream, Apollo pairing, or Task 9 action was performed during this installation phase.
