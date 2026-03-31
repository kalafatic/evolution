package eu.kalafatic.evolution.controller.providers;

import java.util.HashMap;
import java.util.Map;

public class AiProviders {

    public static final Map<String, ProviderConfig> PROVIDERS;

    static {
        PROVIDERS = new HashMap<>();

        PROVIDERS.put("openai", new ProviderConfig(
                "openai",
                "https://api.openai.com/v1/chat/completions",
                "YOUR_API_KEY",
                "openai",
                false
        ));

        PROVIDERS.put("anthropic", new ProviderConfig(
                "anthropic",
                "https://api.anthropic.com/v1/messages",
                "YOUR_API_KEY",
                "anthropic",
                false
        ));

        PROVIDERS.put("gemini", new ProviderConfig(
                "gemini",
                "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent",
                "YOUR_API_KEY",
                "google",
                false
        ));

        PROVIDERS.put("mistral", new ProviderConfig(
                "mistral",
                "https://api.mistral.ai/v1/chat/completions",
                "YOUR_API_KEY",
                "openai",
                false
        ));

        PROVIDERS.put("cohere", new ProviderConfig(
                "cohere",
                "https://api.cohere.ai/v1/chat",
                "YOUR_API_KEY",
                "cohere",
                false
        ));

        // LOCAL example (important for your architecture)
        PROVIDERS.put("ollama", new ProviderConfig(
                "ollama",
                "http://localhost:11434/api/chat",
                null,
                "ollama",
                true
        ));
    }
}