package eu.kalafatic.evolution.controller.resource;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.kalafatic.evolution.controller.log.Log;

/**
 * Utility helper for network interface inspection, bind address resolution,
 * LAN URL construction, and secure reverse-proxy header normalization.
 */
public final class NetworkInterfaceHelper {

    public static final String PROP_NETWORK_MODE = "evo.network.mode";
    public static final String PROP_BIND_ADDRESS = "evo.network.bindAddress";
    public static final String PROP_EXTERNAL_BASE_URL = "evo.network.externalBaseUrl";
    public static final String PROP_TRUSTED_PROXIES = "evo.network.trustedProxies";

    public static final String ENV_NETWORK_MODE = "EVO_NETWORK_MODE";
    public static final String ENV_BIND_ADDRESS = "EVO_BIND_ADDRESS";
    public static final String ENV_EXTERNAL_BASE_URL = "EVO_EXTERNAL_BASE_URL";
    public static final String ENV_TRUSTED_PROXIES = "EVO_TRUSTED_PROXIES";

    private NetworkInterfaceHelper() {}

    /**
     * Resolves the configured or active {@link NetworkMode}.
     * Defaults to {@link NetworkMode#LOOPBACK}.
     */
    public static NetworkMode resolveNetworkMode() {
        String prop = System.getProperty(PROP_NETWORK_MODE);
        if (prop == null || prop.trim().isEmpty()) {
            prop = System.getenv(ENV_NETWORK_MODE);
        }
        return NetworkMode.fromString(prop);
    }

    /**
     * Discovers all non-loopback, active IPv4 addresses on physical/virtual network interfaces.
     */
    public static List<String> getAvailableLanAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return Collections.emptyList();
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                try {
                    if (!iface.isUp() || iface.isLoopback() || iface.isPointToPoint()) {
                        continue;
                    }
                } catch (SocketException ignored) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress addr = inetAddresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isAnyLocalAddress()) {
                        addresses.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            Log.log("[NetworkInterfaceHelper] Failed to enumerate network interfaces: " + e.getMessage());
        }
        return addresses;
    }

    /**
     * Discovers the primary local area network (LAN) IPv4 address.
     * Prefers site-local addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x).
     *
     * @return Discovered LAN IPv4 address.
     * @throws IllegalStateException if no active LAN IPv4 address is available on any network interface.
     */
    public static String getPrimaryLanAddress() {
        // Explicit override via property or env
        String explicit = System.getProperty(PROP_BIND_ADDRESS);
        if (explicit == null || explicit.trim().isEmpty()) {
            explicit = System.getenv(ENV_BIND_ADDRESS);
        }
        if (explicit != null && !explicit.trim().isEmpty() && !"0.0.0.0".equals(explicit.trim())) {
            return explicit.trim();
        }

        List<String> addrs = getAvailableLanAddresses();
        if (addrs.isEmpty()) {
            throw new IllegalStateException("No active LAN IPv4 network interface found. Cannot establish LAN binding.");
        }

        // Prioritize site-local addresses (192.168.*, 10.*, 172.16-31.*)
        for (String addr : addrs) {
            if (addr.startsWith("192.168.") || addr.startsWith("10.")) {
                return addr;
            }
            if (addr.startsWith("172.")) {
                try {
                    String[] parts = addr.split("\\.");
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return addr;
                    }
                } catch (Exception ignored) {}
            }
        }

        return addrs.get(0);
    }

    /**
     * Resolves the effective bind address for a service based on current network configuration.
     *
     * @param serviceId Canonical service ID (e.g. HTTP_PRIMARY, SUPERVISOR_CONTROL)
     * @param defaultHost The default host from PortRegistry (usually 127.0.0.1)
     * @return The IP address string for socket binding.
     * @throws IllegalStateException if LAN mode is requested but no LAN address exists.
     */
    public static String resolveBindAddress(String serviceId, String defaultHost) {
        // Supervisor Control TCP (port 28580) MUST remain strictly loopback under every mode
        if ("SUPERVISOR_CONTROL".equalsIgnoreCase(serviceId) || "CONTROL".equalsIgnoreCase(serviceId)) {
            return "127.0.0.1";
        }

        // Check explicit bind address property
        String explicit = System.getProperty(PROP_BIND_ADDRESS);
        if (explicit == null || explicit.trim().isEmpty()) {
            explicit = System.getenv(ENV_BIND_ADDRESS);
        }
        if (explicit != null && !explicit.trim().isEmpty()) {
            return explicit.trim();
        }

        NetworkMode mode = resolveNetworkMode();
        switch (mode) {
            case ALL_INTERFACES:
                return "0.0.0.0";
            case LAN:
                return getPrimaryLanAddress();
            case LOOPBACK:
            default:
                return (defaultHost != null && !defaultHost.isEmpty()) ? defaultHost : "127.0.0.1";
        }
    }

    /**
     * Resolves the LAN URL for a service (using the primary LAN address if available, or localhost fallback).
     */
    public static String getLanUrl(int port, String path) {
        String cleanPath = (path != null && !path.startsWith("/")) ? ("/" + path) : (path != null ? path : "");
        String lanHost;
        try {
            lanHost = getPrimaryLanAddress();
        } catch (Exception e) {
            lanHost = "127.0.0.1";
        }
        return "http://" + lanHost + ":" + port + cleanPath;
    }

    /**
     * Resolves the advertised base URL for external / LAN clients.
     *
     * @param port Assigned port number
     * @return Base URL string, e.g. "http://192.168.0.2:28080"
     */
    public static String getAdvertisedBaseUrl(int port) {
        String configuredExternal = getExternalBaseUrl();
        if (configuredExternal != null && !configuredExternal.isEmpty()) {
            return configuredExternal;
        }

        NetworkMode mode = resolveNetworkMode();
        String host;
        if (mode == NetworkMode.LOOPBACK) {
            host = "127.0.0.1";
        } else {
            try {
                host = getPrimaryLanAddress();
            } catch (Exception e) {
                host = "127.0.0.1";
            }
        }
        return "http://" + host + ":" + port;
    }

    /**
     * Retrieves the statically configured external base URL if defined.
     */
    public static String getExternalBaseUrl() {
        String url = System.getProperty(PROP_EXTERNAL_BASE_URL);
        if (url == null || url.trim().isEmpty()) {
            url = System.getenv(ENV_EXTERNAL_BASE_URL);
        }
        if (url != null && !url.trim().isEmpty()) {
            String trimmed = url.trim();
            if (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return trimmed;
        }
        return null;
    }

    /**
     * Resolves the set of trusted proxy IP addresses.
     * By default, includes loopback addresses (127.0.0.1, ::1).
     */
    public static Set<String> getTrustedProxies() {
        Set<String> trusted = new HashSet<>(Arrays.asList("127.0.0.1", "0:0:0:0:0:0:0:1", "::1", "localhost"));
        String proxiesProp = System.getProperty(PROP_TRUSTED_PROXIES);
        if (proxiesProp == null || proxiesProp.trim().isEmpty()) {
            proxiesProp = System.getenv(ENV_TRUSTED_PROXIES);
        }
        if (proxiesProp != null && !proxiesProp.trim().isEmpty()) {
            for (String p : proxiesProp.split(",")) {
                if (!p.trim().isEmpty()) {
                    trusted.add(p.trim());
                }
            }
        }
        return trusted;
    }

    /**
     * Checks if a client IP address is in the trusted proxy list.
     */
    public static boolean isTrustedProxy(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return false;
        }
        String cleanIp = clientIp.trim();
        if (cleanIp.startsWith("/")) {
            cleanIp = cleanIp.substring(1);
        }
        return getTrustedProxies().contains(cleanIp);
    }

    /**
     * Dynamically resolves the effective external request URL or origin from HTTP session headers,
     * securely taking into account reverse-proxy headers (X-Forwarded-*, Forwarded, Host) ONLY when
     * the request originates from a trusted proxy or when an explicit external base URL is configured.
     *
     * @param clientIp Remote client IP address
     * @param headers Map of incoming HTTP request headers
     * @param defaultPort Internal listening port
     * @param path Target URI path (e.g. "/dashboard.html")
     * @return Resolved absolute URL
     */
    public static String resolveExternalUrl(String clientIp, Map<String, String> headers, int defaultPort, String path) {
        String cleanPath = (path != null && !path.startsWith("/")) ? ("/" + path) : (path != null ? path : "");

        // 1. Explicitly configured external base URL takes absolute precedence
        String externalBase = getExternalBaseUrl();
        if (externalBase != null && !externalBase.isEmpty()) {
            return externalBase + cleanPath;
        }

        // 2. Only trust reverse-proxy headers if the client is a configured trusted proxy
        boolean trustHeaders = isTrustedProxy(clientIp);

        if (trustHeaders && headers != null && !headers.isEmpty()) {
            // Forwarded header (RFC 7239)
            String forwarded = getHeader(headers, "forwarded");
            if (forwarded != null && !forwarded.isEmpty()) {
                String proto = null;
                String host = null;
                for (String element : forwarded.split(";")) {
                    String[] kv = element.trim().split("=", 2);
                    if (kv.length == 2) {
                        if ("proto".equalsIgnoreCase(kv[0])) proto = kv[1].replace("\"", "");
                        if ("host".equalsIgnoreCase(kv[0])) host = kv[1].replace("\"", "");
                    }
                }
                if (host != null && !host.isEmpty()) {
                    String scheme = (proto != null && !proto.isEmpty()) ? proto : "http";
                    return scheme + "://" + host + cleanPath;
                }
            }

            // X-Forwarded-Host & X-Forwarded-Proto
            String xHost = getHeader(headers, "x-forwarded-host");
            String xProto = getHeader(headers, "x-forwarded-proto");
            String xPort = getHeader(headers, "x-forwarded-port");
            String xPrefix = getHeader(headers, "x-forwarded-prefix");

            if (xPrefix != null && !xPrefix.isEmpty()) {
                if (!xPrefix.startsWith("/")) xPrefix = "/" + xPrefix;
                if (xPrefix.endsWith("/")) xPrefix = xPrefix.substring(0, xPrefix.length() - 1);
                cleanPath = xPrefix + cleanPath;
            }

            if (xHost != null && !xHost.trim().isEmpty()) {
                String host = xHost.split(",")[0].trim();
                String scheme = (xProto != null && !xProto.trim().isEmpty()) ? xProto.split(",")[0].trim() : "http";
                if (xPort != null && !xPort.trim().isEmpty() && !host.contains(":")) {
                    String p = xPort.split(",")[0].trim();
                    if (!("80".equals(p) && "http".equalsIgnoreCase(scheme)) &&
                        !("443".equals(p) && "https".equalsIgnoreCase(scheme))) {
                        host = host + ":" + p;
                    }
                }
                return scheme + "://" + host + cleanPath;
            }
        }

        // 3. Fallback to advertised base URL
        return getAdvertisedBaseUrl(defaultPort) + cleanPath;
    }

    /**
     * Backward-compatible overload without client IP.
     */
    public static String resolveExternalUrl(Map<String, String> headers, int defaultPort, String path) {
        return resolveExternalUrl(null, headers, defaultPort, path);
    }

    private static String getHeader(Map<String, String> headers, String name) {
        if (headers == null || name == null) return null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
