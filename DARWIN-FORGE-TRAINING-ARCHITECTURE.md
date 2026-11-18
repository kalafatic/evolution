# DARWIN FORGE — TRAINING ARCHITECTURE

Describes the **actual** implemented training stack (post-2026-11-17 fixes). Status: IMPLEMENTED / VERIFIED where noted.

## 1. Model architecture (`forge.model/llm`)

Decoder-only, LLaMA-style, pre-norm, bias-free:

```
Embedding [V, D]  (N(0, 1/√D), seed 42)
  + PositionalEncoding (additive sinusoidal absolute; table [maxSeqLen, D])
  N × TransformerBlock:
      attn: RMSNorm → MultiHeadAttention (Q/K/V [D,D], WO [D,D]; heads = D/numHeads;
            scaled dot-product, causal + optional padding mask, KV cache)
      ffn:  RMSNorm → SwiGLU (W1 gate, W3 up, W2 down)
  output: RMSNorm → lmHead [D, V] (tied to embedding when tieEmbeddings)
```

- Analytic backprop in every layer (attention softmax Jacobian, SwiGLU derivative, RMSNorm weight grad) — verified by finite-difference tests.
- Deterministic initialization: all layers seeded 42 (fixed 2026-11-17).
- Dimension validation: positivity, `D % heads == 0`, `dff > D`; `EvoModelValidator` scans NaN/Inf.

## 2. Training loop (`forge.trainer/EvoLlmTrainer`)

- Samples → deterministic shuffle (baseSeed) → train/val split (`validationSplitRatio`).
- Epoch loop → per-epoch shuffle (`baseSeed+epoch+1`) → accumulation windows of micro-batches.
- Per window: valid-token count (loss-mask > 0) → skip empty windows **without advancing the LR schedule** (fixed 2026-11-17) → forward → cross-entropy dLogits (prompt positions loss-masked; padding attention-masked; mask actually applied since 2026-11-17) → `model.backward` → NaN/Inf check (train + validation loss) → optimizer step.
- Loss reduction: `MEAN_PER_TOKEN` (gradient ÷ window token count; one step = mean-per-token gradient) or `SUM` (raw per-token gradients; batch-size independent — fixed 2026-11-17).
- Optimizer: embedded AdamW — decoupled weight decay applied before moment update, bias correction, global L2 gradient clipping, rank-1 params excluded from decay. LR: linear warmup (3 % steps) + cosine decay to minLr; schedule position = global optimizer step.
- Lifecycle: stop (valid model) / pause (checkpointable) / abort (invalid) / interrupt; resume from checkpoint restores weights, moments, step counter (schedule position), vocab; re-submission of ≤ completed epochs is a no-op (fixed 2026-11-17).
- Checkpoints: `EVO_CHK_V2`, atomic write, architecture/vocab compatibility validation, per-tensor size validation, moment maps keyed by canonical tensor name with legacy positional fallback (fixed 2026-11-17), dataset fingerprint = SHA-256 of `.evodata` (fixed 2026-11-17), tokenizer vocab persisted.

## 3. Orchestration (`forge.controller/ForgeOrchestratorImpl`)

Stages: source analysis → dataset preparation (`DatasetPreparationService` → `.evodata`) → tokenizer training (`SimpleBPETokenizer`, vocab padded to target) → tokenizer identity recorded in `.evodata` → preflight → training (EVO_FAST profile; resume discovery; epoch checkpointing; stop/pause/abort via `ForgeSessionManager`) → model validation gate → evaluation → export (`.evo` + optional quantization + GGUF) → native inference smoke test.

## 4. Execution backend

- Training/inference: pure Java float32 CPU (`forge.math.SimpleTensor`). No GPU execution exists; the GPU module performs real diagnostics only (nvidia-smi + FFM `cuInit` verification, honest labels) and never fabricates training.
- Telemetry: trainer callbacks → `ForgeSessionManager.TrainingTelemetry` (loss, tok/s, steps/s, ETA, RAM; throttled).

## 5. Known gaps (scheduled, not hidden)

- Early stopping config (`earlyStoppingEnabled`/`targetLossThreshold`) not consumed.
- Batch-level job progress (orchestrator `onProgress` empty) — training progress stuck at stage granularity.
- Fabricated evaluation metrics in orchestrator (F-3) and fabricated REST facades (F-1/F-2) — telemetry-honesty remediation scheduled Phase 2.
- `warmupSteps`/`timeBudgetHours` config unconsumed.
