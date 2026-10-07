package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CreateInferenceTask extends AbstractSelfDevTask {

    public CreateInferenceTask(String id) {
        super(id, "Create Local Inference Task (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        logInfo("Executing Create Local Inference task ID: " + id);
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }

        int port = context.getEffectiveServerPort();
        String spec = "http://127.0.0.1:" + port + "/task";

        long startTime = System.currentTimeMillis();
        try {
            URL url = new URL(spec);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body = "{\"prompt\":\"hi\",\"sessionId\":\"SelfDevProject\",\"model\":\"local\"}";
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

            long duration = System.currentTimeMillis() - startTime;
            if (code == 200) {
                return new TaskResult.Builder(id)
                        .status(TaskStatus.SUCCESS)
                        .message("Simple 'hi' inference local task submitted successfully to EVO project session")
                        .duration(duration)
                        .diagnostic("response", responseBuffer.toString())
                        .build();
            } else {
                return TaskResult.failure(id, "Failed to create local inference task: HTTP " + code + " - " + responseBuffer.toString().trim(), null);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure(id, "HTTP connection error submitting inference task on port " + port + ": " + e.getMessage(), e);
        }
    }
}
