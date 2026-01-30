package eu.kalafatic.evolution.controller.handlers;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OllamaService extends AbstractAiService {

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl) throws Exception {
        if (orchestrator.getOllama() == null || orchestrator.getOllama().getUrl() == null || orchestrator.getOllama().getUrl().isEmpty()) {
            return null;
        }
        String url = orchestrator.getOllama().getUrl();
        String model = orchestrator.getOllama().getModel();

        HttpClient client = getClient(proxyUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("prompt", prompt);
        jsonObject.put("stream", false);
        String json = jsonObject.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.has("response") ? jsonResponse.getString("response") : jsonResponse.getString("solution");
    }
}
