package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.kalafatic.evolution.controller.orchestration.ContextBuilder;
import eu.kalafatic.evolution.controller.orchestration.ContextPackage;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Task;

public class SelfDevBootstrapController {

    private final File projectRoot;
    private final TaskContext context;
    private final ObjectMapper mapper = new ObjectMapper();

    public SelfDevBootstrapController(File projectRoot, TaskContext context) {
        this.projectRoot = projectRoot;
        this.context = context;
    }

    public void bootstrap(Task initialTask) throws Exception {
        File runDir = new File(projectRoot, "self-dev-run");
        if (!runDir.exists()) runDir.mkdirs();

        // 1. Build initial context
        ContextPackage pkg = ContextBuilder.build(initialTask, context);
        File contextFile = new File(runDir, "context.json");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(contextFile), StandardCharsets.UTF_8)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, pkg.toJson());
        }

        // 2. Define state.json
        ObjectNode state = mapper.createObjectNode();
        state.put("active", true);
        state.put("iteration", 0);
        state.put("goal", initialTask.getGoal());
        state.put("mode", "DARWIN");
        state.putPOJO("plan", new ArrayList<String>());
        state.put("contextPath", contextFile.getAbsolutePath());

        File stateFile = new File(runDir, "state.json");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(stateFile), StandardCharsets.UTF_8)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, state);
        }

        // 3. Extend bootstrap.json
        ObjectNode bootstrap = mapper.createObjectNode();
        bootstrap.put("statePath", stateFile.getAbsolutePath());

        File bootstrapFile = new File(runDir, "bootstrap.json");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(bootstrapFile), StandardCharsets.UTF_8)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, bootstrap);
        }

        // 4. Launch Supervisor (simplified, assuming supervisor.jar is available)
        launchSupervisor();
    }

    private void launchSupervisor() {
        context.log("[BOOTSTRAP] Launching Supervisor for project: " + projectRoot.getAbsolutePath());
        // In a real environment, this would use ProcessBuilder to run the supervisor JAR
        // For this task, we focus on the data handoff logic.
    }
}
