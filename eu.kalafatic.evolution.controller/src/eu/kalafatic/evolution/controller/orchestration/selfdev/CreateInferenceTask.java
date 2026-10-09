package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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

        int serverPort = context.getEffectiveServerPort();
        int supervisorPort = context.getEffectiveSupervisorPort();
        int[] portsToTry = (serverPort != supervisorPort) ? new int[] { serverPort, supervisorPort } : new int[] { serverPort };

        String body = "{\"prompt\":\"hi\",\"sessionId\":\"SelfDevProject\",\"model\":\"local\"}";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        long startTime = System.currentTimeMillis();
        Exception lastException = null;

        for (int port : portsToTry) {
            String spec = "http://127.0.0.1:" + port + "/task";
            logInfo("Submitting inference task to target endpoint: " + spec);

            for (int attempt = 1; attempt <= 3; attempt++) {
                logInfo("Attempt " + attempt + "/3 on port " + port);
                try {
                    URL url = new URL(spec);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(120000);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
                    conn.setFixedLengthStreamingMode(bodyBytes.length);
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bodyBytes);
                        os.flush();
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

                    String respStr = responseBuffer.toString().trim();
                    logInfo("HTTP Response Code: " + code + ", Body: " + respStr);
                    long duration = System.currentTimeMillis() - startTime;

                    if (code == 200) {
                        return new TaskResult.Builder(id)
                                .status(TaskStatus.SUCCESS)
                                .message("Simple 'hi' inference local task submitted successfully to EVO project session on port " + port)
                                .duration(duration)
                                .diagnostic("port", String.valueOf(port))
                                .diagnostic("response", respStr)
                                .build();
                    } else {
                        lastException = new RuntimeException("HTTP " + code + ": " + respStr);
                    }
                } catch (Exception e) {
                    lastException = e;
                    logError("Attempt " + attempt + " failed on port " + port + ": " + e.getMessage(), e);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        String errMsg = "HTTP connection error submitting inference task on port " + serverPort + ": " +
                (lastException != null ? lastException.getMessage() : "All attempts failed");

        StringWriter sw = new StringWriter();
        if (lastException != null) {
            lastException.printStackTrace(new PrintWriter(sw));
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.FAILED)
                .message(errMsg)
                .duration(duration)
                .error(lastException)
                .diagnostic("serverPort", String.valueOf(serverPort))
                .diagnostic("supervisorPort", String.valueOf(supervisorPort))
                .diagnostic("stackTrace", sw.toString())
                .build();
    }
}
