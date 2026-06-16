package eu.kalafatic.evolution.controller.orchestration;

import java.util.UUID;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SessionModelState;
import eu.kalafatic.evolution.model.orchestration.SessionSnapshot;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

public class ForgeSessionManager {
    private static ForgeSessionManager instance;
    private Orchestrator orchestrator;

    private ForgeSessionManager() {}

    public static synchronized ForgeSessionManager getInstance() {
        if (instance == null) {
            instance = new ForgeSessionManager();
        }
        return instance;
    }

    public void initialize(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        if (orchestrator != null && orchestrator.getForgeSessions().isEmpty()) {
            seedDefaultSessions();
        }
    }

    private void seedDefaultSessions() {
        // 1. Simple Neuron
        ForgeSession s1 = createSession("Simple Neuron", "NEURON");
        s1.getModelState().setModelGraph("{\"nodes\":[{\"id\":\"n1\",\"name\":\"input_1\",\"type\":\"NEURON\",\"x\":100,\"y\":100},{\"id\":\"n2\",\"name\":\"bias\",\"type\":\"NEURON\",\"x\":100,\"y\":200},{\"id\":\"n3\",\"name\":\"output\",\"type\":\"NEURON\",\"x\":300,\"y\":150}],\"links\":[{\"source\":\"n1\",\"target\":\"n3\"},{\"source\":\"n2\",\"target\":\"n3\"}]}");

        // 2. Transformer Experiment
        ForgeSession s2 = createSession("Transformer Experiment", "TRANSFORMER");
        s2.getModelState().setModelGraph("{\"nodes\":[{\"id\":\"t1\",\"name\":\"embedding\",\"type\":\"LAYER\",\"x\":50,\"y\":200},{\"id\":\"t2\",\"name\":\"attn_block_1\",\"type\":\"ATTENTION\",\"x\":200,\"y\":200},{\"id\":\"t3\",\"name\":\"ffn_1\",\"type\":\"LAYER\",\"x\":350,\"y\":200},{\"id\":\"t4\",\"name\":\"head\",\"type\":\"LAYER\",\"x\":500,\"y\":200}],\"links\":[{\"source\":\"t1\",\"target\":\"t2\"},{\"source\":\"t2\",\"target\":\"t3\"},{\"source\":\"t3\",\"target\":\"t4\"}]}");

        // 3. Image Classifier (CNN)
        ForgeSession s3 = createSession("Image Classifier (CNN)", "CNN");
        s3.getModelState().setModelGraph("{\"nodes\":[{\"id\":\"c1\",\"name\":\"conv_2d\",\"type\":\"LAYER\",\"x\":100,\"y\":100},{\"id\":\"c2\",\"name\":\"max_pool\",\"type\":\"LAYER\",\"x\":100,\"y\":200},{\"id\":\"c3\",\"name\":\"flatten\",\"type\":\"LAYER\",\"x\":300,\"y\":100},{\"id\":\"c4\",\"name\":\"dense_out\",\"type\":\"LAYER\",\"x\":300,\"y\":200}],\"links\":[{\"source\":\"c1\",\"target\":\"c2\"},{\"source\":\"c2\",\"target\":\"c3\"},{\"source\":\"c3\",\"target\":\"c4\"}]}");

        // 4. Sentiment Analysis (RNN)
        ForgeSession s4 = createSession("Sentiment Analysis (RNN)", "EXPERIMENTAL");
        s4.getModelState().setModelGraph("{\"nodes\":[{\"id\":\"r1\",\"name\":\"embedding\",\"type\":\"LAYER\",\"x\":50,\"y\":150},{\"id\":\"r2\",\"name\":\"lstm_cell\",\"type\":\"CUSTOM\",\"x\":200,\"y\":150},{\"id\":\"r3\",\"name\":\"dense_head\",\"type\":\"LAYER\",\"x\":350,\"y\":150}],\"links\":[{\"source\":\"r1\",\"target\":\"r2\"},{\"source\":\"r2\",\"target\":\"r3\"}]}");
    }

    public List<ForgeSession> getSessions() {
        if (orchestrator == null) return new ArrayList<>();
        return orchestrator.getForgeSessions();
    }

    public ForgeSession createSession(String name, String modelType) {
        return createSession(name, modelType, false);
    }

    public ForgeSession createSession(String name, String modelType, boolean isDemo) {
        if (orchestrator == null) return null;

        ForgeSession session = OrchestrationFactory.eINSTANCE.createForgeSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setName(name);
        session.setSelectedModelType(modelType);
        session.setCreatedAt(System.currentTimeMillis());
        session.setLastModified(System.currentTimeMillis());
        session.setStatus(ForgeStatus.IDLE);

        SessionModelState state = OrchestrationFactory.eINSTANCE.createSessionModelState();
        state.setSessionId(session.getSessionId());
        state.setModelGraph("{}");
        state.setHyperparameters("{}");
        session.setModelState(state);

        orchestrator.getForgeSessions().add(session);

        if (isDemo) {
            applyDemoTemplate(session, modelType);
        }

        publishEvent(session, RuntimeEventType.FORGE_SESSION_CREATED, "SESSION_CREATED");

        return session;
    }

    private void applyDemoTemplate(ForgeSession session, String modelType) {
        String graph = "{}";
        switch (modelType) {
            case "NEURON":
                graph = "{\"nodes\":[{\"id\":\"n1\",\"name\":\"input_1\",\"type\":\"NEURON\",\"x\":100,\"y\":100},{\"id\":\"n2\",\"name\":\"bias\",\"type\":\"NEURON\",\"x\":100,\"y\":200},{\"id\":\"n3\",\"name\":\"output\",\"type\":\"NEURON\",\"x\":300,\"y\":150}],\"links\":[{\"source\":\"n1\",\"target\":\"n3\"},{\"source\":\"n2\",\"target\":\"n3\"}]}";
                break;
            case "MLP":
                graph = "{\"nodes\":[{\"id\":\"m1\",\"name\":\"input\",\"type\":\"LAYER\",\"x\":50,\"y\":150},{\"id\":\"m2\",\"name\":\"hidden_1\",\"type\":\"LAYER\",\"x\":200,\"y\":150},{\"id\":\"m3\",\"name\":\"output\",\"type\":\"LAYER\",\"x\":350,\"y\":150}],\"links\":[{\"source\":\"m1\",\"target\":\"m2\"},{\"source\":\"m2\",\"target\":\"m3\"}]}";
                break;
            case "CNN":
                graph = "{\"nodes\":[{\"id\":\"c1\",\"name\":\"conv_2d\",\"type\":\"LAYER\",\"x\":100,\"y\":100},{\"id\":\"c2\",\"name\":\"max_pool\",\"type\":\"LAYER\",\"x\":100,\"y\":200},{\"id\":\"c3\",\"name\":\"flatten\",\"type\":\"LAYER\",\"x\":300,\"y\":100},{\"id\":\"c4\",\"name\":\"dense_out\",\"type\":\"LAYER\",\"x\":300,\"y\":200}],\"links\":[{\"source\":\"c1\",\"target\":\"c2\"},{\"source\":\"c2\",\"target\":\"c3\"},{\"source\":\"c3\",\"target\":\"c4\"}]}";
                break;
            case "TRANSFORMER":
                graph = "{\"nodes\":[{\"id\":\"t1\",\"name\":\"embedding\",\"type\":\"LAYER\",\"x\":50,\"y\":200},{\"id\":\"t2\",\"name\":\"attn_block_1\",\"type\":\"ATTENTION\",\"x\":200,\"y\":200},{\"id\":\"t3\",\"name\":\"ffn_1\",\"type\":\"LAYER\",\"x\":350,\"y\":200},{\"id\":\"t4\",\"name\":\"head\",\"type\":\"LAYER\",\"x\":500,\"y\":200}],\"links\":[{\"source\":\"t1\",\"target\":\"t2\"},{\"source\":\"t2\",\"target\":\"t3\"},{\"source\":\"t3\",\"target\":\"t4\"}]}";
                break;
        }
        session.getModelState().setModelGraph(graph);
    }

    public boolean deleteSession(String sessionId) {
        if (orchestrator == null) return false;
        ForgeSession toRemove = findSession(sessionId);
        if (toRemove != null) {
            orchestrator.getForgeSessions().remove(toRemove);
            publishEvent(toRemove, RuntimeEventType.VIEW_UPDATED, "SESSION_DELETED");
            return true;
        }
        return false;
    }

    public ForgeSession cloneSession(String sessionId, String newName) {
        ForgeSession original = findSession(sessionId);
        if (original == null) return null;

        ForgeSession clone = createSession(newName, original.getSelectedModelType());
        clone.getModelState().setModelGraph(original.getModelState().getModelGraph());
        clone.getModelState().setHyperparameters(original.getModelState().getHyperparameters());
        clone.getModelState().setDatasetBindings(original.getModelState().getDatasetBindings());

        publishEvent(clone, RuntimeEventType.VIEW_UPDATED, "SESSION_CLONED");
        return clone;
    }

    public ForgeSession findSession(String sessionId) {
        if (orchestrator == null) return null;
        return orchestrator.getForgeSessions().stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .findFirst().orElse(null);
    }

    public void updateModel(String sessionId, String modelGraph) {
        ForgeSession session = findSession(sessionId);
        if (session != null) {
            session.getModelState().setModelGraph(modelGraph);
            session.setLastModified(System.currentTimeMillis());
            publishEvent(session, RuntimeEventType.FORGE_MODEL_CHANGED, "MODEL_CHANGED");
        }
    }

    public void updateStatus(String sessionId, ForgeStatus status) {
        ForgeSession session = findSession(sessionId);
        if (session != null) {
            session.setStatus(status);
            session.setLastModified(System.currentTimeMillis());
            RuntimeEventType type = (status == ForgeStatus.TRAINING) ? RuntimeEventType.FORGE_TRAINING_STARTED :
                                   (status == ForgeStatus.IDLE) ? RuntimeEventType.FORGE_TRAINING_STOPPED :
                                   RuntimeEventType.VIEW_UPDATED;
            publishEvent(session, type, "STATUS_CHANGED");
        }
    }

    public void updateUiState(String sessionId, String key, Object value) {
        ForgeSession session = findSession(sessionId);
        if (session != null && session.getModelState() != null) {
            String current = session.getModelState().getHyperparameters();
            JSONObject json = (current != null && !current.isEmpty() && !current.equals("{}")) ? new JSONObject(current) : new JSONObject();
            json.put(key, value);
            session.getModelState().setHyperparameters(json.toString());
            session.setLastModified(System.currentTimeMillis());
            publishEvent(session, RuntimeEventType.VIEW_UPDATED, "UI_STATE_UPDATED");
        }
    }

    public JSONObject getUiState(String sessionId) {
        ForgeSession session = findSession(sessionId);
        if (session != null && session.getModelState() != null) {
            String current = session.getModelState().getHyperparameters();
            return (current != null && !current.isEmpty() && !current.equals("{}")) ? new JSONObject(current) : new JSONObject();
        }
        return new JSONObject();
    }

    public String generateSyntheticDataset(String type) {
        return "{\"type\": \"" + type + "\", \"samples\": 1000, \"status\": \"generated\"}";
    }

    public void addExperiment(String sessionId, String modelId, String datasetId, String metrics) {
        ForgeSession session = findSession(sessionId);
        if (session != null) {
            eu.kalafatic.evolution.model.orchestration.SessionExperiment exp = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createSessionExperiment();
            exp.setId(UUID.randomUUID().toString());
            exp.setSessionId(sessionId);
            exp.setModelId(modelId);
            exp.setDatasetId(datasetId);
            exp.setMetrics(metrics);
            exp.setLogs("Experiment recorded at " + new java.util.Date());
            session.getExperiments().add(exp);
            publishEvent(session, RuntimeEventType.VIEW_UPDATED, "EXPERIMENT_ADDED");
        }
    }

    public SessionSnapshot createSnapshot(String sessionId, String genomeSnapshotId) {
        ForgeSession session = findSession(sessionId);
        if (session == null) return null;

        SessionSnapshot snapshot = OrchestrationFactory.eINSTANCE.createSessionSnapshot();
        snapshot.setId(UUID.randomUUID().toString());
        snapshot.setSessionId(sessionId);
        snapshot.setGenomeSnapshotId(genomeSnapshotId);
        snapshot.setTimestamp(System.currentTimeMillis());
        // For now, full state is just the model graph
        snapshot.setFullSerializedState(session.getModelState().getModelGraph());

        session.getSnapshots().add(snapshot);
        publishEvent(session, RuntimeEventType.VIEW_UPDATED, "SNAPSHOT_CREATED");
        return snapshot;
    }

    private void publishEvent(ForgeSession session, RuntimeEventType type, String action) {
        SessionContainer container = SessionManager.getInstance().getSession(orchestrator.getId());
        if (container != null) {
            RuntimeEventBus bus = container.getEventBus();
            if (bus != null) {
                RuntimeEvent event = new RuntimeEvent(type, orchestrator.getId(), "ForgeSessionManager", action)
                        .withEntityId(session.getSessionId())
                        .withMetadata("sessionName", session.getName());
                bus.publish(event);
            }
        }
    }
}
