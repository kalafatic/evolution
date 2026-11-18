# DARWIN FORGE — ARCHITECTURE AUDIT

Audit date: 2026-11-17
Scope: native LLM training pipeline (data acquisition → `.evodata` → Darwin Forge → `.evo` → export → inference → ENR).
Status of every finding: **VERIFIED** (confirmed against source at the cited path/line) unless marked otherwise.
Companion documents: `DARWIN-FORGE-DEFECT-REGISTER.md`, `DARWIN-FORGE-REFACTORING-PLAN.md`.

---

## 1. Component inventory

### 1.1 OSGi bundles (Tycho aggregator `pom.xml`)

| Bundle | Role in native pipeline | Key content |
|---|---|---|
| `eu.kalafatic.evolution.forge.math` | Tensor engine | `Tensor` (API), `SimpleTensor` (only impl) |
| `eu.kalafatic.evolution.forge.model` | Native model core + format | `llm/` (EvoLlmModel, Embedding, PositionalEncoding, MultiHeadAttention, TransformerBlock, FeedForward, RMSNorm, serializer/artifact/quantizer/exporter), `inference/` (ReferenceEvoInferenceEngine, KVCache, EvoModelValidator), `protocol/` (EvoModelWriter/Reader, descriptors, hashes) |
| `eu.kalafatic.evolution.forge.tokenizer` | Tokenizer | `SimpleBPETokenizer` (production), `SimpleTokenizer`, `EvoTokenizer`, `WordTokenizer`, `WhitespaceTokenizer` (unused) |
| `eu.kalafatic.evolution.forge.data` | Dataset acquisition/preparation | `EvoDatasetArtifact` (`.evodata`), adapters (Parquet/CSV/JSONL/JSON/OASST1/HF/Git/Text/Folder/EVOData), `HuggingFaceDatasetSource`, `DatasetPreparationService`, `TrainingDataAcquisitionServiceImpl`, `DatasetBuilder` |
| `eu.kalafatic.evolution.forge.trainer` | Training engine | `EvoLlmTrainer` (production), `TrainingCheckpoint`, `EvoAdamW` (legacy), `EvolutionaryTrainer` (stub) |
| `eu.kalafatic.evolution.forge.controller` | Forge lifecycle orchestration | `ForgeOrchestratorImpl`, `SelfEvoForgingServiceImpl`, `ForgePreflightValidator`, agents, legacy controller impls |
| `eu.kalafatic.evolution.forge.runtime` | Legacy graph runtime | `LinearExecutionEngine`, `GraphExecutionEngine` (stubs; NOT the inference runtime) |
| `eu.kalafatic.evolution.forge.observability` | Observability | `EventTracker` interface (no impl), `ForgeEvent` enum |
| `eu.kalafatic.evolution.forge.agent.api` | Export stack | `OllamaExporter` (GGUF writer), `GGUFReader`/`GGUFValidator`, `HuggingFaceExporter`, `LlamaCppBuilder/Runner` |
| `eu.kalafatic.evolution.forge.agent` | Legacy demo bundle | `EvoForgeServer`, `ForgeEvoAgent`, Python scaffolding (`convert_gguf.py`, `train.py`) — outside production lifecycle |
| `eu.kalafatic.evolution.enr` | EVO Native Runtime | `EnrRuntime`, `EnrHttpServer` (NanoHTTPD `/enr/v1`, port 28780), `EnrLlmProvider`, `EnrActivator` |
| `eu.kalafatic.evolution.servers` | Server lifecycle | `ServerManager`, `PortRegistry` (DEVELOP 28380, ENR 28780) |

### 1.2 Non-OSGi modules

| Artifact | Role | Note |
|---|---|---|
| `eu.kalafatic.evolution.gpu` | "GPU" training/diagnostics (Maven jar, FFM preview) | **No GPU execution exists** — see defect register G-1/G-2 |
| `forge-lab/` | Separate Maven multi-module prototype | 8 modules, 69 files, **zero tests**; stubs (`EvolutionaryTrainer.step()` empty, `XmiForgeRepository.load()==null`, `ExporterControllerImpl` fabricates export path) |
| `eu.kalafatic.evolution.forge-lab/` | Orphan stub | Empty aggregator POM, no modules |

### 1.3 Tests

All production-relevant tests live in `eu.kalafatic.evolution.controller.tests` (eclipse-test-plugin, Tycho surefire). Coverage summary: strong E2E/forensic coverage of the happy path (checkpoint, serialization, GGUF validation, small end-to-end training); **no tests** for `SimpleTensor` arithmetic/shape validation, attention-mask application, KV-cache positional encoding, quantizer storage dtypes, GPU execution claims, `.evodata` integrity.

---

## 2. Execution paths (actual, verified)

### 2.1 Forge lifecycle (production UI path)

```
SelfEvoForgingServiceImpl.startForging            (forge.controller service.impl)
 └─ forgeOrchestrator.executeJob(job, projectPath) (ForgeOrchestratorImpl)
     1. Source analysis       sourceAnalyzer.analyzeSources
     2. Dataset preparation   DatasetPreparationService.prepareDatasets
                              → SourceAdapterRegistry (EVOData, Parquet, CSV, JSONL, JSON,
                                OASST1, HuggingFace, Git, Text, Folder)
                              → merge → dedup (exact + normalized) → EvoDatasetArtifact.save
                              tokenizer: SimpleBPETokenizer trained on corpus
     3. Preflight             ForgePreflightValidator (dims, LR bounds, memory, disk)
     4. Training              EvoLlmTrainer (EVO_FAST profile)
                              resume scan: runFolder/checkpoints/*.chk
                              checkpoint on epoch % freq == 0 || final
                              ForgeSessionManager.registerActiveTrainer (stop/pause/abort)
     5. Validation gate       EvoModelValidator.validateModel → block export on failure
     5.5 Evaluation           (metrics partially fabricated — defect T-6)
     6. Persist               .evo artifacts + optional quantization + OllamaExporter (GGUF)
     7. Smoke test            ReferenceEvoInferenceEngine.generateFromArtifact (32 tokens)
     8. Final state           COMPLETED/STOPPED/PAUSED/ABORTED/FAILED
```

### 2.2 Training loop (per optimizer step)

`EvoLlmTrainer.train()`: shuffle(seed) → train/val split → param groups → EmbeddedAdamW →
epoch loop → per-epoch shuffle → accumulation windows of micro-batches →
`zeroGradAll` → `processMicroBatch` (forward → cross-entropy dLogits → `model.backward`) →
NaN/Inf check → `optimizer.step(paramGroups, maxGradNorm)` → telemetry/checkpoints.

Forward graph: `EvoLlmModel.forward(inputIds)` = Embedding → sinusoidal PositionalEncoding → N × TransformerBlock(pre-norm attention + SwiGLU FFN) → RMSNorm → lmHead matmul (tied or untied).

Backward: analytic gradients in each layer (`MultiHeadAttention.backward`, `FeedForward.backward`, `RMSNorm.backward`); gradient accumulation into `Tensor.getGrad()`; no autodiff in forge.math (by design).

### 2.3 Inference path

`ReferenceEvoInferenceEngine.generateFromArtifact` / `generateWithListener`:
prefill `model.forwardWithCache(promptIds, kvCache)` → per-token `forwardWithCache(new int[]{lastToken}, kvCache)`
→ sampling (temperature/topK/topP/penalties) → EOS/maxToken termination → synchronous `TokenStreamListener`.

### 2.4 ENR path

`EnrActivator` (OSGi) → `EnrHttpServer` (NanoHTTPD, `/enr/v1/*`, SSE streaming) → `EnrRuntime`
→ dispatch: `evo-native:` → `NativeEvoProvider` + `ReferenceEvoInferenceEngine`; `.gguf`/`llama-cpp:` → `LlamaService`; else Ollama.

### 2.5 Data acquisition paths

- Authoritative: `DatasetPreparationService` (HF rows API / adapters / local / git worktrees → merge → dedup → `.evodata`).
- Legacy target-driven: `TrainingDataAcquisitionServiceImpl` (streaming `DatasetSource` + `DataCleaner` + `TrainingSampleQualityScorer` + MinHash dedup).

---

## 3. Component evaluation summary

| Component | Verdict |
|---|---|
| `SimpleTensor` | Correct core math (matmul/transpose/softmax); missing shape/rank/bounds validation; no broadcasting; silent mis-indexing (`SimpleTensor.java:168-185`) |
| GPU bundle | Diagnostics only (nvidia-smi + symbol presence); training loop is simulated; no CUDA execution anywhere |
| `EvoLlmModel` / `MultiHeadAttention` / `TransformerBlock` / `FeedForward` / `RMSNorm` | Genuine decoder-only LLaMA-style architecture; real multi-head attention with causal mask + KV cache; correct analytic backward; **attention mask parameter discarded**; **KV-cache generation reuses position-0 encoding** |
| `EvoLlmTrainer` + `EmbeddedAdamW` | Correct AdamW (decoupled decay, bias correction, global clip), cosine LR + warmup, correct MEAN_PER_TOKEN accumulation; `SUM` reduction broken; early stopping missing; checkpoint restores optimizer+scheduler state |
| Checkpoint | Atomic save, magic verification, weights+m/v+steps+vocab restored; moment keys positional (`p_N`) |
| `.evodata` | Working zip container with atomic write; **no checksums, no tokenizer identity, no format-version validation**; full in-RAM load; wall-clock timestamps break byte-reproducibility |
| `.evo` protocol | EVO_NAT2 v2 + legacy v1 read; per-tensor SHA-256 in manifest; **model content hash truncated to 32 bytes at write; checksums not verified on production load** |
| GGUF export | Real GGUF v3 writer + validator; quantization simulated (F32 storage); external-runtime acceptance untested |
| Inference | KV cache, sampling, streaming; **no cancellation** |
| ENR | Working HTTP/SSE server; not RCP-independent despite "standalone" claim; multi-model = Ollama multiplex + native provider; no shared GPU/VRAM resource manager |

---

## 4. Architecture diagram

```
┌──────────────────────────────────────────── DARWIN FORGE (train side) ────────────────────────────────────────────┐
│                                                                                                                     │
│  SOURCES ──► forge.data ──► .evodata ──► forge.trainer ──► forge.model ──► .evo ──► forge.agent.api ──► .gguf      │
│  (HF/parquet/ (DatasetPrep  (EvoDataset  (EvoLlmTrainer,   (EvoLlmModel,  (EvoModel  (OllamaExporter,   (GGUF v3)  │
│  jsonl/csv/   Service,     Artifact:    EmbeddedAdamW,     MHA/PE/FFN/   Writer/    GGUFValidator)                 │
│  txt/md/git)  adapters,    zip+jsonl)   TrainingCheckpoint RMSNorm,      Reader,                                   │
│               HF streaming)             (resume/stop/     EvoModelVal-  EvoModelArtifact,                          │
│                                         pause/abort)      idator, Ref-  Quantizer, Exporter)                       │
│                                                            erenceInferenceEngine)                                  │
│                                                                                                                     │
└──────────────────────────────────────────────┬──────────────────────────────────────────────────────────────────────┘
                                               │ model file (.evo/.gguf) + stable interfaces
┌──────────────────────────────────────────────▼────────────── EVO NATIVE RUNTIME (serve side) ─────────────────────┐
│                                                                                                                     │
│  EnrHttpServer (/enr/v1: models, chat, chat/stream SSE, health, status, providers, capabilities)  port 28780        │
│        └─► EnrRuntime ──► evo-native:  NativeEvoProvider + ReferenceEvoInferenceEngine (CPU, in-JVM)               │
│                      ├──► .gguf/llama-cpp: LlamaService (external process)                                          │
│                      └──► other: Ollama (external service)                                                          │
│  EnrLlmProvider ──► Darwin ILlmProvider / LlmRouter (integration with EVO orchestration)                            │
│                                                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

EVO desktop/TRYST/DStudio ──► Forge orchestrator (train/export) + ENR (inference) + Darwin Engine (orchestration)
```

---

## 5. Defect list (condensed — full detail in DEFECT REGISTER)

**Correctness (Priority 1)**
1. Attention/padding mask silently discarded (`EvoLlmModel.forward(int[],float[])`).
2. KV-cache autoregression applies position-0 positional encoding to every generated token.
3. `SimpleTensor` constructor/indexing/elementwise ops lack shape/rank/bounds validation (silent wrong results).
4. `EvoModelExporter.exportSnapshot` default exports freshly randomized weights.
5. `EvoModelQuantizer` never changes stored dtype (INT8/INT4 remain F32 storage+manifest).
6. `.evo` header: content hash truncated 64→32 hex chars; `sectionCount=5` with 6 sections.
7. `LossReduction.SUM` gradient normalization is mathematically wrong.
8. Batched `forward(int[][],float[][])` processes only `batchInput[0]`.

**Reliability (Priority 2)**
9. GPU bundle: no GPU execution; fake training loop; misleading "CUDA_GPU_VERIFIED"; zero-hash header in its serializer.
10. `.evodata`: no checksums/tokenizer-identity/version validation; `CORRUPTED` status unreachable; provenance lost; non-reproducible metadata; report claims filtering that doesn't run.
11. Checkpoint moments keyed by positional index; no per-tensor size validation.
12. No NaN/Inf check on validation loss; inference generation cannot be cancelled.
13. Fabricated metrics/events/export paths in legacy REST facades and `runE2EDemo`.
14. Model checksum verification not enforced on production load path.
15. `Embedding` UNK fallback hardcodes token id 1 (BOS).

**Performance (Priority 3)**
16. Full in-RAM `.evodata` load (double read during validation); no sharding.
17. No batched matmul; every op allocates (GC pressure); float32 only.

**Architecture (Priority 4)**
18. ENR depends on Eclipse RCP/controller bundle (not standalone).
19. Parquet handling is a heuristic string harvester (no real codec).
20. No shared resource manager for GPU VRAM/RAM across runtime providers.
21. forge-lab prototype duplicates APIs as stubs; forge.runtime/forge.observability are dead shells.

---

## 6. Recommended implementation order

See `DARWIN-FORGE-REFACTORING-PLAN.md` for the full plan. Order: (1) tensor engine validation + tests, (2) model attention/PE fixes + tests, (3) trainer loss-reduction + mask plumbing + NaN checks, (4) checkpoint integrity, (5) `.evo` protocol fixes + load-time verification, (6) exporter snapshot + quantizer storage, (7) inference cancellation + tests, (8) `.evodata` v2 (checksums, tokenizer identity, version validation), (9) telemetry honesty (remove fabricated data), (10) ENR boundary documentation + API doc, (11) GPU bundle honest re-labeling or removal decision, (12) performance (only after correctness).
