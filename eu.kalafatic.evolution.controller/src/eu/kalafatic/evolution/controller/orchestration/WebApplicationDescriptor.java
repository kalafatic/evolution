package eu.kalafatic.evolution.controller.orchestration;

/**
 * Descriptor of a web application running on an EVO server.
 */
public class WebApplicationDescriptor {
    private final String id;
    private final String name;
    private final String path;
    private final int serverPort;
    private final String serviceId;

    public WebApplicationDescriptor(String id, String name, String path, int serverPort, String serviceId) {
        this.id = id;
        this.name = name;
        this.path = path != null ? path : "/";
        this.serverPort = serverPort;
        this.serviceId = serviceId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getFullUrl() {
        return getFullUrl("localhost");
    }

    public String getFullUrl(String host) {
        String h = (host != null && !host.isEmpty()) ? host : "localhost";
        String normalizedPath = path.startsWith("/") ? path : ("/" + path);
        return "http://" + h + ":" + serverPort + normalizedPath;
    }

    @Override
    public String toString() {
        return "WebApplicationDescriptor{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", serverPort=" + serverPort +
                ", path='" + path + '\'' +
                '}';
    }
}
