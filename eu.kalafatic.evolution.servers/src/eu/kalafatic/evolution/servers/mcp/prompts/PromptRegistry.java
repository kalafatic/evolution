package eu.kalafatic.evolution.servers.mcp.prompts;

import com.fasterxml.jackson.databind.JsonNode;
import eu.kalafatic.evolution.servers.mcp.model.Prompt;
import java.util.*;

public class PromptRegistry {
    private final Map<String, Prompt> prompts = new HashMap<>();
    private final Map<String, PromptProvider> providers = new HashMap<>();

    public void register(Prompt prompt, PromptProvider provider) {
        prompts.put(prompt.getName(), prompt);
        providers.put(prompt.getName(), provider);
    }

    public List<Prompt> listPrompts() {
        return new ArrayList<>(prompts.values());
    }

    public Object getPrompt(JsonNode params) throws Exception {
        String name = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        PromptProvider provider = providers.get(name);
        if (provider == null) {
            throw new Exception("Prompt not found: " + name);
        }

        return provider.get(arguments);
    }

    public interface PromptProvider {
        Object get(JsonNode arguments) throws Exception;
    }
}
