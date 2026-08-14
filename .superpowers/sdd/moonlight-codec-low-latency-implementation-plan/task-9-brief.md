### Task 9: MTK 실기기 검증과 release gate

**Files:**

- Temporary logs only: `$env:TEMP\moonlight-low-latency\mtk\`
- No tracked source changes unless a reproducible defect is found

- [ ] capability-gated MTK 성능 주장을 위해 최소 한 대의 `c2.mtk` 기기를 확보한다. API 26-30 speculative vendor bundle을 후속 allowlist로 활성화하려면 `OMX.MTK` legacy 기기 검증도 필수이며, 확보하지 못하면 해당 bundle은 비활성 상태로 남긴다.
- [ ] 각 기기에서 model, SoC, Android API, decoder name, vendor parameter set을 먼저 기록한다.
- [ ] H.264/HEVC를 60 FPS와 지원 시 120 FPS로 ULL off/on 검증한다. AV1 hardware decoder가 있는 기기에서만 AV1을 추가한다.
- [ ] attempt 0-7 중 성공 profile, fallback 종료, reconnect, stream stop/start, codec recovery를 검증한다.
- [ ] ULL off에서 experimental MTK vendor key가 요청되지 않고, ULL on에서 광고된 경우 ULL=1이 요청되는지 확인한다.
- [ ] skip/drop key가 log에 없고 화면 손상, non-reference frame 손실, stutter가 증가하지 않는지 확인한다.
- [ ] 15분 thermal soak와 S25+와 동일한 성능 판정 규칙을 적용한다.
- [ ] MTK 실기기가 없으면 code-path/unit-test 완료로만 보고하고 “MTK 성능 검증 완료” 또는 “획기적 개선”이라고 표시하지 않는다. release note에서 experimental 상태를 유지한다.

## 6. 최종 승인 기준

다음 항목을 모두 만족해야 구현 완료로 판정한다.

- [ ] 작업 commit은 `877c5c22`의 자손이다.
- [ ] Android standard `KEY_LOW_LATENCY`가 FEATURE 지원 decoder의 기본 경로다.
- [ ] ULL preference off에서도 standard low latency는 유지된다.
- [ ] API 31+에서 unadvertised QTI/MTK vendor key를 요청하지 않는다.
- [ ] QTI fence family를 같은 format에 혼합하지 않는다.
- [ ] 광고된 QTI core가 있을 때 진짜 QTI-only fallback이 존재한다.
- [ ] S25+에서 unadvertised QTI core/software-fence를 요청하지 않는다.
- [ ] Qualcomm path에 `vdec-lowlatency`가 없다.
- [ ] MTK ULL off는 vendor ULL key를 생략하고, on은 지원될 때 값 1을 쓴다.
- [ ] MTK skip/drop 네 key가 제거됐다.
- [ ] option profile은 최대 11개이고, renderer-owned base를 포함한 configure attempt가 최대 12회 안에 종료한다.
- [ ] 모든 새 targeted unit test가 통과한다.
- [ ] 전체 suite는 기존 다섯 실패 외 새 실패가 없다.
- [ ] `assembleNonRoot_gameDebug`가 성공한다.
- [ ] S25+ AVC/HEVC/AV1 실제 stream이 configure/start/render되고 recovery loop가 없다.
- [ ] MTK 성능 주장은 실제 MTK hardware A/B 증거가 있을 때만 한다.
- [ ] Fully External Display와 input/mouse lifecycle에 unrelated diff가 없다.
- [ ] `AGENTS.md`, repo-local `docs/`, build artifact가 commit에 없다.

## 7. 구현 후 보고 형식

최종 보고는 다음 네 범주를 분리한다.

1. **코드 계약:** 어떤 profile/key/fallback을 구현했는가
2. **자동 검증:** targeted test, compile, assemble, 전체 suite baseline 비교
3. **기기 호환성:** configure/start/render 성공 여부와 실제 decoder/profile
4. **성능 증거:** A/B 측정값과 thermal/stability 결과

“key가 format log에 있음”, “configure 성공”, “실제 지연 개선”을 같은 증거로 취급하지 않는다.
