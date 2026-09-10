package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SupervisorClient {
    private final String baseUrl;

    public SupervisorClient() {
        this("http://127.0.0.1:8089");
    }

    public SupervisorClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean ping() {
        try {
            URL url = new URL(baseUrl + "/ping");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public TaskResult sendCommand(String endpoint, String param) {
        long startTime = System.currentTimeMillis();
        try {
            String path = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
            if ("ping".equalsIgnoreCase(endpoint)) path = "/ping";

            String spec = baseUrl + path;
            if (param != null && !param.isEmpty()) {
                spec += "?path=" + URLEncoder.encode(param, StandardCharsets.UTF_8.name());
            }

            URL url = new URL(spec);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            StringBuilder responseBuffer = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuffer.append(line).append("\n");
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            if (responseCode == 200) {
                return new TaskResult.Builder("supervisor_command")
                        .status(TaskStatus.SUCCESS)
                        .message("Endpoint '" + endpoint + "' executed successfully: " + responseBuffer.toString().trim())
                        .duration(duration)
                        .exitCode(200)
                        .diagnostic("response", responseBuffer.toString())
                        .build();
            } else {
                return new TaskResult.Builder("supervisor_command")
                        .status(TaskStatus.FAILED)
                        .message("Supervisor HTTP error " + responseCode + ": " + responseBuffer.toString().trim())
                        .duration(duration)
                        .exitCode(responseCode)
                        .diagnostic("response", responseBuffer.toString())
                        .build();
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure("supervisor_command", "HTTP request exception: " + e.getMessage(), e);
        }
    }
}
