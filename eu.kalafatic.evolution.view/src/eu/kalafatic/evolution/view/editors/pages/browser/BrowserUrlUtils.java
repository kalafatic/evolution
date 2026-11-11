package eu.kalafatic.evolution.view.editors.pages.browser;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;

/**
 * Standardized URL normalization and endpoint matching utilities for EVO Browser.
 * Handles loopback/localhost aliases, canonical port resolution, trailing slashes,
 * query parameter order, and matching internal paths to registered WebApplications.
 */
public final class BrowserUrlUtils {

    private static final Set<String> LOOPBACK_HOSTS = new HashSet<>(Arrays.asList(
            "localhost", "127.0.0.1", "0.0.0.0", "::1"
    ));

    private BrowserUrlUtils() {
    }

    /**
     * Normalizes a URL for stable comparison and tab identity lookup.
     * Normalization includes:
     * - Trimming and lowercasing scheme and host
     * - Treating localhost, 127.0.0.1, 0.0.0.0 as equivalent canonical "localhost"
     * - Stripping default ports (80 for http, 443 for https)
     * - Stripping SWT/runtime helper query parameter "?runtime=SWT" or "&runtime=SWT"
     * - Sorting remaining query parameters deterministically
     * - Removing trailing slashes from path (except root "/")
     * - Removing URL fragments (#hash) for browser tab identity comparison
     */
    public static String normalizeUrl(String rawUrl) {
        if (rawUrl == null) {
            return "";
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        // If no scheme is present, add http:// if it looks like a domain/host
        if (!trimmed.contains("://")) {
            if (trimmed.startsWith("localhost") || trimmed.startsWith("127.0.0.1") || trimmed.contains("/")) {
                trimmed = "http://" + trimmed;
            } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
                trimmed = "https://" + trimmed;
            } else {
                return trimmed;
            }
        }

        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "http";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            int port = uri.getPort();

            // Normalize loopback hosts
            if (LOOPBACK_HOSTS.contains(host)) {
                host = "localhost";
            }

            // Normalize default ports
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }

            // Path normalization: ensure starts with '/', remove trailing slash unless root
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            } else {
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                while (path.length() > 1 && path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
            }

            // Query parameter normalization
            String rawQuery = uri.getRawQuery();
            String normalizedQuery = normalizeQuery(rawQuery);

            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host);
            if (port > 0) {
                sb.append(":").append(port);
            }
            sb.append(path);
            if (normalizedQuery != null && !normalizedQuery.isEmpty()) {
                sb.append("?").append(normalizedQuery);
            }
            return sb.toString();
        } catch (Exception e) {
            // Fallback string normalization if URI parsing fails
            return trimmed.replaceAll("(?i)\\?runtime=SWT&?", "?")
                          .replaceAll("(?i)&runtime=SWT", "")
                          .replaceAll("\\?$", "");
        }
    }

    private static String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return null;
        }

        String[] pairs = rawQuery.split("&");
        Set<String> cleanParams = new TreeSet<>();
        for (String pair : pairs) {
            String p = pair.trim();
            if (p.isEmpty()) continue;
            // Filter out internal runtime markers
            if (p.equalsIgnoreCase("runtime=SWT") || p.startsWith("runtime=")) {
                continue;
            }
            try {
                cleanParams.add(URLDecoder.decode(p, StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                cleanParams.add(p);
            }
        }

        if (cleanParams.isEmpty()) {
            return null;
        }
        return String.join("&", cleanParams);
    }

    /**
     * Checks if two URLs represent the exact same web destination.
     */
    public static boolean isSameUrl(String url1, String url2) {
        if (url1 == null || url2 == null) {
            return false;
        }
        return normalizeUrl(url1).equalsIgnoreCase(normalizeUrl(url2));
    }

    /**
     * Checks if a target URL belongs to an existing registered WebApplication.
     * An internal subpath, query, or hash belonging to an application (e.g. /mcp.html#tools)
     * belongs to the registered web application and should not spawn a duplicate tab.
     */
    public static WebApplicationDescriptor findMatchingApp(String url, Collection<WebApplicationDescriptor> registeredApps) {
        if (url == null || registeredApps == null || registeredApps.isEmpty()) {
            return null;
        }

        String normalizedTarget = normalizeUrl(url);

        // 1. Exact match on full URL
        for (WebApplicationDescriptor app : registeredApps) {
            if (normalizeUrl(app.getFullUrl()).equalsIgnoreCase(normalizedTarget)) {
                return app;
            }
        }

        // 2. Port and path prefix match for registered web apps
        try {
            URI targetUri = URI.create(normalizedTarget);
            int targetPort = targetUri.getPort();
            String targetHost = targetUri.getHost();
            String targetPath = targetUri.getPath();

            if (targetHost != null && (targetHost.equals("localhost") || LOOPBACK_HOSTS.contains(targetHost))) {
                for (WebApplicationDescriptor app : registeredApps) {
                    if (app.getServerPort() == targetPort) {
                        String appNormalized = normalizeUrl(app.getFullUrl());
                        URI appUri = URI.create(appNormalized);
                        String appPath = appUri.getPath();

                        if (appPath != null && !appPath.equals("/")) {
                            // If app has a non-root path like /develop or /mcp.html, match if target path starts with it
                            if (targetPath.equalsIgnoreCase(appPath) || targetPath.startsWith(appPath + "/")) {
                                return app;
                            }
                        } else if (appPath != null && appPath.equals("/") && targetPath.equals("/")) {
                            return app;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Extracts host and port key (e.g. "localhost:28080") for server association.
     */
    public static int extractPort(String url) {
        if (url == null) return -1;
        try {
            URI uri = URI.create(url);
            int port = uri.getPort();
            if (port > 0) return port;
            if ("http".equalsIgnoreCase(uri.getScheme())) return 80;
            if ("https".equalsIgnoreCase(uri.getScheme())) return 443;
        } catch (Exception ignored) {}
        return -1;
    }
}
