# .evo Native Model Protocol Specification

**Protocol Identifier**: `protocols/evo-format.md`
**Protocol Magic**: `EVO_NAT2`
**Version**: `2.0`
**Owner**: Native Model Subsystem (`eu.kalafatic.evolution.forge.model`)

---

## 1. Specification Overview

The `.evo` container format represents the canonical, self-contained native neural language model package in EVO. It encapsulates network architecture parameters, tokenizer vocabulary, structural weight tensor payloads, and full evolutionary provenance.

---

## 2. Archive File Directory Structure

```text
model.evo (ZIP Archive)
    ├── model.json          # Container manifest & format version
    ├── config.json         # Network architecture dimensions (EvoLlmArchitecture)
    ├── tokenizer.json      # Standard BPE tokenizer vocabulary mapping
    └── weights.bin         # Binary float32 parameter tensor payload
```

---

## 3. Configuration Manifest (`config.json`)

```json
{
  "vocabSize": 2048,
  "dModel": 512,
  "numHeads": 8,
  "numBlocks": 8,
  "dff": 2048,
  "maxSeqLen": 1024,
  "tieEmbeddings": false,
  "architectureFamily": "evo_llm"
}
```

---

## 4. Binary Weights Format (`weights.bin`)

`weights.bin` stores model parameter tensors in little-endian byte order:

1. **Header Identifier**: 4-byte ASCII Magic `EVO1` (`0x45 0x56 0x4F 0x31`)
2. **Format Version**: 4-byte Int (`1`)
3. **Tensor Count**: 4-byte Int ($N_{tensors}$)
4. **Tensor Descriptor Records**:
   - String Length ($L$) + UTF-8 Tensor Name String
   - Tensor Rank ($R$) + Int Array Dimensions $[d_0, d_1, \dots, d_{R-1}]$
   - Payload Byte Length ($B = \prod d_i \times 4$)
   - Raw Float32 Data Stream ($float[B/4]$)

---

## 5. Model Lineage & Provenance Metadata (`model.json`)

```json
{
  "version": "2.0",
  "format": "EVO_NATIVE_V2",
  "createdTimestamp": 1725868800000,
  "parentModel": "chat-v1.evo",
  "forgeRunId": "FORGE-RUN-20260909-001",
  "gitRevision": "fda4b9b2",
  "contentHash": "a1b2c3d4e5f6...",
  "status": "READY"
}
```
