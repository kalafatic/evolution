package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.agents.RealityDiscoveryAgent;
import eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.workflow.MediatedExportManager;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * End-to-end verification for Mediated Mode execution repair.
 */
public class MediatedModeExecutionRepairTest {

    private Orchestrator orchestrator;
    private File targetDir;

    @Before
    public void setUp() throws Exception {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        targetDir = Files.createTempDirectory("evo-mediated-target-").toFile();

        // Create sample target layout imitating an export/artifact directory
        File exportsOllama = new File(targetDir, "exports/ollama");
        exportsOllama.mkdirs();
        File modelfile = new File(exportsOllama, "Modelfile");
        Files.writeString(modelfile.toPath(), "FROM llama3.2:3b\nPARAMETER temperature 0.7\nSYSTEM You are an assistant.\n");

        File configJson = new File(targetDir, "config.json");
        Files.writeString(configJson.toPath(), "{\"modelName\":\"evo-test\",\"parameterCount\":14000000000,\"vocabSize\":32000}\n");

        File readme = new File(targetDir, "README.md");
        Files.writeString(readme.toPath(), "# Sample Target Model Artifacts\nThis folder contains model configurations.\n");

        SessionManager.getInstance().shutdownAll();
    }

    @After
    public void tearDown() throws Exception {
        SessionManager.getInstance().shutdownAll();
        if (targetDir != null && targetDir.exists()) {
            deleteRecursive(targetDir);
        }
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }

    @Test
    public void testSemanticExtractorResolvesAgainstProjectRoot() {
        TaskContext context = new TaskContext(orchestrator, targetDir);
        TargetScanner scanner = new TargetScanner();
        TargetSnapshot snapshot = scanner.scanToSnapshot(targetDir, TargetSnapshot.TargetType.PROJECT);

        assertNotNull(snapshot);
        assertTrue("Snapshot should discover files in targetDir", snapshot.getNodes().size() >= 3);

        List<String> paths = new ArrayList<>(snapshot.getNodes().keySet());
        SemanticExtractor extractor = new SemanticExtractor();
        extractor.extractToSnapshot(snapshot, paths, targetDir);

        for (String p : paths) {
            var node = snapshot.getNodes().get(p);
            assertNotNull(node);
            assertNotNull("Summary should be extracted for " + p, node.getSummary());
            assertFalse("Summary should not be empty for " + p, node.getSummary().isEmpty());
        }
    }

    @Test
    public void testRealityDiscoveryGracefulDegradationOnFlatArtifacts() throws Exception {
        TaskContext context = new TaskContext(orchestrator, targetDir);
        context.setSessionId("test-mediated-degrade");

        SessionContainer session = SessionManager.getInstance().getOrCreateSession("test-mediated-degrade");
        session.getCognitiveState().setCurrentCapability(CapabilityType.MEDIATED);

        RealityDiscoveryAgent agent = new RealityDiscoveryAgent(session);
        TargetRealityModel model = agent.discover("Analyze target model artifacts", context, targetDir.getAbsolutePath());

        assertNotNull("TargetRealityModel must be built", model);
        assertNotNull("Domain should be established", model.getDomain());
        assertNotNull("Purpose should be established", model.getPurpose());
        assertFalse("Subsystems should be inferred even without LLM", model.getSubsystems().isEmpty());
        assertFalse("Hotspots should be inferred", model.getHotspots().isEmpty());
    }

    @Test
    public void testMediatedExportManagerProducesValidZipPackage() throws Exception {
        TaskContext context = new TaskContext(orchestrator, targetDir);
        context.setSessionId("test-export-mgr");

        TargetRealityModel model = new TargetRealityModel();
        model.setDomain("AI_MODEL");
        model.setPurpose("Target model and configuration repository");
        model.setArchitectureSummary("Discovered model configuration and modelfile artifacts.");
        model.getMetadata().put("optimizedPrompt", "Detailed analysis prompt for subsequent Darwin evolution.");
        model.getSelectedFiles().add("config.json");
        model.getSelectedFiles().add("exports/ollama/Modelfile");
        model.getImplementationFrontierFiles().add("config.json");
        model.getImplementationFrontierFiles().add("exports/ollama/Modelfile");

        MediatedExportManager exportManager = new MediatedExportManager();
        File exportZip = exportManager.createUnifiedExport(
                model,
                MediatedExportManager.ExportProfile.FULL,
                "Analyze model repository",
                targetDir,
                targetDir.getAbsolutePath(),
                "test-export-mgr"
        );

        assertNotNull("ZIP file should be returned", exportZip);
        assertTrue("ZIP file should physically exist", exportZip.exists());
        assertTrue("ZIP file should have size > 0", exportZip.length() > 0);

        Map<String, Boolean> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(exportZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), true);
            }
        }

        assertTrue("Should contain prompt.md", entries.containsKey("prompt.md"));
        assertTrue("Should contain README.md", entries.containsKey("README.md"));
        assertTrue("Should contain architecture.html", entries.containsKey("architecture/architecture.html"));
        assertTrue("Should contain reality-model.json", entries.containsKey("architecture/reality-model.json"));
        assertTrue("Should contain implementation-package.md", entries.containsKey("implementation/implementation-package.md"));
        assertTrue("Should contain affected files in zip",
                entries.containsKey("implementation/files/config.json") || entries.containsKey("implementation/files/exports/ollama/Modelfile"));
    }
}
