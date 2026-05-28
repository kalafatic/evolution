package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * File-based protocol for self-development loop.
 * Coordinates communication between Worker (RCP), Supervisor, and Debug RCP.
 */
public class SelfDevProtocol {

    private final File runDir;

    public SelfDevProtocol(File projectRoot) {
        this.runDir = new File(projectRoot, "self-dev-run");
        if (!runDir.exists()) {
            runDir.mkdirs();
        }
    }

    public void updateState(int iteration, String phase, String status, String message, double progress) {
        try {
            JSONObject state = new JSONObject();
            state.put("iteration", iteration);
            state.put("phase", phase);
            state.put("status", status);
            state.put("message", message);
            state.put("progress", progress);
            Files.write(new File(runDir, "state.json").toPath(), state.toString(4).getBytes());
        } catch (IOException e) {
            System.err.println("[PROTOCOL] Failed to update state.json: " + e.getMessage());
        }
    }

    public void writeCommand(String action, int iteration) {
        try {
            JSONObject command = new JSONObject();
            command.put("action", action);
            command.put("iteration", iteration);
            Files.write(new File(runDir, "command.json").toPath(), command.toString(4).getBytes());

            // Also update state.json to reflect the command if it's a major action
            if ("RESTART".equals(action)) {
                updateState(iteration, "DONE", "RUNNING", "Requested RESTART", 1.0);
            }
        } catch (IOException e) {
            System.err.println("[PROTOCOL] Failed to write command.json: " + e.getMessage());
        }
    }

    public void writePatch(int iteration, List<String> files, String diff, String summary) {
        try {
            JSONObject patch = new JSONObject();
            patch.put("iteration", iteration);
            patch.put("files", new JSONArray(files));
            patch.put("diff", diff);
            patch.put("summary", summary);
            Files.write(new File(runDir, "patch.json").toPath(), patch.toString(4).getBytes());
        } catch (IOException e) {
            System.err.println("[PROTOCOL] Failed to write patch.json: " + e.getMessage());
        }
    }

    public JSONObject readControl() {
        File controlFile = new File(runDir, "control.json");
        if (!controlFile.exists()) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(controlFile.toPath()));
            return new JSONObject(content);
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject readState() {
        File stateFile = new File(runDir, "state.json");
        if (!stateFile.exists()) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(stateFile.toPath()));
            return new JSONObject(content);
        } catch (Exception e) {
            return null;
        }
    }

    public void clearCommand() {
        File commandFile = new File(runDir, "command.json");
        if (commandFile.exists()) {
            commandFile.delete();
        }
    }
}
