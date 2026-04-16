package eu.kalafatic.evolution.controller.orchestration;

import fi.iki.elonen.NanoHTTPD;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Embedded REST server for remote control.
 */
public class EvolutionServer extends NanoHTTPD {

    public EvolutionServer(int port) {
        super(port);
    }

    public void startServer() throws IOException {
        start(30000, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            if (Method.POST.equals(method) && "/task".equals(uri)) {
                return handleCreateTask(session);
            } else if (Method.GET.equals(method) && uri.startsWith("/task/")) {
                String taskId = uri.substring(6);
                if (taskId.contains("/")) {
                    String[] parts = taskId.split("/");
                    if (parts.length == 2 && "approve".equals(parts[1])) {
                        return handleApproveTask(parts[0], session);
                    } else if (parts.length == 2 && "input".equals(parts[1])) {
                        return handleInputTask(parts[0], session);
                    }
                }
                return handleGetTask(taskId);
            } else if (Method.GET.equals(method) && "/workspace/files".equals(uri)) {
                return handleListFiles(session);
            } else if (Method.POST.equals(method) && "/workspace/applyPatch".equals(uri)) {
                return handleApplyPatch(session);
            }
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                new JSONObject().put("error", e.getMessage()).toString());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    private Response handleCreateTask(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        TaskRequest request = new TaskRequest();
        request.setPrompt(json.getString("prompt"));
        request.setProjectRoot(new File(json.optString("projectRoot", System.getProperty("user.dir"))));

        TaskResult result = OrchestratorServiceImpl.getInstance().execute(request);
        return newFixedLengthResponse(Response.Status.OK, "application/json",
            new JSONObject().put("id", result.getId()).put("status", result.getStatus().toString()).toString());
    }

    private Response handleGetTask(String id) {
        TaskResult result = OrchestratorServiceImpl.getInstance().getTaskResult(id);
        if (result == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                new JSONObject().put("error", "Task not found").toString());
        }

        JSONObject json = new JSONObject();
        json.put("id", result.getId());
        json.put("status", result.getStatus().toString());
        json.put("response", result.getResponse());
        json.put("logs", new JSONArray(result.getLogs()));
        json.put("fileChanges", new JSONArray(result.getFileChanges()));
        if (result.getError() != null) json.put("error", result.getError());
        if (result.getWaitingMessage() != null) json.put("waitingMessage", result.getWaitingMessage());

        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private Response handleApproveTask(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData != null ? postData : "{\"approved\": true}");

        OrchestratorServiceImpl.getInstance().provideApproval(id, json.optBoolean("approved", true));
        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("status", "ok").toString());
    }

    private Response handleInputTask(String id, IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        OrchestratorServiceImpl.getInstance().provideInput(id, json.getString("input"));
        return newFixedLengthResponse(Response.Status.OK, "application/json", new JSONObject().put("status", "ok").toString());
    }

    private Response handleListFiles(IHTTPSession session) {
        String rootParam = session.getParms().get("root");
        File root = new File(rootParam != null ? rootParam : System.getProperty("user.dir"));

        // Security check
        if (!isPathSafe(root)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                new JSONObject().put("error", "Access denied: Root directory is outside allowed scope.").toString());
        }

        JSONArray array = new JSONArray();
        listFiles(root, root, array);

        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString());
    }

    private void listFiles(File root, File current, JSONArray array) {
        File[] files = current.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                String relativePath = root.toURI().relativize(f.toURI()).getPath();
                array.put(new JSONObject()
                    .put("name", f.getName())
                    .put("path", relativePath)
                    .put("isDirectory", f.isDirectory()));
                if (f.isDirectory()) {
                    listFiles(root, f, array);
                }
            }
        }
    }

    private Response handleApplyPatch(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String postData = files.get("postData");
        JSONObject json = new JSONObject(postData);

        String path = json.getString("path");
        String content = json.getString("content");
        File root = new File(json.optString("projectRoot", System.getProperty("user.dir")));

        if (!isPathSafe(root)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                new JSONObject().put("error", "Access denied: Project root is outside allowed scope.").toString());
        }

        File target = new File(root, path);
        if (!isChildOf(target, root)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                new JSONObject().put("error", "Access denied: Path is outside project root.").toString());
        }

        Files.writeString(target.toPath(), content);

        return newFixedLengthResponse(Response.Status.OK, "application/json",
            new JSONObject().put("status", "success").toString());
    }

    private boolean isPathSafe(File file) {
        try {
            String path = file.getCanonicalPath();
            String userDir = new File(System.getProperty("user.dir")).getCanonicalPath();
            String tmpDir = new File(System.getProperty("java.io.tmpdir")).getCanonicalPath();

            return path.startsWith(userDir) || path.startsWith(tmpDir);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isChildOf(File child, File parent) {
        try {
            String childPath = child.getCanonicalPath();
            String parentPath = parent.getCanonicalPath();
            return childPath.startsWith(parentPath);
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) port = Integer.parseInt(args[0]);

        EvolutionServer server = new EvolutionServer(port);
        try {
            server.startServer();
            System.out.println("Evolution Server started on port " + port);
        } catch (IOException e) {
            System.err.println("Could not start server:\n" + e);
        }
    }
}
