package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SessionModelState;
import eu.kalafatic.evolution.model.orchestration.SessionSnapshot;

public class ForgeSessionManager {
    private static ForgeSessionManager instance;
    private Orchestrator orchestrator;
    private final java.util.Map<String, List<RuntimeEvent>> eventBuffer = new java.util.concurrent.ConcurrentHashMap<>();

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

        if (isDemo || modelType != null) {
            generateArchitecture(session, modelType);
        }

        publishEvent(session, RuntimeEventType.FORGE_SESSION_CREATED, "SESSION_CREATED");

        return session;
    }

    public void generateArchitecture(ForgeSession session, String modelType) {
        String currentParams = session.getModelState().getHyperparameters();
        JSONObject uiState = (currentParams != null && !currentParams.isEmpty() && !currentParams.equals("{}")) ? new JSONObject(currentParams) : new JSONObject();

        String graph = "{}";
        JSONObject defaults = new JSONObject();

        switch (modelType != null ? modelType.toUpperCase() : "") {
            case "NEURON":
                graph = "{\"nodes\":[{\"id\":\"n1\",\"name\":\"input_1\",\"type\":\"NEURON\",\"x\":100,\"y\":100},{\"id\":\"n2\",\"name\":\"bias\",\"type\":\"NEURON\",\"x\":100,\"y\":200},{\"id\":\"n3\",\"name\":\"output\",\"type\":\"NEURON\",\"x\":300,\"y\":150}],\"links\":[{\"source\":\"n1\",\"target\":\"n3\"},{\"source\":\"n2\",\"target\":\"n3\"}]}";
                defaults.put("lr", 0.01).put("optimizer", "sgd").put("epochs", 10);
                break;
            case "MLP":
                int layers = uiState.optInt("layers", 1);
                int hiddenSize = uiState.optInt("hidden_size", 64);
                graph = generateMlpGraphJson(layers, hiddenSize);
                defaults.put("layers", layers).put("hidden_size", hiddenSize).put("activation", "relu").put("lr", 0.001).put("batch", 32).put("optimizer", "adam").put("epochs", 20);
                break;
            case "CNN":
                int filters = uiState.optInt("filters", 32);
                graph = "{\"nodes\":[{\"id\":\"c1\",\"name\":\"conv_2d_" + filters + "\",\"type\":\"LAYER\",\"x\":100,\"y\":100},{\"id\":\"c2\",\"name\":\"max_pool\",\"type\":\"LAYER\",\"x\":100,\"y\":200},{\"id\":\"c3\",\"name\":\"flatten\",\"type\":\"LAYER\",\"x\":300,\"y\":100},{\"id\":\"c4\",\"name\":\"dense_out\",\"type\":\"LAYER\",\"x\":300,\"y\":200}],\"links\":[{\"source\":\"c1\",\"target\":\"c2\"},{\"source\":\"c2\",\"target\":\"c3\"},{\"source\":\"c3\",\"target\":\"c4\"}]}";
                defaults.put("filters", filters).put("kernel_size", 3).put("lr", 0.001).put("batch", 64).put("optimizer", "adam").put("epochs", 15);
                break;
            case "TRANSFORMER":
                int tLayers = uiState.optInt("layers", 6);
                int heads = uiState.optInt("heads", 8);
                graph = "{\"nodes\":[{\"id\":\"t1\",\"name\":\"embedding\",\"type\":\"LAYER\",\"x\":50,\"y\":200},{\"id\":\"t2\",\"name\":\"attn_" + heads + "heads_x" + tLayers + "\",\"type\":\"ATTENTION\",\"x\":200,\"y\":200},{\"id\":\"t3\",\"name\":\"ffn_1\",\"type\":\"LAYER\",\"x\":350,\"y\":200},{\"id\":\"t4\",\"name\":\"head\",\"type\":\"LAYER\",\"x\":500,\"y\":200}],\"links\":[{\"source\":\"t1\",\"target\":\"t2\"},{\"source\":\"t2\",\"target\":\"t3\"},{\"source\":\"t3\",\"target\":\"t4\"}]}";
                defaults.put("heads", heads).put("d_model", 512).put("layers", tLayers).put("lr", 0.0001).put("batch", 16).put("optimizer", "adam").put("epochs", 10);
                break;
            case "LLM":
                int llmLayers = uiState.optInt("layers", 12);
                graph = "{\"nodes\":[{\"id\":\"l1\",\"name\":\"tokenizer\",\"type\":\"CUSTOM\",\"x\":50,\"y\":250},{\"id\":\"l2\",\"name\":\"embeddings\",\"type\":\"LAYER\",\"x\":150,\"y\":250},{\"id\":\"l3\",\"name\":\"blocks_x" + llmLayers + "\",\"type\":\"TRANSFORMER\",\"x\":300,\"y\":250},{\"id\":\"l4\",\"name\":\"norm\",\"type\":\"LAYER\",\"x\":450,\"y\":250},{\"id\":\"l5\",\"name\":\"head\",\"type\":\"LAYER\",\"x\":600,\"y\":250}],\"links\":[{\"source\":\"l1\",\"target\":\"l2\"},{\"source\":\"l2\",\"target\":\"l3\"},{\"source\":\"l3\",\"target\":\"l4\"},{\"source\":\"l4\",\"target\":\"l5\"}]}";
                defaults.put("vocab_size", 32000).put("context_length", 2048).put("layers", llmLayers).put("heads", 12).put("lr", 0.00005).put("batch", 8).put("optimizer", "adam").put("epochs", 5);
                break;
        }
        session.getModelState().setModelGraph(graph);

        // Merge with existing uiState to preserve workflow status
        for (Object keyObj : uiState.keySet()) {
            String key = (String) keyObj;
            if (!defaults.has(key)) defaults.put(key, uiState.get(key));
        }
        session.getModelState().setHyperparameters(defaults.toString());
        session.setSelectedModelType(modelType);
        session.setLastModified(System.currentTimeMillis());

        updateWorkflowStatus(session.getSessionId(), "ARCH_GENERATED");
        publishEvent(session, RuntimeEventType.FORGE_MODEL_CHANGED, "ARCHITECTURE_GENERATED");
    }

    private String generateMlpGraphJson(int layers, int hiddenSize) {
        org.json.JSONArray nodes = new org.json.JSONArray();
        org.json.JSONArray links = new org.json.JSONArray();

        JSONObject inputNode = new JSONObject().put("id", "m_in").put("name", "input").put("type", "LAYER").put("x", 50).put("y", 150);
        nodes.put(inputNode);

        String lastId = "m_in";
        for (int i = 0; i < layers; i++) {
            String currentId = "m_h" + i;
            nodes.put(new JSONObject().put("id", currentId).put("name", "hidden_" + (i+1) + " (" + hiddenSize + ")").put("type", "LAYER").put("x", 200 + i * 150).put("y", 150));
            links.put(new JSONObject().put("source", lastId).put("target", currentId));
            lastId = currentId;
        }

        String outputId = "m_out";
        nodes.put(new JSONObject().put("id", outputId).put("name", "output").put("type", "LAYER").put("x", 200 + layers * 150).put("y", 150));
        links.put(new JSONObject().put("source", lastId).put("target", outputId));

        return new JSONObject().put("nodes", nodes).put("links", links).toString();
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

            if (status == ForgeStatus.TRAINING) {
                updateWorkflowStatus(sessionId, "TRAINING_ACTIVE");
            }

            publishEvent(session, type, "STATUS_CHANGED");
        }
    }

    public void updateWorkflowStatus(String sessionId, String status) {
        updateUiState(sessionId, "forge.workflow.status", status);
    }

    public void updateUiState(String sessionId, String key, Object value) {
        ForgeSession session = findSession(sessionId);
        if (session != null && session.getModelState() != null) {
            String current = session.getModelState().getHyperparameters();
            JSONObject json = (current != null && !current.isEmpty() && !current.equals("{}")) ? new JSONObject(current) : new JSONObject();

            // Try to parse number if possible for hyperparameters
            if (value instanceof String) {
                try {
                    if (((String) value).contains(".")) {
                        json.put(key, Double.parseDouble((String) value));
                    } else {
                        json.put(key, Integer.parseInt((String) value));
                    }
                } catch (NumberFormatException e) {
                    json.put(key, value);
                }
            } else {
                json.put(key, value);
            }

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
        publishEvent(session, RuntimeEventType.FORGE_SNAPSHOT_CREATED, "SNAPSHOT_CREATED");
        return snapshot;
    }

    public void runE2EDemo(String sessionId) {
        ForgeSession session = findSession(sessionId);
        if (session == null) return;

        new Thread(() -> {
            try {
                // 1. Initializing Architecture
                generateArchitecture(session, "MLP");
                publishEvent(session, RuntimeEventType.FORGE_MODEL_CHANGED, "DEMO_INITIALIZED");
                Thread.sleep(1500);

                // 2. Loading Data
                publishEvent(session, RuntimeEventType.FORGE_DATASET_IMPORTED, "DEMO_DATA_LOADED");
                Thread.sleep(1500);

                // 3. Training
                publishEvent(session, RuntimeEventType.FORGE_TRAINING_STARTED, "DEMO_TRAINING_STARTED");
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(1000);
                    publishEvent(session, RuntimeEventType.EVOLUTION_PROGRESS, "DEMO_TRAINING_PROGRESS_" + i);
                }

                // 4. Exporting
                String modelDir = "./forge-lab/forge-model/src/main/resources/model/demo/";
                File dir = new File(modelDir);
                if (!dir.exists()) dir.mkdirs();

                File modelFile = new File(dir, "demo_transformer.gguf");
                try (FileWriter writer = new FileWriter(modelFile)) {
                    writer.write("DUMMY OLLAMA MODEL CONTENT FOR DEMO");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                publishEvent(session, RuntimeEventType.EXPORT_READY, modelFile.getAbsolutePath());
                Thread.sleep(2000);

                // 5. Finalizing
                publishEvent(session, RuntimeEventType.VIEW_UPDATED, "DEMO_COMPLETED");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public List<RuntimeEvent> getRecentEvents(String sessionId) {
        return eventBuffer.getOrDefault(sessionId, new ArrayList<>());
    }

    private void publishEvent(ForgeSession session, RuntimeEventType type, String action) {
        RuntimeEvent event = new RuntimeEvent(type, orchestrator.getId(), "ForgeSessionManager", action)
                .withEntityId(session.getSessionId())
                .withMetadata("sessionName", session.getName());

        JSONObject uiState = getUiState(session.getSessionId());
        if (uiState.has("forge.workflow.status")) {
            event.withMetadata("forge.workflow.status", uiState.getString("forge.workflow.status"));
        }

        List<RuntimeEvent> buffer = eventBuffer.computeIfAbsent(session.getSessionId(), k -> Collections.synchronizedList(new ArrayList<>()));
        buffer.add(event);
        if (buffer.size() > 50) buffer.remove(0);

        SessionContainer container = SessionManager.getInstance().getSession(orchestrator.getId());
        if (container != null) {
            RuntimeEventBus bus = container.getEventBus();
            if (bus != null) {
                bus.publish(event);
            }
        }
    }
}
