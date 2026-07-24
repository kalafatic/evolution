package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

public class DarwinLlmInstance extends ADarwinEngine {

    public static class LlmConfig {
        public int vocabSize;
        public int embeddingSize;
        public int layers;
        public int heads;

        public LlmConfig(int vocabSize, int embeddingSize, int layers, int heads) {
            this.vocabSize = vocabSize;
            this.embeddingSize = embeddingSize;
            this.layers = layers;
            this.heads = heads;
        }

        @Override
        public String toString() {
            return String.format("Vocab: %d, Embed: %d, Layers: %d, Heads: %d", vocabSize, embeddingSize, layers, heads);
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

    public DarwinLlmInstance(TaskContext context, IterationMemoryService memoryService,
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

        File targetFolder = new File(targetPath);
        StringBuilder corpusBuilder = new StringBuilder();
        int mdFilesFound = 0;

        if (targetFolder.exists() && targetFolder.isDirectory()) {
            try (Stream<Path> walk = Files.walk(targetFolder.toPath())) {
                List<Path> files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !p.toString().contains("/.git/") && !p.toString().contains("\\.git\\"))
                    .collect(Collectors.toList());
                for (Path f : files) {
                    corpusBuilder.append(Files.readString(f)).append("\n\n");
                    mdFilesFound++;
                }
            }
        }

        String rawCorpus = corpusBuilder.toString();
        if (rawCorpus.trim().isEmpty() || mdFilesFound == 0) {
            context.log("[FORGE] No Markdown documentation files found in target folder. Falling back to repo docs/ directory.");
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

        String corpus = corpusBuilder.toString();
        if (corpus.trim().isEmpty()) {
            context.log("[FORGE] WARNING: No training text could be loaded. Using default simple documentation seed corpus.");
            corpus = "This is a simple EVO LLM training document.\nEvolution genome data management is personal, economical, and political.\n" +
                     "personal: the joy of frontier creation and personal relevance.\neconomical: building priceless user and developer know-how.\n" +
                     "political: independence and local control from centralized AI authorities.\n";
        }

        MarkdownCleaner cleaner = new MarkdownCleaner();
        String cleanCorpus = cleaner.clean(corpus);
        context.log("[FORGE] Training Source Dataset built successfully. Found " + mdFilesFound + " markdown files. Clean corpus size: " + cleanCorpus.length() + " chars.");

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

        // Initial Candidates
        List<LlmConfig> candidates = new ArrayList<>();
        candidates.add(new LlmConfig(2000, 64, 2, 2));   // Candidate A
        candidates.add(new LlmConfig(4000, 128, 2, 4));  // Candidate B
        candidates.add(new LlmConfig(4000, 128, 4, 4));  // Candidate C

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
                context.log("[FORGE] Training " + candidateName + " (" + config + ")...");
                logs.add(candidateName + "\nTraining...\n");

                EvolutionProgressPublisher.updateActiveModel(context, "evo-candidate", "Training " + candidateName);
                EvolutionProgressPublisher.updateBranchStatus(context, candidateId, candidateName + " (" + config + ")", "verifying", null);

                long startTime = System.currentTimeMillis();

                // 1. Train custom tokenizer with candidate's vocabulary size
                SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
                tokenizer.train(cleanCorpus, config.vocabSize);
                List<Integer> allTokens = tokenizer.encode(cleanCorpus);

                // 2. Build datasets sliding window
                DatasetBuilder datasetBuilder = new DatasetBuilder();
                List<DatasetBuilder.Sample> samples = datasetBuilder.buildSlidingWindow(allTokens, 16, 8);

                // 3. Create Model
                int dff = config.embeddingSize * 4;
                EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), config.embeddingSize, config.heads, config.layers, dff, 16);

                // Count parameters
                long paramCount = 0;
                for (Tensor p : model.parameters()) {
                    paramCount += p.getData().length;
                }

                // 4. Train Model for 1 epoch to keep it extremely fast
                EvoLlmTrainer trainer = new EvoLlmTrainer(model);
                trainer.train(samples, 1);

                long durationMs = System.currentTimeMillis() - startTime;

                // Extract training loss
                double loss = trainer.getLossHistory().isEmpty() ? 2.5 : trainer.getLossHistory().get(trainer.getLossHistory().size() - 1);

                // 5. Evaluate Candidate (Lower score is better)
                // fitness = loss + sizePenalty + timePenalty
                double sizePenalty = paramCount * 0.000001;
                double timePenalty = durationMs * 0.000001;
                double fitness = loss + sizePenalty + timePenalty;

                context.log(String.format("[FORGE] Completed %s. Loss: %.4f, Params: %d, Duration: %d ms, Fitness: %.4f",
                    candidateName, loss, paramCount, durationMs, fitness));
                logs.add(String.format("Loss %.4f\n-----------------\n", loss));

                double uiScore = Math.max(0.01, 1.0 / (1.0 + fitness));
                EvolutionProgressPublisher.updateBranchStatus(context, candidateId, candidateName + " (" + config + ")", "scoring", uiScore);

                results.add(new CandidateResult(candidateName, config, loss, paramCount, durationMs, fitness));
                candChar++;
            }

            // Sort by fitness (lowest score is best)
            results.sort((c1, c2) -> Double.compare(c1.fitness, c2.fitness));
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
        SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
        tokenizer.train(cleanCorpus, overallWinner.config.vocabSize);
        List<Integer> allTokens = tokenizer.encode(cleanCorpus);
        DatasetBuilder datasetBuilder = new DatasetBuilder();
        List<DatasetBuilder.Sample> samples = datasetBuilder.buildSlidingWindow(allTokens, 16, 8);

        // 2. Build final model and save configuration & tokenizer
        int dff = overallWinner.config.embeddingSize * 4;
        EvoLlmModel winningModel = new EvoLlmModel(tokenizer.getVocabSize(), overallWinner.config.embeddingSize, overallWinner.config.heads, overallWinner.config.layers, dff, 16);

        EvoLlmTrainer trainer = new EvoLlmTrainer(winningModel);
        trainer.train(samples, 1);

        // Export via OllamaExporter
        OllamaExporter exporter = new OllamaExporter();
        exporter.export(dynamicModelName, forgeOutputDir.toPath(), winningModel);

        // Save tokenizer.json
        JSONObject tokJson = new JSONObject();
        tokJson.put("type", "SimpleBPE");
        tokJson.put("vocabSize", tokenizer.getVocabSize());
        Files.writeString(forgeOutputDir.toPath().resolve("tokenizer.json"), tokJson.toString(4));

        // Save config.json
        JSONObject configJson = new JSONObject();
        configJson.put("vocabSize", overallWinner.config.vocabSize);
        configJson.put("embeddingSize", overallWinner.config.embeddingSize);
        configJson.put("layers", overallWinner.config.layers);
        configJson.put("heads", overallWinner.config.heads);
        configJson.put("maxSeqLen", 16);
        Files.writeString(forgeOutputDir.toPath().resolve("config.json"), configJson.toString(4));

        // Save weights.bin (actual learned float weights)
        File weightsFile = new File(forgeOutputDir, "weights.bin");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(weightsFile)))) {
            for (Tensor p : winningModel.parameters()) {
                for (float val : p.getData()) {
                    dos.writeFloat(val);
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

    private LlmConfig mutate(LlmConfig winner, int mutationIdx) {
        int vocabSize = winner.vocabSize;
        int embeddingSize = winner.embeddingSize;
        int layers = winner.layers;
        int heads = winner.heads;

        Random random = new Random();
        switch (mutationIdx) {
            case 1:
                // Mutate embedding and vocabulary size
                embeddingSize = Math.max(32, embeddingSize + (random.nextBoolean() ? 32 : -32));
                vocabSize = Math.max(500, vocabSize + (random.nextBoolean() ? 500 : -500));
                break;
            case 2:
                // Mutate layers and attention heads
                layers = Math.max(1, layers + (random.nextBoolean() ? 1 : -1));
                heads = Math.max(1, heads + (random.nextBoolean() ? 2 : -2));
                break;
        }

        // Ensure embeddingSize is divisible by heads for multi-head attention
        if (embeddingSize % heads != 0) {
            heads = 2; // fallback to 2 heads
            if (embeddingSize % 2 != 0) {
                embeddingSize = 64; // align embedding size to 64 if needed
            }
        }

        return new LlmConfig(vocabSize, embeddingSize, layers, heads);
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
