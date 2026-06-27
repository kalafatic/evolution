package eu.kalafatic.evolution.controller.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manager for local project training of models (Neural Networks, LLMs, Agents).
 */
public class TrainingManager {

    private static final TrainingManager INSTANCE = new TrainingManager();
    private final Random random = new Random();

    private TrainingManager() {}

    public static TrainingManager getInstance() {
        return INSTANCE;
    }

    /**
     * Simulates training a Neural Network.
     */
    public String trainNeuronNetwork(String modelName, String dataset) {
        System.out.println("Starting Neural Network training for model: " + modelName);
        System.out.println("Using dataset: " + dataset);

        // Simple logic with test data
        double initialLoss = 0.5 + random.nextDouble();
        double finalLoss = initialLoss * 0.1;

        return String.format("Neural Network [%s] trained successfully. Initial Loss: %.4f, Final Loss: %.4f. Dataset size: %d samples.",
                modelName, initialLoss, finalLoss, dataset.split("\n").length * 100);
    }

    /**
     * Simulates training/fine-tuning an LLM.
     */
    public String trainLLM(String modelName, String promptData) {
        System.out.println("Starting LLM fine-tuning for model: " + modelName);

        // Simple logic with test data
        int tokensProcessed = promptData.length() / 4 * 1000;
        double perplexity = 15.0 - random.nextDouble() * 5;

        return String.format("LLM [%s] fine-tuned successfully. Perplexity reached: %.2f. Tokens processed: %d.",
                modelName, perplexity, tokensProcessed);
    }

    /**
     * Simulates training an Agent (Reinforcement Learning or behavioral cloning).
     */
    public String trainAgent(String agentId, String taskHistory) {
        System.out.println("Starting Agent training for: " + agentId);

        // Simple logic with test data
        double successRate = 0.6 + random.nextDouble() * 0.35;
        int iterations = taskHistory.length() * 5;

        return String.format("Agent [%s] training complete. Success rate: %.2f%%. Iterations: %d. Knowledge base updated.",
                agentId, successRate * 100, iterations);
    }

    /**
     * Provides sample test data for training.
     */
    public Map<String, String> getTestData() {
        Map<String, String> testData = new HashMap<>();
        testData.put("nn", "sample_feature_1,0.5\nsample_feature_2,0.8\nsample_feature_3,0.2");
        testData.put("llm", "Instruction: Write code. Output: public class Main {}");
        testData.put("agent", "Task: GitCommit, Result: Success\nTask: MavenBuild, Result: Failure(Retry)");
        return testData;
    }
}
