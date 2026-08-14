### Task 6: 진단 품질 강화

**Files:**

- Modify: `app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java`
- Modify: `app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java`
- Modify: `app/src/test/java/com/limelight/binding/video/LowLatencyProfilePlannerTest.java`
- Create: `app/src/main/java/com/limelight/binding/video/DecoderLatencySampler.java`
- Create: `app/src/test/java/com/limelight/binding/video/DecoderLatencySamplerTest.java`

- [ ] 다음을 한 번씩 deterministic하게 log한다.

  - decoder name과 MIME
  - `FEATURE_LowLatency`
  - vendor enumeration 성공/실패
  - low-latency 관련 광고 parameter만 filtering한 set
  - ULL preference state
  - 각 attempt의 profile name과 option map
  - 성공 profile과 fallback 이유

- [ ] crash report에는 SOC 값과 함께 active profile name/index를 추가한다. 전체 vendor parameter dump는 다시 켜지 않고 관련 key만 남긴다.
- [ ] `safeSet()` 성공 log를 “codec이 key를 지원함”으로 표현하지 않는다. `requested`와 `advertised`를 명시적으로 구분한다.
- [ ] log formatting test가 동일 capability에서 동일 순서를 내는지 확인한다.
- [ ] 현재 aggregate average만으로 p50/p95/p99를 만들 수 없으므로, `updateDecodeLatencyStats()`에서 enqueue-to-dequeue microseconds를 기록하는 고정 크기 sampler를 추가한다. `int[16384]` circular buffer를 사용하고 per-frame allocation·sorting·log를 하지 않는다.
- [ ] stream stop 또는 crash-report snapshot에서만 복사·정렬해 nearest-rank p50/p95/p99를 계산한다. 이 값이 input-to-photon이 아니라 decoder enqueue-to-dequeue임을 label에 명시한다.
- [ ] 다음 sampler test를 먼저 작성한다.

```java
@Test public void percentilesUseMicrosecondsAndNearestRank() { }
@Test public void circularBufferKeepsMostRecentSamples() { }
@Test public void emptySamplerReturnsNoMeasurement() { }
@Test public void recordingBeyondCapacityKeepsFixedSampleCount() { }
```

- [ ] Commit: `git commit -m "Add deterministic codec low-latency diagnostics"`

#### Controller adjudication (binding implementation contract)

- Metric boundary/name is exact: successful non-CSD `queueInputBuffer()` call start to the first successful output dequeue for the same PTS, reported as `decoder_input_queue_call_start_to_first_output_dequeue_us`. It is not decode time, render latency, or input-to-photon.
- Capture/insert the timestamp immediately before `queueInputBuffer()`. If queueing throws, remove only the matching pending entry. Never track `BUFFER_FLAG_CODEC_CONFIG` input.
- One `decoderLatencyLock` owns every pending-PTS put/remove/clear plus sampler record/reset/copy. First output dequeue atomically removes and records the PTS exactly once.
- Replace all five direct output `dequeueOutputBuffer()` sites with one wrapper that records immediately after a non-negative dequeue result, before any release/drop/pacing branch. Remove all six delayed `updateDecodeLatencyStats()` calls. Do not modify release/drop/pacing policy.
- `DecoderLatencySampler` is a fixed `int[16384]` ring with O(1) allocation-free record. Snapshot copies under the shared lock and sorts outside it. Nearest-rank uses `ceil(p*n)-1`; clamp microseconds to `Integer.MAX_VALUE` and reject negative deltas.
- Required exact sampler fixtures include `[100,200,300,400] -> p50=200,p95=400,p99=400` and recording `0..16384` -> `count=16384,p50=8192,p95=15565,p99=16221`, plus empty/reset/defensive/deterministic behavior.
- New setup/session resets pending PTS, sampler, and one-shot stop-summary guard. Flush/restart/reset/recreate clears pending PTS before codec buffer invalidation but retains session samples. `prepareForStop()` does not snapshot/reset. `stop()` logs/resets once after renderer/Choreographer joins; `cleanup()` is an idempotent fallback. Crash snapshot never resets.
- Log prefixes and field order are stable: `CodecLLCapability`, `CodecLLAttempt`, `CodecLLFallback`, `CodecLLSelected`, `CodecLatencySummary`. Empty percentiles use `NA`. Capability enumeration is tri-state `not_supported|success|failure`; advertised keys are the sorted intersection with known low-latency keys. `requestedOptions` must never be described as supported.
- Build one immutable low-latency plan/capability snapshot per configuration sequence so retryable enumeration failure is not re-probed or re-logged for every profile index. Retry remains allowed on a later configuration sequence.
- Crash text adds active successful profile index/name and the exact metric snapshot. Failed attempts never replace active state.
- Focused tests must also cover: first dequeue once, failed queue cancellation, CSD exclusion, recovery clear while retaining samples, new-session reset, idempotent stop, all five wrapper sites, capability tri-state/filter/order, exact log formatting, and crash fields.
- Task 6 does not introduce a renderer stutter/frame-drop counter. Existing network frame-loss statistics are semantically different; Task 8 records renderer-visible stutter separately.
- Forbidden changes: `releaseWithPolicy()`, `doFrame()`/Choreographer scheduling, drop thresholds/EWMA/latest-only/balanced behavior, Surface/display/Presentation ownership, input/mouse/controller code, preference/profile ordering, or crash callback types.
