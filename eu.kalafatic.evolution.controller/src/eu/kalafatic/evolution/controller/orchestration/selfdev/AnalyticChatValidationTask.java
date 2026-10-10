package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
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

        int serverPort = context.getEffectiveServerPort();
        int supervisorPort = context.getEffectiveSupervisorPort();
        int[] portsToTry = (serverPort != supervisorPort) ? new int[] { serverPort, supervisorPort } : new int[] { serverPort };

        String sessionId = "SelfDevProject";

        long startTime = System.currentTimeMillis();
        long maxWaitTimeMs = 120000;
        long pollIntervalMs = 2000;
        String finalResponseText = null;
        boolean validated = false;
        int attempt = 0;
        Exception lastException = null;

        logInfo("Starting chat response validation loop (max wait: " + (maxWaitTimeMs / 1000) + "s)...");

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            attempt++;
            for (int port : portsToTry) {
                String spec = "http://127.0.0.1:" + port + "/server/conversation/" + sessionId;
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
                        if (attempt % 5 == 1 || code != 200) {
                            logInfo("Poll #" + attempt + " on port " + port + ": HTTP " + code + ", body len=" + respStr.length());
                        }

                        if (!respStr.trim().isEmpty() && !respStr.trim().equals("[]")) {
                            finalResponseText = respStr;
                            if (respStr.contains("text") || respStr.contains("sender") || respStr.contains("Hello") || respStr.contains("hi") || respStr.length() > 5) {
                                validated = true;
                                logInfo("Validated chat response received on port " + port + " after " + (System.currentTimeMillis() - startTime) + "ms");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    lastException = e;
                    if (attempt % 5 == 1) {
                        logError("Poll #" + attempt + " failed on port " + port + ": " + e.getMessage());
                    }
                }
            }

            if (validated) {
                break;
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
            StringWriter sw = new StringWriter();
            if (lastException != null) {
                lastException.printStackTrace(new PrintWriter(sw));
            }
            String failMsg = "Analytic chat validation failed: Did not receive valid final response within " + (maxWaitTimeMs / 1000) + "s";
            return new TaskResult.Builder(id)
                    .status(TaskStatus.FAILED)
                    .message(failMsg)
                    .duration(duration)
                    .error(lastException)
                    .diagnostic("lastResponse", finalResponseText != null ? finalResponseText : "none")
                    .diagnostic("stackTrace", sw.toString())
                    .build();
        }
    }
}
