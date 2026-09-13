package eu.kalafatic.evolution.controller.resource;

import java.util.Objects;

/**
 * Descriptor representing a configured EVO service endpoint.
 * Derived directly from authoritative EMF model data.
 */
public class EvoService {
    private final String id;
    private final String name;
    private final String host;
    private final int port;
    private final String protocol;
    private final String url;
    private final boolean enabled;
    private final String source;

    public EvoService(String id, String name, String host, int port, String protocol, String path, boolean enabled, String source) {
        this.id = id != null ? id : "UNKNOWN";
        this.name = name != null ? name : this.id;
        this.host = (host != null && !host.trim().isEmpty()) ? host.trim() : "127.0.0.1";
        this.port = port;
        this.protocol = (protocol != null && !protocol.trim().isEmpty()) ? protocol.trim() : "http";
        this.enabled = enabled;
        this.source = source != null ? source : "EMF";

        String cleanHost = this.host;
        if (cleanHost.startsWith("http://") || cleanHost.startsWith("https://")) {
            cleanHost = cleanHost.substring(cleanHost.indexOf("://") + 3);
        }
        if (cleanHost.contains(":")) {
            cleanHost = cleanHost.substring(0, cleanHost.indexOf(":"));
        }
        if (cleanHost.contains("/")) {
            cleanHost = cleanHost.substring(0, cleanHost.indexOf("/"));
        }

        String calculatedUrl = this.protocol + "://" + cleanHost;
        if (this.port > 0) {
            calculatedUrl += ":" + this.port;
        }
        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/")) calculatedUrl += "/";
            calculatedUrl += path;
        }
        this.url = calculatedUrl;
    }

    public EvoService(String id, String name, String host, int port, boolean enabled, String source) {
        this(id, name, host, port, "http", "", enabled, source);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUrl() {
        return url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvoService that = (EvoService) o;
        return port == that.port && Objects.equals(id, that.id) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, port, url);
    }

    @Override
    public String toString() {
        return name + " — " + url + " [" + source + "]";
    }
}
