package eu.kalafatic.evolution.controller.resource;

/**
 * Protocol classification for EVO network service ports and listeners.
 */
public enum PortProtocol {
    HTTP("http"),
    TCP("tcp");

    private final String scheme;

    PortProtocol(String scheme) {
        this.scheme = scheme;
    }

    public String getScheme() {
        return scheme;
    }

    public boolean isHttp() {
        return this == HTTP;
    }

    public boolean isTcp() {
        return this == TCP;
    }

    public static PortProtocol fromString(String val) {
        if (val != null) {
            String clean = val.trim();
            for (PortProtocol p : values()) {
                if (p.scheme.equalsIgnoreCase(clean) || p.name().equalsIgnoreCase(clean)) {
                    return p;
                }
            }
        }
        return HTTP;
    }
}
