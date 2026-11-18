package eu.kalafatic.evolution.forge.controller.service.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.EvaluationResult;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.JobState;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.PreflightResult;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.SmokeTestResult;
import eu.kalafatic.evolution.forge.controller.service.ForgeOrchestrator;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.ConsistencyAgent;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.KnowledgeUnit;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.SourceAnalysisAgent;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionRequest;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionResult;
import eu.kalafatic.evolution.forge.data.api.service.TrainingDataAcquisitionService;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.data.impl.MarkdownCleaner;
import eu.kalafatic.evolution.forge.data.impl.MarkdownLoader;
import eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl;
import eu.kalafatic.evolution.forge.data.impl.source.EvoCodebaseDatasetSource;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;
import eu.kalafatic.evolution.forge.data.impl.source.LocalDatasetSource;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.source.ForgeModelSource;
import eu.kalafatic.evolution.forge.model.source.ForgeModelSourceFactory;
import eu.kalafatic.evolution.forge.model.target.EvoModelValidator;
import eu.kalafatic.evolution.forge.model.target.EvoModelValidator.ModelValidationResult;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.api.TrainingCheckpoint;
import eu.kalafatic.evolution.forge.trainer.api.TrainingResult;
import eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

public class ForgeOrchestratorImpl implements ForgeOrchestrator {

    private final ForgeSourceAnalyzer sourceAnalyzer = new ForgeSourceAnalyzer();
    private final ForgeDatasetComposer datasetComposer = new ForgeDatasetComposer();
    private final ForgePreflightValidator preflightValidator = new ForgePreflightValidator();

    @Override
    public ForgeJob executeJob(ForgeJob job, Path projectPath) throws Exception {
        long timestamp = System.currentTimeMillis();
        Path runFolder = projectPath.resolve("dist/forging-" + job.getJobId() + "-" + timestamp);
        Files.createDirectories(runFolder);
        job.setRunDirectory(runFolder);
        Path logFile = runFolder.resolve("forging.log");

        try {
            logToFile(logFile, "=======================================================");
            logToFile(logFile, "Starting Authoritative Forge Job: " + job.getJobId());
            logToFile(logFile, "=======================================================");

            // STAGE 1: SOURCE ANALYSIS
            job.setState(JobState.ANALYZING);
            job.setProgressPercent(10);
            job.setCurrentStageDescription("Analyzing input training sources...");
            logToFile(logFile, "Stage 1: Source Analysis starting.");

            var profiles = sourceAnalyzer.analyzeSources(job.getSourcePaths());
            job.getSourceProfiles().clear();
            job.getSourceProfiles().addAll(profiles);
            logToFile(logFile, "Source analysis complete. Profiles created: " + profiles.size());

            // STAGE 2: DATASET COMPOSITION & PREPARATION
            job.setState(JobState.PREPARING);
            job.setProgressPercent(30);
            job.setCurrentStageDescription("Composing and preparing optimal dataset mix into canonical .evodata artifact...");
            logToFile(logFile, "Stage 2: Dataset Composition starting.");

            var composition = datasetComposer.computeComposition(profiles, job.getObjective(), job.getCompositionStrategy().getTokenBudget());
            job.setCompositionStrategy(composition);

            long requestedTargetBytes = ForgeJob.toBytes(job.getRequestedMinimumUsableBytes(), 524_288_000L);

            List<eu.kalafatic.evolution.forge.data.api.source.DatasetItem> datasetItems = new ArrayList<>();
            if (job.getSourcePaths() != null) {
                for (String src : job.getSourcePaths()) {
                    if (src != null && !src.trim().isEmpty()) {
                        datasetItems.add(new eu.kalafatic.evolution.forge.data.api.source.DatasetItem(true, src.trim(), "FILE"));
                    }
                }
            }
            if (datasetItems.isEmpty()) {
                datasetItems.add(new eu.kalafatic.evolution.forge.data.api.source.DatasetItem(true, projectPath.toString(), "FOLDER"));
            }

            eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext prepContext =
                    new eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext(requestedTargetBytes, msg -> logToFile(logFile, msg));
            eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService prepService =
                    new eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService();

            File datasetOutputDir = projectPath.resolve("dist/datasets").toFile();
            eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult prepResult =
                    prepService.prepareDatasets(datasetItems, prepContext, datasetOutputDir);

            String defaultTimestampName = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".evodata";
            String evodataFileName = (prepResult != null && prepResult.getOutputPath() != null && !prepResult.getOutputPath().isEmpty())
                    ? new File(prepResult.getOutputPath()).getName() : defaultTimestampName;
            logToFile(logFile, "[FORGE-TRAINING] input=" + evodataFileName);

            StringBuilder corpusBuilder = new StringBuilder();
            if (prepResult != null && prepResult.getArtifact() != null && prepResult.getArtifact().getSamples() != null) {
                for (NormalizedSample sample : prepResult.getArtifact().getSamples()) {
                    corpusBuilder.append(sample.toFullText()).append("\n\n");
                }
            }

            List<Path> scannedPaths = resolveScannedPaths(job.getSourcePaths(), projectPath, logFile);
            if (!scannedPaths.isEmpty()) {
                SourceAnalysisAgent sourceAnalysisAgent = new SourceAnalysisAgent();
                List<KnowledgeUnit> knowledgeUnits = sourceAnalysisAgent.analyze(scannedPaths, projectPath);

                ConsistencyAgent consistencyAgent = new ConsistencyAgent();
                List<ConsistencyAgent.ConsistencyViolation> consistencyViolations = consistencyAgent.checkConsistency(knowledgeUnits);
                logToFile(logFile, "Knowledge units analyzed: " + knowledgeUnits.size() + ", consistency conflicts: " + consistencyViolations.size());

                for (KnowledgeUnit unit : knowledgeUnits) {
                    if (unit.getContent() != null && !unit.getContent().trim().isEmpty()) {
                        corpusBuilder.append(unit.getContent()).append("\n\n");
                    }
                }
            }

            String corpus = corpusBuilder.toString();
            if (corpus.trim().isEmpty()) {
                MarkdownLoader loader = new MarkdownLoader();
                corpus = loader.loadFromDirectory(projectPath);
            }

            MarkdownCleaner cleaner = new MarkdownCleaner();
            String cleanCorpus = cleaner.clean(corpus);

            String primaryTargetStr = job.getSourcePaths().get(0);
            ForgeModelSource modelSource = ForgeModelSourceFactory.createSource(primaryTargetStr);

            if (modelSource.isPretrained() && modelSource.getForgeTarget().getArtifact() != null) {
                var parentArtifact = modelSource.getForgeTarget().getArtifact();
                job.getModelConfig().setHiddenSize(parentArtifact.getEmbeddingSize());
                job.getModelConfig().setLayers(parentArtifact.getLayers());
                job.getModelConfig().setHeads(parentArtifact.getHeads());
                job.getModelConfig().setDff(parentArtifact.getDff());
                job.getModelConfig().setMaxSeqLen(parentArtifact.getMaxSeqLen());
                logToFile(logFile, "Overriding model config with parent model architecture: hidden=" + parentArtifact.getEmbeddingSize());
            }

            SimpleBPETokenizer tokenizer = modelSource.getTokenizer(cleanCorpus, job.getModelConfig().getVocabSize());
            List<Integer> allTokens = tokenizer.encode(cleanCorpus);

            // Validate dataset/tokenizer compatibility before training: a dataset
            // that declares a tokenizer identity must match the training tokenizer.
            if (prepResult != null && prepResult.getArtifact() != null) {
                prepResult.getArtifact().validateTokenizerCompatibility("SimpleBPETokenizer", tokenizer.getVocabSize());
            }

            // Record the tokenizer identity inside the .evodata metadata so that a
            // dataset explicitly declares the tokenizer/vocabulary used to prepare it.
            final String[] datasetFingerprintHolder = {evodataFileName};
            try {
                if (prepResult != null && prepResult.getArtifact() != null
                        && prepResult.getArtifact().getArtifactFile() != null
                        && prepResult.getArtifact().getArtifactFile().exists()) {
                    prepResult.getArtifact().rewriteMetadataWithTokenizerIdentity(
                            "SimpleBPETokenizer",
                            "2.0",
                            tokenizer.getVocabSize(),
                            java.util.Arrays.asList("<unk>", "<s>", "</s>", "<pad>", "<eos>", "<bos>",
                                    "<|im_start|>", "<|im_end|>", "<|endoftext|>")
                    );
                }
                // Content fingerprint of the dataset file for checkpoint compatibility checks.
                File evodataInput = new File(prepResult != null ? prepResult.getOutputPath() : evodataFileName);
                if (evodataInput.exists()) {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    try (java.io.InputStream in = Files.newInputStream(evodataInput.toPath())) {
                        byte[] buf = new byte[8192];
                        int read;
                        while ((read = in.read(buf)) > 0) {
                            md.update(buf, 0, read);
                        }
                    }
                    StringBuilder hex = new StringBuilder();
                    for (byte b : md.digest()) {
                        hex.append(String.format("%02x", b));
                    }
                    datasetFingerprintHolder[0] = "sha256:" + hex;
                }
            } catch (Exception ex) {
                logToFile(logFile, "[FORGE][WARN] Could not record tokenizer identity / dataset fingerprint: " + ex.getMessage());
            }
            final String datasetFingerprint = datasetFingerprintHolder[0];

            DatasetBuilder datasetBuilder = new DatasetBuilder();
            List<DatasetBuilder.Sample> samples = datasetBuilder.buildSlidingWindow(allTokens, 16, 8);
            logToFile(logFile, "Dataset preparation complete. Total tokens: " + allTokens.size() + ", Training samples: " + samples.size());

            // STAGE 3: PREFLIGHT VALIDATION
            job.setState(JobState.READY_TO_TRAIN);
            job.setProgressPercent(40);
            job.setCurrentStageDescription("Performing pre-flight validation...");
            logToFile(logFile, "Stage 3: Preflight Validation starting.");

            PreflightResult preflight = preflightValidator.validate(job);
            job.setPreflightResult(preflight);
            if (!preflight.isPassed()) {
                String failureStr = "Preflight validation failed: " + String.join("; ", preflight.getErrors());
                logToFile(logFile, "[ERROR] " + failureStr);
                job.setState(JobState.FAILED);
                job.setFailureReason(failureStr);
                return job;
            }
            logToFile(logFile, "Preflight validation PASSED.");

            // STAGE 4: TRAINING
            job.setState(JobState.TRAINING);
            job.setProgressPercent(60);
            job.setCurrentStageDescription("Training EVO LLM model...");
            int plannedEpochs = job.getTrainingConfig().getEpochs();
            logToFile(logFile, String.format("[FORGE][TRAINING][RUN=%s] START plannedEpochs=%d", job.getJobId(), plannedEpochs));

            EvoLlmModel model = modelSource.createOrRestoreModel(
                    tokenizer.getVocabSize(),
                    job.getModelConfig().getHiddenSize(),
                    job.getModelConfig().getHeads(),
                    job.getModelConfig().getLayers(),
                    job.getModelConfig().getDff(),
                    job.getModelConfig().getMaxSeqLen()
            );

            EvoLlmTrainer trainer = new EvoLlmTrainer(model, EvoLlmTrainer.TrainingProfile.EVO_FAST);
            if (job.getTrainingConfig().getBatchSize() > 0) {
                trainer.setMicroBatchSize(job.getTrainingConfig().getBatchSize());
            }
            if (job.getTrainingConfig().getLearningRate() > 0) {
                trainer.setLearningRate((float) job.getTrainingConfig().getLearningRate());
            }
            if (job.getTrainingConfig().getValidationRatio() > 0) {
                trainer.setValidationSplitRatio((float) job.getTrainingConfig().getValidationRatio());
            }

            // Check if existing latest checkpoint is present in run directory to support resumption
            Path checkpointsDir = runFolder.resolve("checkpoints");
            Files.createDirectories(checkpointsDir);

            TrainingCheckpoint latestResumeChk = null;
            try (Stream<Path> chkFiles = Files.list(checkpointsDir)) {
                List<Path> sortedChks = chkFiles
                        .filter(p -> p.getFileName().toString().endsWith(".chk"))
                        .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                        .collect(Collectors.toList());
                if (!sortedChks.isEmpty()) {
                    latestResumeChk = TrainingCheckpoint.load(sortedChks.get(0));
                    logToFile(logFile, String.format("[FORGE][RESUME] Discovered existing checkpoint: %s (epoch %d/%d, steps: %d)",
                            sortedChks.get(0).getFileName(), latestResumeChk.getCompletedEpochs(), plannedEpochs, latestResumeChk.getTotalOptimizerSteps()));
                    trainer.setResumeCheckpoint(latestResumeChk);
                }
            } catch (Exception ex) {
                logToFile(logFile, "[FORGE][RESUME][WARN] Could not inspect checkpoints: " + ex.getMessage());
            }

            final Path chkRunFolder = runFolder;
            final Path chkStoreFolder = checkpointsDir;
            final String chkJobId = job.getJobId();
            final SimpleBPETokenizer chkTokenizer = tokenizer;
            final boolean saveCheckpoints = job.getTrainingConfig().isSaveEpochCheckpoints();
            final int checkpointFreq = Math.max(1, job.getTrainingConfig().getCheckpointFrequencyEpochs());

            trainer.addProgressListener(new EvoLlmTrainer.ProgressListener() {
                @Override
                public void onProgress(int epoch, int totalEpochs, int sampleIndex, int totalSamplesCount, double currentLoss) {}

                @Override
                public void onProgressMetrics(int epoch, int totalEpochs, int currentStep, int totalSteps,
                                              long processedTokens, long totalTokens,
                                              double tokensPerSec, double stepsPerSec,
                                              double estimatedRemainingSeconds, double currentLoss) {
                    // Map real optimizer-step progress into the 60-100% training band.
                    if (totalSteps > 0) {
                        job.setProgressPercent(60 + (int) (40.0 * currentStep / totalSteps));
                    }
                    if (epoch > 0) {
                        job.setCurrentStageDescription(String.format(
                                "Training epoch %d/%d (loss %.4f, %.1f tok/s, ETA %.0f s)",
                                epoch, totalEpochs, currentLoss, tokensPerSec, estimatedRemainingSeconds));
                    }
                }

                @Override
                public void onEpochCompleted(int epoch, int totalEpochs, double trainLoss, double valLoss, long durationMs) {
                    // Note: epoch argument is 1-based (1..totalEpochs)
                    logToFile(logFile, String.format("[FORGE][EPOCH] Epoch %d/%d completed in %s | Train Loss: %.4f | Val Loss: %.4f",
                            epoch, totalEpochs, eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer.formatDuration(durationMs), trainLoss, valLoss));

                    boolean isPeriodic = (epoch % checkpointFreq == 0);
                    boolean isFinal = (epoch == totalEpochs);

                    if (saveCheckpoints && (isPeriodic || isFinal)) {
                        try {
                            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                            String chkBaseName = String.format("model_%s_epoch_%d_%s", chkJobId, epoch, ts);

                            // 1. Save atomic .chk resumption container
                            TrainingCheckpoint chk = trainer.captureCheckpoint(
                                    chkJobId,
                                    chkBaseName,
                                    epoch,
                                    (float) trainLoss,
                                    (float) valLoss,
                                    datasetFingerprint,
                                    chkTokenizer.getVocab()
                            );
                            chk.saveAtomic(chkStoreFolder.resolve(String.format("checkpoint_%s_epoch_%04d.chk", chkJobId, epoch)));

                            // 2. Save .evo portable model artifact
                            EvoModelArtifact chkArtifact = new EvoModelArtifact();
                            chkArtifact.initializeFromModel(chkBaseName, model, chkTokenizer.getVocab());
                            chkArtifact.getMetadata().put("epoch", String.valueOf(epoch));
                            chkArtifact.getMetadata().put("trainLoss", String.valueOf(trainLoss));
                            chkArtifact.getMetadata().put("valLoss", String.valueOf(valLoss));
                            chkArtifact.getMetadata().put("epochDurationMs", String.valueOf(durationMs));
                            chkArtifact.save(chkRunFolder.resolve(chkBaseName + ".evo"));
                            logToFile(logFile, String.format("[FORGE][CHECKPOINT] Saved epoch %d checkpoint and model artifact (%s)", epoch, chkBaseName));
                        } catch (Exception ex) {
                            logToFile(logFile, String.format("[FORGE][CHECKPOINT][WARN] Failed to save epoch %d model checkpoint: %s", epoch, ex.getMessage()));
                        }
                    }
                }
            });

            try {
                Class<?> fsmClass = null;
                try {
                    fsmClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
                } catch (ClassNotFoundException e) {
                    if (Thread.currentThread().getContextClassLoader() != null) {
                        try {
                            fsmClass = Thread.currentThread().getContextClassLoader().loadClass("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
                        } catch (ClassNotFoundException ignored) {}
                    }
                }
                if (fsmClass != null) {
                    Object mgr = fsmClass.getMethod("getInstance").invoke(null);
                    fsmClass.getMethod("registerActiveTrainer", String.class, EvoLlmTrainer.class).invoke(mgr, job.getJobId(), trainer);
                }
            } catch (Exception ignored) {}

            List<NormalizedSample> normSamples = (prepResult != null && prepResult.getArtifact() != null && prepResult.getArtifact().getSamples() != null)
                    ? prepResult.getArtifact().getSamples() : java.util.Collections.emptyList();

            List<TrainingSample> trainingSamples;
            if (!normSamples.isEmpty()) {
                trainingSamples = datasetBuilder.buildTrainingSamples(normSamples, tokenizer, job.getModelConfig().getMaxSeqLen());
            } else {
                trainingSamples = samples.stream()
                        .map(s -> {
                            int len = s.input.size();
                            int[] inputIds = new int[len];
                            int[] labels = new int[len];
                            boolean[] lossMask = new boolean[len];
                            float[] attMask = new float[len];
                            for (int i = 0; i < len; i++) {
                                inputIds[i] = s.input.get(i);
                                labels[i] = (i + 1 < len) ? s.input.get(i + 1) : (s.target != null ? s.target : s.input.get(i));
                                lossMask[i] = true;
                                attMask[i] = 1.0f;
                            }
                            return new TrainingSample(inputIds, labels, lossMask, attMask);
                        })
                        .collect(Collectors.toList());
            }

            TrainingResult trainResult;
            try {
                trainResult = trainer.train(trainingSamples, job.getTrainingConfig().getEpochs());
            } finally {
                try {
                    Class<?> fsmClass = null;
                    try {
                        fsmClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
                    } catch (ClassNotFoundException e) {
                        if (Thread.currentThread().getContextClassLoader() != null) {
                            try {
                                fsmClass = Thread.currentThread().getContextClassLoader().loadClass("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
                            } catch (ClassNotFoundException ignored) {}
                        }
                    }
                    if (fsmClass != null) {
                        Object mgr = fsmClass.getMethod("getInstance").invoke(null);
                        fsmClass.getMethod("unregisterActiveTrainer", String.class).invoke(mgr, job.getJobId());
                    }
                } catch (Exception ignored) {}
            }
            job.setTrainingResult(trainResult);

            List<Double> losses = trainer.getLossHistory();
            double finalLoss = (losses != null && !losses.isEmpty()) ? losses.get(losses.size() - 1)
                    : (trainResult != null ? trainResult.getLastLoss() : Double.NaN);

            long totalJobForgingDurationMs = System.currentTimeMillis() - timestamp;
            logToFile(logFile, String.format("[FORGE][TRAINING][RUN=%s] %s completedEpochs=%d plannedEpochs=%d totalForgingDuration=%s",
                    job.getJobId(), trainResult.getTerminationStatus(), trainResult.getCompletedEpochs(), trainResult.getPlannedEpochs(),
                    eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer.formatDuration(totalJobForgingDurationMs)));

            // Handle ABORT and PAUSE outcomes before finalizing
            if (trainResult.getTerminationStatus() == eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus.TRAINING_ABORTED) {
                logToFile(logFile, String.format("[FORGE][RUN=%s] Training aborted by user request. Discarding unfinished model.", job.getJobId()));
                job.setState(JobState.ABORTED);
                job.setJobResultSummary("ABORTED");
                job.setCurrentStageDescription("Training was aborted. Model export skipped.");
                return job;
            }

            if (trainResult.getTerminationStatus() == eu.kalafatic.evolution.forge.trainer.api.TrainingTerminationStatus.TRAINING_PAUSED) {
                // Ensure a resumption checkpoint is preserved in checkpoints directory
                Path chkDir = runFolder.resolve("checkpoints");
                Files.createDirectories(chkDir);
                String chkBaseName = String.format("checkpoint_%s_epoch_%04d.chk", job.getJobId(), trainResult.getCompletedEpochs());
                TrainingCheckpoint pausedChk = trainer.captureCheckpoint(
                        job.getJobId(),
                        "checkpoint_" + job.getJobId(),
                        trainResult.getCompletedEpochs(),
                        (float) trainResult.getLastLoss(),
                        (float) trainResult.getValLoss(),
                        evodataFileName,
                        tokenizer.getVocab()
                );
                pausedChk.saveAtomic(chkDir.resolve(chkBaseName));
                logToFile(logFile, String.format("[FORGE][RUN=%s] Training paused at epoch %d. Checkpoint saved: %s",
                        job.getJobId(), trainResult.getCompletedEpochs(), chkBaseName));
                job.setState(JobState.PAUSED);
                job.setJobResultSummary("PAUSED");
                job.setCurrentStageDescription(String.format("Training paused cleanly at epoch %d/%d. Ready to resume.",
                        trainResult.getCompletedEpochs(), trainResult.getPlannedEpochs()));
                return job;
            }

            // STAGE 5: MODEL VALIDATION BEFORE EXPORT
            logToFile(logFile, String.format("[FORGE][MODEL][RUN=%s] FINALIZE_START", job.getJobId()));
            EvoModelValidator modelValidator = new EvoModelValidator();
            ModelValidationResult modelValRes = modelValidator.validateModel(model, tokenizer.getVocab());

            if (trainResult.isFailed() || !modelValRes.isPassed()) {
                String failureDetails = trainResult.isFailed()
                        ? trainResult.getFailureDetails()
                        : "Model validation failed: " + String.join("; ", modelValRes.getErrors());
                logToFile(logFile, String.format("[FORGE][MODEL][RUN=%s] VALIDATION_FAILURE details=%s", job.getJobId(), failureDetails));
                logToFile(logFile, String.format("[FORGE][EXPORT][RUN=%s] EXPORT_BLOCKED reason=CORRUPTED_OR_FAILED_MODEL", job.getJobId()));

                job.setState(JobState.FAILED);
                job.setJobResultSummary("FAILED");
                job.setFailureReason(failureDetails);
                logToFile(logFile, String.format("[FORGE][RUN=%s] FINAL_STATUS=FAILED", job.getJobId()));
                return job;
            }

            logToFile(logFile, String.format("[FORGE][MODEL][RUN=%s] VALIDATION_SUCCESS params=%d", job.getJobId(), modelValRes.getParameterCount()));

            // STAGE 5.5: EVALUATION
            job.setState(JobState.EVALUATING);
            job.setProgressPercent(75);
            job.setCurrentStageDescription("Evaluating model loss and quality...");

            EvaluationResult eval = new EvaluationResult();
            // Measured values only: real training loss and real validation loss.
            // When no validation split was configured, valLoss is honestly 0 (unknown),
            // not fabricated from the training loss.
            eval.setTrainLoss(finalLoss);
            eval.setValLoss(trainResult.getValLoss());
            eval.setQualityScore(-1.0); // -1 = not computed (no documented metric exists yet)
            // Overfitting flag is only meaningful with a real validation signal:
            // a large and consistent val-train gap. Reported honestly when unknown.
            boolean valAvailable = trainResult.getValLoss() > 0.0;
            eval.setOverfittingDetected(valAvailable && trainResult.getValLoss() > finalLoss + 0.2);
            eval.setSummary(valAvailable
                    ? String.format("Train loss %.4f, validation loss %.4f (epochs: %d/%d).",
                            finalLoss, trainResult.getValLoss(), trainResult.getCompletedEpochs(), trainResult.getPlannedEpochs())
                    : String.format("Train loss %.4f; no validation split configured (epochs: %d/%d).",
                            finalLoss, trainResult.getCompletedEpochs(), trainResult.getPlannedEpochs()));
            job.setEvaluationResult(eval);

            // STAGE 6: FINALIZING & PERSISTENCE
            job.setState(JobState.FINALIZING);
            job.setProgressPercent(85);
            job.setCurrentStageDescription("Serializing .evo model and GGUF exports...");

            String dateVersion = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date(timestamp));
            String modelName = "evo-" + job.getJobId() + "-" + dateVersion;
            job.setOutputModelName(modelName);
            Path exportPath = projectPath.resolve("dist/" + modelName);

            EvoModelArtifact artifact = new EvoModelArtifact();
            artifact.initializeFromModel(modelName, model, tokenizer.getVocab());
            artifact.getMetadata().put("forge_mode", job.getTrainingConfig().getForgeMode());
            artifact.getMetadata().put("objective", job.getObjective().name());

            // Provenance and Early Termination Metadata
            artifact.getMetadata().put("trainingStatus", trainResult.getTerminationStatus().name());
            artifact.getMetadata().put("terminationReason", trainResult.getTerminationReason().name());
            artifact.getMetadata().put("plannedEpochs", String.valueOf(trainResult.getPlannedEpochs()));
            artifact.getMetadata().put("completedEpochs", String.valueOf(trainResult.getCompletedEpochs()));
            artifact.getMetadata().put("plannedSteps", String.valueOf(trainResult.getPlannedSteps()));
            artifact.getMetadata().put("completedSteps", String.valueOf(trainResult.getCompletedSteps()));
            artifact.getMetadata().put("trainingDurationMs", String.valueOf(trainResult.getTrainingDurationMs()));
            artifact.getMetadata().put("earlyStop", String.valueOf(trainResult.isStoppedEarly()));

            // Record Git EVO Model Lineage & Reproducibility Chain
            String gitCommit = resolveGitCommit(projectPath.toFile());
            String gitBranch = resolveGitBranch(projectPath.toFile());
            String gitWorkingTree = resolveGitWorkingTree(projectPath.toFile());

            artifact.getMetadata().put("source_repository", projectPath.getFileName().toString());
            artifact.getMetadata().put("source_commit", gitCommit);
            artifact.getMetadata().put("branch", gitBranch);
            artifact.getMetadata().put("working_tree_state", gitWorkingTree);
            artifact.getMetadata().put("dataset_composition", job.getCompositionStrategy().getSourceWeights().toString());
            artifact.getMetadata().put("hardware_profile", "MaxMemory: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "MB, Cores: " + Runtime.getRuntime().availableProcessors());

            artifact.recalculateManifestAndHash();
            artifact.getMetadata().put("resulting_model_hash", artifact.getModelContentHash() != null ? artifact.getModelContentHash() : "");

            logToFile(logFile, String.format("[FORGE][EXPORT][RUN=%s] EVO_EXPORT_START", job.getJobId()));
            artifact.save(runFolder.resolve(modelName + ".evo"));
            artifact.save(runFolder.resolve("evo.evo"));
            Files.createDirectories(exportPath);
            artifact.save(exportPath.resolve(modelName + ".evo"));
            artifact.save(exportPath.resolve("evo.evo"));
            logToFile(logFile, String.format("[FORGE][EXPORT][RUN=%s] EVO_EXPORT_SUCCESS", job.getJobId()));

            // QUANTIZATION STAGE
            if (job.getQuantizationConfig() != null && job.getQuantizationConfig().isEnabled()) {
                logToFile(logFile, String.format("[FORGE][QUANT][RUN=%s] QUANTIZATION_START scheme=%s", job.getJobId(), job.getQuantizationConfig().getScheme().name()));
                eu.kalafatic.evolution.forge.model.llm.EvoModelQuantizer quantizer = new eu.kalafatic.evolution.forge.model.llm.EvoModelQuantizer();
                eu.kalafatic.evolution.forge.model.llm.EvoModelQuantizer.QuantizationResult qRes = quantizer.quantizeArtifact(artifact, job.getQuantizationConfig());
                EvoModelArtifact quantArtifact = qRes.getQuantizedArtifact();

                logToFile(logFile, String.format("[FORGE][QUANT][RUN=%s] QUANTIZATION_COMPLETE originalBytes=%d quantizedBytes=%d ratio=%.4f",
                        job.getJobId(), qRes.getOriginalSize(), qRes.getQuantizedSize(), qRes.getCompressionRatio()));

                String quantModelName = modelName + "-quant-" + job.getQuantizationConfig().getScheme().name().toLowerCase();
                quantArtifact.save(runFolder.resolve(quantModelName + ".evo"));
                quantArtifact.save(exportPath.resolve(quantModelName + ".evo"));
            }

            logToFile(logFile, String.format("[FORGE][EXPORT][RUN=%s] GGUF_EXPORT_START", job.getJobId()));
            OllamaExporter exporter = new OllamaExporter();
            exporter.exportSnapshot(artifact.toSnapshot(), exportPath, job.getQuantizationConfig());
            logToFile(logFile, String.format("[FORGE][EXPORT][RUN=%s] GGUF_EXPORT_SUCCESS", job.getJobId()));

            // Mirror artifacts to controller models directory
            mirrorArtifactsToControllerModels(exportPath, modelName, logFile);

            // STAGE 7: NATIVE INFERENCE SMOKE TEST
            job.setState(JobState.VALIDATING);
            job.setProgressPercent(95);
            job.setCurrentStageDescription("Executing native inference smoke test...");
            logToFile(logFile, "Stage 7: Native Inference Smoke Test starting.");

            SmokeTestResult smokeTest = runInferenceSmokeTest(runFolder.resolve(modelName + ".evo"));
            job.setSmokeTestResult(smokeTest);

            if (!smokeTest.isPassed()) {
                logToFile(logFile, "[WARN] Smoke test issues detected: " + smokeTest.getError());
            } else {
                logToFile(logFile, "Native inference smoke test PASSED. Generated output: " + smokeTest.getGeneratedText());
            }

            // FINAL RESULT DETERMINATION
            if (trainResult.isStoppedEarly()) {
                job.setState(JobState.STOPPED);
                job.setJobResultSummary("SUCCESS_WITH_EARLY_STOP");
                job.setProgressPercent(100);
                job.setCurrentStageDescription(String.format("Training stopped cleanly after %d/%d epochs (%s). Valid model exported.",
                        trainResult.getCompletedEpochs(), trainResult.getPlannedEpochs(), trainResult.getTerminationReason()));
                logToFile(logFile, String.format("[FORGE][RUN=%s] FINAL_STATUS=SUCCESS_WITH_EARLY_STOP", job.getJobId()));
            } else {
                job.setState(JobState.COMPLETED);
                job.setJobResultSummary("SUCCESS");
                job.setProgressPercent(100);
                job.setCurrentStageDescription("Forge job completed successfully!");
                logToFile(logFile, String.format("[FORGE][RUN=%s] FINAL_STATUS=SUCCESS", job.getJobId()));
            }

            return job;

        } catch (Exception e) {
            logToFile(logFile, "[ERROR] Job execution failed: " + e.getMessage());
            job.setState(JobState.FAILED);
            job.setFailureReason(e.getMessage());
            throw e;
        }
    }

    private SmokeTestResult runInferenceSmokeTest(Path evoModelPath) {
        SmokeTestResult result = new SmokeTestResult();
        long start = System.currentTimeMillis();
        String prompt = "EVO Forge";
        result.setPromptUsed(prompt);

        try {
            EvoModelArtifact artifact = EvoModelArtifact.load(evoModelPath);
            ReferenceEvoInferenceEngine engine = new ReferenceEvoInferenceEngine();

            InferenceRequest req = InferenceRequest.builder()
                    .prompt(prompt)
                    .maxTokens(32)
                    .temperature(0.0f)
                    .build();

            InferenceResult inferRes = engine.generateFromArtifact(artifact, req, null);
            long latency = System.currentTimeMillis() - start;

            result.setLatencyMs(latency);
            result.setGeneratedText(inferRes.getGeneratedText());

            if (inferRes.getGeneratedText() != null && !inferRes.getGeneratedText().trim().isEmpty() && !inferRes.getGeneratedText().contains("NaN")) {
                result.setPassed(true);
            } else if (inferRes.getGeneratedTokenCount() > 0 && !inferRes.getGeneratedText().contains("NaN")) {
                result.setPassed(true);
            } else {
                result.setPassed(false);
                result.setError("Empty or invalid output generated during smoke test");
            }
        } catch (Exception e) {
            result.setPassed(false);
            result.setError(e.getMessage());
        }

        return result;
    }

    private List<Path> resolveScannedPaths(List<String> sourcePaths, Path projectPath, Path logFile) {
        List<Path> scanned = new ArrayList<>();
        if (sourcePaths == null || sourcePaths.isEmpty()) {
            return scanned;
        }

        for (String sourceStr : sourcePaths) {
            try {
                Path path = Paths.get(sourceStr);
                if (Files.isRegularFile(path)) {
                    scanned.add(path);
                } else if (Files.isDirectory(path)) {
                    try (Stream<Path> walk = Files.walk(path)) {
                        walk.filter(Files::isRegularFile)
                            .filter(p -> !p.toString().contains("/.git/") && !p.toString().contains("\\.git\\")
                                      && !p.toString().contains("/target/") && !p.toString().contains("\\target\\"))
                            .forEach(scanned::add);
                    }
                }
            } catch (Exception e) {
                logToFile(logFile, "Failed to resolve source path: " + sourceStr);
            }
        }
        return scanned;
    }

    private void mirrorArtifactsToControllerModels(Path exportPath, String modelName, Path logFile) {
        try {
            Class<?> clazz = Class.forName("eu.kalafatic.evolution.controller.manager.ProjectModelManager");
            String targetCodebase = (String) clazz.getMethod("getCodebasePath").invoke(null);
            if (targetCodebase != null) {
                Path controllerModelsDir = Paths.get(targetCodebase).resolve("eu.kalafatic.evolution.controller/lib/models");
                Files.createDirectories(controllerModelsDir);

                if (Files.exists(exportPath.resolve("exports/ollama/evo.gguf"))) {
                    Files.copy(exportPath.resolve("exports/ollama/evo.gguf"), controllerModelsDir.resolve("evo.gguf"), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(exportPath.resolve("exports/ollama/evo.gguf"), controllerModelsDir.resolve(modelName + ".gguf"), StandardCopyOption.REPLACE_EXISTING);
                }
                try (Stream<Path> stream = Files.list(exportPath)) {
                    stream.filter(f -> f.getFileName().toString().endsWith(".evo"))
                          .forEach(evoFile -> {
                              try {
                                  Files.copy(evoFile, controllerModelsDir.resolve("evo.evo"), StandardCopyOption.REPLACE_EXISTING);
                                  Files.copy(evoFile, controllerModelsDir.resolve(modelName + ".evo"), StandardCopyOption.REPLACE_EXISTING);
                              } catch (Exception ignored) {}
                          });
                }
                logToFile(logFile, "Mirrored artifacts to controller lib/models: " + controllerModelsDir.toAbsolutePath());
            }
        } catch (Exception e) {
            logToFile(logFile, "Warning: Artifact mirroring to controller lib/models failed: " + e.getMessage());
        }
    }

    private String resolveGitCommit(java.io.File dir) {
        try {
            Process p = Runtime.getRuntime().exec("git rev-parse HEAD", null, dir);
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                return line != null ? line.trim() : "head-local";
            }
        } catch (Exception e) {
            return "head-local";
        }
    }

    private String resolveGitBranch(java.io.File dir) {
        try {
            Process p = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD", null, dir);
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                return line != null ? line.trim() : "main";
            }
        } catch (Exception e) {
            return "main";
        }
    }

    private String resolveGitWorkingTree(java.io.File dir) {
        try {
            Process p = Runtime.getRuntime().exec("git status --porcelain", null, dir);
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                return line != null && !line.trim().isEmpty() ? "dirty" : "clean";
            }
        } catch (Exception e) {
            return "clean";
        }
    }

    private void logToFile(Path logFile, String msg) {
        try {
            String formatted = String.format("[%s] %s\n", java.time.Instant.now().toString(), msg);
            Files.writeString(logFile, formatted, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            System.out.println(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
