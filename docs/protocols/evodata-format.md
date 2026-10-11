# .evodata Dataset Artifact Specification

**Protocol Identifier**: `protocols/evodata-format.md`
**Version**: `1.0`
**Owner**: Dataset Preparation Subsystem (`eu.kalafatic.evolution.forge.data`)

---

## 1. Specification Overview

An `.evodata` file is a self-contained ZIP archive containing normalized, quality-filtered, deduplicated training samples and complete dataset provenance metadata. It serves as the authoritative, persistent dataset interchange container for training native LLMs within EVO Forge.

---

## 2. Archive Directory Structure and Entry Requirements

```text
dataset.evodata (ZIP Archive)
    ├── metadata.json       # Dataset identity, lifecycle status, statistics, schema, and source provenance
    ├── data.jsonl          # Primary training samples (JSON Lines format)
    ├── val_data.jsonl      # Validation split samples (JSON Lines format)
    └── report.txt          # Human-readable acquisition and preparation summary report
```

### Archive Contract Rules

1. **Required Entries**: `metadata.json`, `data.jsonl`, `val_data.jsonl`, and `report.txt` MUST be present at the root of the archive.
2. **Encoding**: All text entries MUST be encoded in UTF-8 without Byte Order Mark (BOM).
3. **Record Boundaries**: `data.jsonl` and `val_data.jsonl` MUST contain exactly one valid JSON object per non-empty line terminated by `\n`.
4. **Security & Path Invariants**: Entry names MUST be strictly relative (`metadata.json`, `data.jsonl`, `val_data.jsonl`, `report.txt`). Entries containing path traversal sequences (`..`, `/`, `\`) MUST be rejected or sanitized to prevent Zip Slip vulnerabilities.
5. **Determinism**: Archives created from identical samples and configuration SHOULD use fixed or predictable ZipEntry modification timestamps to support reproducible build outputs.

---

## 3. Metadata Specification (`metadata.json`)

`metadata.json` captures dataset identity, lifecycle state, source provenance, processing statistics, and schema specifications. Both structured object sections and top-level backward-compatibility keys are supported by readers.

### Example `metadata.json`

```json
{
  "formatVersion": "1.0",
  "datasetId": "evodata-codebase-v1",
  "name": "evodata-codebase-v1",
  "createdTimestamp": 1725868800000,
  "timestamp": 1725868800000,
  "status": "READY",
  "source": "EVO_CODEBASE",
  "repository": "eu.kalafatic.evolution",
  "sources": [
    {
      "sourceType": "EVO_CODEBASE",
      "target": "eu.kalafatic.evolution",
      "downloadedBytes": 10485760,
      "acceptedBytes": 8388608
    }
  ],
  "statistics": {
    "totalSamples": 15000,
    "trainSamples": 13500,
    "valSamples": 1500,
    "acceptedRecords": 15000,
    "rejectedRecords": 200,
    "duplicateRecords": 150,
    "totalTrainingBytes": 8388608,
    "estimatedTokens": 2100000
  },
  "schema": {
    "sampleTypes": ["TEXT", "INSTRUCTION", "CODE", "CHAT", "CONVERSATION"],
    "columns": ["id", "type", "prompt", "response", "qualityScore"]
  }
}
```

### Field Definitions

- `formatVersion` (string, required): Specification version (e.g. `"1.0"`).
- `datasetId` / `name` (string, required): Canonical dataset identifier or artifact filename.
- `createdTimestamp` / `timestamp` (number, required): Epoch timestamp in milliseconds.
- `status` (string, required): Lifecycle state of the artifact. Allowed values:
  - `CREATING`: Initialization phase.
  - `PREPARING`: Sample conversion and filtering in progress.
  - `VALIDATING`: Pre-flight verification in progress.
  - `FINALIZING`: Archive serialization in progress.
  - `READY`: Verified complete artifact meeting dataset requirements.
  - `INSUFFICIENT_SOURCE_DATA`: Valid artifact where source data was exhausted before reaching requested target size.
  - `CANCELLED`: User aborted preparation process.
  - `FAILED`: Process failed due to errors.
  - `CORRUPTED`: Artifact failed post-serialization integrity check.
- `sources` (array): List of source descriptors containing `sourceType`, `target`, `downloadedBytes`, and `acceptedBytes`.
- `statistics` (object):
  - `totalSamples` (integer): Total accepted samples across both splits (`trainSamples + valSamples`).
  - `trainSamples` (integer): Count of samples in `data.jsonl`.
  - `valSamples` (integer): Count of samples in `val_data.jsonl` (e.g. `1500` for a 10% validation split of 15,000 total samples).
  - `acceptedRecords` (integer): Count of records accepted into dataset.
  - `rejectedRecords` (integer): Count of raw records filtered out due to quality scores, empty content, or parser rejection.
  - `duplicateRecords` (integer): Count of duplicate records detected and removed by global exact SHA-256 or MinHash shingle deduplication.
  - `totalTrainingBytes` (integer): Serialized sample content size in bytes across training and validation files.
  - `estimatedTokens` (integer): Estimated total token count across all accepted samples based on BPE tokenizer or word heuristic (`Math.max(1, ceil(words * 1.3))`).
- `schema` (object):
  - `sampleTypes`: Array of supported sample types (`"TEXT"`, `"INSTRUCTION"`, `"CODE"`, `"CHAT"`, `"CONVERSATION"`).
  - `columns`: Canonical column field names (`"id"`, `"type"`, `"prompt"`, `"response"`, `"qualityScore"`).

---

## 4. Sample Record Schema (`data.jsonl` / `val_data.jsonl`)

Each line in `data.jsonl` or `val_data.jsonl` is a single valid JSON line object representing a `NormalizedSample`.

### Instruction / Code Sample Record Example

```json
{
  "id": "sample-001",
  "type": "CODE",
  "text": "### Instruction:\n// Java class header\npublic class Calculator {\n\n### Response:\n  public int add(int a, int b) {\n    return a + b;\n  }\n}",
  "instruction": "// Java class header\npublic class Calculator {",
  "response": "  public int add(int a, int b) {\n    return a + b;\n  }\n}",
  "qualityScore": 0.95,
  "source": "EVO_CODEBASE",
  "tokenCount": 32,
  "hash": "a1b2c3d4e5f6..."
}
```

### Conversational / Chat Sample Record Example

```json
{
  "id": "sample-002",
  "type": "CHAT",
  "text": "### user:\nWhat is EVO Forge?\n\n### assistant:\nEVO Forge is a neural model training platform.",
  "messages": [
    {"role": "user", "text": "What is EVO Forge?", "messageId": "msg-01", "parentMessageId": null},
    {"role": "assistant", "text": "EVO Forge is a neural model training platform.", "messageId": "msg-02", "parentMessageId": "msg-01"}
  ],
  "qualityScore": 1.0,
  "source": "ultrachat_200k",
  "tokenCount": 24,
  "hash": "f6e5d4c3b2a1..."
}
```

### Field Requirements & Compatibility Semantics

- `id` (string, optional): Unique sample identifier. Serialized when non-null and preserved across round-trips.
- `type` / `sampleType` (string, required): Sample category. Readers MUST accept both `"type"` and `"sampleType"`. Standard values: `"TEXT"`, `"INSTRUCTION"`, `"CODE"`, `"CHAT"`, `"CONVERSATION"`.
- `text` (string, required): Full concatenated text representation. Code whitespace, tab indentation (`\t`), and line endings (`\n`) MUST be preserved.
- `instruction` (string, optional): Prompt or instruction string for instruction/code sample types.
- `response` (string, optional): Expected answer or output code for instruction/code sample types.
- `messages` (array, optional): List of message objects (`role`, `text`, `messageId`, `parentMessageId`) for chat or multi-turn conversational sample types.
- `qualityScore` (number, optional): Sample quality rating (0.0 to 1.0, default 1.0).
- `source` (string, optional): Origin source identifier or filename.
- `tokenCount` (integer, optional): Token count for training budget calculations.
- `hash` (string, optional): SHA-256 hash of the sample content for exact deduplication verification.
