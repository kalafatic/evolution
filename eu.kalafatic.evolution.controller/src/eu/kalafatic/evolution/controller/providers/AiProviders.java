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
                false,
                "gpt-4o"
        ));

        PROVIDERS.put("anthropic", new ProviderConfig(
                "anthropic",
                "https://api.anthropic.com/v1/messages",
                "YOUR_API_KEY",
                "anthropic",
                false,
                "claude-3-5-sonnet-20240620"
        ));

        PROVIDERS.put("gemini", new ProviderConfig(
                "gemini",
                "https://generativelanguage.googleapis.com/v1/models/%s:generateContent",
                "YOUR_API_KEY",
                "google",
                false,
                "gemini-1.5-pro"
        ));


        PROVIDERS.put("deepseek", new ProviderConfig(
                "deepseek",
                "https://api.deepseek.com/chat/completions",
                "YOUR_API_KEY",
                "openai",
                false,
                "deepseek-chat"
        ));

        PROVIDERS.put("groq", new ProviderConfig(
                "groq",
                "https://api.groq.com/openai/v1/chat/completions",
                "YOUR_API_KEY",
                "openai",
                false,
                "llama-3.1-70b-versatile"
        ));
       

        // LOCAL example (important for your architecture)
        PROVIDERS.put("ollama", new ProviderConfig(
                "ollama",
                "http://localhost:11434/api/chat",
                null,
                "ollama",
                true,
                "llama3.1"
        ));
    }
}
