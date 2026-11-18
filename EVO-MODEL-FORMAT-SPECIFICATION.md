# EVO MODEL FORMAT SPECIFICATION (`.evo`)

Describes the **actual** implemented format: EVO_NAT2 protocol v2.0 (post-2026-11-17 hardening) with legacy V1 read support. Status: IMPLEMENTED (F32/F16/BF16/INT8 storage; INT4 packed storage NOT implemented and rejected).

## 1. File header (64 bytes)

| Offset | Size | Field |
|---|---|---|
| 0 | 8 | magic `EVO_NAT2` (ASCII) |
| 8 | 2 | format version major = 2 |
| 10 | 2 | format version minor = 0 |
| 12 | 2 | protocol version = 1 |
| 14 | 2 | flags (0) |
| 16 | 4 | header size = 64 |
| 20 | 4 | section count = 6 (modelName, arch, tokenizer, vocab, manifest, metadata) |
| 24 | 8 | created timestamp (unix ms) |
| 32 | 32 | content-hash prefix (first 32 hex chars of SHA-256; full digest in metadata) |

## 2. String sections

Six 32-bit length-prefixed UTF-8 sections in order: model name, architecture JSON, tokenizer JSON, vocabulary JSON, manifest JSON array, metadata JSON object.

- `architecture`: `architecture_family`, `vocab_size`, `d_model`, `num_heads`, `num_kv_heads`, `num_blocks`, `dff`, `max_seq_len`, `norm_type=rms_norm`, `norm_eps`, `activation_type=swiglu`, **`positional_encoding=sinusoidal_abs`** (matches the implemented model), `rope_theta`, `embedding_tied`.
- `tokenizer`: `vocab_size`, bos/eos/unk/pad token ids, token vocabulary.
- `manifest` entries: `id, name, dtype, shape, layout, offset, compressed_size, uncompressed_size, compression=1(GZIP), checksum(SHA-256 over float payload)`.
- `metadata`: free-form + mandatory `evo_content_hash` (full 64-char SHA-256 over architecture+tokenizer+sorted manifest+payloads).

## 3. Tensor payloads

- `int32` tensor count, then per tensor: `int32` compressed length + GZIP payload.
- Raw payload layout by dtype (big-endian):
  - `F32` (code 0): 4-byte IEEE-754 floats.
  - `F16` (code 1): 2-byte half-precision.
  - `BF16` (code 2): 2-byte truncated floats (upper 16 bits).
  - `INT8` (code 3): 4-byte float scale prefix + 1-byte quantized values; dequantized to float32 on load. Scale also recorded as `tensor_scale_<name>` in metadata.
  - `INT4` (code 4): **not implemented** — writer rejects, reader raises `EVO_PROTOCOL_ERROR`.
- In-memory representation after load is always float32 (dequantized); in-memory before save is the quantizer's dequantized float32, re-encoded losslessly on write (round-trip exact for F16/BF16/INT8 with stored scale).

## 4. Integrity (enforced on load since 2026-11-17)

`EvoModelArtifact.load` / `EvoModelReader.verifyIntegrity`:
1. Per-tensor SHA-256 vs manifest → mismatch ⇒ `EVO_INTEGRITY_ERROR`.
2. Full content hash recomputation vs stored `evo_content_hash` (exact) or header prefix (legacy).
3. GZIP decompression failures wrapped as `EVO_INTEGRITY_ERROR`.
Corrupted models are refused at load; the production inference path inherits the gate via `EvoModelArtifact.load`.

## 5. Legacy V1

GZIP container with `EVO_ARTIFACT_V1` inner magic (readUTF-based sections, raw float weights, recomputed hash) — read-only support retained.

## 6. Directory form (model.json/config.json/tokenizer.json/weights.bin)

Alternative distribution layout written by `EvoModelArtifact.saveToDirectory`. `config.json` carries sampling config (`temperature`, `top_p`, `top_k`, `repeat_penalty`, `frequency_penalty`, `presence_penalty`), which is restored on directory load. weights.bin has no header (arch-derived layout).

## 7. Export conventions

- GGUF: v3 little-endian, 32-byte alignment, transposed 2D tensors, `general.*`/`llama.*`/`tokenizer.ggml.*` metadata; `general.file_type` currently always `ALL_F32` (quantized GGUF tensor types not produced yet); post-write `GGUFValidator` gate; raw F32 `weights.bin` sidecar (name/shape/f32 records).
- Quantization metadata: `quantized`, `quantization_scheme`, `quantization_dtype`, `source_content_hash`, per-tensor scales.
