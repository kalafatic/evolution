# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/providers/

## Domain: general

## Components
* `ProviderConfig.java`: package eu.kalafatic.evolution.controller.providers; public class ProviderConfig { private String name; private String url; private String apiKey; private String format; // e.g. "openai", "anthropic", "google" private boolean local; private String defaultModel; public ProviderConfig(String name, String url, String apiKey, String format, boolean local) { this(name, url, apiKey, format, local, null); } public ProviderConfig(String name, String url, String apiKey, String format, boolean local, String defaultModel) { this.name = name; this.url = url; this.apiKey = apiKey; this.format = format; this.local = local; this.defaultModel = defaultModel; } public String getName() { return name; }
* `AiProviders.java`: package eu.kalafatic.evolution.controller.providers; import java.util.HashMap; import java.util.Map; public class AiProviders { public static final Map<String, ProviderConfig> PROVIDERS; static { PROVIDERS = new HashMap<>(); PROVIDERS.put("openai", new ProviderConfig( "openai", "https://api.openai.com/v1/chat/completions", "YOUR_API_KEY", "openai", false, "gpt-4o" )); PROVIDERS.put("anthropic", new ProviderConfig( "anthropic", "https://api.anthropic.com/v1/messages", "YOUR_API_KEY", "anthropic",
