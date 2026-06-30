package eu.kalafatic.evolution.supervisor.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SupervisorCommandTool {
    private final File commandFile;
    private final File responseFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public SupervisorCommandTool(File runDir) {
        this.commandFile = new File(runDir, "command.json");
        this.responseFile = new File(runDir, "response.json");
    }

    public SupervisorResponse sendCommand(SupervisorCommand command) throws IOException {
        // Clear previous response if it exists
        if (responseFile.exists()) {
            responseFile.delete();
        }

        // Write command
        mapper.writerWithDefaultPrettyPrinter().writeValue(commandFile, command);

        // Wait for response (poll based)
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds default timeout

        while (System.currentTimeMillis() - startTime < timeout) {
            if (responseFile.exists()) {
                try {
                    SupervisorResponse response = mapper.readValue(responseFile, SupervisorResponse.class);
                    // Optionally delete command/response after consumption
                    // commandFile.delete();
                    // responseFile.delete();
                    return response;
                } catch (IOException e) {
                    // File might be partially written, wait a bit
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
            }
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        return new SupervisorResponse(false, "Timeout waiting for supervisor response");
    }
}
