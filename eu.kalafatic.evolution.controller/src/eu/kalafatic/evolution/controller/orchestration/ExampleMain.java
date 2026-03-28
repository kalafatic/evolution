package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Ollama;

/**
 * Example main to verify the orchestration system.
 * NOTE: Requires a local Ollama server running.
 */
public class ExampleMain {
    public static void main(String[] args) {
        System.out.println("Starting Evolution Orchestration Verification...");

        // 1. Setup Mock EMF Model
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
        Orchestrator orchestrator = factory.createOrchestrator();
        orchestrator.setId("verify-1");
        orchestrator.setName("HelloWorld Verification");

        Ollama ollama = factory.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("llama3.2:3b");
        orchestrator.setOllama(ollama);

        // 2. Setup Context
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "evo-verify-" + System.currentTimeMillis());
        tempDir.mkdirs();
        System.out.println("Working Directory: " + tempDir.getAbsolutePath());

        TaskContext context = new TaskContext(orchestrator, tempDir);

        // 3. Execute Orchestration
        EvolutionOrchestrator engine = new EvolutionOrchestrator();
        String request = "Create a simple HelloWorld Java project with a pom.xml and a Main.java class that prints 'Hello Evolution'.";

        try {
            String result = engine.execute(request, context);
            System.out.println("\n--- Execution Result ---");
            System.out.println(result);

            System.out.println("\n--- Execution Logs ---");
            context.getLogs().forEach(System.out::println);

            System.out.println("\n--- Generated Files ---");
            listFiles(tempDir, "");

        } catch (Exception e) {
            System.err.println("Orchestration Failed!");
            e.printStackTrace();
        } finally {
            // Clean up if desired
            // deleteDirectory(tempDir);
        }
    }

    private static void listFiles(File dir, String indent) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println(indent + (f.isDirectory() ? "[D] " : "[F] ") + f.getName());
                if (f.isDirectory()) {
                    listFiles(f, indent + "  ");
                }
            }
        }
    }
}
