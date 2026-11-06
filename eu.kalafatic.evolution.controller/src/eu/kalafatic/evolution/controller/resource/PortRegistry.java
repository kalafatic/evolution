package eu.kalafatic.evolution.controller.resource;

/**
 * Authoritative EVO port allocation registry.
 * <p>
 * Follows the platform standard base offset of +20000 with a consistent increment of 100:
 * <ul>
 *   <li>Original series: 8080, 8180, 8280, 8380, 8480, 8580, 8680...</li>
 *   <li>New EVO series: 28080, 28180, 28280, 28380, 28480, 28580, 28680...</li>
 * </ul>
 * EVO has absolute priority and deterministic port assignments.
 */
public enum PortRegistry {

    /** Main HTTP Server and REST API (formerly 8080 / 48080). */
    EVO_HTTP_PRIMARY("HTTP_PRIMARY", "EVO Primary HTTP / REST Server", 8080, 28080, "http", "127.0.0.1", true),

    /** Secondary HTTP Server / AI Chat / Portal (formerly 8180 / 58080 / 48081). */
    EVO_HTTP_SECONDARY("HTTP_SECONDARY", "EVO Secondary HTTP / Portal Server", 8180, 28180, "http", "127.0.0.1", true),

    /** Model Context Protocol (MCP) Server & Documentation Server (formerly 8280 / 38080 / 48082). */
    EVO_MCP("MCP", "Model Context Protocol Server", 8280, 28280, "http", "127.0.0.1", true),

    /** Autonomous Coding Agent Server (formerly 8380 / 48083). */
    EVO_DEVELOP("DEVELOP", "EVO Autonomous Coding Agent Server", 8380, 28380, "http", "127.0.0.1", true),

    /** Supervisor Process HTTP REST API (formerly 8480 / 8089). */
    EVO_SUPERVISOR_API("SUPERVISOR_API", "EVO Supervisor REST API Server", 8480, 28480, "http", "127.0.0.1", true),

    /** Supervisor Glassmorphic Control Plane Server (formerly 8580 / 28080). */
    EVO_SUPERVISOR_CONTROL("SUPERVISOR_CONTROL", "EVO Supervisor Control Server", 8580, 28580, "http", "127.0.0.1", true),

    /** Reserved Auxiliary Server (formerly 8680). */
    EVO_AUXILIARY("AUXILIARY", "EVO Auxiliary Network Service", 8680, 28680, "http", "127.0.0.1", false);

    public static final int BASE_OFFSET = 20000;
    public static final int PORT_INCREMENT = 100;

    private final String serviceId;
    private final String serviceName;
    private final int originalPort;
    private final int defaultPort;
    private final String protocol;
    private final String defaultHost;
    private final boolean autoStart;

    PortRegistry(String serviceId, String serviceName, int originalPort, int defaultPort, String protocol, String defaultHost, boolean autoStart) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.originalPort = originalPort;
        this.defaultPort = defaultPort;
        this.protocol = protocol;
        this.defaultHost = defaultHost;
        this.autoStart = autoStart;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getOriginalPort() {
        return originalPort;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * Resolves a PortRegistry entry by service identifier (case-insensitive) or legacy aliases.
     *
     * @param serviceId Service ID or alias.
     * @return Corresponding PortRegistry enum or null.
     */
    public static PortRegistry findByServiceId(String serviceId) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            return null;
        }
        String id = serviceId.trim().toUpperCase();
        for (PortRegistry reg : values()) {
            if (reg.serviceId.equalsIgnoreCase(id) || reg.name().equalsIgnoreCase(id)) {
                return reg;
            }
        }
        // Legacy aliases
        switch (id) {
            case "SERVER":
            case "PRIMARY":
            case "CORE":
                return EVO_HTTP_PRIMARY;
            case "PORTAL":
            case "SECONDARY":
                return EVO_HTTP_SECONDARY;
            case "SUPERVISOR":
                return EVO_SUPERVISOR_API;
            case "CONTROL":
            case "SUPERVISOR_UI":
                return EVO_SUPERVISOR_CONTROL;
            default:
                return null;
        }
    }

    /**
     * Finds a PortRegistry entry matching an assigned port number.
     *
     * @param port The port number.
     * @return Corresponding PortRegistry enum or null.
     */
    public static PortRegistry findByDefaultPort(int port) {
        for (PortRegistry reg : values()) {
            if (reg.defaultPort == port) {
                return reg;
            }
        }
        return null;
    }
}
