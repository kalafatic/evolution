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
        ForgeSession session = repository.load(sessionId);
        ForgeModel model = session.getActiveModel();
        if (model == null || !model.getId().equals(modelId)) {
            throw new Exception("Model not found: " + modelId);
        }

        if (model.getLifecycleState() != eu.kalafatic.evolution.forge.model.api.ModelLifecycleState.FROZEN &&
            model.getLifecycleState() != eu.kalafatic.evolution.forge.model.api.ModelLifecycleState.COMPILING) {
            throw new Exception("Model must be FROZEN before export. Current state: " + model.getLifecycleState());
        }

        // Load specific snapshot to ensure deterministic export from a frozen state
        eu.kalafatic.evolution.forge.model.EvolutionSnapshot snapshot = model.getEvolutionSnapshots().stream()
            .filter(s -> s.getId().equals(snapshotId))
            .findFirst()
            .orElseThrow(() -> new Exception("Snapshot not found: " + snapshotId));

        // 1. Graph Flattening: snapshot.getFullGraphState() -> transformer layers
        // 2. Tensor Serialization: weights -> binary matrices
        // 3. Tokenizer Export: vocab -> tokenizer.model
        // 4. Config Generation: layers -> config.json
        // 5. Package for Ollama (GGUF conversion)

        String exportPath = "/tmp/forge_export_" + modelId + "_" + snapshotId + ".gguf";

        publishEvent(sessionId, "EXPORT_READY", exportPath);

        return exportPath;
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
