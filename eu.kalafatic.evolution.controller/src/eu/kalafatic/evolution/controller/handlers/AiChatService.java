package eu.kalafatic.evolution.controller.handlers;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AiChatService extends AbstractAiService {

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl) throws Exception {
        if (orchestrator.getAiChat() == null || orchestrator.getAiChat().getUrl() == null || orchestrator.getAiChat().getUrl().isEmpty()) {
            return null;
        }
        String url = orchestrator.getAiChat().getUrl();
        String token = orchestrator.getAiChat().getToken();

        HttpClient client = getClient(proxyUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("prompt", prompt);
        String json = jsonObject.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body()).getString("response");
    }
}
