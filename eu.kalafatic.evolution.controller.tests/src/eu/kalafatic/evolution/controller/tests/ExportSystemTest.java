package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.export.ExportPackageBuilder;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class ExportSystemTest {

    private ModeRouter router;
    private Orchestrator orchestrator;
    private TaskContext context;

    @Before
    public void setUp() throws Exception {
        router = new ModeRouter();
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        context = new TaskContext(orchestrator, new File("."));
    }

    @Test
    public void testMediatedAiModeMapping() {
        orchestrator.setAiMode(eu.kalafatic.evolution.model.orchestration.AiMode.MEDIATED);
        // Request with obvious coding keyword should bypass HYBRID_MANUAL_EXPORT and go to appropriate coding mode
        // IterationManager is a self-dev keyword, so it should go to SELF_DEV_MODE
        PlatformMode mode = router.route("Improve class IterationManager", orchestrator);
        assertEquals(PlatformType.SELF_DEV_MODE, mode.getType());
    }

    @Test
    public void testExportModeDetection() {
        String[] prompts = {
            "mode: export - help me fix the kernel",
            "prepare export for manual use",
            "manual self-dev package for this module",
            "export for chatgpt: improve agents"
        };

        for (String prompt : prompts) {
            PlatformMode mode = router.route(prompt, orchestrator);
            assertEquals("Failed for prompt: " + prompt, PlatformType.HYBRID_MANUAL_EXPORT, mode.getType());
        }
    }

    @Test
    public void testExportPackageBuilder() throws IOException {
        ExportPackageBuilder builder = new ExportPackageBuilder();
        String request = "Improve architecture";
        JSONObject analysis = new JSONObject().put("intent", "new").put("confidence", 0.9);
        String optimizedPrompt = "Optimized ChatGPT Prompt";
        String archSummary = "System Summary";
        Map<String, String> contextFiles = new HashMap<>();
        contextFiles.put("src/Main.java", "public class Main {}");

        File zipFile = builder.build(request, analysis, optimizedPrompt, archSummary, contextFiles, context);

        assertTrue("ZIP file should exist", zipFile.exists());
        assertTrue("ZIP file should have content", zipFile.length() > 0);

        Map<String, Boolean> foundEntries = new HashMap<>();
        foundEntries.put("optimized_prompt.md", false);
        foundEntries.put("analysis.md", false);
        foundEntries.put("architecture_summary.md", false);
        foundEntries.put("project_state.json", false);
        foundEntries.put("README.md", false);
        foundEntries.put("context/src_Main.java", false);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                foundEntries.put(entry.getName(), true);
            }
        }

        for (Map.Entry<String, Boolean> entry : foundEntries.entrySet()) {
            assertTrue("Entry missing in ZIP: " + entry.getKey(), entry.getValue());
        }

        zipFile.delete();
    }
}
