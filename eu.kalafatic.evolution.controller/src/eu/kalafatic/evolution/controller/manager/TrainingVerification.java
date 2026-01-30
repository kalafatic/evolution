package eu.kalafatic.evolution.controller.manager;

/**
 * Simple verification for the TrainingManager logic.
 */
public class TrainingVerification {

    public static void main(String[] args) {
        TrainingManager tm = TrainingManager.getInstance();

        System.out.println("--- Training Verification ---");

        String nnResult = tm.trainNeuronNetwork("TestNN", tm.getTestData().get("nn"));
        System.out.println(nnResult);

        String llmResult = tm.trainLLM("TestLLM", tm.getTestData().get("llm"));
        System.out.println(llmResult);

        String agentResult = tm.trainAgent("TestAgent", tm.getTestData().get("agent"));
        System.out.println(agentResult);

        System.out.println("--- Verification Complete ---");
    }
}
