# EVO Native Dataset (.evodata) Export Architecture & Execution Pipeline

## Executive Overview

The `.evodata` export and preparation subsystem converts heterogeneous dataset sources—including raw text (`.txt`), structured JSON/JSONL (`.json`, `.jsonl`), binary Parquet (`.parquet`), local filesystem folders, Git repositories, Hugging Face Hub datasets, and existing `.evodata` artifacts—into a canonical, compressed, deduplicated, and metadata-enriched binary EVO Training Dataset archive (`.evodata`).

Exporting to `.evodata` operates via **two primary execution pathways**:
1. **Direct In-Process Java API Pathway**: Leveraged by Java SWT UI components (`DatasetEditorGroup` in the Properties Page and `ForgeSettingsDialog` in AI Commander) for low-latency background execution within the local Eclipse RCP/workbench process.
2. **REST Web Service HTTP Endpoint Pathway**: Exposed by the embedded `EvolutionServer` (`POST /forge/dataset/prepare`) for web client interfaces (`forge.html`), remote CLI runners, or autonomous agent execution via the Adaptive Cognitive Control Loop (`CognitiveLoopEngine`).

---

## Architectural Systems & Dual Execution Pathways

```
+-----------------------------------------------------------------------------------+
|                                  USER INTERFACES                                  |
|  +-----------------------------------+     +-----------------------------------+  |
|  |  Properties Page Dataset Group    |     |  AI Commander Forge Settings      |  |
|  |       (DatasetEditorGroup)        |     |       (ForgeSettingsDialog)       |  |
|  +-----------------+-----------------+     +-----------------+-----------------+  |
|                    |                                         |                    |
+--------------------|-----------------------------------------|--------------------+
                     | [Direct Java API Call]                  | [Direct Java API Call]
                     v                                         v
+-----------------------------------------------------------------------------------+
|                         LOCAL IN-PROCESS JAVA PIPELINE                            |
|             eu.kalafatic.evolution.forge.data.impl.service                        |
|                                                                                   |
|  DatasetPreparationService.prepareDatasets(apiItems, context, outputDir)          |
+------------------------------------+----------------------------------------------+
                                     ^
                                     | [In-Process Invocation]
+------------------------------------|----------------------------------------------+
|                        REST WEB SERVICE HTTP ENDPOINT                             |
|             eu.kalafatic.evolution.controller.orchestration                       |
|                                                                                   |
|  1. EvolutionServer.handleDatasetPrepare(session)                                 |
|  2. SessionContainer & TaskContext Initialization                                |
|  3. CognitiveLoopEngine.solve(sessionCont, taskContext, cognitiveGoal)            |
|  4. CognitiveStrategy -> DatasetAcquisitionTool.execute(command, dir, context)    |
|  5. TrainingDataAcquisitionServiceImpl.acquireDataset(request)                    |
+-----------------------------------------------------------------------------------+
```

---

## Detailed Ordered Method Calls for `.evodata` Export

### Pathway 1: Direct In-Process Java API Pathway (SWT UI / ForgeSettingsDialog)

When a user triggers **"Export to .evodata"** or **"Create Native .evodata"** in `DatasetEditorGroup` or `ForgeSettingsDialog`:

1. `DatasetEditorGroup.handleExportEvodata()` / `ForgeSettingsDialog.handleCreateEvodata()`
   - Constructs list of `DatasetItem` objects containing checked source paths and item types (`FILE`, `FOLDER`, `REPOSITORY`).
   - Resolves target output directory via `DatasetAcquisitionTool.resolveDatasetOutputDir(customOutputDir, repo)`.
   - Spawns a background worker thread (`new Thread(...)`).

2. `DatasetPreparationService.prepareDatasets(List<DatasetItem> items, DatasetPreparationContext context, File outputDir)`
   - Validates input items and creates timestamped destination file: `<outputDir>/yyyyMMdd_HHmmss.evodata`.
   - Generates deterministic `cacheKey` and verifies whether a matching cached `.evodata` file already exists.

3. `FolderAdapter.convert(File folder, DatasetPreparationContext context)` (for `FOLDER` items)
   - Iterates through candidate data files in directory hierarchy.
   - Evaluates `DatasetMetadataFilter.isDataFile(File file)` to skip `.git/`, `.gitignore`, `README*`, `LICENSE*`, `.cache/`, `dataset_info.json`.
   - Queries `SourceAdapterRegistry.getAdapter(File file)`.

4. `SourceAdapterRegistry.getAdapter(File file)` -> returns concrete adapter (`JSONLAdapter`, `JSONAdapter`, `TextAdapter`, `ParquetAdapter`, `EVODataAdapter`).

5. `DatasetSourceAdapter.extractSamples(File file, DatasetPreparationContext context)`
   - Parses records into `NormalizedSample` instances (`CONVERSATION`, `INSTRUCTION`, or `TEXT`).

6. `DataCleaner.clean(NormalizedSample sample)`
   - Strips non-printable ASCII control characters, normalizes UTF-8 Unicode, collapses excess whitespace.

7. `TrainingSampleQualityScorer.calculateQualityScore(NormalizedSample sample)`
   - Filters out samples scoring below the required quality threshold (e.g. `minQuality = 0.5`).

8. `DatasetDeduplicator.deduplicate(List<NormalizedSample> samples)`
   - Computes SHA-256 exact hashes and MinHash 5-gram shingle fingerprints (for documents $\ge 150$ characters) to reject exact and near-duplicates.

9. `DatasetSampler.split(List<NormalizedSample> samples, double valSplitRatio)`
   - Partitions clean, accepted samples into `train` and `validation` subsets.

10. `EvoDataWriter.write(File targetFile, List<NormalizedSample> samples, DatasetSourceConfig config, DatasetSourceStats stats, double valSplitRatio)`
    - Instantiates `EvoDatasetArtifact(targetFile)`.
    - Writes `manifest.json`, `train.jsonl`, `val.jsonl`, and metadata into the `.evodata` zip container.
    - Computes SHA-256 artifact hash and returns `DatasetPreparationResult`.

11. `Display.getDefault().asyncExec(...)`
    - Returns execution to SWT UI thread, updates `reportArea` and text controls (`repoText`, `outputDirText`, `maxSizeMbText`) with actual output file properties, and displays `MessageDialog.openInformation`.

---

### Pathway 2: Web Service REST HTTP Endpoint Pathway (`EvolutionServer`)

When calling `POST /forge/dataset/prepare`:

```json
POST /forge/dataset/prepare HTTP/1.1
Host: localhost:48080
Content-Type: application/json
x-evo-runtime: SWT

{
  "sourceType": "HUGGING_FACE",
  "repository": "tatsu-lab/alpaca",
  "split": "train",
  "targetUsableBytes": 52428800,
  "outputDir": "/home/user/workspace/forge-input"
}
```

#### Method Call Sequence:

1. `EvolutionServer.serve(IHTTPSession session)` -> `EvolutionServer.handleInternal(session)`
   - Routes URI `/forge/dataset/prepare` to `handleDatasetPrepare(session)`.

2. `EvolutionServer.handleDatasetPrepare(IHTTPSession session)`
   - Parses HTTP JSON payload (`body`).
   - Resolves target directory via `DatasetAcquisitionTool.resolveDatasetOutputDir(rawOutputDir, repo)`.
   - Creates or retrieves `SessionContainer` via `SessionManager.getInstance().getOrCreateSession(sessionId)`.
   - Initializes `TaskContext` and calculates `ExecutionProfile` via `EvolutionIntensityCalculator.calculate(taskContext, null, null)`.
   - Constructs `CognitiveGoal("Acquire and prepare ...")` with `targetUsableBytes`.

3. `CognitiveLoopEngine.solve(SessionContainer session, TaskContext context, CognitiveGoal goal)`
   - Evaluates current `WorldState` and selects an untried strategy candidate.
   - Executes strategy capability: `DatasetAcquisitionTool.execute(command, workingDir, context)`.

4. `DatasetAcquisitionTool.execute(String command, File workingDir, TaskContext context)`
   - Builds `TrainingDataPreferences` with `minimumUsableBytes` and `targetUsableBytes`.
   - Instantiates `HuggingFaceDatasetSource` or `LocalDatasetSource` based on `sourceType`.
   - Calls `TrainingDataAcquisitionServiceImpl.acquireDataset(request)`.

5. `TrainingDataAcquisitionServiceImpl.acquireDataset(TrainingDataAcquisitionRequest request)`
   - Iterates through configured sources, fetching page chunks or files.
   - Delegates normalization, cleaning, deduplication, and compilation to `DatasetPreparationService.prepareDatasets(...)`.
   - Returns `TrainingDataAcquisitionResult` with exact byte metrics (`acceptedContentBytes`, `rejectedBytes`, `duplicateBytes`).

6. `CognitiveLoopEngine` Goal Evaluation
   - Computes math progress: `usableBytes / targetUsableBytes`.
   - Records `CognitiveObservation` with `usableBytes` and failure classification (`TARGET_REACHED` or `SOURCE_EXHAUSTED`).

7. `EvolutionServer.handleDatasetPrepare` HTTP Response Formatting
   - Serializes JSON result:
     ```json
     {
       "sessionId": "dataset_acq_12345",
       "status": "SUCCESS",
       "cognitiveState": "COMPLETED",
       "requestedUsableBytes": 52428800,
       "actualUsableBytes": 52428800,
       "attempts": 1,
       "summary": "Acquired 52.43 MB usable training content."
     }
     ```
   - Returns `NanoHTTPD.newFixedLengthResponse(Status.OK, "application/json", result)`.

---

## Summary Matrix

| Attribute | Direct Java API Pathway | REST Web Service Pathway |
| :--- | :--- | :--- |
| **Primary Entry Point** | `DatasetPreparationService.prepareDatasets(...)` | `EvolutionServer.handleDatasetPrepare(...)` |
| **Caller** | `DatasetEditorGroup` / `ForgeSettingsDialog` (SWT UI) | Web UI (`forge.html`), HTTP REST clients, Agents |
| **Execution Loop** | Direct Background Thread (`new Thread`) | `CognitiveLoopEngine` Adaptive Control Loop |
| **Output Container** | Timestamped `.evodata` zip archive | Timestamped `.evodata` zip archive |
| **Accounting Invariant** | `recordsRead = accepted + rejected + duplicate` | `recordsRead = accepted + rejected + duplicate` |
