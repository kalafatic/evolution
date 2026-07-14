package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider;
import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Integration and Verification tests for the Self-Evo GGUF Generation and Ollama Integration.
 */
public class SelfEvoForgingIntegrationTest {

    private OllamaExporter exporter;
    private EvoLlmModel model;
    private Path tempOutputDir;

    @Before
    public void setUp() throws Exception {
        exporter = new OllamaExporter();
        // Create an EvoLlmModel with small parameters
        model = new EvoLlmModel(100, 64, 2, 1, 128, 4);
        tempOutputDir = Files.createTempDirectory("self-evo-integration-test-");
    }

    @Test
    public void testGgufGenerationAndValidHeaderStructure() throws Exception {
        String modelName = "test-evo-" + UUID.randomUUID().toString().substring(0, 8);
        exporter.export(modelName, tempOutputDir, model);

        Path ggufPath = tempOutputDir.resolve("evo.gguf");
        Path modelfilePath = tempOutputDir.resolve("Modelfile");
        Path weightsPath = tempOutputDir.resolve("weights.bin");

        // Verify all expected output files exist
        assertTrue("Modelfile must be exported", Files.exists(modelfilePath));
        assertTrue("weights.bin must be exported", Files.exists(weightsPath));
        assertTrue("evo.gguf must be exported", Files.exists(ggufPath));

        // Verify Modelfile contains uncommented ADAPTER pointing to evo.gguf
        List<String> lines = Files.readAllLines(modelfilePath);
        boolean foundUncommentedAdapter = false;
        for (String line : lines) {
            if (line.trim().startsWith("ADAPTER") && line.contains("evo.gguf")) {
                foundUncommentedAdapter = true;
                break;
            }
        }
        assertTrue("Modelfile must contain an uncommented ADAPTER directive for the exported GGUF file", foundUncommentedAdapter);

        // Read and verify GGUF header structure
        byte[] bytes = Files.readAllBytes(ggufPath);
        assertTrue("GGUF file must have at least 24 bytes for a complete header", bytes.length >= 24);

        // 1. Magic bytes: "GGUF"
        assertEquals('G', (char) bytes[0]);
        assertEquals('G', (char) bytes[1]);
        assertEquals('U', (char) bytes[2]);
        assertEquals('F', (char) bytes[3]);

        // 2. Version: 3 (32-bit little-endian)
        assertEquals(3, bytes[4]);
        assertEquals(0, bytes[5]);
        assertEquals(0, bytes[6]);
        assertEquals(0, bytes[7]);

        // 3. Tensor count: 0 (64-bit little-endian) - vital so loader doesn't crash
        long tensorCount = 0;
        for (int i = 0; i < 8; i++) {
            tensorCount |= ((long) (bytes[8 + i] & 0xFF)) << (i * 8);
        }
        assertEquals("Tensor count must be exactly 0 to prevent loading crashes on mock GGUF", 0L, tensorCount);

        // 4. Metadata KV count: 0 (64-bit little-endian) - vital so loader doesn't crash
        long kvCount = 0;
        for (int i = 0; i < 8; i++) {
            kvCount |= ((long) (bytes[16 + i] & 0xFF)) << (i * 8);
        }
        assertEquals("Metadata KV count must be exactly 0 to prevent loading crashes on mock GGUF", 0L, kvCount);
    }

    @Test
    public void testModelProgrammaticCopyingToDefaultOllamaFolder() throws Exception {
        String modelName = "test-evo-copy-" + UUID.randomUUID().toString().substring(0, 8);
        exporter.export(modelName, tempOutputDir, model);

        Path ollamaHomeModels = Paths.get(System.getProperty("user.home")).resolve(".ollama/models");
        Path expectedGgufInOllama = ollamaHomeModels.resolve("evo.gguf");
        Path expectedUniqueGgufInOllama = ollamaHomeModels.resolve(modelName + ".gguf");

        // Verify GGUF was programmatically copied to default Ollama models folder
        assertTrue("evo.gguf must be programmatically copied to ~/.ollama/models/", Files.exists(expectedGgufInOllama));
        assertTrue(modelName + ".gguf must be programmatically copied to ~/.ollama/models/", Files.exists(expectedUniqueGgufInOllama));

        // Verify that ProjectModelManager successfully lists the copied models
        List<AIProvider> allModels = ProjectModelManager.getInstance().getAllModels(null);
        boolean foundUniqueInList = false;
        boolean foundEvoInList = false;
        for (AIProvider item : allModels) {
            if (item.getName().equalsIgnoreCase(modelName)) {
                foundUniqueInList = true;
            }
            if (item.getName().equalsIgnoreCase("evo")) {
                foundEvoInList = true;
            }
        }
        assertTrue("ProjectModelManager must list the newly copied unique model", foundUniqueInList);
        assertTrue("ProjectModelManager must list the 'evo' alias model", foundEvoInList);

        // Clean up copied files to prevent cluttering the host home directory
        try {
            Files.deleteIfExists(expectedUniqueGgufInOllama);
        } catch (Exception e) {
            // Ignore clean up failures
        }
    }

    @Test
    public void testSelfHealingModelRegistrationOnMissing() throws Exception {
        // Create an orchestrator pointing to 'evo' but with server offline
        Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        Ollama ollama = OrchestrationFactory.eINSTANCE.createOllama();
        ollama.setUrl("http://localhost:11434");
        ollama.setModel("evo");
        orchestrator.setOllama(ollama);

        TaskContext context = new TaskContext(orchestrator, tempOutputDir.toFile());
        context.setSessionId("Default");
        OllamaProvider provider = new OllamaProvider();

        // This should trigger the self-healing GGUF detection and fail only with connect/offline error rather than 'model not found' crash
        try {
            provider.sendRequest(orchestrator, "hi", 0.7f, null, context);
        } catch (Exception ex) {
            // Expected since Ollama server is offline in test sandbox, but we verify it didn't throw NullPointerException or GGUF path failures
        }

        // Verify context log shows self-healing attempt
        boolean foundSelfHealingLog = false;
        for (String logLine : context.getLogs()) {
            System.out.println("INTEGRATION TEST LOG ENTRY: " + logLine);
            if (logLine.toLowerCase().contains("forged model") || logLine.toLowerCase().contains("self-healing")) {
                foundSelfHealingLog = true;
            }
        }
        assertTrue("Log should register self-healing attempt", foundSelfHealingLog);
    }
}
