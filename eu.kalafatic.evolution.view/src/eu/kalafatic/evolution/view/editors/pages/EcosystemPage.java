package eu.kalafatic.evolution.view.editors.pages;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.osgi.framework.Bundle;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.ecosystem.EcosystemClient;
import eu.kalafatic.evolution.view.ecosystem.EcosystemSnapshot;
import eu.kalafatic.evolution.view.ecosystem.EcosystemTopologyBuilder;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.util.ExternalBrowserLauncher;

/**
 * Ecosystem — live interactive topology of the whole EVO ecosystem.
 *
 * <p>UPM (Unified Project Management) is the authoritative root: this page is
 * a client of UPM's HTTP API only. All products, services, models, sessions
 * and hardware nodes are derived from UPM state ({@link EcosystemTopologyBuilder});
 * every operation (start/stop/restart, launch profiles) executes through UPM's
 * ops API — no parallel lifecycle or monitoring implementation exists here.</p>
 *
 * <p>Rendering uses the embedded SWT Browser with a vanilla SVG/JavaScript
 * graph (ecosystem.html/css/js). The refresh thread polls UPM and pushes full
 * snapshots; the JavaScript applies them incrementally, preserving viewport,
 * selection, collapse state and manual node positions.</p>
 */
public class EcosystemPage extends AEvoPage {

    private static final long REFRESH_INTERVAL_MS = 3000;

    private final EcosystemClient client = new EcosystemClient();
    private Browser browser;
    private volatile Display display;
    private final AtomicBoolean jsReady = new AtomicBoolean(false);
    private volatile boolean bridgesInstalled;
    private volatile boolean disposed;
    private volatile EcosystemSnapshot latest;
    private Thread refreshThread;
    /**
     * SWT widget state mirrored into thread-safe flags. SWT controls may only be
     * touched on the UI thread, so the background refresh loop must never call
     * {@code browser.isDisposed()} / {@code browser.isVisible()}; it reads these
     * atomics instead. They are maintained by listeners that run on the UI thread.
     */
    private final AtomicBoolean browserDisposed = new AtomicBoolean(false);
    private final AtomicBoolean browserVisible = new AtomicBoolean(true);
    /** Coalesces overlapping refreshes (periodic loop + explicit refreshUI). */
    private final AtomicBoolean refreshInFlight = new AtomicBoolean(false);

    public EcosystemPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        createControl();
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new FillLayout());
        comp.setLayoutData(new GridData(GridData.FILL_BOTH));

        browser = new Browser(comp, SWT.NONE);
        browser.setLayoutData(new GridData(GridData.FILL_BOTH));
        display = comp.getDisplay();

        // Mirror visibility/disposal into thread-safe flags so the refresh worker
        // never touches SWT widgets from its background thread (Invalid thread access).
        browser.addListener(SWT.Show, e -> browserVisible.set(true));
        browser.addListener(SWT.Hide, e -> browserVisible.set(false));
        browser.addListener(SWT.Dispose, e -> {
            browserDisposed.set(true);
            browserVisible.set(false);
        });

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                installBridges();
                jsReady.set(true);
                Display.getDefault().asyncExec(EcosystemPage.this::pushLatest);
            }
        });

        loadHtml();

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        addListener(SWT.Dispose, e -> dispose());
        startRefreshThread();
    }

    private void loadHtml() {
        try {
            Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.view");
            if (bundle != null) {
                URL root = FileLocator.toFileURL(bundle.getEntry("/"));
                browser.setUrl(new URL(root, "ecosystem.html").toString());
                return;
            }
        } catch (Exception e) {
            eu.kalafatic.evolution.controller.log.Log.log(
                    "[Ecosystem] HTML load failed: " + e.getMessage());
        }
        browser.setText("<html><body style='background:#12161f;color:#e7ebf3;font-family:sans-serif'>"
                + "<h3>Ecosystem unavailable</h3><p>The ecosystem page resource could not be loaded.</p>"
                + "</body></html>");
    }

    private void installBridges() {
        if (browser == null || browser.isDisposed() || bridgesInstalled) {
            return;
        }
        try {
            new BrowserFunction(browser, "ecoReady") {
                @Override
                public Object function(Object[] arguments) {
                    jsReady.set(true);
                    Display.getDefault().asyncExec(EcosystemPage.this::pushLatest);
                    return null;
                }
            };
            new BrowserFunction(browser, "ecoAction") {
                @Override
                public Object function(Object[] arguments) {
                    String action = arguments.length > 0 ? String.valueOf(arguments[0]) : "";
                    String nodeId = arguments.length > 1 ? String.valueOf(arguments[1]) : "";
                    String payload = arguments.length > 2 ? String.valueOf(arguments[2]) : "";
                    return dispatchAction(action, nodeId, payload).toString();
                }
            };
            bridgesInstalled = true;
        } catch (Throwable t) {
            eu.kalafatic.evolution.controller.log.Log.log(
                    "[Ecosystem] Bridge installation failed: " + t.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Action dispatch (Java side; returns JSON result for the UI)
    // ------------------------------------------------------------------
    private JSONObject dispatchAction(String action, String nodeId, String payload) {
        JSONObject ok = new JSONObject();
        ok.put("ok", true);
        JSONObject err = new JSONObject();
        err.put("ok", false);
        try {
            switch (action == null ? "" : action) {
                case "refresh" -> {
                    requestRefreshAsync();
                    ok.put("message", "refresh scheduled");
                    return ok;
                }
                case "start" -> {
                    return productOp(nodeId, "run");
                }
                case "stop" -> {
                    return productOp(nodeId, "stop");
                }
                case "restart" -> {
                    return productOp(nodeId, "restart");
                }
                case "openWeb" -> {
                    return openInWeb(nodeId);
                }
                case "openConfig" -> {
                    openUpmUi();
                    ok.put("message", "opened UPM configuration UI");
                    return ok;
                }
                case "showLogs" -> {
                    openUpmUi();
                    ok.put("message", "opened UPM UI (logs view)");
                    return ok;
                }
                case "profileLaunch" -> {
                    return profileOp(nodeId, true);
                }
                case "profileStop" -> {
                    return profileOp(nodeId, false);
                }
                default -> {
                    err.put("error", "Unknown action: " + action);
                    return err;
                }
            }
        } catch (Exception e) {
            err.put("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return err;
        }
    }

    private JSONObject productOp(String nodeId, String type) {
        if (client.getBaseUrl() == null && !client.discover()) {
            return err("UPM is not reachable: " + client.getLastError());
        }
        String projectId = nodeToProjectId(nodeId);
        if (projectId == null) {
            return err("Node " + nodeId + " does not map to a UPM product.");
        }
        JSONObject res = client.productOp(projectId, type, "");
        if (res == null) {
            return err("Operation failed: " + client.getLastError());
        }
        requestRefreshAsync();
        JSONObject out = new JSONObject();
        out.put("ok", true);
        out.put("message", type + " submitted for " + projectId);
        out.put("result", res);
        return out;
    }

    private JSONObject profileOp(String profileId, boolean launch) {
        if (client.getBaseUrl() == null && !client.discover()) {
            return err("UPM is not reachable: " + client.getLastError());
        }
        JSONObject res = launch ? client.profileLaunch(profileId) : client.profileStop(profileId);
        if (res == null) {
            return err("Profile operation failed: " + client.getLastError());
        }
        requestRefreshAsync();
        JSONObject out = new JSONObject();
        out.put("ok", true);
        out.put("message", (launch ? "launch" : "stop") + " submitted for profile " + profileId);
        out.put("result", res);
        return out;
    }

    /** Refreshes off the UI thread (never block SWT on HTTP). */
    private void requestRefreshAsync() {
        Thread t = new Thread(this::refreshNow, "evo-ecosystem-refresh-once");
        t.setDaemon(true);
        t.start();
    }

    private JSONObject openInWeb(String nodeId) {
        EcosystemSnapshot s = latest;
        String url = null;
        if (s != null && nodeId != null) {
            var node = s.node(nodeId);
            if (node != null && node.getEndpoint() != null
                    && node.getEndpoint().startsWith("http")) {
                url = node.getEndpoint();
            }
        }
        if (url == null) {
            if (client.getBaseUrl() == null && !client.discover()) {
                return err("No endpoint and UPM unreachable: " + client.getLastError());
            }
            url = client.getBaseUrl();
        }
        ExternalBrowserLauncher.Outcome outcome =
                ExternalBrowserLauncher.launchWebUrl("EcosystemPage", url);
        JSONObject out = new JSONObject();
        out.put("ok", outcome == ExternalBrowserLauncher.Outcome.LAUNCHED);
        out.put("message", "browser launch: " + outcome + " → " + url);
        return out;
    }

    private void openUpmUi() {
        if (client.getBaseUrl() == null && !client.discover()) {
            return;
        }
        if (client.getBaseUrl() != null) {
            ExternalBrowserLauncher.launchWebUrl("EcosystemPage", client.getBaseUrl());
        }
    }

    private static JSONObject err(String message) {
        JSONObject e = new JSONObject();
        e.put("ok", false);
        e.put("error", message);
        return e;
    }

    /** Maps a node id (product:<id>) back to the UPM project id. */
    private String nodeToProjectId(String nodeId) {
        if (nodeId == null) return null;
        if (nodeId.startsWith("product:")) {
            return nodeId.substring("product:".length());
        }
        EcosystemSnapshot s = latest;
        if (s != null) {
            var node = s.node(nodeId);
            if (node != null && node.getProductId() != null
                    && !node.getProductId().isBlank()) {
                return node.getProductId();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Refresh loop
    // ------------------------------------------------------------------
    private void startRefreshThread() {
        if (disposed || (refreshThread != null && refreshThread.isAlive())) {
            return; // never start a second refresh loop for the same page
        }
        refreshThread = new Thread(this::refreshLoop, "evo-ecosystem-refresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void refreshLoop() {
        // Initial discovery + first snapshot.
        if (!client.discover()) {
            uiExec(() -> pushConnection("err", client.getLastError()));
        } else {
            refreshNow();
        }
        while (!disposed) {
            try {
                Thread.sleep(REFRESH_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (disposed || browserDisposed.get()) return;
            if (!browserVisible.get()) continue; // tab not shown — keep sleeping
            refreshNow();
        }
    }

    /** Marshals a UI update onto the SWT thread, tolerating a disposed display. */
    private void uiExec(Runnable update) {
        Display d = display;
        if (d == null || d.isDisposed()) return;
        try {
            d.asyncExec(() -> {
                if (!disposed) {
                    update.run();
                }
            });
        } catch (org.eclipse.swt.SWTException e) {
            // Display disposed between the check and the async post: nothing to update.
            eu.kalafatic.evolution.controller.log.Log.log(
                    "[Ecosystem] UI update skipped (display disposed): " + e.getMessage());
        }
    }

    /** Collects the snapshot on the refresh thread and pushes to the UI thread. */
    private void refreshNow() {
        if (disposed) return;
        if (!refreshInFlight.compareAndSet(false, true)) {
            return; // a refresh is already running; avoid duplicate concurrent pollers
        }
        try {
            if (client.getBaseUrl() == null && !client.discover()) {
                uiExec(() -> pushConnection("err", client.getLastError()));
                return;
            }
            JSONObject state = client.get("/api/state");
            JSONObject ports = client.get("/api/ports/inventory");
            JSONObject metrics = client.get("/api/metrics/current");
            JSONObject enrModels = client.get("/api/enr/models");
            JSONObject enrInference = client.get("/api/enr/inference");
            EcosystemSnapshot snapshot = EcosystemTopologyBuilder.build(
                    state, ports, metrics, enrModels, enrInference);
            latest = snapshot;
            uiExec(() -> {
                pushConnection("ok", null);
                pushSnapshot(snapshot);
            });
        } finally {
            refreshInFlight.set(false);
        }
    }

    private void pushConnection(String state, String message) {
        if (browser == null || browser.isDisposed()) return;
        String msg = message == null ? "" : message.replace("'", "\\'");
        browser.execute("if(window.eco){window.eco.setConnection('" + state + "','" + msg + "');}");
    }

    private void pushLatest() {
        if (latest != null) {
            pushSnapshot(latest);
        }
    }

    private void pushSnapshot(EcosystemSnapshot snapshot) {
        if (browser == null || browser.isDisposed() || !jsReady.get()) {
            return;
        }
        String json = snapshot.toJson().replace("'", "\\'");
        try {
            browser.execute("if(window.eco){window.eco.applySnapshot(JSON.parse('" + json + "'));}");
        } catch (Exception e) {
            eu.kalafatic.evolution.controller.log.Log.log(
                    "[Ecosystem] Snapshot push failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    @Override
    protected void refreshUI() {
        if (disposed) return;
        // Never block the SWT thread on HTTP: run a one-shot background refresh.
        Thread t = new Thread(this::refreshNow, "evo-ecosystem-refresh-once");
        t.setDaemon(true);
        t.start();
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        browserDisposed.set(true);
        browserVisible.set(false);
        if (refreshThread != null) {
            refreshThread.interrupt();
            refreshThread = null;
        }
        super.dispose();
    }
}
