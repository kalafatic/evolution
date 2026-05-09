package eu.kalafatic.evolution.controller.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONObject;
import org.json.JSONArray;

public class WorkflowGraphManager implements RuntimeEventListener {
    private static final WorkflowGraphManager GLOBAL_INSTANCE = new WorkflowGraphManager();

    private final Map<String, SessionGraphData> sessionGraphs = new ConcurrentHashMap<>();

    private WorkflowGraphManager() {
        RuntimeEventBus.getInstance().subscribe(this);
    }

    public static WorkflowGraphManager getInstance(String sessionId) {
        // Return a wrapper or just use the global instance to manage data
        return GLOBAL_INSTANCE;
    }

    private SessionGraphData getSessionGraph(String sessionId) {
        if (sessionId == null) sessionId = "Default";
        return sessionGraphs.computeIfAbsent(sessionId, id -> new SessionGraphData(id));
    }

    public static void removeInstance(String sessionId) {
        if (sessionId != null) {
            GLOBAL_INSTANCE.sessionGraphs.remove(sessionId);
        }
    }

    @Override
    public void onEvent(RuntimeEvent event) {
        String sid = event.getSessionId();
        if (sid == null) sid = "Default";
        getSessionGraph(sid).onEvent(event);
    }

    public JSONObject getGraphJson(String sessionId) {
        return getSessionGraph(sessionId).getGraphJson();
    }

    // Helper classes to maintain state per session
    private static class SessionGraphData {
        private final String sessionId;
        private final Map<String, GraphEntity> entities = new ConcurrentHashMap<>();
        private final List<JSONObject> links = new CopyOnWriteArrayList<>();

        public SessionGraphData(String sessionId) {
            this.sessionId = sessionId;
            addEntity("user", EntityType.USER);
        }

        public void addEntity(String id, EntityType type) {
            entities.put(id, new GraphEntity(id, type));
    @Override
    public void onEvent(RuntimeEvent event) {
        if (!sessionId.equals(event.getSessionId())) return;

        switch (event.getType()) {
            case MODE_CHANGED:
                handleModeChanged(event);
                break;
            case TASK_STARTED:
                handleTaskStarted(event);
                break;
            case TASK_COMPLETED:
                handleTaskCompleted(event);
                break;
            case TASK_FAILED:
                handleTaskFailed(event);
                break;
            case SUPERVISOR_STATUS_CHANGED:
                handleSupervisorStatusChanged(event);
                break;
            case ITERATION_STARTED:
                handleIterationStarted(event);
                break;
            case EXPORT_READY:
                handleExportReady(event);
                break;
            case DEPLOYMENT_STATUS_CHANGED:
                handleDeploymentStatusChanged(event);
                break;
            case MUTATING:
                handleMutating(event);
                break;
            case STEP_WAITING:
                handleStepWaiting(event);
                break;
            case MUTATION_REVIEW:
                handleMutationReview(event);
                break;
            case STEP_RESUMED:
                handleStepResumed(event);
                break;
        }
    }

    private void handleStepWaiting(RuntimeEvent event) {
        String stepId = event.getPayload().toString();
        WorkflowStep step = WorkflowStepRegistry.getInstance().getStep(stepId);
        if (step != null) {
            GraphEntity entity = entities.get(step.getEntityId());
            if (entity == null) {
                // Try to map generic mediated/evo entities if they don't exist yet
                if ("mediated_flow".equals(step.getEntityId())) {
                    setupMediatedTemplate();
                    entity = entities.get("mediated_flow");
                } else if ("evolution_loop".equals(step.getEntityId())) {
                    setupSelfDevTemplate();
                    entity = entities.get("evolution_loop");
                }
            }
            if (entity != null) {
                entity.setStatus("WAITING_USER");
                entity.setRuntimeState("STEP: " + step.getStepType());
                entity.getMetadata().put("currentStepId", stepId);
                entity.getMetadata().put("stepDescription", step.getDescription());
                entity.getActions().clear();
                entity.getActions().add("CONTINUE");
                entity.getActions().add("RETRY");
                entity.getActions().add("SKIP");
                entity.getActions().add("INSPECT");
            }
        }
    }

    private void handleStepResumed(RuntimeEvent event) {
        String stepId = event.getPayload().toString();
        WorkflowStep step = WorkflowStepRegistry.getInstance().getStep(stepId);
        if (step != null) {
            GraphEntity entity = entities.get(step.getEntityId());
            if (entity != null) {
                entity.setStatus("RUNNING");
                entity.getActions().clear();
                // Optionally restore original actions based on entity type
                if (EntityType.SUPERVISOR.equals(entity.getType())) {
                    entity.getActions().add("STOP_SUPERVISOR");
                }
            }
        }

        public void addLink(String from, String to, String type) {
            JSONObject link = new JSONObject();
            link.put("from", from);
            link.put("to", to);
            link.put("type", type);
            links.add(link);
        }

        public void onEvent(RuntimeEvent event) {
            switch (event.getType()) {
                case MODE_CHANGED:
                    handleModeChanged(event);
                    break;
                case TASK_STARTED:
                    handleTaskStarted(event);
                    break;
                case TASK_COMPLETED:
                    handleTaskCompleted(event);
                    break;
                case TASK_FAILED:
                    handleTaskFailed(event);
                    break;
                case SUPERVISOR_STATUS_CHANGED:
                    handleSupervisorStatusChanged(event);
                    break;
                case ITERATION_STARTED:
                    handleIterationStarted(event);
                    break;
                case EXPORT_READY:
                    handleExportReady(event);
                    break;
                case DEPLOYMENT_STATUS_CHANGED:
                    handleDeploymentStatusChanged(event);
                    break;
            }
        }

        private void handleTaskFailed(RuntimeEvent event) {
            String taskId = event.getPayload().toString();
            GraphEntity entity = entities.get(taskId);
            if (entity != null) entity.setStatus("FAILED");
        }

        private void handleSupervisorStatusChanged(RuntimeEvent event) {
            GraphEntity supervisor = entities.get("supervisor");
            if (supervisor == null) {
                addEntity("supervisor", EntityType.SUPERVISOR);
                supervisor = entities.get("supervisor");
            }
            supervisor.setStatus(event.getPayload().toString());
            if ("RUNNING".equals(supervisor.getStatus())) {
                supervisor.getActions().clear();
                supervisor.getActions().add("STOP_SUPERVISOR");
            } else {
                supervisor.getActions().clear();
                supervisor.getActions().add("START_SUPERVISOR");
            }
        }

        private void handleIterationStarted(RuntimeEvent event) {
            String iterId = event.getPayload().toString();
            addEntity(iterId, EntityType.EVOLUTION_LOOP);
            entities.get(iterId).setStatus("RUNNING");
            addLink("supervisor", iterId, "iteration");
        }

        private void handleExportReady(RuntimeEvent event) {
            addEntity("export", EntityType.ZIP_EXPORT);
            GraphEntity export = entities.get("export");
            export.setStatus("READY");
            export.getMetadata().put("path", event.getPayload().toString());
            export.getActions().add("OPEN_ZIP");
        }

        private void handleDeploymentStatusChanged(RuntimeEvent event) {
            String target = event.getMetadata().getOrDefault("target", "target").toString();
            addEntity(target, EntityType.DEPLOYMENT_TARGET);
            entities.get(target).setStatus(event.getPayload().toString());
    private void handleModeChanged(RuntimeEvent event) {
        String mode = event.getPayload().toString();
        // Dynamic graph adjustment based on mode
        if ("SELF_DEV_MODE".equals(mode) || "DARWIN_MODE".equals(mode)) {
            setupSelfDevTemplate();
        } else if ("HYBRID_MANUAL_EXPORT".equals(mode)) {
            setupMediatedTemplate();
        }

        private void handleModeChanged(RuntimeEvent event) {
            String mode = event.getPayload().toString();
            if ("SELF_DEV".equals(mode) || "SELF_DEV_MODE".equals(mode)) {
                setupSelfDevTemplate();
            } else if ("DARWIN_MODE".equals(mode)) {
                setupDarwinTemplate();
            } else if ("MEDIATED".equals(mode) || "HYBRID_MANUAL_EXPORT".equals(mode)) {
                setupMediatedTemplate();
            } else if ("ASSISTED_CODING".equals(mode)) {
                setupAssistedCodingTemplate();
            } else {
                setupDefaultTemplate();
            }
        }

        private void handleTaskStarted(RuntimeEvent event) {
            String taskId = event.getPayload().toString();
            GraphEntity entity = entities.get(taskId);
            if (entity == null) {
                entity = new GraphEntity(taskId, EntityType.SELF_DEV_TASK);
                entities.put(taskId, entity);

                if (entities.containsKey("mutation_loop")) addLink("mutation_loop", taskId, "task");
                else if (entities.containsKey("evolution_loop")) addLink("evolution_loop", taskId, "task");
                else if (entities.containsKey("assisted_coding")) addLink("assisted_coding", taskId, "task");
                else if (entities.containsKey("supervisor")) addLink("supervisor", taskId, "task");
                else if (entities.containsKey("orchestrator")) addLink("orchestrator", taskId, "task");
                else if (entities.containsKey("mediated_flow")) addLink("mediated_flow", taskId, "task");
                else addLink("user", taskId, "task");
            }
            entity.setStatus("RUNNING");
        }

        private void handleTaskCompleted(RuntimeEvent event) {
            String taskId = event.getPayload().toString();
            GraphEntity entity = entities.get(taskId);
            if (entity != null) entity.setStatus("DONE");
        }

        private void setupSelfDevTemplate() {
            addEntity("supervisor", EntityType.SUPERVISOR);
            addEntity("evolution_loop", EntityType.EVOLUTION_LOOP);
            addLink("user", "supervisor", "trigger");
            addLink("supervisor", "evolution_loop", "manages");
        }
    private void setupSelfDevTemplate() {
        if (!entities.containsKey("supervisor")) {
            addEntity("supervisor", EntityType.SUPERVISOR);
            addLink("user", "supervisor", "trigger");
        }
        if (!entities.containsKey("evolution_loop")) {
            addEntity("evolution_loop", EntityType.EVOLUTION_LOOP);
            addLink("supervisor", "evolution_loop", "manages");
        }
    }

    private void setupMediatedTemplate() {
        if (!entities.containsKey("mediated_flow")) {
            addEntity("mediated_flow", EntityType.MEDIATED_FLOW);
            addLink("user", "mediated_flow", "trigger");
        }
        if (!entities.containsKey("zip_export")) {
            addEntity("zip_export", EntityType.ZIP_EXPORT);
            addLink("mediated_flow", "zip_export", "produces");
        }
    }

    private void handleMutating(RuntimeEvent event) {
        GraphEntity loop = entities.get("evolution_loop");
        if (loop != null) loop.setStatus("MUTATING");
    }

    private void handleMutationReview(RuntimeEvent event) {
        // Payload should contain variant metadata
        if (event.getPayload() instanceof JSONArray) {
            JSONArray variants = (JSONArray) event.getPayload();
            String parentId = (String) event.getMetadata().getOrDefault("parentId", "evolution_loop");

            for (int i = 0; i < variants.length(); i++) {
                JSONObject v = variants.getJSONObject(i);
                String vId = v.optString("id", "variant-" + i);
                addEntity(vId, EntityType.DARWIN_VARIANT);
                GraphEntity entity = entities.get(vId);
                entity.setStatus("PENDING");
                entity.setRuntimeState(v.optString("strategy", ""));
                addLink(parentId, vId, "mutation");
            }
        }
    }

        private void setupDarwinTemplate() {
            addEntity("darwin_engine", EntityType.SUPERVISOR);
            addEntity("mutation_loop", EntityType.EVOLUTION_LOOP);
            addLink("user", "darwin_engine", "trigger");
            addLink("darwin_engine", "mutation_loop", "manages");
        }

        private void setupDefaultTemplate() {
            addEntity("orchestrator", EntityType.SUPERVISOR);
            addLink("user", "orchestrator", "trigger");
        }

        private void setupAssistedCodingTemplate() {
            addEntity("assisted_coding", EntityType.SUPERVISOR);
            addLink("user", "assisted_coding", "trigger");
        }

        private void setupMediatedTemplate() {
            addEntity("mediated_flow", EntityType.MEDIATED_FLOW);
            addEntity("zip_export", EntityType.ZIP_EXPORT);
            addLink("user", "mediated_flow", "trigger");
            addLink("mediated_flow", "zip_export", "produces");
        }

        public JSONObject getGraphJson() {
            JSONObject json = new JSONObject();
            JSONArray nodesArr = new JSONArray();
            for (GraphEntity entity : entities.values()) nodesArr.put(entity.toJson());
            json.put("nodes", nodesArr);
            json.put("links", new JSONArray(links));
            return json;
        }
    }
}
