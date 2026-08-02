package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressEvent;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.EvolutionStage;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.FinalResponse;
import eu.kalafatic.evolution.controller.orchestration.ExecutionMetrics;
import eu.kalafatic.evolution.controller.orchestration.FileReference;
import eu.kalafatic.evolution.model.orchestration.ChatSession;

import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.data.impl.MarkdownCleaner;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;

// Sub-agents imports
import eu.kalafatic.evolution.forge.controller.service.impl.agents.*;

public class LLMDarwinEngine extends ADarwinEngine {

    // Centralized model capacity profiles
    public enum Profile {
        SMALL(128, 3, 4, 64, 4, 4000),
        DEFAULT(256, 6, 8, 128, 8, 8000),
        LARGE(384, 8, 8, 256, 10, 16000);

        public final int embeddingSize;
        public final int layers;
        public final int heads;
        public final int maxSeqLen;
        public final int epochs;
        public final int vocabSize;

        Profile(int embeddingSize, int layers, int heads, int maxSeqLen, int epochs, int vocabSize) {
            this.embeddingSize = embeddingSize;
            this.layers = layers;
            this.heads = heads;
            this.maxSeqLen = maxSeqLen;
            this.epochs = epochs;
            this.vocabSize = vocabSize;
        }
    }

    // Configurable bounds on corpus files and sizes to prevent OutOfMemory
    private static final int MAX_CORPUS_CHARS = 100_000;
    private static final int MAX_CORPUS_FILES = 100;
    private static final int MAX_TOKENS_LIMIT = 20_000;
    private static final int MAX_TRAINING_SAMPLES = 5_000;

    // Shared HttpClient with 10s connect timeout
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class LlmConfig {
        public int vocabSize;
        public int embeddingSize;
        public int layers;
        public int heads;
        public int maxSeqLen;
        public int epochs;

        // Evolved Ollama parameters
        public float temperature;
        public float topP;
        public int topK;
        public float repeatPenalty;

        public LlmConfig(int vocabSize, int embeddingSize, int layers, int heads, int maxSeqLen, int epochs) {
            this.vocabSize = vocabSize;
            this.embeddingSize = embeddingSize;
            this.layers = layers;
            this.heads = heads;
            this.maxSeqLen = maxSeqLen;
            this.epochs = epochs;

            // Derive initial Ollama parameters based on core fields
            this.temperature = Math.max(0.1f, Math.min(1.5f, (float) vocabSize / 4000.0f));
            this.topP = Math.max(0.1f, Math.min(1.0f, (float) embeddingSize / 256.0f));
            this.topK = Math.max(10, heads * 10);
            this.repeatPenalty = Math.max(1.0f, Math.min(2.0f, 1.0f + (float) layers * 0.05f));
        }

        // Keep original constructor for compatibility
        public LlmConfig(int vocabSize, int embeddingSize, int layers, int heads) {
            this(vocabSize, embeddingSize, layers, heads, 128, 8);
        }

        public int getMaxSeqLen() {
            return maxSeqLen;
        }

        @Override
        public String toString() {
            return String.format("Vocab: %d, Embed: %d, Layers: %d, Heads: %d, SeqLen: %d, Epochs: %d | Temp: %.2f, TopP: %.2f, TopK: %d, RepPen: %.2f",
                vocabSize, embeddingSize, layers, heads, maxSeqLen, epochs, temperature, topP, topK, repeatPenalty);
        }
    }

    public static class CandidateResult {
        public String name;
        public LlmConfig config;
        public double loss;
        public long paramCount;
        public long durationMs;
        public double fitness;

        public CandidateResult(String name, LlmConfig config, double loss, long paramCount, long durationMs, double fitness) {
            this.name = name;
            this.config = config;
            this.loss = loss;
            this.paramCount = paramCount;
            this.durationMs = durationMs;
            this.fitness = fitness;
        }
    }

    public static class CandidateTrainingResult {
        public double loss;
        public long paramCount;
        public double fitness;

        public CandidateTrainingResult(double loss, long paramCount, double fitness) {
            this.loss = loss;
            this.paramCount = paramCount;
            this.fitness = fitness;
        }
    }

    public LLMDarwinEngine(TaskContext context, IterationMemoryService memoryService,
                             SystemStateSignalProvider stateProvider) {
        super(context, memoryService, stateProvider, PlatformType.FORGE);
    }

    protected String getTargetPath() {
        ChatSession chatSession = getChatSession();
        if (chatSession != null) {
            String path = chatSession.getTargetPath();
            if (path != null && !path.isEmpty()) {
                return path;
            }
        }
        return context.getProjectRoot().getAbsolutePath();
    }

    /**
     * Centralized null-safe preview helper.
     */
    private static String safePreview(String value, int maximumLength) {
        if (value == null || value.isEmpty() || maximumLength <= 0) {
            return "";
        }
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength);
    }

    /**
     * Validates and normalizes candidate configurations to prevent multi-head attention division-by-zero
     * or architectural invalidity. Clamps values to supported bounds.
     */
    private LlmConfig normalizeCandidateConfig(LlmConfig candidate) {
        int vocabSize = Math.max(4000, Math.min(32000, candidate.vocabSize));
        int embeddingSize = Math.max(128, Math.min(512, candidate.embeddingSize));
        int layers = Math.max(2, Math.min(12, candidate.layers));
        int heads = Math.max(1, Math.min(64, candidate.heads));
        int maxSeqLen = Math.max(64, Math.min(256, candidate.maxSeqLen));
        int epochs = Math.max(1, Math.min(20, candidate.epochs));

        // Ensure heads divides embedding size perfectly
        if (embeddingSize % heads != 0) {
            int nearestHead = heads;
            int minDiff = Integer.MAX_VALUE;
            for (int h = 1; h <= embeddingSize; h++) {
                if (embeddingSize % h == 0) {
                    if (h >= 2 && h <= 32) {
                        int diff = Math.abs(h - heads);
                        if (diff < minDiff) {
                            minDiff = diff;
                            nearestHead = h;
                        }
                    }
                }
            }
            if (embeddingSize % nearestHead != 0) {
                nearestHead = 2;
                if (embeddingSize % 2 != 0) {
                    embeddingSize = (embeddingSize / 2) * 2;
                    if (embeddingSize < 128) embeddingSize = 128;
                }
            }
            context.log(String.format("[FORGE] Normalizing heads/embedding: original heads=%d, adjusted to %d to divide embedding size %d.",
                heads, nearestHead, embeddingSize));
            heads = nearestHead;
        }

        if (embeddingSize / heads <= 0) {
            heads = 2;
            embeddingSize = 128;
        }

        LlmConfig normalized = new LlmConfig(vocabSize, embeddingSize, layers, heads, maxSeqLen, epochs);
        normalized.temperature = Math.max(0.1f, Math.min(1.5f, candidate.temperature));
        normalized.topP = Math.max(0.1f, Math.min(1.0f, candidate.topP));
        normalized.topK = Math.max(10, Math.min(100, candidate.topK));
        normalized.repeatPenalty = Math.max(1.0f, Math.min(2.0f, candidate.repeatPenalty));

        return normalized;
    }

    @Override
    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager)
            throws Exception {
        context.setStartTime(Instant.now());
        OrchestrationState state = context.getOrchestrationState();
        iterationManager.transition(SystemState.INIT, context);

        context.log("[FORGE] Darwin LLM Evolution Instance Started.");

        // Load target path files or fallback to docs/
        String targetPath = getTargetPath();
        context.log("[FORGE] Selected Training Target Folder: " + targetPath);

        // Resolve Ollama baseUrl and baseModel using the managed service
        String ollamaUrl = "http://localhost:11434";
        if (context.getOrchestrator().getOllama() != null &&
            context.getOrchestrator().getOllama().getUrl() != null &&
            !context.getOrchestrator().getOllama().getUrl().isEmpty()) {
            ollamaUrl = context.getOrchestrator().getOllama().getUrl();
        }

        String baseModel = "llama3.2:3b";
        try {
            eu.kalafatic.evolution.controller.manager.OllamaService service =
                eu.kalafatic.evolution.controller.manager.OllamaManager.getInstance().getService(ollamaUrl);
            List<eu.kalafatic.evolution.controller.manager.OllamaModel> available = service.loadModels();
            if (available != null && !available.isEmpty()) {
                boolean foundLlama = false;
                for (eu.kalafatic.evolution.controller.manager.OllamaModel m : available) {
                    if (m.getName().contains("llama3.2:3b")) {
                        baseModel = "llama3.2:3b";
                        foundLlama = true;
                        break;
                    }
                }
                if (!foundLlama) {
                    for (eu.kalafatic.evolution.controller.manager.OllamaModel m : available) {
                        String mName = m.getName();
                        if (mName != null && !mName.toLowerCase().contains("evo")) {
                            baseModel = mName;
                            foundLlama = true;
                            break;
                        }
                    }
                }
                if (!foundLlama) {
                    baseModel = available.get(0).getName();
                }
            }
        } catch (Exception e) {
            context.log("Failed to load available models from Ollama, defaulting base to llama3.2:3b: " + e.getMessage());
        }
        context.log("[FORGE] Using base model for evolutionary evaluation: " + baseModel);

        // Load dynamic configuration from ForgeSessionManager for progressive training sources and assistance settings
        JSONObject uiState = eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.getInstance().getUiState(context.getSessionId());
        boolean sourceMarkdown = uiState.optBoolean("source_markdown", true);
        boolean sourceJava = uiState.optBoolean("source_java", false);
        boolean sourceXml = uiState.optBoolean("source_xml", false);
        boolean sourceJson = uiState.optBoolean("source_json", false);
        boolean sourceConfiguration = uiState.optBoolean("source_configuration", false);
        boolean sourceExternal = uiState.optBoolean("source_external", false);

        boolean assistanceExtraction = uiState.optBoolean("assistance_extraction", true);
        boolean assistanceQa = uiState.optBoolean("assistance_qa", true);
        boolean assistanceQuality = uiState.optBoolean("assistance_quality", true);

        File targetFolder = new File(targetPath);
        List<Path> scannedPaths = new ArrayList<>();

        if (targetFolder.exists() && targetFolder.isDirectory()) {
            try (Stream<Path> walk = Files.walk(targetFolder.toPath())) {
                List<Path> files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains("/.git/") && !p.toString().contains("\\.git\\") &&
                                !p.toString().contains("/target/") && !p.toString().contains("\\target\\") &&
                                !p.toString().contains("/node_modules/") && !p.toString().contains("\\node_modules\\"))
                    .collect(Collectors.toList());
                int fileCount = 0;
                for (Path file : files) {
                    if (fileCount >= MAX_CORPUS_FILES) {
                        context.log("[FORGE] Corpus file limit reached. Skipping additional file scans.");
                        break;
                    }
                    String name = file.getFileName().toString().toLowerCase();
                    boolean accept = false;
                    if (name.endsWith(".md") && sourceMarkdown) accept = true;
                    else if (name.endsWith(".java") && sourceJava) accept = true;
                    else if (name.endsWith(".xml") && sourceXml) accept = true;
                    else if (name.endsWith(".json") && sourceJson) accept = true;
                    else if ((name.endsWith(".properties") || name.equals("pom.xml") || name.equals("manifest.mf")) && sourceConfiguration) accept = true;
                    else if (sourceExternal && (name.endsWith(".html") || name.endsWith(".htm"))) accept = true;

                    // Default to markdown if no training sources configured
                    if (!sourceMarkdown && !sourceJava && !sourceXml && !sourceJson && !sourceConfiguration && !sourceExternal) {
                        if (name.endsWith(".md")) accept = true;
                    }

                    if (accept) {
                        scannedPaths.add(file);
                        fileCount++;
                    }
                }
            }
        }

        // 1. SourceAnalysisAgent (Sub-agent)
        SourceAnalysisAgent sourceAnalysisAgent = new SourceAnalysisAgent();
        List<KnowledgeUnit> knowledgeUnits = sourceAnalysisAgent.analyze(scannedPaths, context.getProjectRoot().toPath());
        context.log("[FORGE] SourceAnalysisAgent completed. Analyzed knowledge units: " + knowledgeUnits.size());

        // 2. ConsistencyAgent (Sub-agent)
        ConsistencyAgent consistencyAgent = new ConsistencyAgent();
        List<ConsistencyAgent.ConsistencyViolation> consistencyViolations = consistencyAgent.checkConsistency(knowledgeUnits);
        context.log("[FORGE] ConsistencyAgent complete. Violations detected: " + consistencyViolations.size());
        for (ConsistencyAgent.ConsistencyViolation violation : consistencyViolations) {
            context.log("[FORGE] [CONSISTENCY DRIFT] " + violation.toString());
        }

        // 3. KnowledgeExtractionAgent (Sub-agent)
        LocalOllamaClient ollamaClient = new LocalOllamaClient(ollamaUrl, baseModel);
        KnowledgeExtractionAgent knowledgeExtractionAgent = new KnowledgeExtractionAgent(ollamaClient, assistanceExtraction);
        List<KnowledgeFact> extractedFacts = knowledgeExtractionAgent.extract(knowledgeUnits);
        context.log("[FORGE] KnowledgeExtractionAgent completed. Extracted facts: " + extractedFacts.size());

        // 4. TrainingDataAgent (Sub-agent)
        TrainingDataAgent trainingDataAgent = new TrainingDataAgent();
        List<TrainingRecord> generatedRecords = trainingDataAgent.generate(extractedFacts);
        context.log("[FORGE] TrainingDataAgent completed. Training records generated: " + generatedRecords.size());

        // 5. DatasetQualityAgent (Sub-agent)
        DatasetQualityAgent datasetQualityAgent = new DatasetQualityAgent();
        List<DatasetQualityAgent.QualityReport> qualityReports = datasetQualityAgent.evaluate(generatedRecords);
        List<TrainingRecord> acceptedRecords = new ArrayList<>();
        int rejectedCount = 0;
        for (DatasetQualityAgent.QualityReport report : qualityReports) {
            if (report.getRecommendation() == DatasetQualityAgent.Recommendation.ACCEPT || !assistanceQuality) {
                acceptedRecords.add(report.getRecord());
            } else {
                rejectedCount++;
            }
        }
        context.log("[FORGE] DatasetQualityAgent completed. Accepted records: " + acceptedRecords.size() + ", Rejected: " + rejectedCount);

        // Safe Fallback to raw Markdown if needed
        String corpus = "";
        if (acceptedRecords.isEmpty() || !assistanceQa) {
            context.log("[FORGE] No accepted QA training records or assistance disabled. Falling back to default raw Markdown scan...");
            StringBuilder corpusBuilder = new StringBuilder();
            int mdFilesFound = 0;
            for (KnowledgeUnit unit : knowledgeUnits) {
                if ("MARKDOWN".equals(unit.getFileType())) {
                    corpusBuilder.append(unit.getContent()).append("\n\n");
                    mdFilesFound++;
                }
            }
            corpus = corpusBuilder.toString();
            if (corpus.trim().isEmpty() || mdFilesFound == 0) {
                // Fallback to Repo Docs/
                File fallbackDocs = new File(context.getProjectRoot(), "docs");
                if (fallbackDocs.exists() && fallbackDocs.isDirectory()) {
                    try (Stream<Path> walk = Files.walk(fallbackDocs.toPath())) {
                        List<Path> files = walk
                            .filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".md"))
                            .collect(Collectors.toList());
                        for (Path f : files) {
                            corpusBuilder.append(Files.readString(f)).append("\n\n");
                            mdFilesFound++;
                        }
                    }
                }
            }
            corpus = corpusBuilder.toString();
            if (corpus.trim().isEmpty()) {
                corpus = "This is a simple EVO LLM training document.\nEvolution genome data management is personal, economical, and political.\n" +
                         "personal: the joy of frontier creation and personal relevance.\neconomical: building priceless user and developer know-how.\n" +
                         "political: independence and local control from centralized AI authorities.\n";
            }
        } else {
            StringBuilder corpusBuilder = new StringBuilder();
            for (TrainingRecord r : acceptedRecords) {
                corpusBuilder.append(r.getInstruction()).append("\n").append(r.getResponse()).append("\n\n");
            }
            corpus = corpusBuilder.toString();
        }

        MarkdownCleaner cleaner = new MarkdownCleaner();
        String cleanCorpus = cleaner.clean(corpus);
        if (cleanCorpus.length() > MAX_CORPUS_CHARS) {
            context.log("[FORGE] Corpus character limit exceeded. Truncating clean corpus to " + MAX_CORPUS_CHARS + " chars.");
            cleanCorpus = cleanCorpus.substring(0, MAX_CORPUS_CHARS);
        }
        context.log("[FORGE] Training Source Dataset built successfully. Clean corpus size: " + cleanCorpus.length() + " chars.");

        // Resolve generations count from prompt instructions (preferredMaxIterations) or default to 5
        int generations = 5;
        if (context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null) {
            generations = context.getOrchestrator().getAiChat().getPromptInstructions().getPreferredMaxIterations();
        }
        if (generations <= 1) {
            generations = 5; // default to 5 generations
        }

        context.log("[FORGE] Darwin LLM configured for " + generations + " evolution generations.");

        // Assess JVM Memory to select safe profiles and keep performance snappy
        long maxMemory = Runtime.getRuntime().maxMemory();
        context.log("[FORGE] JVM Max Memory available: " + (maxMemory / (1024 * 1024)) + " MB");
        Profile selectedProfile = Profile.DEFAULT;
        if (maxMemory < 512 * 1024 * 1024) {
            context.log("[FORGE] Tight memory environment. Downgrading candidates to SMALL profile.");
            selectedProfile = Profile.SMALL;
        }

        // Initial Candidates based on safe profiles
        List<LlmConfig> candidates = new ArrayList<>();
        candidates.add(new LlmConfig(selectedProfile.vocabSize, selectedProfile.embeddingSize, selectedProfile.layers, selectedProfile.heads, selectedProfile.maxSeqLen, selectedProfile.epochs));
        candidates.add(new LlmConfig(selectedProfile.vocabSize, selectedProfile.embeddingSize, selectedProfile.layers + 1, selectedProfile.heads, selectedProfile.maxSeqLen, selectedProfile.epochs));
        candidates.add(new LlmConfig(selectedProfile.vocabSize, selectedProfile.embeddingSize, selectedProfile.layers + 2, selectedProfile.heads, selectedProfile.maxSeqLen, selectedProfile.epochs));

        for (int i = 0; i < candidates.size(); i++) {
            candidates.set(i, normalizeCandidateConfig(candidates.get(i)));
        }

        CandidateResult overallWinner = null;
        List<String> logs = new ArrayList<>();
        List<JSONObject> genReports = new ArrayList<>();

        for (int gen = 1; gen <= generations; gen++) {
            context.log("[FORGE] --- GENERATION " + gen + " ---");
            logs.add("Generation " + gen + "\n");
            EvolutionProgressPublisher.startIteration(context, gen, gen, "forge-lineage", 1, generations, 1, 3);
            EvolutionProgressPublisher.updateStage(context, EvolutionStage.GENERATE_BRANCH);

            // Sync the initial active branch statuses
            List<EvolutionProgressEvent.BranchStatus> branchStatuses = new ArrayList<>();
            char bChar = 'A';
            for (LlmConfig config : candidates) {
                EvolutionProgressEvent.BranchStatus bs = new EvolutionProgressEvent.BranchStatus();
                bs.setId("gen_" + gen + "_candidate_" + bChar);
                bs.setStrategy("Candidate " + bChar + " (" + config + ")");
                bs.setStatus("active");
                bs.setScore(0.0);
                branchStatuses.add(bs);
                bChar++;
            }
            EvolutionProgressPublisher.syncBranches(context, branchStatuses);

            List<CandidateResult> results = new ArrayList<>();
            char candChar = 'A';

            for (LlmConfig config : candidates) {
                String candidateId = "gen_" + gen + "_candidate_" + candChar;
                String candidateName = "Candidate " + candChar;
                context.log("[FORGE] Evaluating " + candidateName + " (" + config + ")...");
                logs.add(candidateName + "\nEvaluating...\n");

                EvolutionProgressPublisher.updateActiveModel(context, "evo-candidate", "Evaluating " + candidateName);
                EvolutionProgressPublisher.updateBranchStatus(context, candidateId, candidateName + " (" + config + ")", "verifying", null);

                long startTime = System.currentTimeMillis();
                double loss = 0.0;
                long paramCount = 0;
                double fitness = 0.0;
                long durationMs = 0;

                // 1. Core Ollama Protocol evaluation using the real local Ollama model
                boolean ollamaSuccess = false;
                try {
                    String validationPrompt = "Based on this project documentation:\n\n" +
                        safePreview(cleanCorpus, 1000) +
                        "\n\nSummarize the core architecture in exactly two concise sentences.";

                    String responseText = queryOllama(ollamaUrl, baseModel, validationPrompt, config);
                    ollamaSuccess = true;

                    durationMs = System.currentTimeMillis() - startTime;
                    double keywordMatchCount = 0;
                    String[] keywords = {"evolution", "personal", "economical", "political", "orchestrator", "darwin", "genome", "model", "forge", "java", "task"};
                    String lowerResponse = responseText.toLowerCase();
                    for (String kw : keywords) {
                        if (lowerResponse.contains(kw)) {
                            keywordMatchCount += 1.0;
                        }
                    }
                    double contentLoss = 5.0 / (1.0 + keywordMatchCount);
                    if (responseText.trim().isEmpty()) {
                        contentLoss = 10.0;
                    }
                    double lengthPenalty = 0.0;
                    int len = responseText.length();
                    if (len < 50) {
                        lengthPenalty = 3.0;
                    } else if (len > 800) {
                        lengthPenalty = (len - 800) * 0.01;
                    }
                    double timePenalty = (durationMs / 1000.0) * 0.2;
                    fitness = contentLoss + lengthPenalty + timePenalty;
                    loss = contentLoss;
                    paramCount = config.vocabSize * config.embeddingSize + config.layers * (config.embeddingSize * config.embeddingSize * 4);

                    context.log(String.format("[FORGE] Ollama Protocol Response: %s", safePreview(responseText.trim().replace("\n", " "), 500)));
                } catch (InterruptedException ex) {
                    context.log("[FORGE] Ollama protocol query was interrupted. Restoring interrupt status.");
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    context.log("[FORGE] Ollama protocol query failed for " + candidateName + " (falling back to pure-Java offline training fallback): " + ex.getMessage());
                }

                // 2. Offline Fallback: Train custom tokenizer and model in Java
                if (!ollamaSuccess) {
                    try {
                        CandidateTrainingResult trainingResult = runOfflineTraining(cleanCorpus, config);
                        loss = trainingResult.loss;
                        paramCount = trainingResult.paramCount;
                        fitness = trainingResult.fitness;
                    } catch (Exception e) {
                        context.log("[FORGE] Pure-Java offline training failed for " + candidateName + " - assigning safe penalty score: " + e.getMessage());
                        loss = 10.0;
                        fitness = 25.0; // deterministically penalized
                        paramCount = 1_000_000;
                    }
                    durationMs = System.currentTimeMillis() - startTime;
                }

                if (Double.isNaN(loss) || Double.isInfinite(loss)) loss = 10.0;
                if (Double.isNaN(fitness) || Double.isInfinite(fitness)) fitness = 30.0;

                context.log(String.format("[FORGE] Completed %s. Loss: %.4f, Params: %d, Duration: %d ms, Fitness: %.4f",
                    candidateName, loss, paramCount, durationMs, fitness));
                logs.add(String.format("Loss %.4f\n-----------------\n", loss));

                double uiScore = Math.max(0.01, 1.0 / (1.0 + fitness));
                EvolutionProgressPublisher.updateBranchStatus(context, candidateId, candidateName + " (" + config + ")", "scoring", uiScore);

                results.add(new CandidateResult(candidateName, config, loss, paramCount, durationMs, fitness));
                candChar++;
            }

            // Stable deterministic sorting
            results.sort((c1, c2) -> {
                int cmp = Double.compare(c1.fitness, c2.fitness);
                if (cmp != 0) return cmp;
                int cmpLoss = Double.compare(c1.loss, c2.loss);
                if (cmpLoss != 0) return cmpLoss;
                return c1.name.compareTo(c2.name);
            });

            CandidateResult genWinner = results.get(0);
            context.log("[FORGE] Generation " + gen + " Winner: " + genWinner.name + " (" + genWinner.config + ")");
            logs.add("Winner:\n" + genWinner.name + "\n\n");

            // Save generation report
            JSONObject genReport = new JSONObject();
            genReport.put("generation", gen);
            genReport.put("winner", genWinner.name);
            genReport.put("winnerConfig", genWinner.config.toString());
            genReport.put("winnerFitness", genWinner.fitness);
            genReport.put("winnerLoss", genWinner.loss);
            genReports.add(genReport);

            overallWinner = genWinner;

            // Update the winner and rejected branch statuses for the UI
            String winnerBranchId = "gen_" + gen + "_candidate_" + genWinner.name.substring(genWinner.name.length() - 1);
            EvolutionProgressPublisher.setWinnerId(context, winnerBranchId);

            List<EvolutionProgressEvent.BranchStatus> updatedStatuses = new ArrayList<>();
            for (CandidateResult r : results) {
                char charSuffix = r.name.substring(r.name.length() - 1).charAt(0);
                String bId = "gen_" + gen + "_candidate_" + charSuffix;
                EvolutionProgressEvent.BranchStatus bs = new EvolutionProgressEvent.BranchStatus();
                bs.setId(bId);
                bs.setStrategy(r.name + " (" + r.config + ")");
                double rUiScore = Math.max(0.01, 1.0 / (1.0 + r.fitness));
                bs.setScore(rUiScore);
                if (bId.equals(winnerBranchId)) {
                    bs.setStatus("active");
                } else {
                    bs.setStatus("rejected");
                }
                updatedStatuses.add(bs);
            }
            EvolutionProgressPublisher.syncBranches(context, updatedStatuses);
            EvolutionProgressPublisher.completeIteration(context);

            // Generate candidates for the next generation via mutation
            if (gen < generations) {
                candidates = new ArrayList<>();
                candidates.add(genWinner.config); // Elite survival
                candidates.add(mutate(genWinner.config, 1)); // Mutation type 1
                candidates.add(mutate(genWinner.config, 2)); // Mutation type 2
            }
        }

        // --- WINNER EXPORT ---
        context.log("[FORGE] Evolution complete. Overall Winner: " + overallWinner.config);

        // Generate Dynamic Model Name (based on context target folder, winning config, and timestamp)
        String dynamicModelName = generateDynamicModelName(context, overallWinner.config, targetPath);
        context.log("[FORGE] Dynamically generated winning model ID: " + dynamicModelName);

        // Create new workspace output folder for the winning model
        File workspaceDir;
        String workspacePathStr = ProjectModelManager.getWorkspacePath();
        if (workspacePathStr != null && !workspacePathStr.isEmpty()) {
            workspaceDir = new File(workspacePathStr);
        } else {
            workspaceDir = context.getProjectRoot().getParentFile();
        }

        File forgeOutputDir = new File(workspaceDir, "forge-output/" + dynamicModelName);
        if (forgeOutputDir.exists()) {
            deleteDirectory(forgeOutputDir);
        }
        forgeOutputDir.mkdirs();

        File checkpointDir = new File(forgeOutputDir, "checkpoint");
        checkpointDir.mkdirs();

        // 1. Train final winner tokenizer and dataset
        SimpleBPETokenizer finalTokenizer = new SimpleBPETokenizer();
        finalTokenizer.train(cleanCorpus, overallWinner.config.vocabSize);
        List<Integer> allTokens = finalTokenizer.encode(cleanCorpus);
        if (allTokens.size() > MAX_TOKENS_LIMIT) {
            allTokens = allTokens.subList(0, MAX_TOKENS_LIMIT);
        }
        DatasetBuilder finalDatasetBuilder = new DatasetBuilder();
        int finalSeqLen = overallWinner.config.maxSeqLen;
        int finalStride = Math.max(1, finalSeqLen / 2);
        List<DatasetBuilder.Sample> samples = finalDatasetBuilder.buildSlidingWindow(allTokens, finalSeqLen, finalStride);
        if (samples.size() > MAX_TRAINING_SAMPLES) {
            samples = samples.subList(0, MAX_TRAINING_SAMPLES);
        }

        // 2. Build final model and save configuration & tokenizer
        int dff = overallWinner.config.embeddingSize * 4;
        EvoLlmModel winningModel = new EvoLlmModel(finalTokenizer.getVocabSize(), overallWinner.config.embeddingSize, overallWinner.config.heads, overallWinner.config.layers, dff, finalSeqLen);

        EvoLlmTrainer trainer = new EvoLlmTrainer(winningModel);
        trainer.train(samples, overallWinner.config.epochs);

        // Export via OllamaExporter
        OllamaExporter exporter = new OllamaExporter();
        exporter.export(dynamicModelName, forgeOutputDir.toPath(), winningModel);

        // Overwrite the Modelfile with the winning candidate's optimized evolutionary Ollama parameters
        try {
            StringBuilder modelfileBuilder = new StringBuilder();
            modelfileBuilder.append("FROM ").append(forgeOutputDir.getAbsolutePath().replace("\\", "/")).append("/exports/ollama/evo.gguf\n");
            modelfileBuilder.append(String.format(java.util.Locale.US, "PARAMETER temperature %.4f\n", overallWinner.config.temperature));
            modelfileBuilder.append(String.format(java.util.Locale.US, "PARAMETER top_p %.4f\n", overallWinner.config.topP));
            modelfileBuilder.append(String.format("PARAMETER top_k %d\n", overallWinner.config.topK));
            modelfileBuilder.append(String.format(java.util.Locale.US, "PARAMETER repeat_penalty %.4f\n", overallWinner.config.repeatPenalty));
            modelfileBuilder.append("PARAMETER stop \"<EOS>\"\n");
            modelfileBuilder.append("SYSTEM \"\"\"You are EVO, a specialized language model trained on Evolution project knowledge.\"\"\"");

            String finalModelfileContent = modelfileBuilder.toString();
            Files.writeString(forgeOutputDir.toPath().resolve("Modelfile"), finalModelfileContent);
            Files.writeString(forgeOutputDir.toPath().resolve("exports/ollama/Modelfile"), finalModelfileContent);

            context.log("[FORGE] Overwrote Modelfile with evolved parameters:\n" + finalModelfileContent);

            // Register evolved models in Ollama via Ollama Protocol
            eu.kalafatic.evolution.controller.manager.OllamaService managedService =
                eu.kalafatic.evolution.controller.manager.OllamaManager.getInstance().getService(ollamaUrl);

            context.log("[FORGE] Registering evolved model in Ollama via Ollama Protocol: " + dynamicModelName);
            managedService.createModel(dynamicModelName, finalModelfileContent);

            context.log("[FORGE] Registering evolved model 'evo' alias via Ollama Protocol");
            managedService.createModel("evo", finalModelfileContent);

            context.log("[FORGE] Evolved Ollama model registration complete!");
        } catch (Exception ex) {
            context.log("[FORGE] Warning: Evolved Ollama model registration failed: " + ex.getMessage());
        }

        // Save tokenizer.json
        JSONObject tokJson = new JSONObject();
        tokJson.put("type", "SimpleBPE");
        tokJson.put("vocabSize", finalTokenizer.getVocabSize());
        Files.writeString(forgeOutputDir.toPath().resolve("tokenizer.json"), tokJson.toString(4));

        // Save config.json
        JSONObject configJson = new JSONObject();
        configJson.put("vocabSize", overallWinner.config.vocabSize);
        configJson.put("embeddingSize", overallWinner.config.embeddingSize);
        configJson.put("layers", overallWinner.config.layers);
        configJson.put("heads", overallWinner.config.heads);
        configJson.put("maxSeqLen", overallWinner.config.maxSeqLen);
        Files.writeString(forgeOutputDir.toPath().resolve("config.json"), configJson.toString(4));

        // Save weights.bin atomically to avoid file corruption
        File weightsFile = new File(forgeOutputDir, "weights.bin");
        try {
            writeWeightsAtomically(weightsFile.toPath(), winningModel);
        } catch (Exception e) {
            context.log("[FORGE] Error writing weights atomically: " + e.getMessage());
            // Fallback to normal save if atomic writing failed
            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(weightsFile)))) {
                for (Tensor p : winningModel.parameters()) {
                    for (float val : p.getData()) {
                        dos.writeFloat(val);
                    }
                }
            }
        }

        // Save training-report.json
        JSONObject reportJson = new JSONObject();
        reportJson.put("modelName", dynamicModelName);
        reportJson.put("generationsTrained", generations);
        reportJson.put("cleanCorpusChars", cleanCorpus.length());
        reportJson.put("finalLoss", overallWinner.loss);
        reportJson.put("parameterCount", overallWinner.paramCount);
        reportJson.put("durationMs", overallWinner.durationMs);
        reportJson.put("fitnessScore", overallWinner.fitness);

        JSONArray historyArr = new JSONArray();
        for (JSONObject gr : genReports) {
            historyArr.put(gr);
        }
        reportJson.put("generationHistory", historyArr);
        Files.writeString(forgeOutputDir.toPath().resolve("training-report.json"), reportJson.toString(4));

        context.log("[FORGE] Saved winning model artifacts successfully to " + forgeOutputDir.getAbsolutePath());

        // Prepare Final Markdown Summary
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("# Generation completed successfully!\n\n");
        summaryBuilder.append(String.join("\n", logs)).append("\n");
        summaryBuilder.append("### Winning Model: **" + dynamicModelName + "**\n\n");
        summaryBuilder.append("**Configuration:**\n");
        summaryBuilder.append(String.format("- **Vocabulary Size:** %d\n", overallWinner.config.vocabSize));
        summaryBuilder.append(String.format("- **Embedding Size:** %d\n", overallWinner.config.embeddingSize));
        summaryBuilder.append(String.format("- **Transformer Blocks:** %d\n", overallWinner.config.layers));
        summaryBuilder.append(String.format("- **Attention Heads:** %d\n\n", overallWinner.config.heads));

        summaryBuilder.append("**Training Statistics:**\n");
        summaryBuilder.append(String.format("- **Final Loss:** %.4f\n", overallWinner.loss));
        summaryBuilder.append(String.format("- **Parameter Count:** %d\n", overallWinner.paramCount));
        summaryBuilder.append(String.format("- **Training Duration:** %d ms\n", overallWinner.durationMs));
        summaryBuilder.append(String.format("- **Fitness Score:** %.4f\n\n", overallWinner.fitness));

        summaryBuilder.append("### Generated Artifacts & Export Location:\n");
        String uriPrefix = "file:///" + forgeOutputDir.getAbsolutePath().replace("\\", "/");
        summaryBuilder.append(String.format("- **Output Folder:** [%s/](%s/)\n", "forge-output/" + dynamicModelName, uriPrefix));
        summaryBuilder.append(String.format("- **Model GGUF:** [evo.gguf](%s/evo.gguf)\n", uriPrefix));
        summaryBuilder.append(String.format("- **Ollama Modelfile:** [Modelfile](%s/Modelfile)\n", uriPrefix));
        summaryBuilder.append(String.format("- **Vocabulary / Tokenizer:** [tokenizer.json](%s/tokenizer.json)\n", uriPrefix));
        summaryBuilder.append(String.format("- **Model Configuration:** [config.json](%s/config.json)\n", uriPrefix));
        summaryBuilder.append(String.format("- **Model Weights:** [weights.bin](%s/weights.bin)\n", uriPrefix));
        summaryBuilder.append(String.format("- **Checkpoint Directory:** [checkpoint/](%s/checkpoint/)\n", uriPrefix));
        summaryBuilder.append(String.format("- **Training Report:** [training-report.json](%s/training-report.json)\n", uriPrefix));

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);

        FinalResponse finalResponse = new FinalResponse(
            summaryBuilder.toString(),
            new ArrayList<String>(),
            new ArrayList<FileReference>(),
            true,
            null,
            "Execution completed successfully.",
            new ExecutionMetrics(context.getStartTime(), Instant.now())
        );
        response.setFinalResponse(finalResponse);

        iterationManager.transition(SystemState.DONE, context);
        EvolutionProgressPublisher.completeIteration(context);

        return response;
    }

    /**
     * Helper method to train custom tokenizer and model in Java cleanly to prevent strong memory retention.
     */
    private CandidateTrainingResult runOfflineTraining(String cleanCorpus, LlmConfig config) {
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train(cleanCorpus, config.vocabSize);
        List<Integer> allTokens = tokenizer.encode(cleanCorpus);

        if (allTokens.size() > MAX_TOKENS_LIMIT) {
            allTokens = allTokens.subList(0, MAX_TOKENS_LIMIT);
        }

        DatasetBuilder datasetBuilder = new DatasetBuilder();
        int seqLen = config.maxSeqLen;
        int stride = Math.max(1, seqLen / 2);
        List<DatasetBuilder.Sample> samples = datasetBuilder.buildSlidingWindow(allTokens, seqLen, stride);

        if (samples.size() > MAX_TRAINING_SAMPLES) {
            samples = samples.subList(0, MAX_TRAINING_SAMPLES);
        }

        int dff = config.embeddingSize * 4;
        EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), config.embeddingSize, config.heads, config.layers, dff, seqLen);

        long paramCount = 0;
        for (Tensor p : model.parameters()) {
            paramCount += p.getData().length;
        }

        EvoLlmTrainer trainer = new EvoLlmTrainer(model);
        trainer.train(samples, config.epochs);

        double loss = trainer.getLossHistory().isEmpty() ? 2.5 : trainer.getLossHistory().get(trainer.getLossHistory().size() - 1);
        if (Double.isNaN(loss) || Double.isInfinite(loss)) {
            loss = 10.0;
        }

        double sizePenalty = paramCount * 0.000001;
        double fitness = loss + sizePenalty;

        return new CandidateTrainingResult(loss, paramCount, fitness);
    }

    /**
     * Atomically writes weight files by writing to a temporary file first, flushing/closing, and then moving.
     */
    private void writeWeightsAtomically(Path target, EvoLlmModel model) throws Exception {
        Path tempFile = target.getParent().resolve(target.getFileName().toString() + ".tmp");
        try {
            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile.toFile())))) {
                for (Tensor p : model.parameters()) {
                    for (float val : p.getData()) {
                        dos.writeFloat(val);
                    }
                }
                dos.flush();
            }
            try {
                Files.move(tempFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.io.IOException e) {
                // Fallback to non-atomic replace if system does not support atomic moves
                Files.move(tempFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception ex) {
                // ignore
            }
            throw e;
        }
    }

    private LlmConfig mutate(LlmConfig winner, int mutationIdx) {
        int vocabSize = winner.vocabSize;
        int embeddingSize = winner.embeddingSize;
        int layers = winner.layers;
        int heads = winner.heads;
        int maxSeqLen = winner.maxSeqLen;
        int epochs = winner.epochs;

        Random random = new Random();
        switch (mutationIdx) {
            case 1:
                embeddingSize = Math.max(128, embeddingSize + (random.nextBoolean() ? 64 : -64));
                vocabSize = Math.max(4000, vocabSize + (random.nextBoolean() ? 1000 : -1000));
                break;
            case 2:
                layers = Math.max(2, layers + (random.nextBoolean() ? 1 : -1));
                heads = Math.max(2, heads + (random.nextBoolean() ? 2 : -2));
                break;
            case 3:
                maxSeqLen = Math.max(64, maxSeqLen + (random.nextBoolean() ? 32 : -32));
                epochs = Math.max(1, epochs + (random.nextBoolean() ? 2 : -2));
                break;
        }

        LlmConfig mutated = new LlmConfig(vocabSize, embeddingSize, layers, heads, maxSeqLen, epochs);
        mutated = normalizeCandidateConfig(mutated);

        // Evolve derived Ollama parameters with deltas for active exploration
        mutated.temperature = Math.max(0.1f, Math.min(1.5f, winner.temperature + (random.nextFloat() * 0.2f - 0.1f)));
        mutated.topP = Math.max(0.1f, Math.min(1.0f, winner.topP + (random.nextFloat() * 0.1f - 0.05f)));
        mutated.topK = Math.max(10, Math.min(100, winner.topK + (random.nextBoolean() ? 5 : -5)));
        mutated.repeatPenalty = Math.max(1.0f, Math.min(2.0f, winner.repeatPenalty + (random.nextFloat() * 0.1f - 0.05f)));

        return mutated;
    }

    private String queryOllama(String baseUrl, String baseModel, String prompt, LlmConfig config) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", baseModel);
        jsonObject.put("prompt", prompt);
        jsonObject.put("stream", false);

        JSONObject options = new JSONObject();
        options.put("temperature", (double) config.temperature);
        options.put("top_p", (double) config.topP);
        options.put("top_k", config.topK);
        options.put("repeat_penalty", (double) config.repeatPenalty);
        jsonObject.put("options", options);

        String genUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/generate";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(genUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.optString("response", "");
    }

    public String generateDynamicModelName(TaskContext context, LlmConfig winner, String targetPath) {
        String folderName = "generic";
        if (targetPath != null && !targetPath.isEmpty()) {
            File folder = new File(targetPath);
            folderName = folder.getName().toLowerCase()
                .replaceAll("[^a-zA-Z0-9-]", "-")
                .replaceAll("-+", "-");
        }

        String archSignature = String.format("v%d-e%d-l%d-h%d",
            winner.vocabSize, winner.embeddingSize, winner.layers, winner.heads);

        String timestamp = java.time.format.DateTimeFormatter
            .ofPattern("ddMMyy_HHmmss")
            .format(java.time.LocalDateTime.now());

        return String.format("evo-%s-%s-%s", folderName, archSignature, timestamp);
    }

    @Override
    protected void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents)
                deleteDirectory(file);
        }
        directory.delete();
    }
}
