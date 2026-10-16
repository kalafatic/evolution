package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class SelfDevHttpUtils {

    private static final int MAX_BODY_CHARS = 65536;

    public static class HttpResponse {
        private final int statusCode;
        private final String body;
        private final String contentType;
        private final long durationMs;
        private final Exception exception;
        private final boolean interrupted;

        public HttpResponse(int statusCode, String body, String contentType, long durationMs, Exception exception, boolean interrupted) {
            this.statusCode = statusCode;
            this.body = body != null ? body : "";
            this.contentType = contentType != null ? contentType : "";
            this.durationMs = durationMs;
            this.exception = exception;
            this.interrupted = interrupted;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public String getContentType() {
            return contentType;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public Exception getException() {
            return exception;
        }

        public boolean isInterrupted() {
            return interrupted;
        }

        public boolean isHttpSuccess() {
            return statusCode >= 200 && statusCode < 300 && exception == null && !interrupted;
        }

        public Map<String, String> toDiagnosticMap() {
            Map<String, String> map = new HashMap<>();
            map.put("httpCode", String.valueOf(statusCode));
            map.put("durationMs", String.valueOf(durationMs));
            map.put("contentType", contentType);
            map.put("responseBody", body.length() > 2048 ? body.substring(0, 2048) + "..." : body);
            if (exception != null) {
                map.put("exceptionType", exception.getClass().getName());
                map.put("exceptionMessage", exception.getMessage() != null ? exception.getMessage() : "");
            }
            if (interrupted) {
                map.put("interrupted", "true");
            }
            return map;
        }
    }

    public static HttpResponse executePost(String spec, String jsonBody, int connectTimeoutMs, int readTimeoutMs) {
        long startTime = System.currentTimeMillis();
        HttpURLConnection conn = null;
        byte[] bodyBytes = (jsonBody != null ? jsonBody : "{}").getBytes(StandardCharsets.UTF_8);

        if (Thread.currentThread().isInterrupted()) {
            return new HttpResponse(-1, "", "", System.currentTimeMillis() - startTime, new InterruptedException("Thread interrupted before HTTP POST"), true);
        }

        try {
            URL url = new URL(spec);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            conn.setFixedLengthStreamingMode(bodyBytes.length);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            String contentType = conn.getContentType();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    char[] buffer = new char[1024];
                    int read;
                    while ((read = br.read(buffer, 0, buffer.length)) != -1) {
                        if (Thread.currentThread().isInterrupted()) {
                            return new HttpResponse(code, sb.toString().trim(), contentType, System.currentTimeMillis() - startTime, new InterruptedException("Thread interrupted during body read"), true);
                        }
                        if (sb.length() + read > MAX_BODY_CHARS) {
                            int allowed = MAX_BODY_CHARS - sb.length();
                            if (allowed > 0) {
                                sb.append(buffer, 0, allowed);
                            }
                            sb.append("\n...[truncated]");
                            break;
                        }
                        sb.append(buffer, 0, read);
                    }
                }
            } else {
                String respMsg = conn.getResponseMessage();
                sb.append("HTTP ").append(code).append(": ").append(respMsg != null ? respMsg : "No error stream available");
            }

            long duration = System.currentTimeMillis() - startTime;
            return new HttpResponse(code, sb.toString().trim(), contentType, duration, null, false);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            boolean wasInterrupted = Thread.currentThread().isInterrupted() || e instanceof InterruptedException;
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
            return new HttpResponse(-1, "", "", duration, e, wasInterrupted);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static boolean isProjectCreationSuccessful(HttpResponse response, String requestedProjectName) {
        if (!response.isHttpSuccess()) {
            return false;
        }
        String body = response.getBody();
        if (body == null || body.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject json = new JSONObject(body);

            // Check if status is explicitly ERROR or error field is populated
            if (json.has("status") && "ERROR".equalsIgnoreCase(json.optString("status"))) {
                return false;
            }
            if (json.has("error") && !json.isNull("error") && !json.optString("error").trim().isEmpty()) {
                return false;
            }

            // Check positive signals
            String status = json.optString("status", "");
            String project = json.optString("project", "");
            String projectName = json.optString("projectName", "");

            if ("OK".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                return true;
            }
            if (requestedProjectName != null && !requestedProjectName.isEmpty()) {
                if (requestedProjectName.equals(project) || requestedProjectName.equals(projectName)) {
                    return true;
                }
            }
            return !project.isEmpty() || !projectName.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isInferenceSubmissionSuccessful(HttpResponse response) {
        if (!response.isHttpSuccess()) {
            return false;
        }
        String body = response.getBody();
        if (body == null || body.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject json = new JSONObject(body);

            // Check if status is explicitly ERROR or error field is populated
            if (json.has("status") && "ERROR".equalsIgnoreCase(json.optString("status"))) {
                return false;
            }
            if (json.has("error") && !json.isNull("error") && !json.optString("error").trim().isEmpty()) {
                return false;
            }

            // Positive signals
            String status = json.optString("status", "");
            if ("SUCCESS".equalsIgnoreCase(status) || "OK".equalsIgnoreCase(status)) {
                return true;
            }
            if (json.has("summary") || json.has("content") || json.has("sessionId") || json.has("type")) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
