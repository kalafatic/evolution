# EVO Forge — `.evodata` Dataset Artifact Architectural Review & Verification Report

**Document ID**: `genome-docs/11102026/evodata_arch_review.md`
**Date**: October 11, 2026
**Subsystem**: Dataset Preparation & Compilation (`eu.kalafatic.evolution.forge.data`)
**Artifact Format Version**: `1.0`

---

## 1. Executive Summary

This report delivers a thorough architectural and technical verification of the `.evodata` dataset artifact format within EVO Forge. Following an evidence-based codebase review across dataset acquisition, normalization, deduplication, compilation, persistence, and model training, we have verified the actual implementation behavior, resolved documentation/implementation discrepancies, strengthened artifact security against Zip Slip vulnerabilities, enhanced metadata structure while guaranteeing backward compatibility, and validated all changes through unit tests.

---

## 2. `.evodata` Production & Consumption Pipeline Map

```
[ heterogeneous Sources ]
(HuggingFace, Local Files, Git, EVO Codebase, .evodata Archives)
               │
               ▼
┌───────────────────────────────┐
│ 1. Acquisition & Discovery     │  TrainingDataAcquisitionService / SourceAdapters
└──────────────┬────────────────┘
               │ NormalizedSample
               ▼
┌───────────────────────────────┐
│ 2. Quality Filter & Scorer    │  DataCleaner & TrainingSampleQualityScorer
└──────────────┬────────────────┘  (Strips control chars, evaluates 0.0 - 1.0 quality score)
               │ Filtered Samples
               ▼
┌───────────────────────────────┐
│ 3. Deduplication Engine       │  DatasetDeduplicator
└──────────────┬────────────────┘  (SHA-256 exact matching + MinHash 5-gram shingle near-dup)
               │ Unique Samples
               ▼
┌───────────────────────────────┐
│ 4. Train / Val Splitter       │  EvoDatasetArtifact.save(...)
└──────────────┬────────────────┘  (Splits into trainSamples [data.jsonl] & valSamples [val_data.jsonl])
               │ Split Samples & Metadata
               ▼
┌───────────────────────────────┐
│ 5. Container Compilation      │  EvoDataWriter / EvoDatasetArtifact
└──────────────┬────────────────┘  (Creates dataset.evodata ZIP container with deterministic timestamps)
               │ dataset.evodata Archive
               ▼
┌───────────────────────────────┐
│ 6. Inspection & Analysis      │  SourceAnalysisAgent / EVODataAdapter / EvodataSourceAdapter
└──────────────┬────────────────┘  (Loads EvoDatasetArtifact for model analysis or reuse)
               │ NormalizedSamples
               ▼
┌───────────────────────────────┐
│ 7. Tokenization & Tensors     │  DatasetBuilder.buildTrainingSamples(...)
└──────────────┬────────────────┘  (Tokenizes prompts & responses, generates loss masks)
               │ TrainingSample Tensors
               ▼
┌───────────────────────────────┐
│ 8. EVO LLM Neural Training    │  EvoLlmTrainer / ForgeOrchestratorImpl
└───────────────────────────────┘  (Auto-regressive loss calculation on response tokens)
```

---

## 3. Specification-to-Implementation Compliance Matrix

| Artifact / Entry | Specification Requirement (`evodata-format.md`) | Verified Implementation (`eu.kalafatic.evolution.forge.data`) | Compliance Status |
| :--- | :--- | :--- | :--- |
| **Archive Extension** | Binary ZIP container named `*.evodata` | `EvoDatasetArtifact` ZipOutputStream / ZipInputStream | **100% Compliant** |
| **`data.jsonl`** | Training samples (JSON Lines format) | Written by `EvoDatasetArtifact.save()`, read by `load()` | **100% Compliant** |
| **`val_data.jsonl`** | Validation split samples (JSON Lines) | Written by `EvoDatasetArtifact.save()`, read by `load()` | **100% Compliant** |
| **`metadata.json`** | Identity, status, sources, statistics, schema | Written by `EvoDatasetArtifact.buildMetadataJson()` | **100% Compliant** |
| **`report.txt`** | Human-readable preparation report | Written by `EvoDatasetArtifact.buildReportText()` | **100% Compliant** |
| **Zip Entry Security** | Strict relative names, path traversal checks | `EvoDatasetArtifact.load()` Zip Slip filter (`..`, `/`, `\`) | **100% Verified & Hardened** |
| **Determinism** | Fixed ZipEntry modification timestamps | `FixedTimestamp` (`1704067200000L`) on all zip entries | **100% Compliant** |
| **Sample Schema** | JSON object per line matching `NormalizedSample` | `NormalizedSample.toJsonLine()`, `EvoDatasetArtifact.load()` | **100% Compliant** |
| **Field Aliases** | Accepts `"type"` and `"sampleType"` | `EvoDatasetArtifact.load()` reads `type` or `sampleType` | **100% Compliant** |
| **Sample ID** | Preserved sample identifier | `sample.getId()` written to JSON and restored in `load()` | **100% Compliant** |
| **Whitespace Rules**| Code indentation & line endings preserved | `escapeJson` unescaped accurately without trim corruption | **100% Compliant** |

---

## 4. Verified Inconsistencies & Root Cause Analysis

1. **Specification Example Math Typo (`valSamples = 15000`)**:
   - *Issue*: In Section 3 of `protocols/evodata-format.md`, the example declared `totalSamples: 15000`, `trainSamples: 13500`, and `valSamples: 15000`.
   - *Root Cause*: Manual typing error in the original specification document example.
   - *Correction*: Corrected `valSamples` in `protocols/evodata-format.md` to `1500` (`13500 + 1500 = 15000`).

2. **Sample Record Schema Field Discrepancy (`"sampleType"` vs `"type"`)**:
   - *Issue*: Section 3 declared `columns = ["id", "type", ...]`, while Section 4 sample record example used `"sampleType": "CODE"`.
   - *Root Cause*: Terminology divergence between early documentation drafts and `NormalizedSample.java` implementation.
   - *Correction*: Updated `protocols/evodata-format.md` example to `"type": "CODE"` and hardened `EvoDatasetArtifact.load()` to parse both `"type"` and `"sampleType"`.

3. **Metadata Structure Alignment**:
   - *Issue*: Code produced flat JSON key-values (`requestedUsableBytes`, `acceptedBytes`), whereas specification defined structured sections (`sources`, `statistics`, `schema`).
   - *Root Cause*: Early iterations favored flat serialization while specification proposed structured domain sections.
   - *Correction*: Updated `EvoDatasetArtifact.buildMetadataJson()` to emit structured `sources`, `statistics`, and `schema` sections alongside top-level flat properties, and updated `EvoDatasetArtifact.load()` to seamlessly parse both structured and legacy flat JSON metadata.

4. **Zip Slip Security & Determinism**:
   - *Issue*: `ZipInputStream` did not filter path traversal sequences (`..`), and Zip entries used variable current timestamps.
   - *Root Cause*: Missing zip entry path validation in early loader code.
   - *Correction*: Added path traversal checks in `EvoDatasetArtifact.load()` and fixed entry timestamps in `EvoDatasetArtifact.save()`.

---

## 5. Backward Compatibility Assessment

- **Existing `.evodata` Files**: Fully compatible. The updated `EvoDatasetArtifact.load()` reads flat legacy `metadata.json` files and structured files equally well.
- **Downstream Consumers**: `EVODataAdapter`, `EvodataSourceAdapter`, `SourceAnalysisAgent`, `ForgeOrchestratorImpl`, and `DatasetBuilder` depend on `EvoDatasetArtifact.load(...)`, which retains exact method signatures and sample output collections.

---

## 6. Recommended Next Steps

1. Continue using `.evodata` as the sole canonical binary archive format for training datasets across EVO Forge.
2. Monitor dataset acquisition statistics for large-scale multi-gigabyte datasets to verify MinHash shingle deduplication performance.
