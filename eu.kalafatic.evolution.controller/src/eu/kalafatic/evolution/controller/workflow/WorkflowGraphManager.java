package eu.kalafatic.evolution.controller.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONObject;
import org.json.JSONArray;

public class WorkflowGraphManager implements RuntimeEventListener {
    private static final Map<String, WorkflowGraphManager> instances = new ConcurrentHashMap<>();
    private final String sessionId;
    private final Map<String, GraphEntity> entities = new ConcurrentHashMap<>();
    private final List<JSONObject> links = new CopyOnWriteArrayList<>();

    private WorkflowGraphManager(String sessionId) {
        this.sessionId = sessionId;
        RuntimeEventBus.getInstance().subscribe(this);
        initializeBaseGraph();
    }

    public static WorkflowGraphManager getInstance(String sessionId) {
        if (sessionId == null) sessionId = "default";
        return instances.computeIfAbsent(sessionId, WorkflowGraphManager::new);
    }

    public static void removeInstance(String sessionId) {
        WorkflowGraphManager manager = instances.remove(sessionId);
        if (manager != null) {
            RuntimeEventBus.getInstance().unsubscribe(manager);
        }
    }

    private void initializeBaseGraph() {
        addEntity("user", EntityType.USER);
    }

    public void addEntity(String id, EntityType type) {
        entities.put(id, new GraphEntity(id, type));
    }

    public void addLink(String from, String to, String type) {
        JSONObject link = new JSONObject();
        link.put("from", from);
        link.put("to", to);
        link.put("type", type);
        links.add(link);
    }

    public GraphEntity getEntity(String id) { return entities.get(id); }

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
    }

    private void handleModeChanged(RuntimeEvent event) {
        String mode = event.getPayload().toString();
        // Dynamic graph adjustment based on mode
        if ("SELF_DEV".equals(mode)) {
            setupSelfDevTemplate();
        } else if ("MEDIATED".equals(mode)) {
            setupMediatedTemplate();
        }
    }

    private void handleTaskStarted(RuntimeEvent event) {
        String taskId = event.getPayload().toString();
        GraphEntity entity = entities.get(taskId);
        if (entity == null) {
            entity = new GraphEntity(taskId, EntityType.SELF_DEV_TASK);
            entities.put(taskId, entity);
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
