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
    private final java.util.Map<String, eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer> activeTrainers = new java.util.concurrent.ConcurrentHashMap<>();
    private final AutomaticArchitectureGenerator architectureGenerator = new AutomaticArchitectureGenerator();

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

    private Orchestrator getOrchestratorModel() {
        if (orchestrator != null) return orchestrator;
        return OrchestratorServiceImpl.getInstance().getOrchestrator();
    }

    private void seedDefaultSessions() {
        // 0. Self-Evo Forging
        ForgeSession s0 = createSession("Self-Evo Forging", "SELF_EVO");
        s0.getModelState().setModelGraph("{\"nodes\":[{\"id\":\"se1\",\"name\":\"Scanner\",\"type\":\"CUSTOM\",\"x\":100,\"y\":150},{\"id\":\"se2\",\"name\":\"Data Enhancer\",\"type\":\"ATTENTION\",\"x\":250,\"y\":150},{\"id\":\"se3\",\"name\":\"Trainer\",\"type\":\"TRANSFORMER\",\"x\":400,\"y\":150},{\"id\":\"se4\",\"name\":\"GGUF Export\",\"type\":\"LAYER\",\"x\":550,\"y\":150}],\"links\":[{\"source\":\"se1\",\"target\":\"se2\"},{\"source\":\"se2\",\"target\":\"se3\"},{\"source\":\"se3\",\"target\":\"se4\"}]}");

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
        Orchestrator orch = getOrchestratorModel();
        if (orch == null) return new ArrayList<>();
        return orch.getForgeSessions();
    }

    public ForgeSession createSession(String name, String modelType) {
        return createSession(name, modelType, false);
    }

    public ForgeSession createSession(String name, String modelType, boolean isDemo) {
        Orchestrator orch = getOrchestratorModel();
        if (orch == null) return null;

        ForgeSession session = OrchestrationFactory.eINSTANCE.createForgeSession();

        eu.kalafatic.evolution.model.orchestration.Git git = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createGit();
        git.setRepositoryUrl(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryRemote(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_LLM));
        git.setLocalPath(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryPath(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_LLM));
        git.setBranch(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getRepositoryBranch(eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.REPO_LLM));
        session.setGit(git);

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

        orch.getForgeSessions().add(session);

        if (isDemo) {
            updateUiState(session.getSessionId(), "isDemo", true);
        }

        if (isDemo || modelType != null) {
            generateArchitecture(session, modelType);
        }

        publishEvent(session, RuntimeEventType.FORGE_SESSION_CREATED, "SESSION_CREATED");

        return session;
    }

    public void generateArchitecture(ForgeSession session, String modelType) {
        String currentParams = session.getModelState().getHyperparameters();
        JSONObject uiState = (currentParams != null && !currentParams.isEmpty() && !currentParams.equals("{}")) ? new JSONObject(currentParams) : new JSONObject();

        AutomaticArchitectureGenerator.ArchitectureResult result = architectureGenerator.generate(modelType, uiState);
        session.getModelState().setModelGraph(result.graph);

        // Merge with existing uiState to preserve workflow status
        JSONObject defaults = result.defaults;
        for (Object keyObj : uiState.keySet()) {
            String key = (String) keyObj;
            if (!defaults.has(key)) defaults.put(key, uiState.get(key));
        }
        session.getModelState().setHyperparameters(defaults.toString());
        session.setSelectedModelType(modelType);
        session.setLastModified(System.currentTimeMillis());

        updateWorkflowStatus(session.getSessionId(), "ARCH_GENERATED");
        if (uiState.optBoolean("isDemo", false)) {
            updateUiState(session.getSessionId(), "isDemo", true);
        }
        publishEvent(session, RuntimeEventType.FORGE_MODEL_CHANGED, "ARCHITECTURE_GENERATED");
    }

    public boolean deleteSession(String sessionId) {
        if (sessionId == null) return false;
        Orchestrator orch = getOrchestratorModel();
        if (orch == null) return false;

        ForgeSession toRemove = findSessionById(sessionId);
        if (toRemove == null) {
            toRemove = findSession(sessionId);
        }

        if (toRemove != null) {
            String resolvedId = toRemove.getSessionId();
            synchronized (orch.getForgeSessions()) {
                orch.getForgeSessions().remove(toRemove);
            }

            // Stop active trainer for deleted session
            eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer trainer = activeTrainers.remove(resolvedId);
            if (trainer != null) {
                trainer.requestStop();
            }
            if (!resolvedId.equals(sessionId)) {
                activeTrainers.remove(sessionId);
            }

            // Clean up event buffer for deleted session
            eventBuffer.remove(resolvedId);
            if (!resolvedId.equals(sessionId)) {
                eventBuffer.remove(sessionId);
            }

            publishEvent(toRemove, RuntimeEventType.VIEW_UPDATED, "SESSION_DELETED");
            return true;
        }
        return false;
    }

    public ForgeSession cloneSession(String sessionId, String newName) {
        ForgeSession original = findSessionById(sessionId);
        if (original == null) {
            original = findSession(sessionId);
        }
        if (original == null) return null;

        ForgeSession clone = createSession(newName, original.getSelectedModelType());
        if (original.getModelState() != null && clone.getModelState() != null) {
            clone.getModelState().setModelGraph(original.getModelState().getModelGraph());
            clone.getModelState().setHyperparameters(original.getModelState().getHyperparameters());
            clone.getModelState().setDatasetBindings(original.getModelState().getDatasetBindings());
        }

        publishEvent(clone, RuntimeEventType.VIEW_UPDATED, "SESSION_CLONED");
        return clone;
    }

    public ForgeSession findSessionById(String sessionId) {
        if (sessionId == null) return null;
        Orchestrator orch = getOrchestratorModel();
        if (orch == null) return null;
        synchronized (orch.getForgeSessions()) {
            return orch.getForgeSessions().stream()
                    .filter(s -> s != null && sessionId.equals(s.getSessionId()))
                    .findFirst().orElse(null);
        }
    }

    public ForgeSession findSession(String sessionId) {
        if (sessionId == null) {
            return getFallbackSession(null);
        }
        Orchestrator orch = getOrchestratorModel();
        if (orch == null) return null;

        ForgeSession found = findSessionById(sessionId);
        if (found != null) return found;

        synchronized (orch.getForgeSessions()) {
            found = orch.getForgeSessions().stream()
                    .filter(s -> s != null && s.getName() != null && s.getName().equalsIgnoreCase(sessionId))
                    .findFirst().orElse(null);
        }
        if (found != null) return found;

        return getFallbackSession(sessionId);
    }

    private ForgeSession getFallbackSession(String requestedId) {
        Orchestrator orch = getOrchestratorModel();
        if (orch == null) return null;

        ForgeSession fallback = null;
        synchronized (orch.getForgeSessions()) {
            if (!orch.getForgeSessions().isEmpty()) {
                fallback = orch.getForgeSessions().get(0);
            }
        }

        if (fallback == null) {
            System.err.println("[FORGE_SESSION_FALLBACK] Requested session ID '" + requestedId + "' not found and no sessions exist. Creating 'Active Forge Session'.");
            fallback = createSession("Active Forge Session", "SELF_EVO");
        } else {
            System.err.println("[FORGE_SESSION_FALLBACK] Requested session ID '" + requestedId + "' not found. Falling back to default session '" + fallback.getName() + "' (" + fallback.getSessionId() + ").");
        }

        if (fallback != null) {
            publishEvent(fallback, RuntimeEventType.VIEW_UPDATED, "SESSION_LOOKUP_FALLBACK");
        }

        return fallback;
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
            Object finalValue = value;
            if (value instanceof String) {
                try {
                    if (((String) value).contains(".")) {
                        finalValue = Double.parseDouble((String) value);
                    } else {
                        finalValue = Integer.parseInt((String) value);
                    }
                } catch (NumberFormatException e) {
                    // Stay as string
                }
            }
            json.put(key, finalValue);

            session.getModelState().setHyperparameters(json.toString());
            session.setLastModified(System.currentTimeMillis());

            // If structural property changed, regenerate architecture
            java.util.List<String> structuralProps = java.util.Arrays.asList("layers", "hidden_size", "filters", "heads", "vocab_size", "context_length", "d_model", "kernel_size");
            if (structuralProps.contains(key)) {
                generateArchitecture(session, session.getSelectedModelType());
            }

            publishEvent(session, RuntimeEventType.UI_STATE_UPDATED, "UI_STATE_UPDATED");
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

    public void registerActiveTrainer(String sessionId, eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer trainer) {
        if (sessionId != null && trainer != null) {
            activeTrainers.put(sessionId, trainer);
        }
    }

    public void unregisterActiveTrainer(String sessionId) {
        if (sessionId != null) {
            activeTrainers.remove(sessionId);
        }
    }

    public boolean requestFinish(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        boolean requested = false;

        // Target trainer registered directly under sessionId
        eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer trainer = activeTrainers.get(sessionId);
        if (trainer != null) {
            trainer.requestStop();
            requested = true;
        }

        // Target trainer registered under actual resolved session UUID
        ForgeSession session = findSessionById(sessionId);
        if (session == null) {
            session = findSession(sessionId);
        }

        if (session != null) {
            String actualId = session.getSessionId();
            eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer sessionTrainer = activeTrainers.get(actualId);
            if (sessionTrainer != null && sessionTrainer != trainer) {
                sessionTrainer.requestStop();
                requested = true;
            }
            updateWorkflowStatus(actualId, "FINISHING_EXPORTING");
        }

        return requested;
    }

    public boolean requestFinishAll() {
        boolean requested = false;
        for (eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer t : activeTrainers.values()) {
            if (t != null) {
                t.requestStop();
                requested = true;
            }
        }
        return requested;
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
        ForgeSession session = findSessionById(sessionId);
        if (session == null) {
            session = findSession(sessionId);
        }
        if (session == null) return null;

        SessionSnapshot snapshot = OrchestrationFactory.eINSTANCE.createSessionSnapshot();
        snapshot.setId(UUID.randomUUID().toString());
        snapshot.setSessionId(session.getSessionId());
        snapshot.setGenomeSnapshotId(genomeSnapshotId);
        snapshot.setTimestamp(System.currentTimeMillis());

        JSONObject stateJson = new JSONObject();
        if (session.getModelState() != null) {
            stateJson.put("modelGraph", session.getModelState().getModelGraph());
            stateJson.put("hyperparameters", session.getModelState().getHyperparameters());
            stateJson.put("datasetBindings", session.getModelState().getDatasetBindings());
        }
        stateJson.put("selectedModelType", session.getSelectedModelType());
        if (session.getStatus() != null) {
            stateJson.put("status", session.getStatus().getName());
        }

        snapshot.setFullSerializedState(stateJson.toString());

        synchronized (session.getSnapshots()) {
            session.getSnapshots().add(snapshot);
        }
        publishEvent(session, RuntimeEventType.FORGE_SNAPSHOT_CREATED, "SNAPSHOT_CREATED");
        return snapshot;
    }

    public void runE2EDemo(String sessionId) {
        ForgeSession session = findSessionById(sessionId);
        if (session == null) {
            session = findSession(sessionId);
        }
        if (session == null) return;

        final String targetSessionId = session.getSessionId();

        Thread demoThread = new Thread(() -> {
            try {
                ForgeSession current = findSessionById(targetSessionId);
                if (current == null) return;

                // 1. Initializing Architecture
                String type = current.getSelectedModelType() != null ? current.getSelectedModelType() : "MLP";
                generateArchitecture(current, type);
                publishEvent(current, RuntimeEventType.FORGE_MODEL_CHANGED, "DEMO_INITIALIZED");
                Thread.sleep(1500);

                current = findSessionById(targetSessionId);
                if (current == null) return;

                // 2. Loading Data
                publishEvent(current, RuntimeEventType.FORGE_DATASET_IMPORTED, "DEMO_DATA_LOADED");
                Thread.sleep(1500);

                current = findSessionById(targetSessionId);
                if (current == null) return;

                // 3. Training
                publishEvent(current, RuntimeEventType.FORGE_TRAINING_STARTED, "DEMO_TRAINING_STARTED");
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(1000);
                    current = findSessionById(targetSessionId);
                    if (current == null) return;
                    publishEvent(current, RuntimeEventType.EVOLUTION_PROGRESS, "DEMO_TRAINING_PROGRESS_" + i);
                }

                // 4. Exporting
                String modelDir = "./forge-lab/forge-model/src/main/resources/model/demo/";
                File dir = new File(modelDir);
                if (!dir.exists()) dir.mkdirs();

                File modelFile = new File(dir, "demo_transformer.gguf");
                try (FileWriter writer = new FileWriter(modelFile)) {
                    writer.write("DUMMY OLLAMA MODEL CONTENT FOR DEMO");
                } catch (IOException e) {
                    System.err.println("[FORGE_DEMO] Failed to write demo model artifact: " + e.getMessage());
                }

                current = findSessionById(targetSessionId);
                if (current == null) return;

                publishEvent(current, RuntimeEventType.EXPORT_READY, modelFile.getAbsolutePath());
                Thread.sleep(2000);

                current = findSessionById(targetSessionId);
                if (current == null) return;

                // 5. Finalizing
                publishEvent(current, RuntimeEventType.VIEW_UPDATED, "DEMO_COMPLETED");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[FORGE_DEMO] Demo task interrupted for session: " + targetSessionId);
            }
        }, "Forge-E2E-Demo-" + targetSessionId);

        demoThread.setDaemon(true);
        demoThread.start();
    }

    public List<RuntimeEvent> getRecentEvents(String sessionId) {
        return eventBuffer.getOrDefault(sessionId, new ArrayList<>());
    }

    private void publishEvent(ForgeSession session, RuntimeEventType type, String action) {
        Orchestrator orch = getOrchestratorModel();
        if (orch == null || orch.getId() == null) return;

        RuntimeEvent event = new RuntimeEvent(type, orch.getId(), "ForgeSessionManager", action)
                .withEntityId(session.getSessionId())
                .withMetadata("sessionName", session.getName());

        JSONObject uiState = getUiState(session.getSessionId());
        if (uiState.has("forge.workflow.status")) {
            event.withMetadata("forge.workflow.status", uiState.getString("forge.workflow.status"));
        }

        List<RuntimeEvent> buffer = eventBuffer.computeIfAbsent(session.getSessionId(), k -> Collections.synchronizedList(new ArrayList<>()));
        buffer.add(event);
        if (buffer.size() > 50) buffer.remove(0);

        SessionContainer container = SessionManager.getInstance().getSession(orch.getId());
        if (container != null) {
            RuntimeEventBus bus = container.getEventBus();
            if (bus != null) {
                bus.publish(event);
            }
        }
    }
}
