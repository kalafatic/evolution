# .evodata Dataset Artifact Specification

**Protocol Identifier**: `protocols/evodata-format.md`
**Version**: `1.0`
**Owner**: Dataset Preparation Subsystem (`eu.kalafatic.evolution.forge.data`)

---

## 1. Specification Overview

An `.evodata` file is a self-contained ZIP archive containing normalized, quality-filtered, deduplicated training samples and complete dataset provenance metadata.

---

## 2. Archive File Directory Structure

```text
dataset.evodata (ZIP Archive)
    ├── metadata.json       # Dataset identity, statistics, splits, and source provenance
    ├── data.jsonl          # Training samples (JSON Lines format)
    ├── val_data.jsonl      # Validation samples (JSON Lines format)
    └── report.txt          # Human-readable acquisition and preparation summary report
```

---

## 3. Metadata Specification (`metadata.json`)

```json
{
  "formatVersion": "1.0",
  "datasetId": "evodata-codebase-v1",
  "createdTimestamp": 1725868800000,
  "status": "READY",
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
    "valSamples": 15000,
    "acceptedRecords": 15000,
    "rejectedRecords": 200,
    "duplicateRecords": 150,
    "totalTrainingBytes": 8388608,
    "estimatedTokens": 2100000
  },
  "schema": {
    "sampleTypes": ["TEXT", "INSTRUCTION", "CODE"],
    "columns": ["id", "type", "prompt", "response", "qualityScore"]
  }
}
```

---

## 4. Sample Record Schema (`data.jsonl` / `val_data.jsonl`)

Each line in `data.jsonl` is a valid JSON object matching `NormalizedSample`:

```json
{
  "id": "sample-001",
  "sampleType": "CODE",
  "prompt": "// Java class header\npublic class Calculator {",
  "response": "  public int add(int a, int b) {\n    return a + b;\n  }\n}",
  "qualityScore": 0.95,
  "source": "EVO_CODEBASE"
}
```
