package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
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
import eu.kalafatic.evolution.controller.orchestration.FinalResponseAssembler;
import eu.kalafatic.evolution.controller.orchestration.ExecutionMetrics;
import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
import eu.kalafatic.evolution.controller.orchestration.FileReference;
import eu.kalafatic.evolution.controller.orchestration.util.FileFilterUtil;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.model.orchestration.ChatSession;

import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.data.impl.MarkdownCleaner;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;

// Sub-agents imports
import eu.kalafatic.evolution.forge.controller.service.impl.agents.*;

public class LLMDarwinEngine extends ADarwinEngine {

	// Centralized model capacity profiles
	public enum Profile {
		SMALL(128, 4, 4, 64, 4, 4000), DEFAULT(256, 6, 8, 128, 8, 8000), LARGE(384, 8, 8, 256, 10, 16000);

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

	// Supported head counts divisor checking
	private static final int[] SUPPORTED_HEAD_COUNTS = { 2, 4, 8, 12, 16, 32 };

	// Shared HttpClient with 10s connect timeout
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
			.build();

	public static class LlmConfig {
		public int vocabSize;
		public int embeddingSize;
		public int layers;
		public int heads;
		public int dff;
		public int maxSeqLen;
		public int epochs;

		// Evolved Ollama parameters
		public float temperature;
		public float topP;
		public int topK;
		public float repeatPenalty;

		public LlmConfig(int vocabSize, int embeddingSize, int layers, int heads, int dff, int maxSeqLen, int epochs) {
			this.vocabSize = vocabSize;
			this.embeddingSize = embeddingSize;
			this.layers = layers;
			this.heads = heads;
			this.dff = dff > embeddingSize ? dff : embeddingSize * 4;
			this.maxSeqLen = maxSeqLen;
			this.epochs = epochs;

			// Derive initial Ollama parameters based on core fields
			this.temperature = Math.max(0.1f, Math.min(1.5f, (float) vocabSize / 4000.0f));
			this.topP = Math.max(0.1f, Math.min(1.0f, (float) embeddingSize / 256.0f));
			this.topK = Math.max(10, heads * 10);
			this.repeatPenalty = Math.max(1.0f, Math.min(2.0f, 1.0f + (float) layers * 0.05f));
		}

		public LlmConfig(int vocabSize, int embeddingSize, int layers, int heads, int maxSeqLen, int epochs) {
			this(vocabSize, embeddingSize, layers, heads, embeddingSize * 4, maxSeqLen, epochs);
		}

		// Keep original constructor for compatibility
		public LlmConfig(int vocabSize, int embeddingSize, int layers, int heads) {
			this(vocabSize, embeddingSize, layers, heads, embeddingSize * 4, 128, 8);
		}

		public int getMaxSeqLen() {
			return maxSeqLen;
		}

		@Override
		public String toString() {
			return String.format(
					"Vocab: %d, Embed: %d, Layers: %d, Heads: %d, DFF: %d, SeqLen: %d, Epochs: %d | Temp: %.2f, TopP: %.2f, TopK: %d, RepPen: %.2f",
					vocabSize, embeddingSize, layers, heads, dff, maxSeqLen, epochs, temperature, topP, topK,
					repeatPenalty);
		}
	}

	/**
	 * Defensive configuration copier to prevent Elite Configuration Aliasing.
	 */
	public static LlmConfig copyConfig(LlmConfig source) {
		if (source == null)
			return null;
		LlmConfig copy = new LlmConfig(source.vocabSize, source.embeddingSize, source.layers, source.heads, source.dff,
				source.maxSeqLen, source.epochs);
		copy.temperature = source.temperature;
		copy.topP = source.topP;
		copy.topK = source.topK;
		copy.repeatPenalty = source.repeatPenalty;
		return copy;
	}

	public static class CandidateResult {
		public String name;
		public LlmConfig config;
		public double loss;
		public long paramCount;
		public long durationMs;
		public double fitness;
		public boolean failed;
		public String failureReason;

		public CandidateResult(String name, LlmConfig config, double loss, long paramCount, long durationMs,
				double fitness, boolean failed, String failureReason) {
			this.name = name;
			this.config = copyConfig(config);
			this.loss = loss;
			this.paramCount = paramCount;
			this.durationMs = durationMs;
			this.fitness = fitness;
			this.failed = failed;
			this.failureReason = failureReason;
		}

		// Keep old constructor for compatibility
		public CandidateResult(String name, LlmConfig config, double loss, long paramCount, long durationMs,
				double fitness) {
			this(name, config, loss, paramCount, durationMs, fitness, false, null);
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
	 * Centralized candidate comparison method. Lower fitness is better. Successful
	 * candidates always rank above failed ones.
	 */
	private static int compareCandidates(CandidateResult c1, CandidateResult c2) {
		if (c1 == null && c2 == null)
			return 0;
		if (c1 == null)
			return 1;
		if (c2 == null)
			return -1;

		if (c1.failed && !c2.failed)
			return 1;
		if (!c1.failed && c2.failed)
			return -1;

		int cmp = Double.compare(c1.fitness, c2.fitness);
		if (cmp != 0)
			return cmp;

		int cmpLoss = Double.compare(c1.loss, c2.loss);
		if (cmpLoss != 0)
			return cmpLoss;

		return c1.name.compareTo(c2.name);
	}

	/**
	 * Validates and normalizes candidate configurations.
	 */
	private LlmConfig normalizeCandidateConfig(LlmConfig candidate) {
		int vocabSize = Math.max(500, Math.min(100000, candidate.vocabSize));
		int embeddingSize = Math.max(64, Math.min(16384, candidate.embeddingSize));
		int layers = Math.max(1, Math.min(128, candidate.layers));
		int maxSeqLen = Math.max(64, Math.min(65536, candidate.maxSeqLen));
		int epochs = Math.max(1, Math.min(100, candidate.epochs));
		int dff = candidate.dff > embeddingSize ? candidate.dff : embeddingSize * 4;

		// Find nearest supported head count that perfectly divides embeddingSize
		int bestHead = 8;
		double minDiff = Double.MAX_VALUE;
		for (int h : SUPPORTED_HEAD_COUNTS) {
			if (embeddingSize % h == 0) {
				int diff = Math.abs(h - candidate.heads);
				if (diff < minDiff) {
					minDiff = diff;
					bestHead = h;
				}
			}
		}

		// Fallback if none of the SUPPORTED_HEAD_COUNTS divide embeddingSize perfectly
		if (embeddingSize % bestHead != 0) {
			bestHead = 2;
			int minD = Integer.MAX_VALUE;
			for (int h = 2; h <= embeddingSize; h++) {
				if (embeddingSize % h == 0) {
					int d = Math.abs(h - candidate.heads);
					if (d < minD) {
						minD = d;
						bestHead = h;
					}
				}
			}
		}

		LlmConfig normalized = new LlmConfig(vocabSize, embeddingSize, layers, bestHead, dff, maxSeqLen, epochs);
		normalized.temperature = Math.max(0.1f, Math.min(1.5f, candidate.temperature));
		normalized.topP = Math.max(0.1f, Math.min(1.0f, candidate.topP));
		normalized.topK = Math.max(10, Math.min(100, candidate.topK));
		normalized.repeatPenalty = Math.max(1.0f, Math.min(2.0f, candidate.repeatPenalty));

		if (normalized.vocabSize != candidate.vocabSize || normalized.embeddingSize != candidate.embeddingSize
				|| normalized.layers != candidate.layers || normalized.heads != candidate.heads
				|| normalized.dff != candidate.dff || normalized.maxSeqLen != candidate.maxSeqLen
				|| normalized.epochs != candidate.epochs) {
			context.log(
					String.format("[FORGE] Normalized config: %s -> %s", candidate.toString(), normalized.toString()));
		}

		return normalized;
	}

	/**
	 * Helper to safely append bounded corpus information.
	 */
	private static void appendBounded(StringBuilder builder, String content, int limit) {
		if (content == null || content.isEmpty())
			return;
		if (builder.length() >= limit)
			return;
		int remaining = limit - builder.length();
		if (content.length() <= remaining) {
			builder.append(content).append("\n\n");
		} else {
			builder.append(content.substring(0, remaining));
		}
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
		if (context.getOrchestrator().getOllama() != null && context.getOrchestrator().getOllama().getUrl() != null
				&& !context.getOrchestrator().getOllama().getUrl().isEmpty()) {
			ollamaUrl = context.getOrchestrator().getOllama().getUrl();
		}

		String baseModel = "llama3.2:3b";
		try {
			eu.kalafatic.evolution.controller.manager.OllamaService service = eu.kalafatic.evolution.controller.manager.OllamaManager
					.getInstance().getService(ollamaUrl);
			List<eu.kalafatic.evolution.controller.manager.OllamaModel> available = service.loadModels();
			if (available != null && !available.isEmpty()) {
				boolean foundLlama = false;
				for (eu.kalafatic.evolution.controller.manager.OllamaModel m : available) {
					if (m.getName().contains("llama3.2:3b")) {
						baseModel = m.getName();
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
			context.log(
					"Failed to load available models from Ollama, defaulting base to llama3.2:3b: " + e.getMessage());
		}
		context.log("[FORGE] Using base model for reference evaluation assistance: " + baseModel);

		// Load dynamic configuration from ForgeSessionManager for progressive training
		// sources and assistance settings
		JSONObject uiState = eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.getInstance()
				.getUiState(context.getSessionId());
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
				List<Path> files = walk.filter(Files::isRegularFile)
						.filter(p -> !p.toString().contains("/.git/") && !p.toString().contains("\\.git\\")
								&& !p.toString().contains("/target/") && !p.toString().contains("\\target\\")
								&& !p.toString().contains("/node_modules/")
								&& !p.toString().contains("\\node_modules\\"))
						.sorted() // Deterministic file ordering
						.limit(MAX_CORPUS_FILES).collect(Collectors.toList());
				for (Path file : files) {
					String name = file.getFileName().toString().toLowerCase();
					boolean accept = false;
					if (name.endsWith(".md") && sourceMarkdown)
						accept = true;
					else if (name.endsWith(".java") && sourceJava)
						accept = true;
					else if (name.endsWith(".xml") && sourceXml)
						accept = true;
					else if (name.endsWith(".json") && sourceJson)
						accept = true;
					else if ((name.endsWith(".properties") || name.equals("pom.xml") || name.equals("manifest.mf"))
							&& sourceConfiguration)
						accept = true;
					else if (sourceExternal && (name.endsWith(".html") || name.endsWith(".htm")))
						accept = true;

					// Default to markdown if no training sources configured
					if (!sourceMarkdown && !sourceJava && !sourceXml && !sourceJson && !sourceConfiguration
							&& !sourceExternal) {
						if (name.endsWith(".md"))
							accept = true;
					}

					if (accept) {
						scannedPaths.add(file);
					}
				}
			}
		}

		// 1. SourceAnalysisAgent (Sub-agent)
		SourceAnalysisAgent sourceAnalysisAgent = new SourceAnalysisAgent();
		List<KnowledgeUnit> knowledgeUnits = sourceAnalysisAgent.analyze(scannedPaths,
				context.getProjectRoot().toPath());
		context.log("[FORGE] SourceAnalysisAgent completed. Analyzed knowledge units: " + knowledgeUnits.size());

		// 2. ConsistencyAgent (Sub-agent)
		ConsistencyAgent consistencyAgent = new ConsistencyAgent();
		List<ConsistencyAgent.ConsistencyViolation> consistencyViolations = consistencyAgent
				.checkConsistency(knowledgeUnits);
		context.log("[FORGE] ConsistencyAgent complete. Violations detected: " + consistencyViolations.size());
		for (ConsistencyAgent.ConsistencyViolation violation : consistencyViolations) {
			context.log("[FORGE] [CONSISTENCY DRIFT] " + violation.toString());
		}

		// 3. KnowledgeExtractionAgent (Sub-agent)
		LocalOllamaClient ollamaClient = new LocalOllamaClient(ollamaUrl, baseModel);
		KnowledgeExtractionAgent knowledgeExtractionAgent = new KnowledgeExtractionAgent(ollamaClient,
				assistanceExtraction);
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
		context.log("[FORGE] DatasetQualityAgent completed. Accepted records: " + acceptedRecords.size()
				+ ", Rejected: " + rejectedCount);

		// Safe Fallback to raw Markdown if needed
		StringBuilder corpusBuilder = new StringBuilder();
		if (acceptedRecords.isEmpty() || !assistanceQa) {
			context.log(
					"[FORGE] No accepted QA training records or assistance disabled. Falling back to default raw Markdown scan...");
			int mdFilesFound = 0;
			for (KnowledgeUnit unit : knowledgeUnits) {
				if ("MARKDOWN".equals(unit.getFileType())) {
					appendBounded(corpusBuilder, unit.getContent(), MAX_CORPUS_CHARS);
					mdFilesFound++;
				}
			}
			if (corpusBuilder.length() == 0 || mdFilesFound == 0) {
				// Fallback to Repo Docs/
				File fallbackDocs = new File(context.getProjectRoot(), "docs");
				if (fallbackDocs.exists() && fallbackDocs.isDirectory()) {
					try (Stream<Path> walk = Files.walk(fallbackDocs.toPath())) {
						List<Path> files = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".md"))
								.sorted().limit(MAX_CORPUS_FILES).collect(Collectors.toList());
						for (Path f : files) {
							appendBounded(corpusBuilder, Files.readString(f), MAX_CORPUS_CHARS);
							mdFilesFound++;
						}
					}
				}
			}
			if (corpusBuilder.length() == 0) {
				appendBounded(corpusBuilder,
						"This is a simple EVO LLM training document.\nEvolution genome data management is personal, economical, and political.\n"
								+ "personal: the joy of frontier creation and personal relevance.\neconomical: building priceless user and developer know-how.\n"
								+ "political: independence and local control from centralized AI authorities.\n",
						MAX_CORPUS_CHARS);
			}
		} else {
			for (TrainingRecord r : acceptedRecords) {
				appendBounded(corpusBuilder, r.getInstruction() + "\n" + r.getResponse(), MAX_CORPUS_CHARS);
			}
		}

		String corpus = corpusBuilder.toString();
		// Clear collections immediately to reclaim heap space
		scannedPaths.clear();
		knowledgeUnits.clear();
		consistencyViolations.clear();
		extractedFacts.clear();
		generatedRecords.clear();
		qualityReports.clear();
		acceptedRecords.clear();

		MarkdownCleaner cleaner = new MarkdownCleaner();
		String cleanCorpus = cleaner.clean(corpus);
		corpus = null; // release immediately

		if (cleanCorpus.length() > MAX_CORPUS_CHARS) {
			cleanCorpus = cleanCorpus.substring(0, MAX_CORPUS_CHARS);
		}
		context.log("[FORGE] Training Source Dataset built successfully. Clean corpus size: " + cleanCorpus.length()
				+ " chars.");

		// Resolve generations count from prompt instructions (preferredMaxIterations)
		// or default to 5
		int generations = 5;
		if (context.getOrchestrator().getAiChat() != null
				&& context.getOrchestrator().getAiChat().getPromptInstructions() != null) {
			generations = context.getOrchestrator().getAiChat().getPromptInstructions().getPreferredMaxIterations();
		}
		if (generations <= 1) {
			generations = 5; // default to 5 generations
		}

		boolean forceSolution = false;
		if (context.getOrchestrationState().getMetadata().containsKey("forceSolution")) {
			forceSolution = true;
		}
		if (taskRequest != null && taskRequest.getPrompt() != null) {
			String promptLower = taskRequest.getPrompt().toLowerCase().trim();
			if (promptLower.contains("force solution")) {
				forceSolution = true;
				context.getOrchestrationState().getMetadata().put("forceSolution", true);
			}
		}

		if (forceSolution) {
			generations = 1;
			context.log(
					"[FORGE] [STABILITY] Force Solution detected! Accelerating evolutionary search: generations set to 1, pruning candidate count, and reducing epochs to 1.");
		}

		context.log("[FORGE] Darwin LLM configured for " + generations + " evolution generations.");

		// Resolve model size preset from prompt or ForgeSessionManager uiState
		String modelSizeName = uiState.optString("modelSize", "").toUpperCase();

		if (taskRequest != null && taskRequest.getPrompt() != null) {
			String promptUpper = taskRequest.getPrompt().toUpperCase();
			for (ModelSizePreset.Size s : ModelSizePreset.Size.values()) {
				if (promptUpper.contains("(" + s.name() + ")")
						|| promptUpper.contains("(" + s.getDisplayName().toUpperCase() + ")")
						|| promptUpper.contains("MODEL (" + s.name() + ")")) {
					modelSizeName = s.name();
					break;
				}
			}
		}

		if (modelSizeName.isEmpty()) {
			modelSizeName = "SMALL";
		}

		ModelSizePreset.Size selectedPreset = ModelSizePreset.Size.SMALL;
		for (ModelSizePreset.Size s : ModelSizePreset.Size.values()) {
			if (s.name().equalsIgnoreCase(modelSizeName) || s.getDisplayName().toUpperCase().contains(modelSizeName)
					|| modelSizeName.contains(s.name())) {
				selectedPreset = s;
				break;
			}
		}

		int pVocabSize = selectedPreset.getVocabSize() > 0 ? selectedPreset.getVocabSize() : 8000;
		int pEmbedSize = selectedPreset.getDModel() > 0 ? selectedPreset.getDModel() : 384;
		int pLayers = selectedPreset.getNumBlocks() > 0 ? selectedPreset.getNumBlocks() : 6;
		int pHeads = selectedPreset.getNumHeads() > 0 ? selectedPreset.getNumHeads() : 8;
		int pDff = selectedPreset.getDff() > 0 ? selectedPreset.getDff() : 1024;
		int pMaxSeqLen = selectedPreset.getMaxSeqLen() > 0 ? selectedPreset.getMaxSeqLen() : 128;
		int pEpochs = 64; // For 1MB corpus with 16M model parameters, 64 epochs is a reasonable starting point for convergence.

		context.log(String.format(
				"[FORGE] Model size preset selected: %s (%s) -> Vocab: %d, Embed: %d, Layers: %d, Heads: %d, DFF: %d, MaxSeqLen: %d",
				selectedPreset.name(), selectedPreset.getDisplayName(), pVocabSize, pEmbedSize, pLayers, pHeads, pDff,
				pMaxSeqLen));

		// Initial Candidates based on selected preset
		List<LlmConfig> candidates = new ArrayList<>();
		if (forceSolution) {
			LlmConfig fastConfig = new LlmConfig(pVocabSize, pEmbedSize, pLayers, pHeads, pDff, pMaxSeqLen, 1);
			candidates.add(fastConfig);
		} else {
			int expansionValue = getExpansionValue();
			int branchingLimit = (expansionValue > 5) ? 2 : 1;
			candidates.add(new LlmConfig(pVocabSize, pEmbedSize, pLayers, pHeads, pDff, pMaxSeqLen, pEpochs));
			if (branchingLimit >= 2) {
				candidates.add(new LlmConfig(pVocabSize, pEmbedSize, pLayers + 1, pHeads, pDff, pMaxSeqLen, pEpochs));
			}
		}

		for (int i = 0; i < candidates.size(); i++) {
			candidates.set(i, normalizeCandidateConfig(candidates.get(i)));
		}

		CandidateResult overallWinner = null;
		List<String> logs = new ArrayList<>();
		List<JSONObject> genReports = new ArrayList<>();
		int startGen = 1;

		if (context.getMetadata().containsKey("forge_resume_gen")) {
			startGen = (Integer) context.getMetadata().get("forge_resume_gen");
			overallWinner = (CandidateResult) context.getMetadata().get("forge_overall_winner");
			logs = (List<String>) context.getMetadata().get("forge_logs");
			genReports = (List<JSONObject>) context.getMetadata().get("forge_gen_reports");
			candidates = (List<LlmConfig>) context.getMetadata().get("forge_candidates");
		}

		for (int gen = startGen; gen <= generations; gen++) {
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

			// Publish initial [DARWIN_BRANCHES] JSON payload to UI for real-time
			// visualization
			JSONObject branchesJson = new JSONObject();
			branchesJson.put("iteration", gen);
			branchesJson.put("minIterations", 1);
			branchesJson.put("maxIterations", generations);
			branchesJson.put("branchingLimit", candidates.size());
			branchesJson.put("minBranchingLimit", 1);
			JSONArray branchesArr = new JSONArray();
			bChar = 'A';
			for (LlmConfig config : candidates) {
				JSONObject bObj = new JSONObject();
				bObj.put("id", "gen_" + gen + "_candidate_" + bChar);
				bObj.put("strategy", "Candidate " + bChar + " (" + config + ")");
				bObj.put("status", "active");
				bObj.put("score", 0.0);
				branchesArr.put(bObj);
				bChar++;
			}
			branchesJson.put("branches", branchesArr);
			context.log("[DARWIN_BRANCHES] " + branchesJson.toString());

			List<CandidateResult> results = new ArrayList<>();
			boolean resumedResults = false;
			if (context.getMetadata().containsKey("forge_current_results")) {
				results = (List<CandidateResult>) context.getMetadata().remove("forge_current_results");
				resumedResults = true;
			}

			int candIndex = 0;

			if (!resumedResults) {
				for (LlmConfig config : candidates) {
					char candChar = (char) ('A' + candIndex);
					String candidateId = "gen_" + gen + "_candidate_" + candChar;
					String candidateName = "Candidate " + candChar;
					context.log("[FORGE] Evaluating " + candidateName + " (" + config + ")...");
					logs.add(candidateName + "\nEvaluating...\n");

					EvolutionProgressPublisher.updateActiveModel(context, "evo-candidate",
							"Evaluating " + candidateName);
					EvolutionProgressPublisher.updateBranchStatus(context, candidateId,
							candidateName + " (" + config + ")", "verifying", null);

					// Publish verifying status update to [DARWIN_BRANCHES] D3.js visualization
					// graph
					JSONObject verifyingBranchesJson = new JSONObject();
					verifyingBranchesJson.put("iteration", gen);
					JSONArray verifyingBranchesArr = new JSONArray();
					char bCharVerify = 'A';
					for (int i = 0; i < candidates.size(); i++) {
						JSONObject bObj = new JSONObject();
						String bId = "gen_" + gen + "_candidate_" + bCharVerify;
						bObj.put("id", bId);
						bObj.put("strategy", "Candidate " + bCharVerify + " (" + candidates.get(i) + ")");
						if (bId.equals(candidateId)) {
							bObj.put("status", "verifying");
							bObj.put("score", 0.0);
						} else if (i < candIndex) {
							double completedScore = Math.max(0.01, 1.0 / (1.0 + results.get(i).fitness));
							bObj.put("status", "completed");
							bObj.put("score", completedScore);
						} else {
							bObj.put("status", "active");
							bObj.put("score", 0.0);
						}
						verifyingBranchesArr.put(bObj);
						bCharVerify++;
					}
					verifyingBranchesJson.put("branches", verifyingBranchesArr);
					context.log("[DARWIN_BRANCHES] " + verifyingBranchesJson.toString());

					long startTime = System.currentTimeMillis();
					double nativeLoss = 10.0;
					long paramCount = 0;
					double nativeFitness = 30.0;
					double ollamaReferenceScore = 0.0;
					boolean candFailed = false;
					String candFailureReason = null;

					// Path A: EVERY candidate is trained and evaluated using its own native
					// EvoLlmModel.
					boolean nativeSuccess = false;
					try {
						CandidateTrainingResult trainingResult = runOfflineTraining(cleanCorpus, config);
						nativeLoss = trainingResult.loss;
						paramCount = trainingResult.paramCount;
						nativeFitness = trainingResult.fitness;
						nativeSuccess = true;
					} catch (Exception e) {
						context.log("[FORGE] Pure-Java offline training failed for " + candidateName
								+ " - assigning safe penalty score: " + e.getMessage());
						nativeLoss = 10.0;
						nativeFitness = 30.0;
						paramCount = 1_000_000;
						candFailed = true;
						candFailureReason = e.getMessage();
					}

					long durationMs = System.currentTimeMillis() - startTime;

					// Path B: Optional reference evaluation via Ollama is performed *in addition*
					// to native training
					boolean ollamaSuccess = false;
					try {
						String validationPrompt = "Based on this project documentation:\n\n"
								+ safePreview(cleanCorpus, 1000)
								+ "\n\nSummarize the core architecture in exactly two concise sentences.";

						String responseText = queryOllama(ollamaUrl, baseModel, validationPrompt, config);
						ollamaSuccess = true;

						// Calculate reference score purely for diagnostic logging and metadata reports
						double keywordMatchCount = 0;
						String[] keywords = { "evolution", "personal", "economical", "political", "orchestrator",
								"darwin", "genome", "model", "forge", "java", "task" };
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
						ollamaReferenceScore = contentLoss + lengthPenalty;
					} catch (InterruptedException ex) {
						context.log("[FORGE] Ollama protocol query was interrupted. Restoring interrupt status.");
						Thread.currentThread().interrupt();
					} catch (Exception ex) {
						context.log("[FORGE] Optional Ollama reference query skipped: " + ex.getMessage());
					}

					// Candidate Selection / Fitness MUST strictly equal nativeFitness (which
					// includes both train & validation losses)
					// Ollama score does NOT participate in evolutionary ranking.
					double candidateFitness = nativeFitness;

					if (Double.isNaN(candidateFitness) || Double.isInfinite(candidateFitness))
						candidateFitness = 40.0;

					context.log(String.format(
							"[FORGE] Completed %s. Native Val Loss: %.4f, Params: %d, Ollama Ref Loss: %.4f, Duration: %d ms, Candidate Fitness: %.4f",
							candidateName, nativeLoss, paramCount, ollamaReferenceScore, durationMs, candidateFitness));
					logs.add(String.format("Loss %.4f\n-----------------\n", nativeLoss));

					double uiScore = Math.max(0.01, 1.0 / (1.0 + candidateFitness));
					EvolutionProgressPublisher.updateBranchStatus(context, candidateId,
							candidateName + " (" + config + ")", "scoring", uiScore);

					// Publish scoring update to [DARWIN_BRANCHES] D3.js visualization graph
					JSONObject scoringBranchesJson = new JSONObject();
					scoringBranchesJson.put("iteration", gen);
					JSONArray scoringBranchesArr = new JSONArray();
					char bCharScoring = 'A';
					for (int i = 0; i < candidates.size(); i++) {
						JSONObject bObj = new JSONObject();
						String bId = "gen_" + gen + "_candidate_" + bCharScoring;
						bObj.put("id", bId);
						bObj.put("strategy", "Candidate " + bCharScoring + " (" + candidates.get(i) + ")");
						if (bId.equals(candidateId)) {
							bObj.put("status", "scoring");
							bObj.put("score", uiScore);
						} else if (i < candIndex) {
							double completedScore = Math.max(0.01, 1.0 / (1.0 + results.get(i).fitness));
							bObj.put("status", "completed");
							bObj.put("score", completedScore);
						} else {
							bObj.put("status", "active");
							bObj.put("score", 0.0);
						}
						scoringBranchesArr.put(bObj);
						bCharScoring++;
					}
					scoringBranchesJson.put("branches", scoringBranchesArr);
					context.log("[DARWIN_BRANCHES] " + scoringBranchesJson.toString());

					results.add(new CandidateResult(candidateName, config, nativeLoss, paramCount, durationMs,
							candidateFitness, candFailed, candFailureReason));
					candIndex++;
				}
			}

			// Stable deterministic sorting
			results.sort((c1, c2) -> compareCandidates(c1, c2));

			CandidateResult genWinner = results.get(0);

			String selectedId = null;
			List<eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant> variants = new ArrayList<>();
			char bCharVar = 'A';
			for (CandidateResult r : results) {
				eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant v = new eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant();
				String bId = "gen_" + gen + "_candidate_" + bCharVar;
				v.setId(bId);
				v.setBranchId(bId);
				v.setStrategy(r.name + " (" + r.config + ")");
				v.setScore(Math.max(0.01, 1.0 / (1.0 + r.fitness)));
				v.setSurvivalArgument(String.format("Loss: %.4f, Parameters: %d, Duration: %d ms", r.loss, r.paramCount,
						r.durationMs));
				v.setTradeoffs("Zero-dependency local Transformer architecture");
				v.setActivationState(
						eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant.ActivationState.ARCHIVED);
				variants.add(v);
				bCharVar++;
			}

			eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant recommended = variants.stream()
					.max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore())).orElse(null);

			if (context.getMetadata().containsKey("resume_manual_id")) {
				selectedId = (String) context.getMetadata().remove("resume_manual_id");
			} else {
				try {
					selectedId = awaitApproval(variants, iterationManager);
				} catch (DarwinWaitException dwe) {
					context.getMetadata().put("forge_resume_gen", gen);
					context.getMetadata().put("forge_overall_winner", overallWinner);
					context.getMetadata().put("forge_logs", logs);
					context.getMetadata().put("forge_gen_reports", genReports);
					context.getMetadata().put("forge_candidates", candidates);
					context.getMetadata().put("forge_current_results", results);

					OrchestratorResponse waitResponse = new OrchestratorResponse();
					waitResponse.setResultType(ResultType.CHAT);
					waitResponse.setSummary("Evolution paused. Waiting for user decision.");
					waitResponse.setContent("Evolution paused. Waiting for user decision.");
					return waitResponse;
				}
			}

			if (selectedId != null && !selectedId.isEmpty() && !"REGENERATE".equals(selectedId)
					&& !"REGENERATE_SAME_DIMENSION".equals(selectedId) && !"STOP".equals(selectedId)
					&& !"FAILED".equals(selectedId)) {
				char winnerSuffix = selectedId.charAt(selectedId.length() - 1);
				for (CandidateResult r : results) {
					if (r.name.endsWith(String.valueOf(winnerSuffix))) {
						genWinner = r;
						break;
					}
				}
			}

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

			// Correct overall winner logic across ALL generations (not just replacing with
			// the final generation's winner)
			if (overallWinner == null || compareCandidates(genWinner, overallWinner) < 0) {
				overallWinner = new CandidateResult(genWinner.name, genWinner.config, genWinner.loss,
						genWinner.paramCount, genWinner.durationMs, genWinner.fitness, genWinner.failed,
						genWinner.failureReason);
			}

			// Update the winner and rejected branch statuses for the UI
			String winnerBranchId = "gen_" + gen + "_candidate_"
					+ genWinner.name.substring(genWinner.name.length() - 1);
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

			// Publish final [DARWIN_BRANCHES] JSON payload with APPROVED status stamp to UI
			// chat and workflow graphs
			JSONObject finalGenBranchesJson = new JSONObject();
			finalGenBranchesJson.put("iteration", gen);
			finalGenBranchesJson.put("minIterations", 1);
			finalGenBranchesJson.put("maxIterations", generations);
			finalGenBranchesJson.put("branchingLimit", candidates.size());
			finalGenBranchesJson.put("minBranchingLimit", 1);
			JSONArray finalGenBranchesArr = new JSONArray();
			char bCharFinal = 'A';
			for (int i = 0; i < candidates.size(); i++) {
				JSONObject bObj = new JSONObject();
				String bId = "gen_" + gen + "_candidate_" + bCharFinal;
				bObj.put("id", bId);
				bObj.put("strategy", "Candidate " + bCharFinal + " (" + candidates.get(i) + ")");
				double completedScore = Math.max(0.01, 1.0 / (1.0 + results.get(i).fitness));
				bObj.put("score", completedScore);
				if (bId.equals(winnerBranchId)) {
					bObj.put("status", "APPROVED");
					finalGenBranchesJson.put("winnerId", bId);
				} else {
					bObj.put("status", "REJECTED");
				}
				finalGenBranchesArr.put(bObj);
				bCharFinal++;
			}
			finalGenBranchesJson.put("branches", finalGenBranchesArr);

			StringBuilder outcomeBuilder = new StringBuilder();
			char bCharOutcome = 'A';
			for (int i = 0; i < candidates.size(); i++) {
				String bId = "gen_" + gen + "_candidate_" + bCharOutcome;
				String status = bId.equals(winnerBranchId) ? "APPROVED" : "REJECTED";
				outcomeBuilder.append("[").append(status).append(":").append(bId).append("] ");
				bCharOutcome++;
			}
			String decisionType = (selectedId != null) ? "MANUAL" : "AUTO";
			outcomeBuilder.append("[DECISION:").append(decisionType).append("] ");
			outcomeBuilder.append("[DARWIN_BRANCHES] ").append(finalGenBranchesJson.toString());
			context.log(outcomeBuilder.toString());

			// Generate candidates for the next generation via mutation using reproducible
			// configurations
			if (gen < generations) {
				candidates = new ArrayList<>();
				int expansionValue = getExpansionValue();
				boolean seriousReason = (overallWinner != null && overallWinner.failed);
				int currentBranchLimit = (expansionValue > 5 || seriousReason) ? 2 : 1;

				if (currentBranchLimit == 1) {
					// Elite configuration is mutated directly to explore the space since we only
					// have 1 slot
					candidates.add(mutate(overallWinner != null ? overallWinner.config : genWinner.config, 1, gen));
				} else {
					candidates.add(copyConfig(genWinner.config)); // Elite survival config copy to avoid aliasing
					candidates.add(mutate(genWinner.config, 1, gen)); // Reproducible mutation type 1
				}
			}
		}

		// --- WINNER EXPORT ---
		context.getMetadata().remove("forge_resume_gen");
		context.getMetadata().remove("forge_overall_winner");
		context.getMetadata().remove("forge_logs");
		context.getMetadata().remove("forge_gen_reports");
		context.getMetadata().remove("forge_candidates");
		context.getMetadata().remove("forge_current_results");

		context.log("[FORGE] Evolution complete. Overall Winner across generations: " + overallWinner.config);

		// Generate Dynamic Model Name (based on context target folder, winning config,
		// and timestamp)
		String dynamicModelName = generateDynamicModelName(context, overallWinner.config, targetPath);
		context.log("[FORGE] Dynamically generated winning model ID: " + dynamicModelName);

		// Create new workspace output folder for the winning model
		File workspaceDir;
		String workspacePathStr = ProjectModelManager.getWorkspacePath();
		if (workspacePathStr != null && !workspacePathStr.isEmpty()) {
			workspaceDir = new File(workspacePathStr);
		} else {
			workspaceDir = context.getProjectRoot();
		}

		File forgeOutputDir = new File(workspaceDir, "forge-output/" + dynamicModelName);
		if (forgeOutputDir.exists()) {
			deleteDirectory(forgeOutputDir);
		}
		forgeOutputDir.mkdirs();

		// Reconstruct/retrain the winning candidate for final export
		context.log("[FORGE] Reconstructing selected global winner for final export.");
		SimpleBPETokenizer finalTokenizer = new SimpleBPETokenizer();
		finalTokenizer.train(cleanCorpus, overallWinner.config.vocabSize);
		List<Integer> allTokens = finalTokenizer.encode(cleanCorpus);
		if (allTokens.size() > MAX_TOKENS_LIMIT) {
			List<Integer> truncated = new ArrayList<>(allTokens.subList(0, MAX_TOKENS_LIMIT));
			allTokens.clear();
			allTokens = truncated;
		}
		DatasetBuilder finalDatasetBuilder = new DatasetBuilder();
		int finalSeqLen = overallWinner.config.maxSeqLen;
		int finalStride = Math.max(1, finalSeqLen / 2);
		List<DatasetBuilder.Sample> samples = finalDatasetBuilder.buildSlidingWindow(allTokens, finalSeqLen,
				finalStride);
		if (samples.size() > MAX_TRAINING_SAMPLES) {
			List<DatasetBuilder.Sample> truncated = new ArrayList<>(samples.subList(0, MAX_TRAINING_SAMPLES));
			samples.clear();
			samples = truncated;
		}

		// Build final model and save configuration & tokenizer
		int dff = overallWinner.config.dff;
		EvoLlmModel winningModel = new EvoLlmModel(overallWinner.config.vocabSize, overallWinner.config.embeddingSize,
				overallWinner.config.heads, overallWinner.config.layers, dff, finalSeqLen);

		EvoLlmTrainer trainer = new EvoLlmTrainer(winningModel);
		trainer.setProgressListener((epoch, totalEpochs, sampleIndex, totalSamples, currentLoss) -> {
			int logInterval = Math.max(1, totalSamples / 10);
			if (sampleIndex % logInterval == 0 || sampleIndex == totalSamples) {
				double pct = (double) sampleIndex / totalSamples * 100.0;
				context.log(String.format(
						"[EVO Training Progress] Global Winner Refinement - Epoch %d/%d | Progress: %d/%d (%.1f%%) | Loss: %.4f",
						epoch + 1, totalEpochs, sampleIndex, totalSamples, pct, currentLoss));
			}
		});
		int finalEpochs = forceSolution ? 1 : overallWinner.config.epochs;
		trainer.train(samples, finalEpochs);

		// CREATE NATIVE EVO MODEL ARTIFACT AND PERSIST (as a *.evo packaged file)
		context.log("[FORGE] Persisting final native EVO model artifact to *.evo package.");
		Path evoFilePath = Paths.get(workspaceDir.getAbsolutePath(), "forge-output", dynamicModelName + ".evo");

		// First, get the full vocabulary from the tokenizer
		Map<String, Integer> finalVocab = finalTokenizer.getVocab();
		// Log vocabulary size for debugging
		context.log("[FORGE] Final vocabulary size: " + finalVocab.size());

		EvoModelArtifact artifact = new EvoModelArtifact();

		artifact.initializeFromModel(dynamicModelName, winningModel, finalVocab); // ✅ Pass full vocab

		// Set inference parameters
		artifact.setTemperature(overallWinner.config.temperature);
		artifact.setTopP(overallWinner.config.topP);
		artifact.setTopK(overallWinner.config.topK);
		artifact.setRepeatPenalty(overallWinner.config.repeatPenalty);

		// artifact.initializeFromModel(dynamicModelName, winningModel,
		// finalTokenizer.getVocab());
		// artifact.setTemperature(overallWinner.config.temperature);
		// artifact.setTopP(overallWinner.config.topP);
		// artifact.setTopK(overallWinner.config.topK);
		// artifact.setRepeatPenalty(overallWinner.config.repeatPenalty);
		artifact.save(evoFilePath);
		// ============ VERIFICATION STEP ============

// After saving, verify the artifact loaded correctly
		EvoModelArtifact loadedArtifact = EvoModelArtifact.load(evoFilePath);
		context.log("[FORGE] Verified artifact contains " + loadedArtifact.getTokenizerVocab().size()
				+ " vocabulary entries");

// Sample check
		Map<Integer, String> sampleVocab = loadedArtifact.getIdToToken();
		int sampleCount = 0;
		for (Map.Entry<Integer, String> entry : sampleVocab.entrySet()) {
			if (sampleCount++ < 5) {
				context.log("[FORGE] Sample vocab: " + entry.getKey() + " -> \"" + entry.getValue() + "\"");
			}
		}

		// LOAD FROM THE *.EVO MODEL FILE TO CONFIRM PORTABILITY
		context.log("[FORGE] Loading native EVO model artifact from *.evo file for subsequent target export.");
		// EvoModelArtifact loadedArtifact = EvoModelArtifact.load(evoFilePath);

		// EXPORT TO OLLAMA/GGUF WITHOUT RETRAINING (Consumes the loaded artifact)
		OllamaExporter exporter = new OllamaExporter();
		exporter.export(loadedArtifact, forgeOutputDir.toPath());

		// Register evolved models in Ollama via Ollama Protocol using Modelfile
		// generated by OllamaExporter
		try {
			// Register evolved models in Ollama via Ollama Protocol
			eu.kalafatic.evolution.controller.manager.OllamaService managedService = eu.kalafatic.evolution.controller.manager.OllamaManager
					.getInstance().getService(ollamaUrl);

			Path finalModelfilePath = forgeOutputDir.toPath().resolve("Modelfile");
			if (Files.exists(finalModelfilePath)) {
				String finalModelfileContent = Files.readString(finalModelfilePath);
				context.log("[FORGE] Registering evolved model in Ollama via Ollama Protocol: " + dynamicModelName);
				managedService.createModel(dynamicModelName, finalModelfileContent);

				context.log("[FORGE] Registering evolved model 'evo' alias via Ollama Protocol");
				managedService.createModel("evo", finalModelfileContent);

				context.log("[FORGE] Evolved Ollama model registration complete!");
			}

			// Final validation - query the newly generated custom EVO model itself
			context.log(
					"[FORGE] Initiating final validation on the generated custom EVO artifact: " + dynamicModelName);
			try {
				String validationPrompt = "Explain evolution genome data management in exactly one sentence.";
				String validationResponse = queryOllama(ollamaUrl, dynamicModelName, validationPrompt,
						overallWinner.config);
				context.log("[FORGE] Custom EVO Model Response: " + safePreview(validationResponse.trim(), 500));
			} catch (Exception ex) {
				context.log("[FORGE] Warning: Querying the registered custom EVO model failed: " + ex.getMessage());
			}
		} catch (Exception ex) {
			context.log("[FORGE] Warning: Evolved Ollama model registration failed: " + ex.getMessage());
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

		// Record all created artifacts in FileChangeTracker
		recordCreatedFile(evoFilePath.toFile());
		recordCreatedFile(forgeOutputDir);

		// Stage created files in Git if in a Git repository
		try {
			GitManager gitManager = iterationManager.getGitManager();
			if (gitManager != null && gitManager.isGitRepository()) {
				gitManager.getGitTool().execute("add .", context.getProjectRoot(), context);
			}
		} catch (Exception e) {
			context.log("[FORGE] Warning staging generated files in git: " + e.getMessage());
		}

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
		String evoPrefix = "file:///" + forgeOutputDir.getParentFile().getAbsolutePath().replace("\\", "/");
		summaryBuilder.append(String.format("- **Portable Native Model File (*.evo):** [%s.evo](%s/%s.evo)\n",
				dynamicModelName, evoPrefix, dynamicModelName));
		summaryBuilder.append(
				String.format("- **Output Folder:** [%s/](%s/)\n", "forge-output/" + dynamicModelName, uriPrefix));
		summaryBuilder.append(String.format("- **Model GGUF:** [evo.gguf](%s/evo.gguf)\n", uriPrefix));
		summaryBuilder.append(String.format("- **Ollama Modelfile:** [Modelfile](%s/Modelfile)\n", uriPrefix));
		summaryBuilder.append(
				String.format("- **Vocabulary / Tokenizer:** [tokenizer.json](%s/tokenizer.json)\n", uriPrefix));
		summaryBuilder.append(String.format("- **Model Configuration:** [config.json](%s/config.json)\n", uriPrefix));
		summaryBuilder.append(String.format("- **Model Weights:** [weights.bin](%s/weights.bin)\n", uriPrefix));
		summaryBuilder.append(String.format("- **Checkpoint Directory:** [checkpoint/](%s/checkpoint/)\n", uriPrefix));
		summaryBuilder.append(
				String.format("- **Training Report:** [training-report.json](%s/training-report.json)\n", uriPrefix));

		FinalResponseAssembler assembler = new FinalResponseAssembler();
		FinalResponse finalResponse = assembler.assemble(context, summaryBuilder.toString(), true,
				context.getStartTime());

		OrchestratorResponse response = new OrchestratorResponse();
		response.setResultType(ResultType.CHAT);
		response.setFinalResponse(finalResponse);

		iterationManager.transition(SystemState.DONE, context);
		EvolutionProgressPublisher.completeIteration(context);

		return response;
	}

	private void recordCreatedFile(File file) {
		if (file == null || !file.exists())
			return;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					recordCreatedFile(child);
				}
			}
			return;
		}
		try {
			Path projectPath = context.getProjectRoot().toPath().toAbsolutePath().normalize();
			Path filePath = file.toPath().toAbsolutePath().normalize();
			String relPath;
			if (filePath.startsWith(projectPath)) {
				relPath = projectPath.relativize(filePath).toString().replace('\\', '/');
			} else {
				relPath = projectPath.relativize(filePath).toString().replace('\\', '/');
			}
			if (!FileFilterUtil.isSystemFile(relPath)) {
				context.getFileChangeTracker().recordChange(relPath, FileChangeTracker.ChangeType.NEW);
			}
		} catch (Exception e) {
			context.log("[FORGE] Warning recording file change for " + file + ": " + e.getMessage());
		}
	}

	private SimpleBPETokenizer trainTokenizerWithFullVocab(String corpus, int vocabSize) {
		SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
		tokenizer.train(corpus, vocabSize);

		// Ensure vocabulary is complete - log for debugging
		Map<String, Integer> vocab = tokenizer.getVocab();
		System.out.println("[EVO] Trained tokenizer with " + vocab.size() + " vocabulary entries");

		// Log sample entries
		int count = 0;
		for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
			if (count++ < 10) {
				System.out.println("[EVO] Vocab entry: " + entry.getValue() + " -> \"" + entry.getKey() + "\"");
			}
		}

		return tokenizer;
	}

	/**
	 * Helper method to train custom tokenizer and model in Java cleanly.
	 */
	private CandidateTrainingResult runOfflineTraining(String cleanCorpus, LlmConfig config) {
		SimpleBPETokenizer tokenizer = null;
		List<Integer> allTokens = null;
		List<DatasetBuilder.Sample> samples = null;
		List<DatasetBuilder.Sample> trainSamples = null;
		List<DatasetBuilder.Sample> valSamples = null;
		EvoLlmModel model = null;
		EvoLlmTrainer trainer = null;
		try {
			// ✅ FIX: Use the new tokenizer method
			tokenizer = trainTokenizerWithFullVocab(cleanCorpus, config.vocabSize);
			allTokens = tokenizer.encode(cleanCorpus);
			tokenizer.train(cleanCorpus, config.vocabSize);

			if (allTokens.size() > MAX_TOKENS_LIMIT) {
				List<Integer> truncated = new ArrayList<>(allTokens.subList(0, MAX_TOKENS_LIMIT));
				allTokens.clear();
				allTokens = truncated;
			}

			DatasetBuilder datasetBuilder = new DatasetBuilder();
			int seqLen = config.maxSeqLen;
			int stride = Math.max(1, seqLen / 2);
			samples = datasetBuilder.buildSlidingWindow(allTokens, seqLen, stride);

			if (samples.size() > MAX_TRAINING_SAMPLES) {
				List<DatasetBuilder.Sample> truncated = new ArrayList<>(samples.subList(0, MAX_TRAINING_SAMPLES));
				samples.clear();
				samples = truncated;
			}

			// Bounded, deterministic 80/20 train/validation split
			int totalCount = samples.size();
			int trainCount = (int) (totalCount * 0.8);
			if (trainCount <= 0 && totalCount > 0) {
				trainCount = totalCount;
			}

			trainSamples = new ArrayList<>(samples.subList(0, trainCount));
			valSamples = new ArrayList<>(samples.subList(trainCount, totalCount));

			int dff = config.dff;
			model = new EvoLlmModel(config.vocabSize, config.embeddingSize, config.heads, config.layers, dff, seqLen);

			long paramCount = 0;
			for (Tensor p : model.parameters()) {
				paramCount += p.getData().length;
			}

			trainer = new EvoLlmTrainer(model);
			trainer.setProgressListener((epoch, totalEpochs, sampleIndex, totalSamples, currentLoss) -> {
				int logInterval = Math.max(1, totalSamples / 10);
				if (sampleIndex % logInterval == 0 || sampleIndex == totalSamples) {
					double pct = (double) sampleIndex / totalSamples * 100.0;
					context.log(String.format(
							"[EVO Training Progress] Candidate Evaluation - Epoch %d/%d | Progress: %d/%d (%.1f%%) | Loss: %.4f",
							epoch + 1, totalEpochs, sampleIndex, totalSamples, pct, currentLoss));
				}
			});
			double trainLoss = 2.5;
			if (!trainSamples.isEmpty()) {
				trainer.train(trainSamples, config.epochs);
				trainLoss = trainer.getLossHistory().isEmpty() ? 2.5
						: trainer.getLossHistory().get(trainer.getLossHistory().size() - 1);
			}

			// Compute Validation Loss (forward pass only, no gradient backpropagation)
			double valLossSum = 0;
			int valCount = 0;
			if (!valSamples.isEmpty()) {
				for (DatasetBuilder.Sample valSample : valSamples) {
					int[] inputIds = valSample.input.stream().mapToInt(i -> i).toArray();
					Tensor logits = model.forward(inputIds);
					float[] logitsData = logits.getData();
					int sLen = (int) logits.getShape()[0];
					int vSize = (int) logits.getShape()[1];
					int lastOffset = (sLen - 1) * vSize;
					int target = valSample.target;

					// Softmax
					float max = Float.NEGATIVE_INFINITY;
					for (int i = 0; i < vSize; i++) {
						if (logitsData[lastOffset + i] > max)
							max = logitsData[lastOffset + i];
					}
					float sum = 0;
					float[] probs = new float[vSize];
					for (int i = 0; i < vSize; i++) {
						probs[i] = (float) Math.exp(logitsData[lastOffset + i] - max);
						sum += probs[i];
					}
					for (int i = 0; i < vSize; i++)
						probs[i] /= sum;

					double sampleLoss = -Math.log(Math.max(probs[target], 1e-10));
					valLossSum += sampleLoss;
					valCount++;
				}
			}

			double valLoss = valCount > 0 ? (valLossSum / valCount) : trainLoss;

			if (Double.isNaN(trainLoss) || Double.isInfinite(trainLoss)) {
				trainLoss = 10.0;
			}
			if (Double.isNaN(valLoss) || Double.isInfinite(valLoss)) {
				valLoss = 10.0;
			}

			// Combine validation loss (75%) and training loss (25%) with size penalty to
			// prevent overfitting and encourage generalizability
			double sizePenalty = paramCount * 0.000001;
			double fitness = (valLoss * 0.75) + (trainLoss * 0.25) + sizePenalty;

			return new CandidateTrainingResult(valLoss, paramCount, fitness);
		} finally {
			if (allTokens != null)
				allTokens.clear();
			if (samples != null)
				samples.clear();
			if (trainSamples != null)
				trainSamples.clear();
			if (valSamples != null)
				valSamples.clear();
			tokenizer = null;
			allTokens = null;
			samples = null;
			trainSamples = null;
			valSamples = null;
			model = null;
			trainer = null;
		}
	}

	/**
	 * Atomically writes weight files by writing to a unique temporary file first,
	 * flushing/closing, and then moving.
	 */
	private void writeWeightsAtomically(Path target, EvoLlmModel model) throws Exception {
		Path parent = target.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Path tempFile = parent.resolve("weights.bin." + UUID.randomUUID().toString() + ".tmp");
		try {
			try (DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(tempFile.toFile())))) {
				for (Tensor p : model.parameters()) {
					for (float val : p.getData()) {
						dos.writeFloat(val);
					}
				}
				dos.flush();
			}
			try {
				Files.move(tempFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
						java.nio.file.StandardCopyOption.ATOMIC_MOVE);
			} catch (java.io.IOException e) {
				Files.move(tempFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
			context.log("[FORGE] Weights written atomically to: " + target.toAbsolutePath() + " (size: "
					+ Files.size(target) + " bytes)");
		} catch (Exception e) {
			try {
				Files.deleteIfExists(tempFile);
			} catch (Exception ex) {
				// ignore
			}
			throw e;
		}
	}

	private LlmConfig mutate(LlmConfig winner, int mutationIdx, int gen) {
		int vocabSize = winner.vocabSize;
		int embeddingSize = winner.embeddingSize;
		int layers = winner.layers;
		int heads = winner.heads;
		int dff = winner.dff;
		int maxSeqLen = winner.maxSeqLen;
		int epochs = winner.epochs;

		// Bounded, reproducible seed derived from generation, session, and index
		long seed = (long) gen * 31 + mutationIdx * 17 + context.getSessionId().hashCode();
		Random random = new Random(seed);

		switch (mutationIdx) {
		case 1:
			embeddingSize = Math.max(64, embeddingSize + (random.nextBoolean() ? 64 : -64));
			vocabSize = Math.max(500, vocabSize + (random.nextBoolean() ? 500 : -500));
			dff = embeddingSize * 4;
			break;
		case 2:
			layers = Math.max(1, layers + (random.nextBoolean() ? 1 : -1));
			heads = Math.max(2, heads + (random.nextBoolean() ? 2 : -2));
			break;
		case 3:
			maxSeqLen = Math.max(64, maxSeqLen + (random.nextBoolean() ? 32 : -32));
			epochs = Math.max(1, epochs + (random.nextBoolean() ? 2 : -2));
			break;
		}

		LlmConfig mutated = new LlmConfig(vocabSize, embeddingSize, layers, heads, dff, maxSeqLen, epochs);
		mutated = normalizeCandidateConfig(mutated);

		// Evolve derived Ollama parameters with deltas for active exploration
		mutated.temperature = Math.max(0.1f, Math.min(1.5f, winner.temperature + (random.nextFloat() * 0.2f - 0.1f)));
		mutated.topP = Math.max(0.1f, Math.min(1.0f, winner.topP + (random.nextFloat() * 0.1f - 0.05f)));
		mutated.topK = Math.max(10, Math.min(100, winner.topK + (random.nextBoolean() ? 5 : -5)));
		mutated.repeatPenalty = Math.max(1.0f,
				Math.min(2.0f, winner.repeatPenalty + (random.nextFloat() * 0.1f - 0.05f)));

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
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(genUrl))
				.header("Content-Type", "application/json").timeout(Duration.ofSeconds(60))
				.POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString())).build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new RuntimeException("HTTP " + response.statusCode() + ": " + safePreview(response.body(), 1000));
		}

		JSONObject jsonResponse = new JSONObject(response.body());
		return jsonResponse.optString("response", "");
	}

	public String generateDynamicModelName(TaskContext context, LlmConfig winner, String targetPath) {
		String folderName = "generic";
		if (targetPath != null && !targetPath.isEmpty()) {
			File folder = new File(targetPath);
			folderName = folder.getName().toLowerCase().replaceAll("[^a-zA-Z0-9-]", "-").replaceAll("-+", "-");
		}

		String archSignature = String.format("v%d-e%d-l%d-h%d", winner.vocabSize, winner.embeddingSize, winner.layers,
				winner.heads);

		String timestamp = java.time.format.DateTimeFormatter.ofPattern("ddMMyy_HHmmss")
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
