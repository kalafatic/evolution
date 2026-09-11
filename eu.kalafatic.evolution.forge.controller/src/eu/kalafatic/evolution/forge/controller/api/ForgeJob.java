package eu.kalafatic.evolution.forge.controller.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeJob {

    public enum JobState {
        CREATED,
        ANALYZING,
        PREPARING,
        READY_TO_TRAIN,
        TRAINING,
        EVALUATING,
        OPTIMIZING,
        FINALIZING,
        VALIDATING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public enum ModelObjective {
        AUTO,
        GENERAL,
        CODING,
        REASONING,
        CHAT,
        KNOWLEDGE,
        DOMAIN_SPECIALIST,
        EVO_DEVELOPER_ASSISTANT
    }

    public static class SourceProfile {
        private String sourcePath;
        private String sourceType; // HF, LOCAL_DIR, FILE, EVODATA, EVO_MODEL
        private long sampleCount;
        private long estimatedTokens;
        private double qualityScore;
        private String language = "EN";
        private double knowledgeRatio;
        private double codeRatio;
        private double instructionRatio;
        private double chatRatio;

        public SourceProfile() {}

        public SourceProfile(String sourcePath, String sourceType) {
            this.sourcePath = sourcePath;
            this.sourceType = sourceType;
        }

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }

        public long getSampleCount() { return sampleCount; }
        public void setSampleCount(long sampleCount) { this.sampleCount = sampleCount; }

        public long getEstimatedTokens() { return estimatedTokens; }
        public void setEstimatedTokens(long estimatedTokens) { this.estimatedTokens = estimatedTokens; }

        public double getQualityScore() { return qualityScore; }
        public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public double getKnowledgeRatio() { return knowledgeRatio; }
        public void setKnowledgeRatio(double knowledgeRatio) { this.knowledgeRatio = knowledgeRatio; }

        public double getCodeRatio() { return codeRatio; }
        public void setCodeRatio(double codeRatio) { this.codeRatio = codeRatio; }

        public double getInstructionRatio() { return instructionRatio; }
        public void setInstructionRatio(double instructionRatio) { this.instructionRatio = instructionRatio; }

        public double getChatRatio() { return chatRatio; }
        public void setChatRatio(double chatRatio) { this.chatRatio = chatRatio; }
    }

    public static class CompositionStrategy {
        private Map<String, Double> sourceWeights = new HashMap<>();
        private long tokenBudget = 500_000_000L; // Default target token budget
        private double qualityThreshold = 0.5;
        private double knowledgePercent = 0.40;
        private double codePercent = 0.25;
        private double instructionPercent = 0.20;
        private double chatPercent = 0.15;

        public Map<String, Double> getSourceWeights() { return sourceWeights; }
        public void setSourceWeights(Map<String, Double> sourceWeights) { this.sourceWeights = sourceWeights; }

        public long getTokenBudget() { return tokenBudget; }
        public void setTokenBudget(long tokenBudget) { this.tokenBudget = tokenBudget; }

        public double getQualityThreshold() { return qualityThreshold; }
        public void setQualityThreshold(double qualityThreshold) { this.qualityThreshold = qualityThreshold; }

        public double getKnowledgePercent() { return knowledgePercent; }
        public void setKnowledgePercent(double knowledgePercent) { this.knowledgePercent = knowledgePercent; }

        public double getCodePercent() { return codePercent; }
        public void setCodePercent(double codePercent) { this.codePercent = codePercent; }

        public double getInstructionPercent() { return instructionPercent; }
        public void setInstructionPercent(double instructionPercent) { this.instructionPercent = instructionPercent; }

        public double getChatPercent() { return chatPercent; }
        public void setChatPercent(double chatPercent) { this.chatPercent = chatPercent; }
    }

    public static class ModelConfig {
        private String modelSize = "SMALL";
        private int hiddenSize = 512;
        private int layers = 8;
        private int heads = 8;
        private int dff = 2048;
        private int maxSeqLen = 1024;
        private int vocabSize = 4096;

        public String getModelSize() { return modelSize; }
        public void setModelSize(String modelSize) { this.modelSize = modelSize; }

        public int getHiddenSize() { return hiddenSize; }
        public void setHiddenSize(int hiddenSize) { this.hiddenSize = hiddenSize; }

        public int getLayers() { return layers; }
        public void setLayers(int layers) { this.layers = layers; }

        public int getHeads() { return heads; }
        public void setHeads(int heads) { this.heads = heads; }

        public int getDff() { return dff; }
        public void setDff(int dff) { this.dff = dff; }

        public int getMaxSeqLen() { return maxSeqLen; }
        public void setMaxSeqLen(int maxSeqLen) { this.maxSeqLen = maxSeqLen; }

        public int getVocabSize() { return vocabSize; }
        public void setVocabSize(int vocabSize) { this.vocabSize = vocabSize; }
    }

    public static class TrainingConfig {
        private double learningRate = 0.01;
        private int epochs = 1;
        private int batchSize = 16;
        private int warmupSteps = 10;
        private String forgeMode = "NEW_MODEL";

        public double getLearningRate() { return learningRate; }
        public void setLearningRate(double learningRate) { this.learningRate = learningRate; }

        public int getEpochs() { return epochs; }
        public void setEpochs(int epochs) { this.epochs = epochs; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public int getWarmupSteps() { return warmupSteps; }
        public void setWarmupSteps(int warmupSteps) { this.warmupSteps = warmupSteps; }

        public String getForgeMode() { return forgeMode; }
        public void setForgeMode(String forgeMode) { this.forgeMode = forgeMode; }
    }

    public static class PreflightResult {
        private boolean passed = false;
        private final List<String> checks = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }

        public List<String> getChecks() { return checks; }
        public List<String> getErrors() { return errors; }

        public void addCheck(String check) { checks.add(check); }
        public void addError(String error) { errors.add(error); }
    }

    public static class EvaluationResult {
        private double trainLoss;
        private double valLoss;
        private double qualityScore;
        private boolean overfittingDetected;
        private String summary = "";

        public double getTrainLoss() { return trainLoss; }
        public void setTrainLoss(double trainLoss) { this.trainLoss = trainLoss; }

        public double getValLoss() { return valLoss; }
        public void setValLoss(double valLoss) { this.valLoss = valLoss; }

        public double getQualityScore() { return qualityScore; }
        public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

        public boolean isOverfittingDetected() { return overfittingDetected; }
        public void setOverfittingDetected(boolean overfittingDetected) { this.overfittingDetected = overfittingDetected; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    public static class SmokeTestResult {
        private boolean passed;
        private String promptUsed;
        private String generatedText;
        private long latencyMs;
        private String error;

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }

        public String getPromptUsed() { return promptUsed; }
        public void setPromptUsed(String promptUsed) { this.promptUsed = promptUsed; }

        public String getGeneratedText() { return generatedText; }
        public void setGeneratedText(String generatedText) { this.generatedText = generatedText; }

        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    private final String jobId;
    private JobState state = JobState.CREATED;
    private ModelObjective objective = ModelObjective.AUTO;
    private long requestedMinimumUsableBytes = 524_288_000L; // Default 500 MB
    private List<String> sourcePaths = new ArrayList<>();
    private final List<SourceProfile> sourceProfiles = new ArrayList<>();
    private CompositionStrategy compositionStrategy = new CompositionStrategy();
    private ModelConfig modelConfig = new ModelConfig();
    private TrainingConfig trainingConfig = new TrainingConfig();
    private PreflightResult preflightResult = new PreflightResult();
    private EvaluationResult evaluationResult = new EvaluationResult();
    private SmokeTestResult smokeTestResult = new SmokeTestResult();
    private Path runDirectory;
    private String outputModelName;
    private String failureReason;
    private int progressPercent = 0;
    private String currentStageDescription = "";

    public ForgeJob(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() { return jobId; }

    public JobState getState() { return state; }
    public void setState(JobState state) { this.state = state; }

    public ModelObjective getObjective() { return objective; }
    public void setObjective(ModelObjective objective) { this.objective = objective; }

    public long getRequestedMinimumUsableBytes() { return requestedMinimumUsableBytes; }
    public void setRequestedMinimumUsableBytes(long requestedMinimumUsableBytes) { this.requestedMinimumUsableBytes = requestedMinimumUsableBytes; }

    public List<String> getSourcePaths() { return sourcePaths; }
    public void setSourcePaths(List<String> sourcePaths) { this.sourcePaths = sourcePaths; }

    public List<SourceProfile> getSourceProfiles() { return sourceProfiles; }

    public CompositionStrategy getCompositionStrategy() { return compositionStrategy; }
    public void setCompositionStrategy(CompositionStrategy compositionStrategy) { this.compositionStrategy = compositionStrategy; }

    public ModelConfig getModelConfig() { return modelConfig; }
    public void setModelConfig(ModelConfig modelConfig) { this.modelConfig = modelConfig; }

    public TrainingConfig getTrainingConfig() { return trainingConfig; }
    public void setTrainingConfig(TrainingConfig trainingConfig) { this.trainingConfig = trainingConfig; }

    public PreflightResult getPreflightResult() { return preflightResult; }
    public void setPreflightResult(PreflightResult preflightResult) { this.preflightResult = preflightResult; }

    public EvaluationResult getEvaluationResult() { return evaluationResult; }
    public void setEvaluationResult(EvaluationResult evaluationResult) { this.evaluationResult = evaluationResult; }

    public SmokeTestResult getSmokeTestResult() { return smokeTestResult; }
    public void setSmokeTestResult(SmokeTestResult smokeTestResult) { this.smokeTestResult = smokeTestResult; }

    public Path getRunDirectory() { return runDirectory; }
    public void setRunDirectory(Path runDirectory) { this.runDirectory = runDirectory; }

    public String getOutputModelName() { return outputModelName; }
    public void setOutputModelName(String outputModelName) { this.outputModelName = outputModelName; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public String getCurrentStageDescription() { return currentStageDescription; }
    public void setCurrentStageDescription(String currentStageDescription) { this.currentStageDescription = currentStageDescription; }
}
