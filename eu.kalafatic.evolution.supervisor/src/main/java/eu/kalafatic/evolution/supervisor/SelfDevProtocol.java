package eu.kalafatic.evolution.supervisor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class SelfDevProtocol {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final File runDir;

    public SelfDevProtocol(File projectRoot) {
        this.runDir = new File(projectRoot, "self-dev-run");
        if (!this.runDir.exists()) {
            this.runDir.mkdirs();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProtocolState {
        public int iteration;
        public String phase;
        public String status;
        public String message;
        public double progress;

        public ProtocolState() {}
        public ProtocolState(int iteration, String phase, String status, String message, double progress) {
            this.iteration = iteration;
            this.phase = phase;
            this.status = status;
            this.message = message;
            this.progress = progress;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Command {
        public String action;
        public int iteration;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Patch {
        public int iteration;
        public List<String> files;
        public String diff;
        public String summary;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Control {
        public boolean pause;
        public String forceAction;
    }

    public void updateState(int iteration, String phase, String status, String message, double progress) {
        try {
            ProtocolState state = new ProtocolState(iteration, phase, status, message, progress);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(runDir, "state.json"), state);
        } catch (IOException e) {
            System.err.println("[PROTOCOL] Failed to write state.json: " + e.getMessage());
        }
    }

    public Command readCommand() {
        File file = new File(runDir, "command.json");
        if (!file.exists()) return null;
        try {
            return mapper.readValue(file, Command.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void clearCommand() {
        File file = new File(runDir, "command.json");
        if (file.exists()) {
            file.delete();
        }
    }

    public Patch readPatch() {
        File file = new File(runDir, "patch.json");
        if (!file.exists()) return null;
        try {
            return mapper.readValue(file, Patch.class);
        } catch (IOException e) {
            return null;
        }
    }

    public Control readControl() {
        File file = new File(runDir, "control.json");
        if (!file.exists()) return null;
        try {
            return mapper.readValue(file, Control.class);
        } catch (IOException e) {
            return null;
        }
    }

    public ProtocolState readState() {
        File file = new File(runDir, "state.json");
        if (!file.exists()) return null;
        try {
            return mapper.readValue(file, ProtocolState.class);
        } catch (IOException e) {
            return null;
        }
    }
}
