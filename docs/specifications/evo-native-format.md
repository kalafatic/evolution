# EVO Native Model Protocol Specification (EVO_NATIVE_V2)

## Status: Canonical Model Protocol Standard
**Version**: 2.0.0
**Format Magic**: `EVO_NATIVE_V2` (`0x45564F4E20563200`)
**Target File Extension**: `.evo`

---

## 1. Overview & Architecture Design

The `.evo` file format is the canonical, versioned, deterministic, self-validating, binary model artifact produced by **EVO LLM Training / Forge**.

It serves as the single authoritative model representation consumed by:
1. EVO Native Inference Engine (`ReferenceEvoInferenceEngine`)
2. Model Validation Tooling (`EvoModelValidator`)
3. Model Inspection Tooling (`EvoModelInspector`)
4. Exporters (GGUF, Hugging Face, ONNX)
5. Quantization and Conversion Utilities

### Pipeline Flow Architecture
```text
Dataset
   ↓
Tokenizer
   ↓
Forge / Training
   ↓
EVO Native Model Artifact (model.evo)
   ├── EVO Native Inference Engine
   ├── GGUF Exporter → llama.cpp / Ollama
   ├── Future Exporters (ONNX, Hugging Face)
   └── Validation / Inspection (EvoModelValidator, EvoModelInspector)
```

The `.evo` format is **language-independent** and **runtime-independent**. It does **NOT** serialize Java object graphs, class signatures, or thread/optimizer states.

---

## 2. Binary Container Layout

An `.evo` binary file is structured into explicit, aligned, byte-addressed sections.

```text
+-------------------------------------------------------------------+
| FILE HEADER (64 Bytes)                                            |
|   Magic: 'EVO_NATIVE_V2'                                          |
|   Format Version, Protocol Version, Flags, Section Count, Hash    |
+-------------------------------------------------------------------+
| SECTION DIRECTORY                                                 |
|   Section Offsets, Compressed/Uncompressed Lengths, Checksums     |
+-------------------------------------------------------------------+
| ARCHITECTURE DESCRIPTOR (JSON / Binary Section)                   |
|   Family, VocabSize, dModel, Heads, Layers, dff, Norm, Act, RoPE  |
+-------------------------------------------------------------------+
| TOKENIZER DESCRIPTOR & VOCABULARY                                 |
|   Tokenizer Type, Special Tokens (BOS/EOS/UNK/PAD), Vocabulary    |
+-------------------------------------------------------------------+
| TENSOR MANIFEST                                                   |
|   Canonical Tensor Descriptors (Names, Dtype, Shapes, Offsets)    |
+-------------------------------------------------------------------+
| TENSOR DATA PAYLOADS                                              |
|   Aligned Raw / Compressed Float Payloads                         |
+-------------------------------------------------------------------+
| MODEL METADATA & PROVENANCE (Optional Section)                     |
|   Created timestamp, Training epochs, Loss history, Metadata      |
+-------------------------------------------------------------------+
| INTEGRITY FOOTER                                                  |
|   Per-section Checksums, Manifest SHA-256, Content Hash            |
+-------------------------------------------------------------------+
```

---

## 3. Section Descriptions

### 3.1 File Header (64 Bytes)

| Offset | Type | Field Name | Description |
|---|---|---|---|
| 0x00 | `uint64` | `magic` | Constant `0x45564F4E20563200` (`"EVO_NAT2"`) |
| 0x08 | `uint16` | `formatVersionMajor` | Major format version (e.g., `2`) |
| 0x0A | `uint16` | `formatVersionMinor` | Minor format version (e.g., `0`) |
| 0x0C | `uint16` | `protocolVersion` | Protocol version (e.g., `1`) |
| 0x0E | `uint16` | `flags` | Bit 0: Compressed payloads, Bit 1: Memory-mapped ready |
| 0x10 | `uint32` | `headerSize` | Header size in bytes (64) |
| 0x14 | `uint32` | `sectionCount` | Total number of sections |
| 0x18 | `uint64` | `createdAt` | Unix timestamp in milliseconds |
| 0x20 | `byte[32]`| `modelContentHash` | SHA-256 canonical content hash |

### 3.2 Architecture Descriptor

Describes the model architecture cleanly without coupling to Java runtime classes:

```json
{
  "architecture_family": "evo_llm",
  "vocab_size": 32768,
  "d_model": 512,
  "num_heads": 8,
  "num_kv_heads": 8,
  "num_blocks": 6,
  "dff": 2048,
  "max_seq_len": 2048,
  "norm_type": "rms_norm",
  "norm_eps": 1e-5,
  "activation_type": "swiglu",
  "positional_encoding": "rope",
  "rope_theta": 10000.0,
  "embedding_tied": false
}
```

### 3.3 Tokenizer Descriptor & Vocabulary

Contains token vocabulary and special token metadata:

* `tokenizer_type`: `BPE` or `WORD`
* `vocab_size`: Integer vocabulary size
* Special token mappings (`bos_token_id`, `eos_token_id`, `unk_token_id`, `pad_token_id`)
* `id_to_token` / `token_to_id` mappings

### 3.4 Tensor Manifest

Each parameter entry in the manifest is defined by:
* `id`: Integer tensor ID
* `canonical_name`: Standardized tensor name (e.g. `embedding.weight`, `layers.0.attention.q.weight`, `lm_head.weight`)
* `dtype`: `F32` (0), `F16` (1), `BF16` (2), `INT8` (3), `INT4` (4)
* `rank`: Number of dimensions
* `shape`: Array of dimension sizes `[dim0, dim1, ...]`
* `layout`: Matrix layout (`0` = Row-Major)
* `offset`: Absolute byte offset in file/section
* `compressed_size`: Size on disk in bytes
* `uncompressed_size`: Uncompressed byte size
* `compression`: Compression scheme (`0` = NONE, `1` = GZIP, `2` = ZLIB)
* `checksum`: SHA-256 hash of uncompressed float bytes

---

## 4. Canonical Tensor Naming Standards

To prevent transposition or name mapping bugs:

```text
token_embd.weight            / embedding.weight
blk.N.attn_norm.weight       / layers.N.attention_norm.weight
blk.N.attn_q.weight          / layers.N.attention.q.weight
blk.N.attn_k.weight          / layers.N.attention.k.weight
blk.N.attn_v.weight          / layers.N.attention.v.weight
blk.N.attn_output.weight     / layers.N.attention.output.weight
blk.N.ffn_norm.weight        / layers.N.ffn_norm.weight
blk.N.ffn_gate.weight        / layers.N.ffn.gate.weight
blk.N.ffn_up.weight          / layers.N.ffn.up.weight
blk.N.ffn_down.weight        / layers.N.ffn.down.weight
output_norm.weight           / output_norm.weight
output.weight                / lm_head.weight
```

---

## 5. Model Content Hash Calculation

The `modelContentHash` is calculated deterministically as:

```text
modelContentHash = SHA256(
    Canonical Architecture Descriptor JSON bytes
  + Canonical Tokenizer Descriptor JSON bytes
  + Sorted Tensor Manifest Entries (Name + Shape + Dtype)
  + Uncompressed Canonical Float Tensor Bytes (in order)
)
```

This hash remains identical even if file section order or compression schemes change.

---

## 6. Validation and Compatibility

The model loader (`EvoModelReader`) and validator (`EvoModelValidator`) enforce strict checks:
1. `magic == "EVO_NAT2"`
2. Per-tensor SHA-256 checksums
3. Whole-file & Manifest checksums
4. Parameter count vs architecture invariant check
5. NaN / Infinity byte scanning on float payloads

Failure in any check produces explicit diagnostic errors (`EVO_PROTOCOL_ERROR`).
