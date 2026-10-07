package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CreateEvoProjectTask extends AbstractSelfDevTask {

    public CreateEvoProjectTask(String id) {
        super(id, "Create EVO Project (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        logInfo("Executing Create EVO Project task ID: " + id);
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }

        int port = context.getEffectiveServerPort();
        String projectName = "SelfDevProject";
        String spec = "http://127.0.0.1:" + port + "/server/project/create";

        long startTime = System.currentTimeMillis();
        try {
            URL url = new URL(spec);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body = "{\"name\":\"" + projectName + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            StringBuilder responseBuffer = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuffer.append(line).append("\n");
                }
            }

            String respText = responseBuffer.toString().trim();
            long duration = System.currentTimeMillis() - startTime;
            if (code == 200 && (respText.contains("\"status\":\"OK\"") || respText.contains("\"status\": \"OK\"") || respText.contains("SelfDevProject"))) {
                return new TaskResult.Builder(id)
                        .status(TaskStatus.SUCCESS)
                        .message("EVO Project '" + projectName + "' created successfully via HTTP endpoint on port " + port)
                        .duration(duration)
                        .diagnostic("response", respText)
                        .build();
            } else {
                return TaskResult.failure(id, "Failed to create EVO Project: HTTP " + code + " - " + respText, null);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure(id, "HTTP connection error during project creation on port " + port + ": " + e.getMessage(), e);
        }
    }
}
