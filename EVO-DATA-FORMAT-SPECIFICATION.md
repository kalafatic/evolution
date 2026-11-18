# EVO DATA FORMAT SPECIFICATION (`.evodata`)

Describes the **actual** implemented format (post-2026-11-17 hardening). Status: IMPLEMENTED.

## 1. Container

- Plain ZIP archive (DEFLATE, default level), UTF-8 entry names, deterministic entry timestamps (`1704067200000L`) for byte-level reproducibility.
- Detection: file extension `.evodata` (no magic header).
- Atomic writes: temp file + rename.
- Entries:
  - `data.jsonl` — training records
  - `val_data.jsonl` — validation records
  - `metadata.json` — manifest (see §3)
  - `report.txt` — human-readable preparation report

## 2. Record format (`data.jsonl` / `val_data.jsonl`)

One JSON object per line (fields written by `NormalizedSample.toJsonLine`):

| Field | Type | Notes |
|---|---|---|
| `id` | string | optional |
| `type` | string | `TEXT`, `INSTRUCTION`, `CHAT`, `QA`, `CODE`, `REASONING`, `CLASSIFICATION`, `CONVERSATION` |
| `text` | string | full serialized text (canonical `### role:` formatting for chat/conversation) |
| `conversationId` | string | optional |
| `messages` | array | conversation messages `{role, text, content, messageId, parentMessageId}` |
| `instruction` / `prompt` | string | optional |
| `response` | string | optional |
| `source` | string | originating source name |
| `qualityScore` | number | 0..1 |
| `tokenCount` | number | estimated token count (words × 1.3 heuristic; dataset is stored as raw text) |
| `hash` | string | SHA-256 of `toFullText()` |

Malformed JSONL lines are counted and rejected — never synthesized into samples.

## 3. `metadata.json` (format version 1.1)

```json
{
  "formatVersion": "1.1",
  "datasetId": "...",
  "name": "...",
  "createdTimestamp": 1704067200000,        // deterministic
  "timestamp": 1704067200000,               // deterministic
  "status": "READY",                         // READY | INSUFFICIENT_SOURCE_DATA | CANCELLED | FAILED | CORRUPTED
  "source": "LOCAL",
  "repository": "...",
  "sources": [ { "sourceType", "repository", "revision", "configuration", "split", "downloadedBytes", "acceptedBytes" } ],
  "files": {
    "data.jsonl": { "sha256": "...", "bytes": 1234 },
    "val_data.jsonl": { "sha256": "...", "bytes": 123 }
  },
  "tokenizer": { "type": "SimpleBPETokenizer", "version": "2.0", "vocabSize": 300, "specialTokens": [...] },
  "statistics": { "totalSamples", "trainSamples", "valSamples", "acceptedRecords", "rejectedRecords",
                  "duplicateRecords", "exactDuplicates", "normalizedDuplicates", "nearDuplicates",
                  "malformedJsonCount", "emptyMeaninglessCount", "corruptedTextCount", "invalidQaCount",
                  "trainValLeakageCount", "totalTrainingBytes", "estimatedTokens" },
  "estimatedTokens": 12345,
  "schema": { "sampleTypes": [...], "columns": [...] },
  "trainSamples": 350, "valSamples": 7, "trainTokens": ..., "valTokens": ...
}
```

## 4. Integrity & compatibility rules (implemented)

- Load verifies per-entry SHA-256 (recorded in `files{}`) → mismatch sets `Status.CORRUPTED` and `loadDiagnostics` (never silently accepted).
- `formatVersion` major must be `1`; anything else → `CORRUPTED` with diagnostic.
- Legacy artifacts without `metadata.json` still load (diagnostic noted).
- Zip-slip protection enforced.
- Tokenizer identity: datasets store `tokenizer{}` when prepared with one; training binds a tokenizer and must validate compatibility (checkpoint/model vocab-size checks implemented; dataset-vs-tokenizer check performed at preparation time via `rewriteMetadataWithTokenizerIdentity`).

## 5. Reproducibility

Identical inputs + identical processing config + identical artifact filename ⇒ byte-identical archive. The artifact filename is embedded in `datasetId`/`name`, so reproducibility requires the same filename.

## 6. Known limitations (not implemented — planned)

- Sharding / random-access index (single JSONL entries; full in-RAM load).
- Configurable compression level.
- Streaming (non-RAM) writer/reader.
- Tokenizer-realized token counts (currently heuristic estimates; dataset is stored as raw text and re-tokenized at training time).
