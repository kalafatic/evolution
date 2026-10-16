package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevHttpUtils.HttpResponse;

public class CreateInferenceTask extends AbstractSelfDevTask {

    private static final String DEFAULT_SESSION_ID = "SelfDevProject";
    private static final String DEFAULT_PROMPT = "hi";
    private static final String DEFAULT_MODEL = "local";

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_ATTEMPTS = 2;

    public CreateInferenceTask(String id) {
        super(id, "Create Local Inference Task (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        logInfo("Executing Create Local Inference task ID: " + id);
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }

        long startTime = System.currentTimeMillis();
        String sessionId = DEFAULT_SESSION_ID;
        String prompt = DEFAULT_PROMPT;
        String model = DEFAULT_MODEL;

        String jsonPayload = "{\"prompt\":\"" + prompt + "\",\"sessionId\":\"" + sessionId + "\",\"model\":\"" + model + "\"}";

        int serverPort = context.getEffectiveServerPort();
        int supervisorPort = context.getEffectiveSupervisorPort();

        // Build target ports list: primary server port first, supervisor port second if distinct
        Set<Integer> portsToTry = new LinkedHashSet<>();
        portsToTry.add(serverPort);
        if (supervisorPort > 0) {
            portsToTry.add(supervisorPort);
        }

        // Canonical primary endpoint first, fallback endpoint second
        String[] endpointPaths = new String[] { "/task", "/create-inference-task" };

        HttpResponse lastResponse = null;
        HttpResponse primaryErrorResponse = null;
        String lastAttemptedEndpoint = "";
        String primaryErrorEndpoint = "";
        int totalAttempts = 0;

        for (int port : portsToTry) {
            for (String path : endpointPaths) {
                String spec = "http://127.0.0.1:" + port + path;
                lastAttemptedEndpoint = spec;

                for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                    if (Thread.currentThread().isInterrupted()) {
                        logInfo("Thread interrupted during inference task creation. Stopping retries.");
                        long duration = System.currentTimeMillis() - startTime;
                        return new TaskResult.Builder(id)
                                .status(TaskStatus.SKIPPED)
                                .message("Inference task creation interrupted")
                                .duration(duration)
                                .build();
                    }

                    totalAttempts++;
                    logInfo("Attempt " + attempt + "/" + MAX_ATTEMPTS + " submitting inference task for session '" + sessionId + "' to target endpoint: " + spec);

                    HttpResponse response = SelfDevHttpUtils.executePost(spec, jsonPayload, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                    lastResponse = response;

                    if (response.isInterrupted()) {
                        Thread.currentThread().interrupt();
                        long duration = System.currentTimeMillis() - startTime;
                        return new TaskResult.Builder(id)
                                .status(TaskStatus.SKIPPED)
                                .message("Inference task submission interrupted during HTTP execution")
                                .duration(duration)
                                .build();
                    }

                    logInfo("HTTP Response Code: " + response.getStatusCode() + ", Duration: " + response.getDurationMs() + "ms, Body: " + response.getBody());

                    if (SelfDevHttpUtils.isInferenceSubmissionSuccessful(response)) {
                        long totalDuration = System.currentTimeMillis() - startTime;
                        return new TaskResult.Builder(id)
                                .status(TaskStatus.SUCCESS)
                                .message("Simple 'hi' local inference task submitted successfully to session '" + sessionId + "' via port " + port + path)
                                .duration(totalDuration)
                                .diagnostic("sessionId", sessionId)
                                .diagnostic("prompt", prompt)
                                .diagnostic("model", model)
                                .diagnostic("port", String.valueOf(port))
                                .diagnostic("endpoint", path)
                                .diagnostic("totalAttempts", String.valueOf(totalAttempts))
                                .diagnostic("responseCode", String.valueOf(response.getStatusCode()))
                                .diagnostic("responseBody", response.getBody())
                                .build();
                    }

                    if (primaryErrorResponse == null && response.getStatusCode() > 0 && response.getStatusCode() != 404) {
                        primaryErrorResponse = response;
                        primaryErrorEndpoint = spec;
                    }

                    // If HTTP 404 (wrong endpoint path on this port), break to try next path
                    if (response.getStatusCode() == 404) {
                        logInfo("Endpoint " + spec + " returned HTTP 404. Trying alternative endpoint path.");
                        break;
                    }

                    // Sleep before attempt retry
                    if (attempt < MAX_ATTEMPTS) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            long duration = System.currentTimeMillis() - startTime;
                            return new TaskResult.Builder(id)
                                    .status(TaskStatus.SKIPPED)
                                    .message("Inference task creation interrupted during retry delay")
                                    .duration(duration)
                                    .build();
                        }
                    }
                }

                // If primary endpoint responded with non-404 error (e.g. 500, 400), do not try fallback paths
                if (primaryErrorResponse != null) {
                    break;
                }
            }
            if (primaryErrorResponse != null) {
                break;
            }
        }

        HttpResponse reportedResponse = primaryErrorResponse != null ? primaryErrorResponse : lastResponse;
        String reportedEndpoint = primaryErrorResponse != null ? primaryErrorEndpoint : lastAttemptedEndpoint;

        long totalDuration = System.currentTimeMillis() - startTime;
        String failureReason;
        Exception lastException = reportedResponse != null ? reportedResponse.getException() : null;

        if (reportedResponse != null && reportedResponse.getStatusCode() > 0) {
            failureReason = "Inference task rejected by server at " + reportedEndpoint +
                    " (HTTP " + reportedResponse.getStatusCode() + "): " + reportedResponse.getBody();
        } else if (lastException != null) {
            failureReason = "Failed to connect to inference submission endpoint (" + reportedEndpoint + "): " + lastException.getMessage();
        } else {
            failureReason = "Failed to submit inference task for session '" + sessionId + "' after " + totalAttempts + " attempts across endpoints";
        }

        StringWriter sw = new StringWriter();
        if (lastException != null) {
            lastException.printStackTrace(new PrintWriter(sw));
        }

        TaskResult.Builder failBuilder = new TaskResult.Builder(id)
                .status(TaskStatus.FAILED)
                .message(failureReason)
                .duration(totalDuration)
                .error(lastException)
                .diagnostic("sessionId", sessionId)
                .diagnostic("prompt", prompt)
                .diagnostic("model", model)
                .diagnostic("serverPort", String.valueOf(serverPort))
                .diagnostic("supervisorPort", String.valueOf(supervisorPort))
                .diagnostic("lastEndpoint", reportedEndpoint)
                .diagnostic("totalAttempts", String.valueOf(totalAttempts));

        if (reportedResponse != null) {
            for (var entry : reportedResponse.toDiagnosticMap().entrySet()) {
                failBuilder.diagnostic(entry.getKey(), entry.getValue());
            }
        }
        if (sw.getBuffer().length() > 0) {
            failBuilder.diagnostic("stackTrace", sw.toString());
        }

        return failBuilder.build();
    }
}
