# EVO NATIVE MODEL COMPATIBILITY

Status of compatibility across the native model lifecycle (verified 2026-11-17).

## 1. `.evo` format compatibility

| From | To | Status |
|---|---|---|
| EVO_NAT2 v2 (any writer) | EvoModelReader | READ (backward compatible: legacy 32-char header-hash prefix accepted; full hash from metadata preferred) |
| EVO_NAT2 v2 (pre-fix files) | post-fix reader | READ — prefix matching keeps old files loadable |
| EVO_ARTIFACT_V1 (legacy gzip) | post-fix reader | READ-ONLY (hash recomputed at load; no stored digest) |
| post-fix writer output | pre-fix reader | READ (extra metadata field ignored; header layout unchanged) |
| INT4-dtype payload | any reader | REJECTED (`EVO_PROTOCOL_ERROR`) — packed nibble storage not implemented |
| GPU-bundle serializer (`EvoGpuModelSerializer`) | forge.model reader | READ (same EVO_NAT2 layout; now writes real hash) |

## 2. Quantization

- F16 / BF16 / INT8: real compact on-disk storage with exact round-trip of the in-memory dequantized values; manifest dtype reflects storage; load returns float32.
- INT4: not implemented — quantizer and writer refuse honestly (no fake F32 artifacts).
- GGUF: always `general.file_type=ALL_F32` today; quantized GGUF tensor types not produced (documented limitation).

## 3. Checkpoints

- `EVO_CHK_V2`: weights + AdamW m/v + step counter + vocab + dataset fingerprint (SHA-256). Moment maps keyed by canonical tensor name with legacy positional fallback — old and new checkpoints restore.
- Architecture/vocab-size/tensor-count/tensor-size validation rejects incompatible checkpoints with clear errors.
- Dataset fingerprint change between capture and resume is detectable (digest recorded; enforcement/warning at resume is a scheduled follow-up).

## 4. `.evodata`

- Format 1.0 archives (no checksums/tokenizer identity) still load (diagnostic recorded).
- Format 1.1 archives load with integrity verification; corrupted → `CORRUPTED` status, never silent.
- Unsupported major format versions are rejected with diagnostics.

## 5. Tokenizer / vocabulary

- `SimpleBPETokenizer`: vocab padded deterministically to the training target (`<pad_N>` placeholders) — vocab size now always equals the requested target; special token ids stable (`<unk>=0, <s>=1, </s>=2, " "=3`, `<pad>` etc. resolved via `isSpecialToken`).
- Encoder unchanged for real corpus tokens; padding tokens never emitted by encoding.
- Embedding OOV fallback now uses the UNK id (0), not BOS (1).

## 6. Existing EVO application workflows

- Forge orchestrator unchanged end-to-end (source → `.evodata` → train → `.evo` → GGUF); additions: tokenizer identity recorded in `.evodata`, dataset SHA-256 fingerprint in checkpoints, integrity gate on model load, honest evaluation of INT4, weights.bin export sidecar.
- Ollama integration unchanged (optional interoperability path).
- Model presets (SMALL/DEFAULT/LARGE and Size presets) untouched; dimension validation unchanged.

## 7. External formats

- GGUF export validated internally (GGUFReader/GGUFValidator); **external llama.cpp/Ollama acceptance not verified** (no runtime available during this initiative) — documented limitation, not claimed success.
- HuggingFaceExporter untouched.

## 8. Breaking changes introduced (none by design)

No public API signatures removed. Behavior changes (all documented in DARWIN-FORGE-DEFECT-REGISTER.md):
- Quantizer INT4 now throws `UnsupportedOperationException`.
- Corrupted `.evo`/`.evodata` now refuse to load instead of loading silently.
- Tokenizer vocab sizes now equal the training target (padding).
- GPU module no longer produces fake "trained" models; it reports diagnostics only.

---

## 9. Completion-phase compatibility updates (2026-11-17)

- **Legacy `.evo` vocab duplicates**: real artifacts produced by earlier pipeline revisions can contain duplicate vocab JSON keys; the reader now parses the vocab duplicate-tolerantly (last entry wins). Content hashes of legacy files were computed by an earlier, unreproducible algorithm — per-tensor SHA-256 checksums remain the enforced integrity layer; legacy content hashes are informational. Current-algorithm files (full digest in metadata) still get exact hash verification.
- **Sampling configuration in binary `.evo`**: `temperature/topP/topK/repeatPenalty/frequencyPenalty/presencePenalty` now persist in the artifact metadata and are restored on load (the directory form already used `config.json`).
- **Ollama folder copy**: `OllamaExporter.export(...)` copies the produced GGUF into `~/.ollama/models/` under the export name and `evo.gguf`, restoring the previous programmatic-registration behavior.
- **Model-path resolution gating**: `LlamaService.resolveEvoModelPath` no longer resolves arbitrary unknown model names to default `evo.gguf` artifacts — default-model fallbacks apply only to evo-variant names.
- **Preflight split resolution fixed**: `ResolvedSource` now reports the actually resolved split (e.g. `train_sft` fallback), not the requested one.
- **HF acceptance accounting**: `HuggingFaceDatasetSource.next()` now counts accepted records, matching the test contract.
- **Deterministic resume**: per-epoch shuffles derive from a canonical ordering — checkpoints resume bit-exactly when the planned epoch total is unchanged (the LR schedule is a function of the total planned steps, documented).
- All previously documented compatibility guarantees remain: EVO_NAT2 v2 + legacy V1 read, checkpoint backward/forward moment-key compat, `.evodata` 1.0 loading, tokenizer vocab padding.
