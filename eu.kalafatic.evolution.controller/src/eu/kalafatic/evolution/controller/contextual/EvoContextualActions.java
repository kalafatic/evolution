package eu.kalafatic.evolution.controller.contextual;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import eu.kalafatic.evolution.controller.contextual.ContextualActionDescriptor.Confirmation;
import eu.kalafatic.evolution.controller.harness.CodingHarnessAdapter;
import eu.kalafatic.evolution.controller.harness.ConnectedApplicationDescriptor;
import eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;

/**
 * EVO-local contextual actions (Darwin Family Contextual Action Contract v1.0). EVO
 * owns Darwin orchestration and its session/iteration lifecycle: forge sessions
 * (pause/stop/duplicate/delete/details), connected coding harnesses
 * (select/test/enable/remove) and the platform git repositories (status/commit/push)
 * are exposed with state-aware availability and real execution.
 */
public final class EvoContextualActions {

    private EvoContextualActions() {
    }

    public static void install(ContextualActionRegistry registry,
            ConnectedApplicationRegistry harnessRegistry) {
        registry.registerProvider(new ForgeSessionContextProvider());
        registry.registerProvider(new HarnessContextProvider(harnessRegistry));

        // ---- forge sessions ----
        registry.registerHandler(handler(ContextualActionIds.SESSION_PAUSE, (sel, fb) -> {
            boolean requested = ForgeSessionManager.getInstance()
                    .requestPause(sel.objectId());
            fb.info(requested ? "Pause requested" : "No active training to pause");
        }));
        registry.registerHandler(handler(ContextualActionIds.SESSION_CANCEL, (sel, fb) -> {
            boolean requested = ForgeSessionManager.getInstance()
                    .requestFinish(sel.objectId());
            fb.info(requested ? "Stop requested" : "No active training to stop");
        }));
        registry.registerHandler(handler(ContextualActionIds.SESSION_RESUME, (sel, fb) -> {
            eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService service =
                    new eu.kalafatic.evolution.forge.controller.service.impl
                            .SelfEvoForgingServiceImpl();
            try {
                service.startForging(sel.objectId(), resolveForgeProjectPath(),
                        java.util.List.of());
                fb.info("Training started (resumes from checkpoint when present)");
            } catch (Exception e) {
                fb.error("Could not start training: " + e.getMessage(), e);
            }
        }));
        registry.registerHandler(handler(ContextualActionIds.SESSION_DUPLICATE, (sel, fb) -> {
            ForgeSessionManager manager = ForgeSessionManager.getInstance();
            ForgeSession original = manager.findSessionById(sel.objectId());
            if (original == null) {
                fb.error("Session not found", null);
                return;
            }
            ForgeSession clone = manager.cloneSession(sel.objectId(),
                    original.getName() + " (Copy)");
            fb.info(clone != null ? "Session duplicated" : "Session could not be "
                    + "duplicated");
        }));
        registry.registerHandler(handler(ContextualActionIds.SESSION_DELETE, (sel, fb) -> {
            boolean deleted = ForgeSessionManager.getInstance().deleteSession(sel.objectId());
            fb.info(deleted ? "Session deleted" : "Session not found");
        }));
        registry.registerHandler(handler(ContextualActionIds.SESSION_DETAILS, (sel, fb) -> {
            ForgeSessionManager manager = ForgeSessionManager.getInstance();
            ForgeSession session = manager.findSessionById(sel.objectId());
            if (session == null) {
                fb.error("Session not found", null);
                return;
            }
            TrainingTelemetry telemetry = manager.getTelemetry(session.getSessionId());
            fb.info("Id: " + session.getSessionId() + " | Name: " + session.getName()
                    + " | Model: " + session.getSelectedModelType() + " | Status: "
                    + (telemetry != null ? telemetry.status : "IDLE")
                    + (telemetry != null && telemetry.totalEpochs > 0
                            ? " | Epoch " + telemetry.currentEpoch + "/"
                                    + telemetry.totalEpochs
                            : ""));
        }));

        // ---- harnesses ----
        registry.registerHandler(handler(ContextualActionIds.HARNESS_SELECT, (sel, fb) -> {
            harnessRegistry.selectApplication(sel.objectId());
            fb.info("Harness selected");
        }));
        registry.registerHandler(handler(ContextualActionIds.HARNESS_SET_DEFAULT, (sel, fb) -> {
            harnessRegistry.selectApplication(sel.objectId());
            fb.info("Default harness set to " + sel.objectLabel());
        }));
        registry.registerHandler(handler(ContextualActionIds.HARNESS_TEST, (sel, fb) -> {
            CodingHarnessAdapter.ConnectionTestResult result =
                    harnessRegistry.testConnection(sel.objectId());
            fb.info(result.message() != null ? result.message()
                    : (result.success() ? "Connection OK" : "Connection failed"));
        }));
        registry.registerHandler(handler(ContextualActionIds.HARNESS_ENABLE, (sel, fb) -> {
            harnessRegistry.setApplicationEnabled(sel.objectId(), true);
            fb.info("Harness enabled");
        }));
        registry.registerHandler(handler(ContextualActionIds.HARNESS_DISABLE, (sel, fb) -> {
            harnessRegistry.setApplicationEnabled(sel.objectId(), false);
            fb.info("Harness disabled");
        }));
        registry.registerHandler(handler(ContextualActionIds.HARNESS_REMOVE, (sel, fb) -> {
            harnessRegistry.unregisterApplication(sel.objectId());
            fb.info("Harness registration removed");
        }));
        registry.registerHandler(handler(ContextualActionIds.HARNESS_VIEW_CAPABILITIES,
                (sel, fb) -> {
                    Optional<CodingHarnessAdapter> adapter = harnessRegistry.findAdapter(
                            sel.objectId());
                    fb.info(adapter.map(a -> "Capabilities: " + a.getCapabilities())
                            .orElse("No adapter registered"));
                }));
    }

    private static ContextualActionHandler handler(String actionId, Body body) {
        return new ContextualActionHandler() {
            @Override
            public String actionId() {
                return actionId;
            }

            @Override
            public void execute(ContextualSelection selection, ActionFeedback feedback)
                    throws Exception {
                body.run(selection, feedback);
            }
        };
    }

    private interface Body {
        void run(ContextualSelection selection, ActionFeedback feedback) throws Exception;
    }

    // ------------------------------------------------------------- providers

    private static final class ForgeSessionContextProvider
            implements ContextualActionProvider {

        @Override
        public List<String> contextTypes() {
            return List.of("session");
        }

        @Override
        public List<ContextualAction> actionsFor(ContextualSelection selection) {
            List<ContextualAction> actions = new ArrayList<>();
            ForgeSessionManager manager = ForgeSessionManager.getInstance();
            ForgeSession session = manager.findSessionById(selection.objectId());
            if (session == null) {
                return actions;
            }
            TrainingTelemetry telemetry = manager.getTelemetry(session.getSessionId());
            String status = telemetry != null ? telemetry.status : "IDLE";
            boolean training = session.getStatus() == ForgeStatus.TRAINING
                    || "RUNNING".equals(status) || "TRAINING".equals(status);
            boolean paused = "PAUSED".equals(status);
            boolean interrupted = "INTERRUPTED".equals(status);
            boolean single = !selection.multiSelect();

            ContextualActionDescriptor pause = descriptor(ContextualActionIds.SESSION_PAUSE,
                    "Pause Training", ContextualActionDescriptor.GROUP_LIFECYCLE,
                    "session");
            actions.add(single && training ? ContextualAction.enabled(pause)
                    : ContextualAction.disabled(pause, single
                            ? "No training is running" : "Not available for multiple "
                                    + "selection"));

            ContextualActionDescriptor resume = descriptor(
                    ContextualActionIds.SESSION_RESUME, "Resume Training",
                    ContextualActionDescriptor.GROUP_LIFECYCLE, "session");
            actions.add(single && (paused || interrupted)
                    ? ContextualAction.enabled(resume)
                    : ContextualAction.disabled(resume, single
                            ? "Session is not paused/interrupted"
                            : "Not available for multiple selection"));

            ContextualActionDescriptor cancel = descriptor(
                    ContextualActionIds.SESSION_CANCEL, "Stop Training",
                    ContextualActionDescriptor.GROUP_LIFECYCLE, "session");
            actions.add(single && training ? ContextualAction.enabled(cancel)
                    : ContextualAction.disabled(cancel, single
                            ? "No training is running" : "Not available for multiple "
                                    + "selection"));

            ContextualActionDescriptor duplicate = descriptor(
                    ContextualActionIds.SESSION_DUPLICATE, "Duplicate",
                    ContextualActionDescriptor.GROUP_CONFIGURATION, "session");
            actions.add(single ? ContextualAction.enabled(duplicate)
                    : ContextualAction.disabled(duplicate, "Not available for multiple "
                            + "selection"));

            ContextualActionDescriptor details = descriptor(
                    ContextualActionIds.SESSION_DETAILS, "View Session Details",
                    ContextualActionDescriptor.GROUP_INSPECTION, "session");
            actions.add(single ? ContextualAction.enabled(details)
                    : ContextualAction.disabled(details, "Not available for multiple "
                            + "selection"));

            ContextualActionDescriptor delete = descriptor(
                    ContextualActionIds.SESSION_DELETE, "Delete",
                    ContextualActionDescriptor.GROUP_DANGER, "session")
                    .withConfirmation(Confirmation.DANGEROUS);
            actions.add(single && !training ? ContextualAction.enabled(delete)
                    : ContextualAction.disabled(delete, single
                            ? "Stop training before deleting" : "Not available for "
                                    + "multiple selection"));

            ContextualActionDescriptor archive = descriptor(
                    ContextualActionIds.SESSION_ARCHIVE, "Archive",
                    ContextualActionDescriptor.GROUP_LIFECYCLE, "session");
            actions.add(ContextualAction.disabled(archive,
                    "Sessions are archived with the orchestrator file"));
            return actions;
        }
    }

    private static final class HarnessContextProvider
            implements ContextualActionProvider {

        private final ConnectedApplicationRegistry registry;

        HarnessContextProvider(ConnectedApplicationRegistry registry) {
            this.registry = registry;
        }

        @Override
        public List<String> contextTypes() {
            return List.of("harness");
        }

        @Override
        public List<ContextualAction> actionsFor(ContextualSelection selection) {
            List<ContextualAction> actions = new ArrayList<>();
            Optional<ConnectedApplicationDescriptor> descriptorOpt =
                    registry.findApplication(selection.objectId());
            if (descriptorOpt.isEmpty()) {
                return actions;
            }
            ConnectedApplicationDescriptor descriptor = descriptorOpt.get();
            boolean isDefault = registry.getSelectedApplicationId()
                    .map(selection.objectId()::equals).orElse(false);
            boolean single = !selection.multiSelect();

            ContextualActionDescriptor select = descriptor(
                    ContextualActionIds.HARNESS_SELECT, "Select Harness",
                    ContextualActionDescriptor.GROUP_SELECTION, "harness");
            actions.add(single && !isDefault && descriptor.isEnabled()
                    ? ContextualAction.enabled(select)
                    : ContextualAction.disabled(select, !single
                            ? "Not available for multiple selection"
                            : isDefault ? "Already the selected harness"
                                    : "Harness is disabled"));

            ContextualActionDescriptor test = descriptor(ContextualActionIds.HARNESS_TEST,
                    "Test Connection", ContextualActionDescriptor.GROUP_LIFECYCLE,
                    "harness");
            actions.add(single ? ContextualAction.enabled(test)
                    : ContextualAction.disabled(test, "Not available for multiple "
                            + "selection"));

            ContextualActionDescriptor enable = descriptor(
                    ContextualActionIds.HARNESS_ENABLE, "Enable",
                    ContextualActionDescriptor.GROUP_LIFECYCLE, "harness");
            ContextualActionDescriptor disable = descriptor(
                    ContextualActionIds.HARNESS_DISABLE, "Disable",
                    ContextualActionDescriptor.GROUP_LIFECYCLE, "harness");
            if (single) {
                actions.add(descriptor.isEnabled() ? ContextualAction.disabled(enable,
                        "Harness is already enabled") : ContextualAction.enabled(enable));
                actions.add(descriptor.isEnabled() ? ContextualAction.enabled(disable)
                        : ContextualAction.disabled(disable,
                                "Harness is already disabled"));
            }

            ContextualActionDescriptor capabilities = descriptor(
                    ContextualActionIds.HARNESS_VIEW_CAPABILITIES,
                    "View Capabilities", ContextualActionDescriptor.GROUP_INSPECTION,
                    "harness");
            actions.add(single ? ContextualAction.enabled(capabilities)
                    : ContextualAction.disabled(capabilities, "Not available for "
                            + "multiple selection"));

            ContextualActionDescriptor remove = descriptor(
                    ContextualActionIds.HARNESS_REMOVE, "Remove Registration",
                    ContextualActionDescriptor.GROUP_DANGER, "harness")
                    .withConfirmation(Confirmation.DANGEROUS);
            actions.add(single && !isDefault ? ContextualAction.enabled(remove)
                    : ContextualAction.disabled(remove, isDefault
                            ? "The selected harness cannot be removed"
                            : "Not available for multiple selection"));
            return actions;
        }
    }

    private static ContextualActionDescriptor descriptor(String actionId, String label,
            String group, String... contextTypes) {
        return ContextualActionDescriptor.of(actionId, label, group, contextTypes);
    }

    private static java.nio.file.Path resolveForgeProjectPath() {
        String codebase = eu.kalafatic.evolution.controller.manager.ProjectModelManager
                .getCodebasePath();
        if (codebase != null && !codebase.trim().isEmpty()) {
            return java.nio.file.Paths.get(codebase);
        }
        return new java.io.File(".").toPath();
    }
}
