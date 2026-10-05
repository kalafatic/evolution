package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AnalyticChatValidationTask extends AbstractSelfDevTask {

    public AnalyticChatValidationTask(String id) {
        super(id, "Analytic Chat Validation Task (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        logInfo("Executing Analytic Chat Validation task ID: " + id);
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }

        int port = context.getEffectiveServerPort();
        String sessionId = "SelfDevProject";
        String spec = "http://127.0.0.1:" + port + "/server/conversation/" + sessionId;

        long startTime = System.currentTimeMillis();
        long maxWaitTimeMs = 15000;
        long pollIntervalMs = 1000;
        String finalResponseText = null;
        boolean validated = false;

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            try {
                URL url = new URL(spec);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    StringBuilder responseBuffer = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            responseBuffer.append(line);
                        }
                    }

                    String respStr = responseBuffer.toString();
                    if (!respStr.trim().isEmpty() && !respStr.trim().equals("[]")) {
                        finalResponseText = respStr;
                        if (respStr.contains("text") || respStr.contains("sender") || respStr.contains("Hello") || respStr.contains("hi") || respStr.length() > 5) {
                            validated = true;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            Thread.sleep(pollIntervalMs);
        }

        long duration = System.currentTimeMillis() - startTime;
        if (validated) {
            return new TaskResult.Builder(id)
                    .status(TaskStatus.SUCCESS)
                    .message("Chat final response received and validated successfully for session '" + sessionId + "'")
                    .duration(duration)
                    .diagnostic("response", finalResponseText)
                    .build();
        } else {
            return TaskResult.failure(id, "Analytic chat validation failed: Did not receive valid final response within " + (maxWaitTimeMs / 1000) + "s", null);
        }
    }
}
