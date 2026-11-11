package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Descriptor of a configured server component and its runtime state,
 * including bind address, advertised URL, and LAN URL.
 */
public class ServerEntry {
    private final String serviceId;
    private final String name;
    private final String serverType;
    private final int port;
    private final String host; // bind address
    private final String endpointUrl;
    private final String lanUrl;
    private final String advertisedUrl;
    private volatile ServerState state;
    private volatile String lastError;

    public ServerEntry(String serviceId, String name, String serverType, int port, String host, String endpointUrl, ServerState state) {
        this(serviceId, name, serverType, port, host, endpointUrl, state, null);
    }

    public ServerEntry(String serviceId, String name, String serverType, int port, String host, String endpointUrl, ServerState state, String lastError) {
        this(serviceId, name, serverType, port, host, endpointUrl, endpointUrl, endpointUrl, state, lastError);
    }

    public ServerEntry(String serviceId, String name, String serverType, int port, String host,
                       String endpointUrl, String lanUrl, String advertisedUrl,
                       ServerState state, String lastError) {
        this.serviceId = serviceId;
        this.name = name;
        this.serverType = serverType;
        this.port = port;
        this.host = host != null ? host : "127.0.0.1";
        this.endpointUrl = endpointUrl != null ? endpointUrl : ("http://" + this.host + ":" + port);
        this.lanUrl = lanUrl != null ? lanUrl : this.endpointUrl;
        this.advertisedUrl = advertisedUrl != null ? advertisedUrl : this.endpointUrl;
        this.state = state != null ? state : ServerState.STOPPED;
        this.lastError = lastError;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getName() {
        return name;
    }

    public String getServerType() {
        return serverType;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getBindAddress() {
        return host;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getLanUrl() {
        return lanUrl;
    }

    public String getAdvertisedUrl() {
        return advertisedUrl;
    }

    public ServerState getState() {
        return state;
    }

    public void setState(ServerState state) {
        this.state = state;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public boolean isRunning() {
        return state == ServerState.RUNNING;
    }

    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        if (state.canStart()) {
            actions.add("Start");
        }
        if (state.canStop()) {
            actions.add("Stop");
        }
        if (state.canRestart()) {
            actions.add("Restart");
        }
        if (hasValidWebEndpoint()) {
            actions.add("Open in Browser");
        }
        return Collections.unmodifiableList(actions);
    }

    public boolean hasValidWebEndpoint() {
        return endpointUrl != null && (endpointUrl.startsWith("http://") || endpointUrl.startsWith("https://"));
    }

    public boolean canStart() {
        return state.canStart();
    }

    public boolean canStop() {
        return state.canStop();
    }

    public boolean canRestart() {
        return state.canRestart();
    }

    public boolean canOpenInBrowser() {
        return hasValidWebEndpoint();
    }

    @Override
    public String toString() {
        return "ServerEntry{" +
                "serviceId='" + serviceId + '\'' +
                ", name='" + name + '\'' +
                ", serverType='" + serverType + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", endpointUrl='" + endpointUrl + '\'' +
                ", lanUrl='" + lanUrl + '\'' +
                ", advertisedUrl='" + advertisedUrl + '\'' +
                ", state=" + state +
                '}';
    }
}
