package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.controller.contextual.ContextualAction;
import eu.kalafatic.evolution.controller.contextual.ContextualActionIds;
import eu.kalafatic.evolution.controller.contextual.ContextualActionRegistry;
import eu.kalafatic.evolution.controller.contextual.ContextualSelection;
import eu.kalafatic.evolution.controller.contextual.DefaultContextualActionRegistry;
import eu.kalafatic.evolution.controller.contextual.EvoContextualActions;
import eu.kalafatic.evolution.controller.harness.CodingHarnessAdapter;
import eu.kalafatic.evolution.controller.harness.ConnectedApplicationDescriptor;
import eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * EVO contextual actions (Darwin Family Contextual Action Contract v1.0): forge-session
 * and connected-harness availability and real domain execution through the single
 * registry — the same operations the RCP global menu and the EvolutionServer web
 * surface execute.
 */
public class EvoContextualActionsTest {

    private ContextualActionRegistry newRegistry(
            ConnectedApplicationRegistry harnessRegistry) {
        ContextualActionRegistry registry = new DefaultContextualActionRegistry();
        EvoContextualActions.install(registry, harnessRegistry);
        return registry;
    }

    @Test
    public void testForgeSessionActionsResolveAndDeleteExecutes() throws Exception {
        ForgeSessionManager manager = ForgeSessionManager.getInstance();
        Path stateDir = Files.createTempDirectory("evo-ctx-state");
        ForgeSessionManager.setForgeStateDirectory(stateDir);
        try {
            Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
            orchestrator.setId("evo-ctx-test");
            manager.initialize(orchestrator);
            ForgeSession session = manager.createSession("Ctx Session", "SELF_EVO");
            assertNotNull(session);

            ContextualActionRegistry registry = newRegistry(
                    ConnectedApplicationRegistry.getInstance());
            ContextualSelection selection = ContextualSelection.builder("EVO", "RCP",
                    "session").object(session.getSessionId(), session.getName()).build();

            List<ContextualAction> actions = registry.resolve(selection);
            assertFalse("forge session must expose contextual actions", actions.isEmpty());

            ContextualAction delete = registry.resolve(ContextualActionIds.SESSION_DELETE,
                    selection).orElse(null);
            assertNotNull(delete);
            assertTrue("idle session can be deleted", delete.enabled());

            assertTrue(registry.execute(ContextualActionIds.SESSION_DELETE, selection)
                    .ok());
            assertEquals(null, manager.findSessionById(session.getSessionId()));
        } finally {
            ForgeSessionManager.setForgeStateDirectory(null);
        }
    }

    @Test
    public void testHarnessActionsStateAwareAndSelectExecutes() {
        ConnectedApplicationRegistry harnessRegistry = new TestConnectedApplicationRegistry();
        harnessRegistry.registerAdapter(new TestCodingHarnessAdapter("dcode",
                "DCode", "AGENTIC", List.of("CODE_IMPLEMENTATION")));
        harnessRegistry.registerAdapter(new TestCodingHarnessAdapter("opencode",
                "OpenCode", "CLI", List.of("CODE_IMPLEMENTATION", "STREAMING")));
        harnessRegistry.selectApplication("dcode");

        ContextualActionRegistry registry = newRegistry(harnessRegistry);
        ContextualSelection dcodeSelection = ContextualSelection.builder("EVO", "RCP",
                "harness").object("dcode", "DCode").build();
        ContextualSelection opencodeSelection = ContextualSelection.builder("EVO", "RCP",
                "harness").object("opencode", "OpenCode").build();

        ContextualAction selectDefault = registry.resolve(
                ContextualActionIds.HARNESS_SELECT, dcodeSelection).orElseThrow();
        assertFalse("already-selected harness cannot be selected again",
                selectDefault.enabled());

        ContextualAction selectOther = registry.resolve(
                ContextualActionIds.HARNESS_SELECT, opencodeSelection).orElseThrow();
        assertTrue(selectOther.enabled());
        assertTrue(registry.execute(ContextualActionIds.HARNESS_SELECT,
                opencodeSelection).ok());
        assertEquals("opencode", harnessRegistry.getSelectedApplicationId().orElse(""));

        ContextualAction removeDefault = registry.resolve(
                ContextualActionIds.HARNESS_REMOVE, opencodeSelection).orElseThrow();
        assertFalse("selected harness cannot be removed", removeDefault.enabled());

        assertTrue(registry.execute(ContextualActionIds.HARNESS_TEST,
                dcodeSelection).ok());
    }

    @Test
    public void testUnknownTargetYieldsStableErrorCode() {
        ContextualActionRegistry registry = newRegistry(
                new TestConnectedApplicationRegistry());
        var result = registry.execute(ContextualActionIds.SESSION_DETAILS,
                ContextualSelection.builder("EVO", "RCP", "session")
                        .object("missing-session", "x").build());
        assertFalse(result.ok());
        assertEquals("TARGET_NOT_FOUND", result.errorCode());
    }

    private static final class TestConnectedApplicationRegistry
            extends ConnectedApplicationRegistry {

        private final java.util.Map<String, CodingHarnessAdapter> adapters =
                new java.util.LinkedHashMap<>();
        private final java.util.Map<String, ConnectedApplicationDescriptor> descriptors =
                new java.util.LinkedHashMap<>();
        private String selectedId = "";

        @Override
        public void registerAdapter(CodingHarnessAdapter adapter) {
            adapters.put(adapter.getApplicationId(), adapter);
            descriptors.put(adapter.getApplicationId(), new ConnectedApplicationDescriptor(
                    adapter.getApplicationId(), adapter.getApplicationName(),
                    adapter.getApplicationType(), adapter.getCapabilities(), true,
                    ConnectedApplicationDescriptor.ApplicationStatus.UNKNOWN, "",
                    java.util.Map.of(), java.time.Instant.now(), java.time.Instant.now()));
        }

        @Override
        public java.util.List<ConnectedApplicationDescriptor> listApplications() {
            return List.copyOf(descriptors.values());
        }

        @Override
        public java.util.Optional<ConnectedApplicationDescriptor> findApplication(
                String applicationId) {
            return java.util.Optional.ofNullable(descriptors.get(applicationId));
        }

        @Override
        public java.util.Optional<CodingHarnessAdapter> findAdapter(String applicationId) {
            return java.util.Optional.ofNullable(adapters.get(applicationId));
        }

        @Override
        public void selectApplication(String applicationId) {
            this.selectedId = applicationId;
        }

        @Override
        public java.util.Optional<String> getSelectedApplicationId() {
            return selectedId.isBlank() ? java.util.Optional.empty()
                    : java.util.Optional.of(selectedId);
        }

        @Override
        public CodingHarnessAdapter.ConnectionTestResult testConnection(
                String applicationId) {
            CodingHarnessAdapter adapter = adapters.get(applicationId);
            return adapter == null
                    ? new CodingHarnessAdapter.ConnectionTestResult(false,
                            "Unknown harness: " + applicationId)
                    : adapter.testConnection();
        }

        @Override
        public void setApplicationEnabled(String applicationId, boolean enabled) {
            ConnectedApplicationDescriptor descriptor = descriptors.get(applicationId);
            if (descriptor != null) {
                descriptor.setEnabled(enabled);
            }
        }

        @Override
        public void unregisterApplication(String applicationId) {
            adapters.remove(applicationId);
            descriptors.remove(applicationId);
        }

        @Override
        public void updateConfiguration(String applicationId,
                java.util.Map<String, String> configuration) {
        }

        @Override
        public void clearAll() {
            adapters.clear();
            descriptors.clear();
            selectedId = "";
        }
    }

    private static final class TestCodingHarnessAdapter
            implements CodingHarnessAdapter {

        private final String id;
        private final String name;
        private final String type;
        private final List<String> capabilities;

        TestCodingHarnessAdapter(String id, String name, String type,
                List<String> capabilities) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.capabilities = capabilities;
        }

        @Override
        public String getApplicationId() {
            return id;
        }

        @Override
        public String getApplicationName() {
            return name;
        }

        @Override
        public String getApplicationType() {
            return type;
        }

        @Override
        public List<String> getCapabilities() {
            return capabilities;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public CodingHarnessAdapter.ConnectionTestResult testConnection() {
            return new CodingHarnessAdapter.ConnectionTestResult(true, "OK");
        }

        @Override
        public CodingHarnessAdapter.HarnessExecutionResult executeTask(
                java.io.File projectDirectory, String instructions, String prompt,
                java.util.function.Consumer<String> logConsumer,
                java.util.function.BooleanSupplier cancellationSupplier,
                java.time.Duration timeout) {
            return new CodingHarnessAdapter.HarnessExecutionResult(false, "",
                    "not executed in test", false, false, java.util.List.of());
        }
    }
}
