package eu.kalafatic.evolution.controller.tests.harness;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eu.kalafatic.evolution.controller.harness.CodingHarnessAdapter;
import eu.kalafatic.evolution.controller.harness.DStudioCodingHarnessAdapter;

/**
 * Phase 4 live cross-product test: EVO → TEIP 1.1 → DStudio Harness Platform.
 *
 * <p>Requires a real DStudio headless server at {@code http://localhost:48080} (or the
 * {@code dstudio.live.endpoint} system property). Skips silently when the server is not
 * running. This test proves the EVO connector end to end against the actual DStudio
 * runtime, including real harness execution inside DStudio.</p>
 */
public class DStudioLiveE2eTest {

    private static final String LIVE_ENDPOINT = System.getProperty(
            "dstudio.live.endpoint", "http://localhost:48080");

    @Test
    public void evoDelegatesRealTaskThroughLiveDStudio() throws Exception {
        DStudioCodingHarnessAdapter adapter = new DStudioCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of(
                "endpoint", LIVE_ENDPOINT,
                "harness", "opencode",
                "timeoutSeconds", "300"));
        if (!adapter.testConnection().success()) {
            System.out.println("[DStudioLiveE2e] DStudio not reachable at "
                    + LIVE_ENDPOINT + " — skipping live cross-product test");
            return;
        }
        Path workspace = Files.createTempDirectory("evo-dstudio-e2e");
        Files.writeString(workspace.resolve("README.md"), "# EVO-DStudio E2E\n");
        try {
            List<String> logs = new ArrayList<>();
            CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                    workspace.toFile(),
                    "Create a new file named evo-dstudio-marker.txt containing exactly "
                            + "the text: evo-dstudio-e2e-ok",
                    "Create a new file named evo-dstudio-marker.txt containing exactly "
                            + "the text: evo-dstudio-e2e-ok",
                    logs::add, () -> false, Duration.ofMinutes(5));
            assertTrue("EVO→DStudio execution failed: " + result.errorMessage()
                    + "\nlogs:\n" + String.join("\n", logs), result.success());
            Path marker = workspace.resolve("evo-dstudio-marker.txt");
            assertTrue("marker file not created by DStudio harness",
                    Files.isRegularFile(marker));
            assertTrue("marker content mismatch",
                    Files.readString(marker).contains("evo-dstudio-e2e-ok"));
            System.out.println("[DStudioLiveE2e] EVO → DStudio → harness: PASS ("
                    + result.modifiedFiles() + ")");
        } finally {
            deleteRecursively(workspace.toFile());
        }
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
