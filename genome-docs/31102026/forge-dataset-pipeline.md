# EVO Forge Training Data Preparation & Acquisition Pipeline

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/forge-dataset-pipeline.md`

---

## 1. Pipeline Overview & Components

The `.evodata` preparation pipeline in `eu.kalafatic.evolution.forge.data` converts heterogeneous datasets into deterministic ZIP archives:

```
INPUT SOURCES (Hugging Face, Local JSONL, CSV, Parquet, TXT, PDF)
  ↓
SOURCE DISCOVERY (HuggingFaceSourceDiscovery, DatasetSourceConfig)
  ↓
FORMAT-SPECIFIC ADAPTERS (ParquetAdapter, JSONLAdapter, CSVAdapter, TextAdapter)
  ↓
CLEANING & NORMALIZATION (DataCleaner, NormalizedSample)
  ↓
DEDUPLICATION (DatasetDeduplicator - SHA-256 & 5-gram token MinHash)
  ↓
QUALITY SCORING (TrainingSampleQualityScorer)
  ↓
LEAKAGE PREVENTION & PARTITIONING (Train/Val Separation)
  ↓
NATIVE .EVODATA CONTAINER (EvoDatasetArtifact: metadata.json, data.jsonl, val_data.jsonl, report.txt)
```
