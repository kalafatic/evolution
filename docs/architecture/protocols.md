# Serialization Schemas and Protocol Specifications

**Document Identifier**: `docs/architecture/protocols.md`
**Date**: September 9, 2026

---

## 1. Protocol Specifications

### 1.1 EVO Native Model Protocol (`.evo` Binary Container v2)
- **Magic**: `EVO_NAT2` (8-byte header: `EVO_NAT2`)
- **Version**: `2` (32-bit Big-Endian integer)
- **Encoding**: 32-bit length-prefixed UTF-8 string encoding (bypassing 64KB `DataOutputStream.writeUTF()` limits).
- **Directory Structure**: Section Directory Table defining offsets and lengths for Architecture, Tokenizer, Tensor Manifest, and Tensor Binary Stream.
- **Content Hash**: Computed deterministically via chunked `ByteBuffer` streams (`EvoModelContentHash`).

### 1.2 EVO Dataset Protocol (`.evodata` Archive v1)
- **Archive Format**: ZIP container containing `metadata.json`, `data.jsonl`, `val_data.jsonl`, and `report.txt`.
- **Metadata Fields**: Format version, source provenance, requested/usable bytes, accepted/rejected record counts, coverage percentage, estimated token counts, deduplication stats, quality scores.

### 1.3 LLM Reasoning Protocol
- **Structure**: Provider-independent separation of `<think>...</think>` tags or separated fields into `LlmResponse` DTO containing `reasoning` and `finalContent`.
- **UI Render**: Rendered as a distinct collapsible light-gray Thinking bubble (`.thinking-bubble`) separate from standard chat message payloads.

### 1.4 Supervisor Control Protocol
- **Server**: HTTP Control Server on `EVOSupervisorControlServer`.
- **Endpoints**: `/status`, `/build`, `/deploy`, `/start`, `/stop`, `/logs`, `/task-log`.
- **Payloads**: Structured JSON requests and responses containing task status, exit codes, and failure logs.
