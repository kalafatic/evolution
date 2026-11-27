package eu.kalafatic.evolution.view.workbench;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.ServerEntry;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.view.util.ExternalBrowserLauncher;

/**
 * Desktop-facing launchers shared by the menu bar, context menus and the F1
 * help handling (Family UX Standard v1.0):
 * <ul>
 * <li>{@link #openTerminal()} — system terminal in the workspace / selected
 * project directory,</li>
 * <li>{@link #openWebInterface()} — EVO web portal in the system browser,
 * using the live primary HTTP port (never hardcoded),</li>
 * <li>{@link #resolvePrimaryPort()} — authoritative resolution of the primary
 * EVO HTTP port shared with {@link HelpLauncher}.</li>
 * </ul>
 */
public final class EvoDesktopLauncher {

    /** Terminal emulators probed, in priority order (family convention). */
    private static final List<String> TERMINAL_EMULATORS = List.of(
            "x-terminal-emulator", "gnome-terminal", "konsole", "xterm");

    private EvoDesktopLauncher() {
    }

    /**
     * Opens a system terminal. The working directory is the selected
     * project/folder directory when determinable, otherwise the workspace
     * root, otherwise {@code user.home}.
     */
    public static void openTerminal() {
        String directory = resolveSelectionDirectory();
        if (directory == null || directory.isBlank()) {
            directory = resolveWorkspaceDirectory();
        }
        openTerminalIn(directory);
    }

    /**
     * Opens a system terminal in the given directory (fallback: user.home).
     * Shows a clear error dialog when no terminal emulator is available or
     * the launch fails.
     */
    public static void openTerminalIn(String directory) {
        String dir = (directory == null || directory.isBlank())
                ? System.getProperty("user.home", ".") : directory;

        String[] command = terminalCommand(dir);
        if (command == null) {
            showError("Open Terminal",
                    "No supported terminal emulator found on this system "
                            + "(tried x-terminal-emulator, gnome-terminal, konsole, xterm).");
            return;
        }
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            Log.log("[EvoDesktopLauncher] Failed to open terminal in " + dir + ": " + e.getMessage());
            showError("Open Terminal", "Failed to open terminal:\n\n" + e.getMessage());
        }
    }

    /**
     * Opens the EVO web interface (Intelligence Portal) in the system browser.
     * The URL is resolved from the authoritative server infrastructure: the
     * live {@link ServerEntry} for the primary HTTP server (actual bind host
     * and actually allocated port — never hardcoded). When the primary HTTP
     * server is not running, a clear message dialog is shown instead of
     * opening an invalid URL.
     */
    public static void openWebInterface() {
        ServerManager serverManager = ServerManager.getInstance();
        ServerEntry entry = serverManager.getServerEntryByServiceId(
                PortRegistry.EVO_HTTP_PRIMARY.getServiceId());
        int port = entry != null ? entry.getPort() : resolvePrimaryPort();
        if (!serverManager.isRunning(port)) {
            String state = entry != null ? " (state: " + entry.getState() + ")" : "";
            showMessage("Open in Web",
                    "The EVO web server is not running on port " + port + state + ".\n\n"
                            + "It starts automatically with the workbench. If it failed to start, "
                            + "check the Server page or the log console for server startup errors.");
            return;
        }
        String url = primaryWebUrl(entry, port);
        launchBrowser(url);
    }

    /**
     * Opens the ENR Web Control Center (authoritative port 28880) in the system
     * browser. The URL is resolved from the authoritative {@link PortRegistry} ENR Web
     * allocation (never hardcoded). When the ENR Web listener is not running, a clear
     * message dialog is shown instead of opening an invalid URL.
     */
    public static void openEnrWeb() {
        int port = resolveEnrWebPort();
        if (!isReachable("127.0.0.1", port)) {
            showMessage("Open ENR Web",
                    "The ENR Web Control Center is not running on http://127.0.0.1:" + port
                            + ".\n\nIt starts automatically with the ENR runtime. If it failed "
                            + "to start, check the Server page or the log console for ENR Web "
                            + "startup errors.");
            return;
        }
        launchBrowser("http://127.0.0.1:" + port + "/");
    }

    /**
     * Resolves the actual ENR Web port: the authoritative port allocation
     * (PortAllocationManager) first, then the PortRegistry default (28880).
     */
    public static int resolveEnrWebPort() {
        int port = PortAllocationManager.getInstance()
                .getPort(PortRegistry.EVO_ENR_WEB.getServiceId());
        if (port <= 0) {
            port = PortRegistry.EVO_ENR_WEB.getDefaultPort();
        }
        return port;
    }

    /** Lightweight TCP reachability probe (the ENR Web listener is not an EVO-managed server). */
    private static boolean isReachable(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 1500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Builds the primary web interface URL from the live {@link ServerEntry}
     * endpoint (configured host + actually allocated port). Falls back to the
     * resolved primary port when no entry is available. Never hardcoded.
     */
    public static String primaryWebUrl(ServerEntry entry, int resolvedPort) {
        String base = null;
        if (entry != null && entry.getEndpointUrl() != null && !entry.getEndpointUrl().isBlank()) {
            base = entry.getEndpointUrl();
        }
        if (base == null || base.isBlank()) {
            base = "http://127.0.0.1:" + resolvedPort + "/";
        }
        return base.endsWith("/") ? base + "dashboard.html" : base;
    }

    /**
     * Resolves the actual primary EVO HTTP port: the authoritative port
     * allocation (PortAllocationManager) first, then the live ServerManager
     * state, then the PortRegistry default. Never hardcoded.
     */
    public static int resolvePrimaryPort() {
        int port = PortAllocationManager.getInstance()
                .getPort(PortRegistry.EVO_HTTP_PRIMARY.getServiceId());
        if (port <= 0) {
            port = ServerManager.getInstance().getPort();
        }
        if (port <= 0) {
            port = PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort();
        }
        return port;
    }

    /** Opens the given URL in the system browser (shared with HelpLauncher). */
    public static void launchBrowser(String url) {
        ExternalBrowserLauncher.launchWebUrl("EvoDesktopLauncher", url);
    }

    // ---------------------------------------------------------------
    // Directory resolution
    // ---------------------------------------------------------------

    /**
     * Resolves a directory from the current workbench selection: the parent
     * directory of a selected file, the location of a selected
     * folder/project/resource, or a selected {@link File}. Returns null when
     * no real filesystem path can be derived.
     */
    public static String resolveSelectionDirectory() {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null || window.getSelectionService() == null) {
                return null;
            }
            ISelection selection = window.getSelectionService().getSelection();
            if (!(selection instanceof IStructuredSelection)) {
                return null;
            }
            Object first = ((IStructuredSelection) selection).getFirstElement();
            return directoryFor(first);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Derives a real filesystem directory from a selection element, if any. */
    public static String directoryFor(Object element) {
        if (element == null) {
            return null;
        }
        if (element instanceof File) {
            File file = (File) element;
            File dir = file.isDirectory() ? file : file.getParentFile();
            return dir != null ? dir.getAbsolutePath() : null;
        }
        IResource resource = toResource(element);
        if (resource != null) {
            return resourceDirectory(resource);
        }
        return null;
    }

    /** Derives the exact filesystem path of a selection element, if any. */
    public static String pathFor(Object element) {
        if (element == null) {
            return null;
        }
        if (element instanceof File) {
            return ((File) element).getAbsolutePath();
        }
        IResource resource = toResource(element);
        if (resource != null) {
            IPath location = resource.getLocation();
            return location != null ? location.toOSString() : null;
        }
        return null;
    }

    private static IResource toResource(Object element) {
        if (element instanceof IResource) {
            return (IResource) element;
        }
        if (element instanceof IAdaptable) {
            Object adapter = ((IAdaptable) element).getAdapter(IResource.class);
            if (adapter instanceof IResource) {
                return (IResource) adapter;
            }
        }
        return null;
    }

    private static String resourceDirectory(IResource resource) {
        if (resource instanceof IFile) {
            IPath location = resource.getLocation();
            if (location == null) {
                return null;
            }
            File parent = location.toFile().getParentFile();
            return parent != null ? parent.getAbsolutePath() : null;
        }
        if (resource instanceof IProject || resource.getType() == IResource.FOLDER
                || resource.getType() == IResource.PROJECT || resource.getType() == IResource.ROOT) {
            IPath location = resource.getLocation();
            return location != null ? location.toFile().getAbsolutePath() : null;
        }
        return null;
    }

    /** Resolves the Eclipse workspace root directory, or null. */
    public static String resolveWorkspaceDirectory() {
        try {
            File root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
            if (root.isDirectory() && Files.isDirectory(root.toPath())) {
                return root.getAbsolutePath();
            }
        } catch (Throwable t) {
            Log.log("[EvoDesktopLauncher] Could not resolve workspace directory: " + t.getMessage());
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Terminal probing (family convention)
    // ---------------------------------------------------------------

    private static String[] terminalCommand(String directory) {
        if (hasCommand("x-terminal-emulator")) {
            return new String[]{"x-terminal-emulator", "--working-directory=" + directory};
        }
        if (hasCommand("gnome-terminal")) {
            return new String[]{"gnome-terminal", "--working-directory=" + directory};
        }
        if (hasCommand("konsole")) {
            return new String[]{"konsole", "--workdir", directory};
        }
        if (hasCommand("xterm")) {
            return new String[]{"xterm", "-e", "cd " + shellQuote(directory) + " && $SHELL"};
        }
        return null;
    }

    private static boolean hasCommand(String command) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(":")) {
            if (Files.isExecutable(Path.of(dir, command))) {
                return true;
            }
        }
        return false;
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    // ---------------------------------------------------------------
    // User feedback
    // ---------------------------------------------------------------

    private static void showError(String title, String message) {
        showMessage(title, message, true);
    }

    private static void showMessage(String title, String message) {
        showMessage(title, message, false);
    }

    private static void showMessage(String title, String message, boolean error) {
        try {
            Display display = Display.getDefault();
            Shell shell = (display != null) ? display.getActiveShell() : null;
            if (shell == null || shell.isDisposed()) {
                Log.log("[EvoDesktopLauncher] " + title + ": " + message);
                return;
            }
            if (error) {
                MessageDialog.openError(shell, title, message);
            } else {
                MessageDialog.openInformation(shell, title, message);
            }
        } catch (Throwable t) {
            Log.log("[EvoDesktopLauncher] " + title + ": " + message);
        }
    }
}
