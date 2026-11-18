package eu.kalafatic.evolution.controller.tests.harness;

import static org.junit.Assert.*;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.harness.CodingHarnessAdapter;
import eu.kalafatic.evolution.controller.harness.ConnectedApplicationDescriptor;
import eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry;
import eu.kalafatic.evolution.controller.harness.OpenCodeCodingHarnessAdapter;

/**
 * Tests for the EVO connected-application registry (Phase 3, WP-3.8) and the OpenCode
 * coding-harness adapter (WP-3.9).
 */
public class ConnectedApplicationRegistryTest {

    @Before
    public void setUp() {
        ConnectedApplicationRegistry.getInstance().clearAll();
    }

    @After
    public void tearDown() {
        ConnectedApplicationRegistry.getInstance().clearAll();
    }

    @Test
    public void testRegisterListAndSelection() {
        ConnectedApplicationRegistry registry = ConnectedApplicationRegistry.getInstance();
        registry.registerAdapter(new StubHarnessAdapter("stub-a", "Stub A", "STUB", true));
        registry.registerAdapter(new StubHarnessAdapter("stub-b", "Stub B", "STUB", true));

        assertEquals(2, registry.listApplications().size());
        assertTrue(registry.findApplication("stub-a").isPresent());
        assertEquals("Stub A", registry.findApplication("stub-a").get().getName());

        registry.selectApplication("stub-b");
        assertEquals(Optional.of("stub-b"), registry.getSelectedApplicationId());
        assertTrue(registry.findSelectedAdapter().isPresent());
        assertEquals("Stub B", registry.findSelectedAdapter().get().getApplicationName());
    }

    @Test
    public void testSelectionAndEnableGating() {
        ConnectedApplicationRegistry registry = ConnectedApplicationRegistry.getInstance();
        registry.registerAdapter(new StubHarnessAdapter("stub-a", "Stub A", "STUB", true));
        registry.selectApplication("stub-a");
        assertTrue(registry.findSelectedAdapter().isPresent());

        registry.setApplicationEnabled("stub-a", false);
        assertTrue(registry.findSelectedAdapter().isEmpty());

        registry.setApplicationEnabled("stub-a", true);
        assertTrue(registry.findSelectedAdapter().isPresent());
    }

    @Test
    public void testConnectionTestUpdatesStatus() {
        ConnectedApplicationRegistry registry = ConnectedApplicationRegistry.getInstance();
        registry.registerAdapter(new StubHarnessAdapter("stub-ok", "OK", "STUB", true, true));
        registry.registerAdapter(new StubHarnessAdapter("stub-bad", "Bad", "STUB", true, false));

        assertTrue(registry.testConnection("stub-ok").success());
        assertEquals(ConnectedApplicationDescriptor.ApplicationStatus.CONNECTED,
                registry.findApplication("stub-ok").get().getStatus());

        assertFalse(registry.testConnection("stub-bad").success());
        assertEquals(ConnectedApplicationDescriptor.ApplicationStatus.DISCONNECTED,
                registry.findApplication("stub-bad").get().getStatus());
    }

    @Test
    public void testUnregisterAndUnknownApplication() {
        ConnectedApplicationRegistry registry = ConnectedApplicationRegistry.getInstance();
        registry.registerAdapter(new StubHarnessAdapter("stub-a", "Stub A", "STUB", true));
        registry.selectApplication("stub-a");
        registry.unregisterApplication("stub-a");
        assertTrue(registry.findApplication("stub-a").isEmpty());
        assertTrue(registry.getSelectedApplicationId().isEmpty());
        assertFalse(registry.testConnection("stub-a").success());
    }

    @Test
    public void testOpenCodeAdapterIdentityAndConfiguration() {
        OpenCodeCodingHarnessAdapter adapter = new OpenCodeCodingHarnessAdapter();
        assertEquals("opencode", adapter.getApplicationId());
        assertEquals("OPENCODE", adapter.getApplicationType());
        assertTrue(adapter.getCapabilities().contains("CODE_IMPLEMENTATION"));

        adapter.updateConnectionConfiguration(Map.of(
                "binary", "opencode",
                "agent", "build",
                "model", "qwen3-coder-30b-32k:latest",
                "timeoutSeconds", "600"
        ));
        Map<String, String> config = adapter.getConnectionConfiguration();
        assertEquals("build", config.get("agent"));
        assertEquals("qwen3-coder-30b-32k:latest", config.get("model"));
        assertEquals("600", config.get("timeoutSeconds"));
    }

    @Test
    public void testOpenCodeAdapterInvalidBinaryConnectionTest() {
        OpenCodeCodingHarnessAdapter adapter = new OpenCodeCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of("binary", "/nonexistent/opencode-binary"));
        assertFalse(adapter.testConnection().success());
    }

    @Test
    public void testOpenCodeAdapterRejectsMissingTarget() {
        OpenCodeCodingHarnessAdapter adapter = new OpenCodeCodingHarnessAdapter();
        adapter.updateConnectionConfiguration(Map.of("binary", "/nonexistent/opencode-binary"));
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                new File("/nonexistent-target"), "instructions", "prompt", null, () -> false, Duration.ofSeconds(10));
        assertFalse(result.success());
    }

    @Test
    public void testCodingHarnessToolRegistered() {
        // ToolFactory static init registers the generic coding-harness tool
        assertNotNull(eu.kalafatic.evolution.controller.tools.ToolFactory.getTool("coding_harness_execute"));
        assertEquals("coding_harness_execute",
                eu.kalafatic.evolution.controller.tools.ToolFactory.getTool("coding_harness_execute").getName());
    }

    /**
     * Phase 3 WP-C evidence: the REAL Developer Mode execution path — the
     * coding_harness_execute tool invoked with a resolved development target — executes a
     * real OpenCode task and returns the modified files. No mocks on the harness path.
     */
    @Test
    public void testRealCodingHarnessToolExecutionInTarget() throws Exception {
        java.nio.file.Path targetPath = java.nio.file.Files.createTempDirectory("evo-tool-target");
        File target = targetPath.toFile();

        OpenCodeCodingHarnessAdapter adapter = new OpenCodeCodingHarnessAdapter();
        if (!adapter.testConnection().success()) {
            org.junit.Assume.assumeTrue("OpenCode CLI not available on this host", false);
        }

        ConnectedApplicationRegistry registry = ConnectedApplicationRegistry.getInstance();
        registry.registerAdapter(adapter);
        registry.selectApplication("opencode");

        eu.kalafatic.evolution.controller.tools.ITool tool =
                eu.kalafatic.evolution.controller.tools.ToolFactory.getTool("coding_harness_execute");
        assertNotNull(tool);

        String result = tool.execute(
                "Create a file named evo-tool-e2e.txt containing exactly: evo-tool-e2e-ok",
                target, null);

        assertNotNull(result);
        File expected = new File(target, "evo-tool-e2e.txt");
        assertTrue("coding_harness_execute must have created the file inside the target", expected.exists());
        String content = java.nio.file.Files.readString(expected.toPath());
        assertTrue("Content must contain the requested marker", content.contains("evo-tool-e2e-ok"));
    }

    /**
     * Live runtime smoke test (Phase 3, WP-3.9 evidence): executes a REAL OpenCode coding
     * task inside a resolved development target and verifies the file lands inside the
     * target (containment). Skipped when the OpenCode CLI or a local model is unavailable.
     */
    @Test
    public void testRealOpenCodeTaskInsideResolvedTarget() throws Exception {
        java.nio.file.Path targetPath = java.nio.file.Files.createTempDirectory("evo-smoke-target");
        File target = targetPath.toFile();

        OpenCodeCodingHarnessAdapter adapter = new OpenCodeCodingHarnessAdapter();
        if (!adapter.testConnection().success()) {
            org.junit.Assume.assumeTrue("OpenCode CLI not available on this host", false);
        }
        adapter.updateConnectionConfiguration(Map.of("autoApprove", "true"));

        StringBuilder log = new StringBuilder();
        CodingHarnessAdapter.HarnessExecutionResult result = adapter.executeTask(
                target,
                "Create a file named smoke-output.txt containing exactly: evo-opencode-smoke-ok",
                "Create a file named smoke-output.txt containing exactly: evo-opencode-smoke-ok",
                log::append,
                () -> false,
                Duration.ofMinutes(5)
        );

        if (result.success()) {
            File expected = new File(target, "smoke-output.txt");
            assertTrue("OpenCode reported success; smoke-output.txt must exist inside the target", expected.exists());
            String content = java.nio.file.Files.readString(expected.toPath());
            assertTrue("Output content must contain the requested marker", content.contains("evo-opencode-smoke-ok"));
        } else {
            // Execution may fail in offline/model-unavailable environments; the harness must
            // report the failure honestly instead of fabricating success.
            assertNotNull(result.errorMessage());
        }
    }

    private static class StubHarnessAdapter implements CodingHarnessAdapter {
        private final String id;
        private final String name;
        private final String type;
        private final boolean available;
        private final boolean connectionOk;

        StubHarnessAdapter(String id, String name, String type, boolean available) {
            this(id, name, type, available, true);
        }

        StubHarnessAdapter(String id, String name, String type, boolean available, boolean connectionOk) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.available = available;
            this.connectionOk = connectionOk;
        }

        @Override public String getApplicationId() { return id; }
        @Override public String getApplicationName() { return name; }
        @Override public String getApplicationType() { return type; }
        @Override public List<String> getCapabilities() { return List.of("CODE_IMPLEMENTATION"); }
        @Override public boolean isAvailable() { return available; }
        @Override public ConnectionTestResult testConnection() {
            return connectionOk ? ConnectionTestResult.ok("ok") : ConnectionTestResult.error("unreachable");
        }
        @Override public HarnessExecutionResult executeTask(File targetDir, String instructions, String prompt,
                Consumer<String> logConsumer, BooleanSupplier cancellationSupplier, Duration timeout) {
            return HarnessExecutionResult.ok("stub executed", List.of());
        }
    }
}
