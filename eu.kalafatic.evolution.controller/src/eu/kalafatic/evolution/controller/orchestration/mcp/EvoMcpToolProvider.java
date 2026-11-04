package eu.kalafatic.evolution.controller.orchestration.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ServerOperation;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FunctionalityInventoryManager;
import eu.kalafatic.evolution.controller.tools.GitTool;
import eu.kalafatic.evolution.controller.tools.ToolFactory;
import eu.kalafatic.evolution.servers.mcp.model.McpExecutionContext;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

/**
 * Authoritative bridge exposing EVO capabilities as standard MCP tools.
 * Enforces bounded, permission-aware access to:
 * - Files and Project Context (via ProjectModelManager and canonical boundary checks)
 * - Git operations (via GitTool / GitRepositoryManager)
 * - Build and Test operations (via ToolFactory / MavenTool)
 * - Darwin Capabilities (via DarwinEngineRegistry / SessionManager)
 * - Forge Functions (via ForgeSessionManager)
 * - Genome Information (via FunctionalityInventoryManager)
 * - Server Processes (via SessionManager / ServerOperation)
 */
public class EvoMcpToolProvider {

    private static final Logger LOGGER = Logger.getLogger(EvoMcpToolProvider.class.getName());

    public static void registerAuthoritativeTools(ToolRegistry registry) {
        if (registry == null) return;

        // 1. Files & Project Context
        registerProjectTools(registry);

        // 2. Git Operations
        registerGitTools(registry);

        // 3. Build & Test Operations
        registerBuildTools(registry);

        // 4. Darwin Engine & Trajectories
        registerDarwinTools(registry);

        // 5. Forge LLM Training
        registerForgeTools(registry);

        // 6. Genome & Functionalities
        registerGenomeTools(registry);

        // 7. Server Processes
        registerProcessTools(registry);

        LOGGER.info("[MCP] Authoritative EVO Capability Tools registered successfully.");
    }

    private static void registerProjectTools(ToolRegistry registry) {
        // evo_project_info
        Map<String, Object> projectSchema = new HashMap<>();
        projectSchema.put("type", "object");
        registry.register(
            new Tool("evo_project_info", "Retrieves active project metadata and root directory information", projectSchema),
            (args, context) -> {
                File root = resolveProjectRoot();
                Map<String, Object> res = new HashMap<>();
                res.put("projectRoot", root.getAbsolutePath());
                res.put("exists", root.exists());
                res.put("isDirectory", root.isDirectory());
                res.put("name", root.getName());
                return res;
            }
        );

        // evo_file_read
        Map<String, Object> fileReadSchema = new HashMap<>();
        fileReadSchema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        props.put("path", Map.of("type", "string", "description", "Relative path to file inside project root"));
        props.put("maxLines", Map.of("type", "integer", "description", "Maximum lines to read (default 200)"));
        fileReadSchema.put("properties", props);
        fileReadSchema.put("required", List.of("path"));

        registry.register(
            new Tool("evo_file_read", "Reads content from a project file safely within workspace boundary", fileReadSchema),
            (args, context) -> {
                String relPath = args.get("path").asText();
                int maxLines = args.has("maxLines") ? args.get("maxLines").asInt() : 200;

                File root = resolveProjectRoot();
                Path targetPath = root.toPath().resolve(relPath).normalize().toAbsolutePath();
                Path rootPath = root.toPath().normalize().toAbsolutePath();

                if (!targetPath.startsWith(rootPath)) {
                    throw new SecurityException("Access denied: Path traversal detected outside project boundary: " + relPath);
                }
                if (!Files.exists(targetPath)) {
                    throw new IllegalArgumentException("File not found: " + relPath);
                }
                if (Files.isDirectory(targetPath)) {
                    throw new IllegalArgumentException("Target is a directory: " + relPath);
                }

                List<String> lines = Files.readAllLines(targetPath, StandardCharsets.UTF_8);
                boolean truncated = false;
                if (lines.size() > maxLines) {
                    lines = lines.subList(0, maxLines);
                    truncated = true;
                }

                Map<String, Object> res = new HashMap<>();
                res.put("path", relPath);
                res.put("linesCount", lines.size());
                res.put("truncated", truncated);
                res.put("content", String.join("\n", lines));
                return res;
            }
        );
    }

    private static void registerGitTools(ToolRegistry registry) {
        // evo_git_status
        Map<String, Object> gitStatusSchema = new HashMap<>();
        gitStatusSchema.put("type", "object");
        registry.register(
            new Tool("evo_git_status", "Returns local Git repositories and discovery status", gitStatusSchema),
            (args, context) -> {
                List<File> repos = GitTool.getCachedLocalRepositories();
                List<String> paths = new ArrayList<>();
                for (File r : repos) {
                    paths.add(r.getAbsolutePath());
                }
                Map<String, Object> res = new HashMap<>();
                res.put("discoveredCount", repos.size());
                res.put("repositories", paths);
                return res;
            }
        );
    }

    private static void registerBuildTools(ToolRegistry registry) {
        // evo_build_test_status
        Map<String, Object> buildSchema = new HashMap<>();
        buildSchema.put("type", "object");
        registry.register(
            new Tool("evo_build_test_status", "Inspects Maven and build tooling availability and project build descriptor", buildSchema),
            (args, context) -> {
                File root = resolveProjectRoot();
                File pom = new File(root, "pom.xml");
                Map<String, Object> res = new HashMap<>();
                res.put("hasPomXml", pom.exists());
                res.put("pomPath", pom.getAbsolutePath());
                res.put("mavenToolRegistered", ToolFactory.getTool("MavenTool") != null);
                res.put("registeredTools", ToolFactory.getRegisteredTools());
                return res;
            }
        );
    }

    private static void registerDarwinTools(ToolRegistry registry) {
        // evo_darwin_status
        Map<String, Object> darwinSchema = new HashMap<>();
        darwinSchema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        props.put("sessionId", Map.of("type", "string", "description", "Optional session ID to query"));
        darwinSchema.put("properties", props);

        registry.register(
            new Tool("evo_darwin_status", "Inspects active Darwin engine execution state and trajectories", darwinSchema),
            (args, context) -> {
                String targetSid = (args != null && args.has("sessionId"))
                        ? args.get("sessionId").asText()
                        : (context != null ? context.getSessionId() : null);

                Map<String, Object> res = new HashMap<>();
                if (targetSid != null && DarwinEngineRegistry.hasEngine(targetSid)) {
                    ADarwinEngine engine = DarwinEngineRegistry.get(targetSid);
                    res.put("sessionId", targetSid);
                    res.put("hasEngine", true);
                    res.put("engineClass", engine != null ? engine.getClass().getSimpleName() : "null");
                } else {
                    res.put("sessionId", targetSid);
                    res.put("hasEngine", false);
                }
                return res;
            }
        );
    }

    private static void registerForgeTools(ToolRegistry registry) {
        // evo_forge_status
        Map<String, Object> forgeSchema = new HashMap<>();
        forgeSchema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        props.put("sessionId", Map.of("type", "string", "description", "Optional session ID to query Forge telemetry"));
        forgeSchema.put("properties", props);

        registry.register(
            new Tool("evo_forge_status", "Retrieves active Forge LLM training telemetry and metrics", forgeSchema),
            (args, context) -> {
                String sid = (args != null && args.has("sessionId"))
                        ? args.get("sessionId").asText()
                        : (context != null ? context.getSessionId() : null);

                Map<String, Object> res = new HashMap<>();
                ForgeSessionManager fsm = ForgeSessionManager.getInstance();
                res.put("activeTrainingSessionId", fsm.getActiveTrainingSessionId());

                if (sid != null) {
                    ForgeSessionManager.TrainingTelemetry telemetry = fsm.getTelemetry(sid);
                    if (telemetry != null) {
                        res.put("telemetry", telemetry);
                    }
                }
                return res;
            }
        );
    }

    private static void registerGenomeTools(ToolRegistry registry) {
        // evo_genome_inventory
        Map<String, Object> genomeSchema = new HashMap<>();
        genomeSchema.put("type", "object");
        registry.register(
            new Tool("evo_genome_inventory", "Lists core EVO capabilities and architecture inventory metadata", genomeSchema),
            (args, context) -> {
                File root = resolveProjectRoot();
                List<FunctionalityInventoryManager.FunctionalityEntry> entries =
                        FunctionalityInventoryManager.getInstance().discoverCoreFunctionalities(root);

                List<Map<String, Object>> list = new ArrayList<>();
                for (FunctionalityInventoryManager.FunctionalityEntry e : entries) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", e.id);
                    item.put("name", e.name);
                    item.put("primaryClass", e.primaryClass);
                    item.put("module", e.module);
                    item.put("status", e.status);
                    item.put("overallScore", e.calculateOverallScore());
                    list.add(item);
                }

                Map<String, Object> res = new HashMap<>();
                res.put("count", list.size());
                res.put("functionalities", list);
                return res;
            }
        );
    }

    private static void registerProcessTools(ToolRegistry registry) {
        // evo_process_list
        Map<String, Object> procListSchema = new HashMap<>();
        procListSchema.put("type", "object");
        registry.register(
            new Tool("evo_process_list", "Lists active server operations and execution statuses", procListSchema),
            (args, context) -> {
                Collection<ServerOperation> ops = SessionManager.getInstance().getAllOperations();
                List<Map<String, Object>> list = new ArrayList<>();
                for (ServerOperation op : ops) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("operationId", op.getOperationId());
                    m.put("sessionId", op.getSessionId());
                    m.put("state", op.getState().name());
                    m.put("operationType", op.getOperationType());
                    m.put("ownerPrincipal", op.getOwnerPrincipal());
                    m.put("statusMessage", op.getStatusMessage());
                    list.add(m);
                }
                return Collections.singletonMap("operations", list);
            }
        );

        // evo_process_cancel
        Map<String, Object> cancelSchema = new HashMap<>();
        cancelSchema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        props.put("operationId", Map.of("type", "string", "description", "The operation ID to cancel"));
        props.put("reason", Map.of("type", "string", "description", "Cancellation reason"));
        cancelSchema.put("properties", props);
        cancelSchema.put("required", List.of("operationId"));

        registry.register(
            new Tool("evo_process_cancel", "Cancels a running server operation", cancelSchema),
            (args, context) -> {
                String opId = args.get("operationId").asText();
                String reason = args.has("reason") ? args.get("reason").asText() : "Cancelled via MCP tool";

                ServerOperation op = SessionManager.getInstance().findOperation(opId);
                if (op == null) {
                    throw new NoSuchElementException("Operation not found: " + opId);
                }

                // Session boundary protection: non-system users can only cancel operations in their own session
                if (context != null && !"SYSTEM".equalsIgnoreCase(context.getPrincipal()) && !"admin".equalsIgnoreCase(context.getPrincipal())) {
                    if (!Objects.equals(context.getSessionId(), op.getSessionId()) && !Objects.equals(context.getPrincipal(), op.getOwnerPrincipal())) {
                        throw new SecurityException("Unauthorized cross-session cancellation attempt on operation: " + opId);
                    }
                }

                boolean cancelled = SessionManager.getInstance().cancelOperation(opId, reason);
                Map<String, Object> res = new HashMap<>();
                res.put("operationId", opId);
                res.put("cancelled", cancelled);
                res.put("state", op.getState().name());
                res.put("reason", reason);
                return res;
            }
        );
    }

    private static File resolveProjectRoot() {
        try {
            SourceDiscoveryResult result = ProjectModelManager.getInstance().getOrDiscoverWorkspace();
            if (result != null && result.getPrimaryRepository() != null && result.getPrimaryRepository().exists()) {
                return result.getPrimaryRepository();
            }
        } catch (Exception ignored) {}
        return new File(".").getAbsoluteFile();
    }
}
