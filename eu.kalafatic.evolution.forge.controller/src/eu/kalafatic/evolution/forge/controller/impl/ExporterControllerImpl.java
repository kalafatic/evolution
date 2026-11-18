package eu.kalafatic.evolution.forge.controller.impl;

import eu.kalafatic.evolution.forge.controller.api.ExporterController;
import eu.kalafatic.evolution.forge.controller.repository.ForgeRepository;
import eu.kalafatic.evolution.forge.model.ForgeModel;
import eu.kalafatic.evolution.forge.model.ForgeSession;
import java.lang.reflect.Method;

public class ExporterControllerImpl implements ExporterController {
    private final ForgeRepository repository;

    public ExporterControllerImpl(ForgeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String exportModel(String sessionId, String modelId, String snapshotId) throws Exception {
        // Honest refusal: the legacy forge export controller never performed a real
        // export (the previous implementation returned a fabricated file path).
        // Real exports run through ForgeOrchestratorImpl (EvoModelArtifact/OllamaExporter).
        throw new UnsupportedOperationException(
                "Legacy forge export controller is not implemented; use the Darwin Forge orchestrator export pipeline");
    }

    private void publishEvent(String sessionId, String typeName, Object payload) {
        try {
            Class<?> sessionManagerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.SessionManager");
            Method getInstance = sessionManagerClass.getMethod("getInstance");
            Object sm = getInstance.invoke(null);

            Method getSession = sm.getClass().getMethod("getSession", String.class);
            Object session = getSession.invoke(sm, sessionId);

            if (session != null) {
                Method getEventBus = session.getClass().getMethod("getEventBus");
                Object bus = getEventBus.invoke(session);

                if (bus != null) {
                    Class<?> eventTypeClass = Class.forName("eu.kalafatic.evolution.controller.workflow.RuntimeEventType");
                    Object type = Enum.valueOf((Class<Enum>)eventTypeClass, typeName);

                    Class<?> eventClass = Class.forName("eu.kalafatic.evolution.controller.workflow.RuntimeEvent");
                    Object event = eventClass.getConstructor(eventTypeClass, String.class, String.class, Object.class)
                                             .newInstance(type, sessionId, "ExporterController", payload);

                    Method publish = bus.getClass().getMethod("publish", eventClass);
                    publish.invoke(bus, event);
                }
            }
        } catch (Exception e) {
            // Decoupled: fail silently if controller bundle is not present
        }
    }
}
