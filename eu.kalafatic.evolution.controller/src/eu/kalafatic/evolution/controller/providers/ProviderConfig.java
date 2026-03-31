package eu.kalafatic.evolution.controller.providers;

public class ProviderConfig {

    private String name;
    private String url;
    private String apiKey;
    private String format; // e.g. "openai", "anthropic"
    private boolean local;

    public ProviderConfig(String name, String url, String apiKey, String format, boolean local) {
        this.name = name;
        this.url = url;
        this.apiKey = apiKey;
        this.format = format;
        this.local = local;
    }

    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getApiKey() { return apiKey; }
    public String getFormat() { return format; }
    public boolean isLocal() { return local; }
}