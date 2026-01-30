package eu.kalafatic.evolution.controller.handlers;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.engine.NeuronEngine;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NeuronAiService extends AbstractAiService {

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl) throws Exception {
        if (orchestrator.getNeuronAI() == null) {
            return null;
        }
        String url = orchestrator.getNeuronAI().getUrl();
        String model = orchestrator.getNeuronAI().getModel();

        if (url == null || url.isEmpty() || url.equalsIgnoreCase("local")) {
            return new NeuronEngine().runModel(orchestrator.getNeuronAI().getType(), model, prompt);
        }

        HttpClient client = getClient(proxyUrl);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("prompt", prompt);
        String json = jsonObject.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.optString("response", jsonResponse.optString("output", "No response from Neuron AI"));
    }
}
