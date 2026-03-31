package eu.kalafatic.evolution.controller.providers;

public class ProviderConfig {

    private String name;
    private String url;
    private String apiKey;
    private String format; // e.g. "openai", "anthropic", "google"
    private boolean local;
    private String defaultModel;

    public ProviderConfig(String name, String url, String apiKey, String format, boolean local) {
        this(name, url, apiKey, format, local, null);
    }

    public ProviderConfig(String name, String url, String apiKey, String format, boolean local, String defaultModel) {
        this.name = name;
        this.url = url;
        this.apiKey = apiKey;
        this.format = format;
        this.local = local;
        this.defaultModel = defaultModel;
    }

    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getApiKey() { return apiKey; }
    public String getFormat() { return format; }
    public boolean isLocal() { return local; }
    public String getDefaultModel() { return defaultModel; }

	public String getTestEndpoint() {
		// TODO Auto-generated method stub
		return null;
	}
}
