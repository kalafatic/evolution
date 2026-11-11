package eu.kalafatic.evolution.controller.resource;

/**
 * Deployment network mode defining the network interfaces on which EVO services bind.
 * <ul>
 *   <li>{@link #LOOPBACK}: Binds strictly to 127.0.0.1 (localhost-only). Default safe behavior.</li>
 *   <li>{@link #LAN}: Binds to the local network IP address (e.g. 192.168.x.x) or explicit host.</li>
 *   <li>{@link #ALL_INTERFACES}: Binds to 0.0.0.0 (all interfaces), subject to explicit configuration.</li>
 * </ul>
 */
public enum NetworkMode {

    LOOPBACK("127.0.0.1", "Loopback only (localhost)"),
    LAN(null, "Local Area Network (LAN IP)"),
    ALL_INTERFACES("0.0.0.0", "All Network Interfaces (0.0.0.0)");

    private final String defaultBindAddress;
    private final String description;

    NetworkMode(String defaultBindAddress, String description) {
        this.defaultBindAddress = defaultBindAddress;
        this.description = description;
    }

    public String getDefaultBindAddress() {
        return defaultBindAddress;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolves NetworkMode from string name (case-insensitive).
     * Defaults to {@link #LOOPBACK} if null or unrecognized.
     */
    public static NetworkMode fromString(String modeStr) {
        if (modeStr == null || modeStr.trim().isEmpty()) {
            return LOOPBACK;
        }
        String clean = modeStr.trim().toUpperCase();
        for (NetworkMode mode : values()) {
            if (mode.name().equalsIgnoreCase(clean)) {
                return mode;
            }
        }
        // Aliases
        if ("LOCALHOST".equals(clean) || "LOCAL".equals(clean)) {
            return LOOPBACK;
        }
        if ("ALL".equals(clean) || "ANY".equals(clean) || "0.0.0.0".equals(clean)) {
            return ALL_INTERFACES;
        }
        return LOOPBACK;
    }
}
