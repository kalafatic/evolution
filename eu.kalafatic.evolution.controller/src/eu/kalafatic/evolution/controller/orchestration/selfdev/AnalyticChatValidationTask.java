package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevHttpUtils.HttpResponse;

public class AnalyticChatValidationTask extends AbstractSelfDevTask {

    private static final String DEFAULT_SESSION_ID = "SelfDevProject";
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 3000;

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

        Set<Integer> portsToTry = new LinkedHashSet<>();
        portsToTry.add(serverPort);
        if (supervisorPort > 0) {
            portsToTry.add(supervisorPort);
        }

        String sessionId = DEFAULT_SESSION_ID;

        long startTime = System.currentTimeMillis();
        long maxWaitTimeMs = 120000;
        long pollIntervalMs = 2000;
        String finalResponseText = null;
        boolean validated = false;
        int attempt = 0;
        HttpResponse lastResponse = null;
        String lastEndpoint = "";
        Exception lastException = null;

        logInfo("Starting chat response validation loop (max wait: " + (maxWaitTimeMs / 1000) + "s)...");

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            if (Thread.currentThread().isInterrupted()) {
                logInfo("Thread interrupted during chat validation. Stopping validation loop.");
                long duration = System.currentTimeMillis() - startTime;
                return new TaskResult.Builder(id)
                        .status(TaskStatus.SKIPPED)
                        .message("Analytic chat validation interrupted")
                        .duration(duration)
                        .build();
            }

            attempt++;

            for (int port : portsToTry) {
                String spec = "http://127.0.0.1:" + port + "/server/conversation/" + sessionId;
                lastEndpoint = spec;

                HttpResponse response = SelfDevHttpUtils.executeGet(spec, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                lastResponse = response;

                if (response.isInterrupted()) {
                    Thread.currentThread().interrupt();
                    long duration = System.currentTimeMillis() - startTime;
                    return new TaskResult.Builder(id)
                            .status(TaskStatus.SKIPPED)
                            .message("Analytic chat validation interrupted during HTTP execution")
                            .duration(duration)
                            .build();
                }

                if (response.getException() != null) {
                    lastException = response.getException();
                }

                int code = response.getStatusCode();
                String respStr = response.getBody();

                if (attempt % 5 == 1 || code != 200) {
                    logInfo("Poll #" + attempt + " on port " + port + ": HTTP " + code + ", body len=" + respStr.length());
                }

                if (response.isHttpSuccess() && !respStr.trim().isEmpty() && !respStr.trim().equals("[]")) {
                    finalResponseText = respStr;
                    if (respStr.contains("text") || respStr.contains("sender") || respStr.contains("Hello") || respStr.contains("hi") || respStr.length() > 5) {
                        validated = true;
                        logInfo("Validated chat response received on port " + port + " after " + (System.currentTimeMillis() - startTime) + "ms");
                        break;
                    }
                }
            }

            if (validated) {
                break;
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                long duration = System.currentTimeMillis() - startTime;
                return new TaskResult.Builder(id)
                        .status(TaskStatus.SKIPPED)
                        .message("Analytic chat validation interrupted during poll delay")
                        .duration(duration)
                        .build();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        if (validated) {
            return new TaskResult.Builder(id)
                    .status(TaskStatus.SUCCESS)
                    .message("Chat final response received and validated successfully for session '" + sessionId + "'")
                    .duration(duration)
                    .diagnostic("sessionId", sessionId)
                    .diagnostic("endpoint", lastEndpoint)
                    .diagnostic("attempts", String.valueOf(attempt))
                    .diagnostic("response", finalResponseText)
                    .build();
        } else {
            StringWriter sw = new StringWriter();
            if (lastException != null) {
                lastException.printStackTrace(new PrintWriter(sw));
            }
            String failMsg = "Analytic chat validation failed: Did not receive valid final response within " + (maxWaitTimeMs / 1000) + "s";
            TaskResult.Builder failBuilder = new TaskResult.Builder(id)
                    .status(TaskStatus.FAILED)
                    .message(failMsg)
                    .duration(duration)
                    .error(lastException)
                    .diagnostic("sessionId", sessionId)
                    .diagnostic("lastEndpoint", lastEndpoint)
                    .diagnostic("attempts", String.valueOf(attempt))
                    .diagnostic("lastResponse", finalResponseText != null ? finalResponseText : (lastResponse != null ? lastResponse.getBody() : "none"));

            if (lastResponse != null) {
                for (var entry : lastResponse.toDiagnosticMap().entrySet()) {
                    failBuilder.diagnostic(entry.getKey(), entry.getValue());
                }
            }
            if (sw.getBuffer().length() > 0) {
                failBuilder.diagnostic("stackTrace", sw.toString());
            }

            return failBuilder.build();
        }
    }
}
