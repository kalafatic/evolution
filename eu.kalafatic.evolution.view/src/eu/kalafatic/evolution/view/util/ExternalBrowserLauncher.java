package eu.kalafatic.evolution.view.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.controller.log.Log;

/**
 * Single shared external-browser launcher for EVO. All "open in the system
 * browser" actions (menu-bar web interface, F1 help, BrowserPage "Open in
 * External Browser", Forge diagram "Open in Browser", Supervisor control hub)
 * must route through this class instead of calling {@code Program.launch} or
 * {@code Desktop.browse} directly.
 *
 * <p>Guarantees:</p>
 * <ul>
 *   <li>Only {@code http}, {@code https} and {@code file} targets are ever
 *       launched. {@code mailto}, {@code data}, {@code about} and every other
 *       scheme are rejected with a diagnostic — a web URL is never handed to
 *       a mail handler.</li>
 *   <li>Local HTML files are converted to proper {@code file://} URIs.</li>
 *   <li>Backslash-escaped URLs ({@code http\://…}, a known
 *       {@code Properties}-serialization artifact) are normalized explicitly
 *       and the normalization is logged with the calling component — never
 *       silently.</li>
 *   <li>On Linux the OS default handler is checked before launching: if the
 *       handler that would open the target is a known mail client (e.g. a
 *       broken {@code text/html} association pointing at Thunderbird), the
 *       launch is refused with an actionable message instead of silently
 *       opening the mail client. No browser is hardcoded.</li>
 *   <li>Every launch records a structured diagnostic line: calling component,
 *       original target (credentials scrubbed), normalized target, scheme,
 *       mechanism, outcome and exception details.</li>
 * </ul>
 *
 * <p>Launching is delegated to SWT {@link Program#launch(String)} which uses
 * the operating system's preferred handler (GIO
 * {@code g_app_info_launch_default_for_uri} on Linux, {@code ShellExecute} on
 * Windows, {@code open} on macOS). The SWT-internal Browser widget is
 * untouched.</p>
 */
public final class ExternalBrowserLauncher {

    /** Outcome of an external launch attempt. */
    public enum Outcome {
        /** The target was handed to the OS preferred handler. */
        LAUNCHED,
        /** The target was refused (unsupported scheme, mail handler, invalid input). */
        REJECTED,
        /** The OS reported that no handler exists or the launch failed. */
        FAILED
    }

    private static final boolean IS_LINUX =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");

    private static final long HANDLER_CACHE_TTL_MS = 30_000;

    private static final Map<String, CachedHandler> HANDLER_CACHE = new ConcurrentHashMap<>();

    private ExternalBrowserLauncher() {
    }

    // =====================================================================
    // Public launch API
    // =====================================================================

    /**
     * Opens a web URL (http/https only) in the system browser. {@code mailto},
     * {@code file}, {@code data} and any other scheme are rejected.
     */
    public static Outcome launchWebUrl(String component, String url) {
        String original = url;
        String target = normalizeTarget(url);
        logNormalization(component, original, target);

        String scheme = schemeOf(target);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return reject(component, target, scheme,
                    "Only http:// and https:// URLs can be opened in the web browser.");
        }
        return launchValidated(component, original, target, scheme, handlerFor(scheme));
    }

    /**
     * Opens a local HTML file (written by EVO, e.g. a generated Forge diagram)
     * in the system browser via a proper {@code file://} URI.
     */
    public static Outcome launchHtmlFile(String component, Path file) {
        if (file == null) {
            return reject(component, null, "", "No file was provided to open.");
        }
        String path = file.toAbsolutePath().toString();
        if (!Files.isRegularFile(file)) {
            return reject(component, path, "file", "The file does not exist: " + path);
        }
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".html") && !name.endsWith(".htm")) {
            return reject(component, path, "file",
                    "The file is not an HTML document (.html/.htm): " + path);
        }
        String uri = toFileUri(file);
        if (uri == null) {
            return reject(component, path, "file", "Could not build a file URI for: " + path);
        }
        return launchValidated(component, path, uri, "file", handlerFor("text/html"));
    }

    /**
     * Opens an arbitrary target produced by EVO UI (e.g. the URL currently
     * displayed in the internal BrowserPage): http/https URLs and local HTML
     * files are supported; everything else is rejected with a diagnostic.
     */
    public static Outcome launchExternal(String component, String target) {
        String original = target;
        String normalized = normalizeTarget(target);
        logNormalization(component, original, normalized);
        if (normalized == null || normalized.isBlank()) {
            return reject(component, original, "", "No target URL was provided.");
        }

        String scheme = schemeOf(normalized);
        switch (scheme) {
            case "http":
            case "https":
                return launchValidated(component, original, normalized, scheme, handlerFor(scheme));
            case "file":
                return launchFileUri(component, original, normalized);
            case "":
                // Scheme-less: only local HTML files are acceptable web targets.
                String lower = normalized.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".html") || lower.endsWith(".htm")) {
                    return launchHtmlFile(component, Path.of(normalized));
                }
                return reject(component, original, scheme,
                        "The target is not a web URL or a local HTML file: " + scrubForLog(normalized));
            default:
                // Windows drive-letter HTML paths have a single-letter "scheme".
                if (isWindowsDriveHtmlPath(normalized)) {
                    return launchHtmlFile(component, Path.of(normalized));
                }
                return reject(component, original, scheme,
                        "Unsupported URI scheme '" + scheme + "' — only http, https and local "
                                + "HTML files can be opened in the web browser.");
        }
    }

    // =====================================================================
    // Launch internals
    // =====================================================================

    private static Outcome launchFileUri(String component, String original, String fileUri) {
        String lower = fileUri.toLowerCase(Locale.ROOT);
        boolean html = lower.endsWith(".html") || lower.endsWith(".htm");
        String handler = html ? handlerFor("text/html") : "";
        return launchValidated(component, original, fileUri, "file", handler);
    }

    /**
     * Launches a validated target through SWT's {@link Program#launch}. On
     * Linux the resolved OS handler is refused when it is a mail client.
     */
    private static Outcome launchValidated(String component, String original, String target,
                                           String scheme, String osHandler) {
        if (IS_LINUX && isMailHandlerId(osHandler)) {
            return reject(component, original, scheme,
                    "Your system's default handler for " + (osHandler == null ? "this target" : osHandler)
                            + " is a mail client.\n\n"
                            + "No web browser would be opened, so the launch was cancelled. "
                            + "Set your default web browser (for example with: "
                            + "xdg-mime default <browser>.desktop text/html) and try again.");
        }
        long start = System.currentTimeMillis();
        try {
            boolean ok = Program.launch(target);
            long duration = System.currentTimeMillis() - start;
            if (ok) {
                Log.log("[ExternalBrowser] " + component + " | outcome=LAUNCHED | scheme=" + scheme
                        + " | handler=" + (osHandler == null ? "system-default" : osHandler)
                        + " | target=" + scrubForLog(target) + " | durationMs=" + duration);
                return Outcome.LAUNCHED;
            }
            Log.log("[ExternalBrowser] " + component + " | outcome=FAILED | reason=no-handler"
                    + " | scheme=" + scheme + " | target=" + scrubForLog(target)
                    + " | durationMs=" + duration);
            showError("Open in Browser",
                    "No application is associated with " + scheme + " targets on this system.\n\n"
                            + "Set a default web browser and try again.");
            return Outcome.FAILED;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            Log.log("[ExternalBrowser] " + component + " | outcome=FAILED | scheme=" + scheme
                    + " | target=" + scrubForLog(target) + " | exception=" + t.getClass().getSimpleName()
                    + ": " + t.getMessage() + " | durationMs=" + duration);
            showError("Open in Browser", "Could not open the web browser for:\n"
                    + scrubForLog(target) + "\n\n" + t.getMessage());
            return Outcome.FAILED;
        }
    }

    private static Outcome reject(String component, String original, String scheme, String message) {
        Log.log("[ExternalBrowser] " + component + " | outcome=REJECTED | scheme="
                + (scheme == null || scheme.isEmpty() ? "none" : scheme)
                + " | target=" + scrubForLog(original) + " | reason=" + message.replace('\n', ' '));
        showError("Open in Browser", message + "\n\nTarget: " + scrubForLog(original));
        return Outcome.REJECTED;
    }

    /**
     * Resolves the OS default handler for a mime type or scheme on Linux via
     * {@code xdg-mime} (cached briefly). Returns "" when unavailable.
     */
    static String handlerFor(String mimeOrScheme) {
        if (!IS_LINUX || mimeOrScheme == null || mimeOrScheme.isEmpty()) {
            return "";
        }
        CachedHandler cached = HANDLER_CACHE.get(mimeOrScheme);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.timestamp < HANDLER_CACHE_TTL_MS) {
            return cached.handlerId;
        }
        String handler = "";
        try {
            Process p = new ProcessBuilder("xdg-mime", "query", "default", mimeOrScheme).start();
            byte[] out = p.getInputStream().readAllBytes();
            if (p.waitFor(3, TimeUnit.SECONDS)) {
                if (p.exitValue() == 0) {
                    handler = new String(out, StandardCharsets.UTF_8).trim();
                }
            } else {
                p.destroyForcibly();
            }
        } catch (IOException | InterruptedException e) {
            // xdg-mime unavailable: fall through, launch uses the OS default anyway.
        }
        HANDLER_CACHE.put(mimeOrScheme, new CachedHandler(handler, now));
        return handler;
    }

    // =====================================================================
    // Pure helpers (unit-testable, no OS interaction)
    // =====================================================================

    /**
     * Trims and repairs backslash-escaped schemes ({@code http\://} →
     * {@code http://}), a known {@code Properties}-serialization artifact.
     * Returns the original trimmed value when no backslashes are present.
     */
    public static String normalizeTarget(String target) {
        if (target == null) {
            return null;
        }
        String trimmed = target.trim();
        if (trimmed.indexOf('\\') < 0) {
            return trimmed;
        }
        // Only treat the value as a backslash-escaped URL when the escaping
        // pattern is actually present (scheme\:// or \: inside the authority).
        if (trimmed.matches("(?i)^[a-z][a-z0-9+.-]*\\\\://.*")
                || trimmed.matches("(?i)^[a-z][a-z0-9+.-]*\\\\:.*")) {
            return trimmed.replace("\\", "");
        }
        return trimmed;
    }

    /** Lower-case scheme of the target: {@code http}, {@code https}, {@code file} or {@code ""}. */
    public static String schemeOf(String target) {
        if (target == null) {
            return "";
        }
        int idx = target.indexOf(':');
        if (idx <= 0) {
            return "";
        }
        String scheme = target.substring(0, idx).toLowerCase(Locale.ROOT);
        if (!scheme.matches("[a-z][a-z0-9+.-]*")) {
            return "";
        }
        return scheme;
    }

    /** Proper {@code file://} URI for a local file, or null when it cannot be built. */
    public static String toFileUri(Path file) {
        try {
            return file.toAbsolutePath().toUri().toASCIIString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** True when the given xdg desktop-id belongs to a known mail client. */
    public static boolean isMailHandlerId(String desktopId) {
        if (desktopId == null) {
            return false;
        }
        String id = desktopId.toLowerCase(Locale.ROOT);
        return id.contains("thunderbird")
                || id.contains("geary")
                || id.contains("kmail")
                || id.contains("kontact")
                || id.contains("claws")
                || id.contains("mutt")
                || id.contains("evolution")
                || id.contains("mail");
    }

    private static boolean isWindowsDriveHtmlPath(String target) {
        if (target == null || target.length() < 4) {
            return false;
        }
        String lower = target.toLowerCase(Locale.ROOT);
        return Character.isLetter(target.charAt(0))
                && target.charAt(1) == ':'
                && (target.charAt(2) == '\\' || target.charAt(2) == '/')
                && (lower.endsWith(".html") || lower.endsWith(".htm"));
    }

    /**
     * Removes credentials, query strings and fragments from a target before it
     * is written to a log. Never logs userinfo or tokens.
     */
    public static String scrubForLog(String target) {
        if (target == null) {
            return "(null)";
        }
        String value = target;
        if (value.length() > 500) {
            value = value.substring(0, 500) + "…";
        }
        String scheme = schemeOf(value);
        if ("http".equals(scheme) || "https".equals(scheme)) {
            try {
                URI uri = new URI(value);
                String authority = uri.getRawAuthority();
                String path = uri.getRawPath() != null ? uri.getRawPath() : "";
                String clean = scheme + "://" + hostOnly(authority) + path;
                return clean.length() <= 500 ? clean : clean.substring(0, 500) + "…";
            } catch (URISyntaxException e) {
                // Not a valid URI: strip anything after '?'/'#' and any userinfo.
                String clean = value;
                int q = clean.indexOf('?');
                if (q >= 0) clean = clean.substring(0, q);
                int h = clean.indexOf('#');
                if (h >= 0) clean = clean.substring(0, h);
                int at = clean.indexOf('@');
                int schemeEnd = clean.indexOf("://");
                if (at > schemeEnd) {
                    clean = clean.substring(0, schemeEnd + 3) + clean.substring(at + 1);
                }
                return clean;
            }
        }
        // Non-URI (e.g. local paths): strip query/fragment-like suffixes only.
        int q = value.indexOf('?');
        if (q >= 0) value = value.substring(0, q);
        int h = value.indexOf('#');
        if (h >= 0) value = value.substring(0, h);
        return value;
    }

    private static String hostOnly(String rawAuthority) {
        if (rawAuthority == null || rawAuthority.isEmpty()) {
            return "";
        }
        int at = rawAuthority.lastIndexOf('@');
        if (at >= 0) {
            rawAuthority = rawAuthority.substring(at + 1);
        }
        return rawAuthority;
    }

    private static void logNormalization(String component, String original, String normalized) {
        if (original != null && normalized != null && !original.trim().equals(normalized)) {
            Log.log("[ExternalBrowser] " + component + " | normalized backslash-escaped target: '"
                    + scrubForLog(original) + "' -> '" + scrubForLog(normalized) + "'");
        }
    }

    // =====================================================================
    // User feedback (dialog when a shell exists, log otherwise)
    // =====================================================================

    private static void showError(String title, String message) {
        try {
            Display display = Display.getDefault();
            Shell shell = (display != null) ? display.getActiveShell() : null;
            if (shell == null || shell.isDisposed()) {
                Log.log("[ExternalBrowser] " + title + ": " + message.replace('\n', ' '));
                return;
            }
            MessageDialog.openError(shell, title, message);
        } catch (Throwable t) {
            Log.log("[ExternalBrowser] " + title + ": " + message.replace('\n', ' '));
        }
    }

    private static final class CachedHandler {
        final String handlerId;
        final long timestamp;

        CachedHandler(String handlerId, long timestamp) {
            this.handlerId = handlerId;
            this.timestamp = timestamp;
        }
    }
}
