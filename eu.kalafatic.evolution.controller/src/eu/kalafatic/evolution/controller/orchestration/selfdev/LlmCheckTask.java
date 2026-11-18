package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LlmCheckTask extends AbstractSelfDevTask {

    public LlmCheckTask(String id) {
        super(id, "LLM Check (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        String llmUrl = "http://localhost:11434/api/tags";
        logInfo("Verifying LLM availability at " + llmUrl);

        try {
            URL url = new URL(llmUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream is = conn.getInputStream()) {
                    byte[] bytes = is.readAllBytes();
                    logInfo("LLM service responded HTTP " + responseCode + " (" + bytes.length + " bytes)");
                }
                return TaskResult.success(id, "LLM service available at " + llmUrl + " (HTTP " + responseCode + ")");
            } else {
                return TaskResult.failure(id, "LLM service check returned HTTP " + responseCode + " at " + llmUrl, null);
            }
        } catch (Exception e) {
            // WP2 (D-05): endpoint failure is an honest failure, never a silent
            // "fallback success".
            return TaskResult.failure(id, "LLM service check failed at " + llmUrl + ": " + e.getMessage(), e);
        }
    }
}
