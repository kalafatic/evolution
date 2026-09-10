package eu.kalafatic.evolution.controller.services;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Service to evaluate model performance across Analysis, Chat, and Programming categories.
 */
public class ModelEvaluationService {

    private final AiService aiService = new AiService();

    private static final String PROMPT_ANALYZE = "Analyze the pros and cons of microservices vs monolithic architecture. Be concise.";
    private static final String PROMPT_CHAT = "Tell me a joke and then explain why it's funny in a friendly tone.";
    private static final String PROMPT_PROGRAMMING = "Write a Java function to calculate the Nth Fibonacci number using iteration.";
    private static final String PROMPT_EVO_ARCHITECTURE = "What is the role of IterationManager, ForgeOrchestrator, and EvoDatasetArtifact in the EVO platform?";
    private static final String PROMPT_ECLIPSE_OSGI = "Explain how OSGi bundle MANIFEST.MF dependencies, plugin.xml extension points, and Tycho build pom.xml interact in Eclipse RCP.";
    private static final String PROMPT_JAVA_COMPETENCE = "Write a clean Java method to compute SHA-256 hash of a string using java.security.MessageDigest.";

    private static final String EVALUATION_CRITERIA =
            "Evaluate the following AI response on a scale of 1 to 10 for the given category. " +
            "Category: %s. " +
            "Response: %s. " +
            "Criteria: Accuracy, technical correctness, and depth. " +
            "Output MUST be a valid JSON object with a single field 'rating' (integer 1-10).";

    public static class EvaluationReport {
        private String modelName;
        private int ratingAnalyze;
        private int ratingChat;
        private int ratingProgramming;
        private int ratingEvoArchitecture;
        private int ratingEclipseOsgi;
        private int ratingJavaCompetence;
        private int overallRating;

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public int getRatingAnalyze() { return ratingAnalyze; }
        public void setRatingAnalyze(int ratingAnalyze) { this.ratingAnalyze = ratingAnalyze; }

        public int getRatingChat() { return ratingChat; }
        public void setRatingChat(int ratingChat) { this.ratingChat = ratingChat; }

        public int getRatingProgramming() { return ratingProgramming; }
        public void setRatingProgramming(int ratingProgramming) { this.ratingProgramming = ratingProgramming; }

        public int getRatingEvoArchitecture() { return ratingEvoArchitecture; }
        public void setRatingEvoArchitecture(int ratingEvoArchitecture) { this.ratingEvoArchitecture = ratingEvoArchitecture; }

        public int getRatingEclipseOsgi() { return ratingEclipseOsgi; }
        public void setRatingEclipseOsgi(int ratingEclipseOsgi) { this.ratingEclipseOsgi = ratingEclipseOsgi; }

        public int getRatingJavaCompetence() { return ratingJavaCompetence; }
        public void setRatingJavaCompetence(int ratingJavaCompetence) { this.ratingJavaCompetence = ratingJavaCompetence; }

        public int getOverallRating() { return overallRating; }
        public void setOverallRating(int overallRating) { this.overallRating = overallRating; }
    }

    public void evaluateModel(Orchestrator orchestrator, AIProvider provider, TaskContext context) throws Exception {
        context.log("Starting evaluation for model: " + provider.getName());

        // Save original settings to restore them later after testing
        String originalRemoteModel = orchestrator.getRemoteModel();
        String originalLocalModel = orchestrator.getLocalModel();
        String originalOllamaModel = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getModel() : null;

        boolean isEvoModel = provider.getName() != null && (
                provider.getName().toLowerCase().contains("evo") ||
                provider.getName().toLowerCase().contains("forging") ||
                provider.isLocal() ||
                "evo native".equalsIgnoreCase(LlamaService.detectInferenceEngine(provider.getName())) ||
                "llama-cpp".equalsIgnoreCase(LlamaService.detectInferenceEngine(provider.getName()))
        );

        if (isEvoModel) {
            orchestrator.setLocalModel(provider.getName());
            if (orchestrator.getOllama() != null) {
                orchestrator.getOllama().setModel(provider.getName());
            }
            // Force EVO Native Inference Engine first when testing selected EVO LLM
            if (context != null && context.getMetadata() != null) {
                context.getMetadata().put("inferenceEngine", "evo native");
            }
        }
        orchestrator.setRemoteModel(provider.getName());

        try {
            EvaluationReport report = evaluateModelWithReport(orchestrator, provider.getName(), context);

            provider.setRatingAnalyze(report.getRatingAnalyze());
            provider.setRatingChat(report.getRatingChat());
            provider.setRatingProgramming(report.getRatingProgramming());
            provider.setRating(report.getOverallRating());

            context.log("Evaluation complete. Overall score: " + report.getOverallRating() + "/10 (Analyze: " + report.getRatingAnalyze() + ", Chat: " + report.getRatingChat() + ", Prog: " + report.getRatingProgramming() + ", EVO Architecture: " + report.getRatingEvoArchitecture() + ", Eclipse/OSGi: " + report.getRatingEclipseOsgi() + ")");

            if (context.getSessionId() != null) {
                context.getKernelContext().getEventBus().publish(
                    new RuntimeEvent(RuntimeEventType.EVALUATION_COMPLETED, context.getSessionId(), "ModelEvaluationService", provider.getName())
                );
            }

        } finally {
            orchestrator.setRemoteModel(originalRemoteModel);
            if (originalLocalModel != null) {
                orchestrator.setLocalModel(originalLocalModel);
            }
            if (orchestrator.getOllama() != null && originalOllamaModel != null) {
                orchestrator.getOllama().setModel(originalOllamaModel);
            }
        }
    }

    public EvaluationReport evaluateModelWithReport(Orchestrator orchestrator, String modelName, TaskContext context) throws Exception {
        EvaluationReport report = new EvaluationReport();
        report.setModelName(modelName);

        report.setRatingAnalyze(runTest(orchestrator, PROMPT_ANALYZE, "Analysis", context));
        report.setRatingChat(runTest(orchestrator, PROMPT_CHAT, "Chat", context));
        report.setRatingProgramming(runTest(orchestrator, PROMPT_PROGRAMMING, "Programming", context));
        report.setRatingEvoArchitecture(runTest(orchestrator, PROMPT_EVO_ARCHITECTURE, "EVO Architecture", context));
        report.setRatingEclipseOsgi(runTest(orchestrator, PROMPT_ECLIPSE_OSGI, "Eclipse RCP & OSGi", context));
        report.setRatingJavaCompetence(runTest(orchestrator, PROMPT_JAVA_COMPETENCE, "Java Competence", context));

        int sum = report.getRatingAnalyze() + report.getRatingChat() + report.getRatingProgramming() +
                  report.getRatingEvoArchitecture() + report.getRatingEclipseOsgi() + report.getRatingJavaCompetence();
        report.setOverallRating((int) Math.round(sum / 6.0));

        return report;
    }

    public String compareModels(EvaluationReport v1, EvaluationReport v2) {
        if (v1 == null || v2 == null) {
            return "Comparison unavailable: missing model evaluation report.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("EVO MODEL REGRESSION EVALUATION REPORT:\n");
        sb.append("Baseline Model (v1): ").append(v1.getModelName()).append(" (Overall Score: ").append(v1.getOverallRating()).append("/10)\n");
        sb.append("Candidate Model (v2): ").append(v2.getModelName()).append(" (Overall Score: ").append(v2.getOverallRating()).append("/10)\n\n");

        boolean regressionDetected = false;

        regressionDetected |= checkCategoryRegression("Analysis", v1.getRatingAnalyze(), v2.getRatingAnalyze(), sb);
        regressionDetected |= checkCategoryRegression("Chat / Conversation", v1.getRatingChat(), v2.getRatingChat(), sb);
        regressionDetected |= checkCategoryRegression("Programming", v1.getRatingProgramming(), v2.getRatingProgramming(), sb);
        regressionDetected |= checkCategoryRegression("EVO Architecture", v1.getRatingEvoArchitecture(), v2.getRatingEvoArchitecture(), sb);
        regressionDetected |= checkCategoryRegression("Eclipse RCP & OSGi", v1.getRatingEclipseOsgi(), v2.getRatingEclipseOsgi(), sb);
        regressionDetected |= checkCategoryRegression("Java Competence", v1.getRatingJavaCompetence(), v2.getRatingJavaCompetence(), sb);

        if (regressionDetected) {
            sb.append("\nVERDICT: REGRESSION DETECTED in candidate model ").append(v2.getModelName()).append(". Recommended to adjust training composition or restore parent model.");
        } else {
            sb.append("\nVERDICT: ACCEPTED. Candidate model ").append(v2.getModelName()).append(" preserved or improved capabilities across all benchmarks.");
        }

        return sb.toString();
    }

    private boolean checkCategoryRegression(String category, int v1Score, int v2Score, StringBuilder sb) {
        int diff = v2Score - v1Score;
        String status;
        boolean reg = false;
        if (diff < -1) {
            status = "REGRESSION (" + diff + ")";
            reg = true;
        } else if (diff > 1) {
            status = "IMPROVED (+" + diff + ")";
        } else {
            status = "STABLE";
        }
        sb.append(String.format("• %-20s: v1=%d/10, v2=%d/10 -> %s\n", category, v1Score, v2Score, status));
        return reg;
    }

    private int runTest(Orchestrator orchestrator, String testPrompt, String category, TaskContext context) throws Exception {
        String response = aiService.sendRequest(orchestrator, testPrompt, context);

        String evalPrompt = String.format(EVALUATION_CRITERIA, category, response);

        // Use a generic model for evaluation to be objective, or the same model if only one available
        // For simplicity, we use the currently configured "orchestrator" model (which we set to the provider being tested)
        String evalResponse = aiService.sendRequest(orchestrator, evalPrompt, context);

        return parseRating(evalResponse);
    }

    private int parseRating(String evalResponse) {
        try {
            int start = evalResponse.indexOf("{");
            int end = evalResponse.lastIndexOf("}");
            if (start != -1 && end != -1) {
                JSONObject json = new JSONObject(evalResponse.substring(start, end + 1));
                int rating = json.optInt("rating", 5);
                return Math.max(1, Math.min(10, rating));
            }
        } catch (Exception e) {
            // Fallback: try to find a digit in the text
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b([1-9]|10)\\b").matcher(evalResponse);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        return 5; // Default middle rating if everything fails
    }
}
