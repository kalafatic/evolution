package eu.kalafatic.evolution.view.editors.pages;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.ServerOperation;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.teip.dto.TeipCapabilityDescriptor;
import eu.kalafatic.evolution.controller.teip.dto.TeipEventsResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionResult;
import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionStatusResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipHealthResponse;
import eu.kalafatic.evolution.controller.teip.dto.TeipJsonConverter;
import eu.kalafatic.evolution.controller.teip.mcp.TrystMcpClient;
import eu.kalafatic.evolution.controller.teip.service.TeipExecutionManager;
import eu.kalafatic.evolution.controller.teip.store.TeipEventStore;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * Workbench page embedding the TEIP Protocol administration and operations dashboard.
 * Extends {@link AEvoPage} and wires directly into EVO Java services without unnecessary localhost HTTP roundtrips.
 */
public class TeipProtocolPage extends AEvoPage {

    private Browser browser;
    private final AtomicBoolean bridgesInstalled = new AtomicBoolean(false);
    private BrowserFunction teipActionFunction;
    private BrowserFunction teipLogFunction;

    public TeipProtocolPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        setLayout(new FillLayout());
        createControl();
    }

    private void createControl() {
        Composite content = toolkit.createComposite(this);
        content.setLayout(new GridLayout(1, false));
        this.setContent(content);

        this.browser = new Browser(content, SWT.NONE);
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
            @Override
            public void completed(org.eclipse.swt.browser.ProgressEvent event) {
                setupJavaScriptBridges();
                Display.getDefault().asyncExec(() -> {
                    refreshUI();
                });
            }
        });

        setupJavaScriptBridges();
        initBrowser();
    }

    private void initBrowser() {
        try {
            org.osgi.framework.Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.controller");
            if (bundle != null) {
                java.net.URL entry = bundle.getEntry("eu/kalafatic/evolution/controller/orchestration/teip.html");
                if (entry == null) {
                    entry = bundle.getEntry("src/eu/kalafatic/evolution/controller/orchestration/teip.html");
                }

                if (entry != null) {
                    java.net.URL teipUrl = FileLocator.toFileURL(entry);
                    browser.setUrl(teipUrl.toString());
                } else {
                    browser.setText("<html><body><h1>teip.html not found in bundle</h1></body></html>");
                }
            } else {
                browser.setText("<html><body><h1>Controller bundle not found</h1></body></html>");
            }
        } catch (Exception e) {
            browser.setText("<html><body><h1>Error loading teip.html: " + e.getMessage() + "</h1></body></html>");
        }
    }

    private void setupJavaScriptBridges() {
        if (browser == null || browser.isDisposed() || !bridgesInstalled.compareAndSet(false, true)) {
            return;
        }

        try {
            teipActionFunction = new BrowserFunction(browser, "teipAction") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments == null || arguments.length == 0) {
                        return "{}";
                    }
                    String action = String.valueOf(arguments[0]);
                    String payloadStr = arguments.length > 1 && arguments[1] != null ? String.valueOf(arguments[1]) : "{}";
                    JSONObject payload = new JSONObject(payloadStr);

                    return handleBridgeAction(action, payload);
                }
            };

            teipLogFunction = new BrowserFunction(browser, "teipLog") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments != null && arguments.length > 0) {
                        System.out.println("[TEIP_JS] " + arguments[0]);
                    }
                    return null;
                }
            };
        } catch (Throwable t) {
            System.err.println("[TEIP_PAGE] Failed to register JavaScript bridge: " + t.getMessage());
        }
    }

    private String handleBridgeAction(String action, JSONObject payload) {
        JSONObject res = new JSONObject();
        try {
            switch (action) {
                case "getOverview": {
                    TeipHealthResponse health = TeipExecutionManager.getInstance().checkHealth();
                    TeipCapabilityDescriptor cap = TeipExecutionManager.getInstance().getCapabilities();
                    List<TeipExecutionStatusResponse> allExecs = TeipExecutionManager.getInstance().listAllExecutions();

                    int activeCount = 0;
                    for (TeipExecutionStatusResponse e : allExecs) {
                        if (e.getStatus() != null && !e.getStatus().isTerminal()) {
                            activeCount++;
                        }
                    }

                    int activeSessions = 0;
                    for (SessionContainer sc : SessionManager.getInstance().getAllSessions()) {
                        if (sc.getSessionId().startsWith("evo-teip-")) {
                            activeSessions++;
                        }
                    }

                    res.put("health", new JSONObject(TeipJsonConverter.toJson(health)));
                    res.put("capabilities", new JSONObject(TeipJsonConverter.toJson(cap)));
                    res.put("totalExecutions", allExecs.size());
                    res.put("activeExecutions", activeCount);
                    res.put("activeSessions", activeSessions);
                    res.put("teipEnabled", TeipExecutionManager.getInstance().isTeipEnabled());
                    res.put("trystMcpUrl", System.getProperty("evo.tryst.mcp.url", "http://localhost:3000/mcp"));

                    int port = 28080;
                    if (orchestrator != null && orchestrator.getServerSettings() != null) {
                        port = orchestrator.getServerSettings().getPort();
                    }
                    res.put("serverPort", port);

                    // Add recent events from all active executions
                    JSONArray recentEvents = new JSONArray();
                    for (TeipExecutionStatusResponse ex : allExecs) {
                        TeipEventsResponse evResp = TeipEventStore.getInstance().getEvents(ex.getExecutionId(), 0, 10);
                        if (evResp != null && evResp.getEvents() != null) {
                            for (var ev : evResp.getEvents()) {
                                recentEvents.put(new JSONObject(TeipJsonConverter.toJson(ev)));
                            }
                        }
                    }
                    res.put("recentEvents", recentEvents);
                    break;
                }

                case "getCapabilities": {
                    TeipCapabilityDescriptor cap = TeipExecutionManager.getInstance().getCapabilities();
                    res.put("capabilities", new JSONObject(TeipJsonConverter.toJson(cap)));
                    break;
                }

                case "getSessions": {
                    JSONArray arr = new JSONArray();
                    for (SessionContainer sc : SessionManager.getInstance().getAllSessions()) {
                        if (sc.getSessionId().startsWith("evo-teip-")) {
                            JSONObject sObj = new JSONObject();
                            sObj.put("sessionId", sc.getSessionId());
                            sObj.put("type", "TEIP_SESSION");
                            sObj.put("owner", "TRYST");
                            sObj.put("operationCount", sc.getOperations().size());
                            arr.put(sObj);
                        }
                    }
                    res.put("sessions", arr);
                    break;
                }

                case "inspectSession": {
                    String sid = payload.optString("sessionId", "");
                    SessionContainer sc = SessionManager.getInstance().getSession(sid);
                    if (sc != null) {
                        res.put("sessionId", sc.getSessionId());
                        JSONArray opsArr = new JSONArray();
                        for (ServerOperation op : sc.getOperations()) {
                            opsArr.put(op.toJsonObject());
                        }
                        res.put("operations", opsArr);
                        if (sc.getIterationManager() != null) {
                            res.put("hasIterationManager", true);
                        }
                    } else {
                        res.put("error", "Session not found: " + sid);
                    }
                    break;
                }

                case "getExecutions": {
                    List<TeipExecutionStatusResponse> list = TeipExecutionManager.getInstance().listAllExecutions();
                    JSONArray arr = new JSONArray();
                    for (TeipExecutionStatusResponse item : list) {
                        arr.put(new JSONObject(TeipJsonConverter.toJson(item)));
                    }
                    res.put("executions", arr);
                    break;
                }

                case "inspectExecution": {
                    String execId = payload.optString("executionId", "");
                    TeipExecutionStatusResponse status = TeipExecutionManager.getInstance().getStatus(execId);
                    if (status != null) {
                        res.put("status", new JSONObject(TeipJsonConverter.toJson(status)));
                        TeipExecutionResult result = TeipExecutionManager.getInstance().getResult(execId);
                        if (result != null) {
                            res.put("result", new JSONObject(TeipJsonConverter.toJson(result)));
                        }
                    } else {
                        res.put("error", "Execution not found: " + execId);
                    }
                    break;
                }

                case "cancelExecution": {
                    String execId = payload.optString("executionId", "");
                    String reason = payload.optString("reason", "Cancelled by user via TEIP Protocol Page");
                    boolean cancelled = TeipExecutionManager.getInstance().cancelExecution(execId, reason);
                    res.put("cancelled", cancelled);
                    res.put("executionId", execId);
                    break;
                }

                case "getEvents": {
                    String execId = payload.optString("executionId", "");
                    long afterSeq = payload.optLong("afterSequence", 0);
                    int limit = payload.optInt("limit", 50);

                    JSONArray arr = new JSONArray();
                    if (!execId.isEmpty()) {
                        TeipEventsResponse evResp = TeipEventStore.getInstance().getEvents(execId, afterSeq, limit);
                        if (evResp != null && evResp.getEvents() != null) {
                            for (var ev : evResp.getEvents()) {
                                arr.put(new JSONObject(TeipJsonConverter.toJson(ev)));
                            }
                        }
                    } else {
                        // Gather across all executions
                        for (TeipExecutionStatusResponse ex : TeipExecutionManager.getInstance().listAllExecutions()) {
                            TeipEventsResponse evResp = TeipEventStore.getInstance().getEvents(ex.getExecutionId(), afterSeq, limit);
                            if (evResp != null && evResp.getEvents() != null) {
                                for (var ev : evResp.getEvents()) {
                                    arr.put(new JSONObject(TeipJsonConverter.toJson(ev)));
                                }
                            }
                        }
                    }
                    res.put("events", arr);
                    break;
                }

                case "getAnalytics": {
                    List<TeipExecutionStatusResponse> list = TeipExecutionManager.getInstance().listAllExecutions();
                    int completed = 0, failed = 0, cancelled = 0;
                    long totalDurationSec = 0;
                    int countedDuration = 0;

                    for (TeipExecutionStatusResponse item : list) {
                        if (item.getStatus() != null) {
                            switch (item.getStatus()) {
                                case COMPLETED:
                                    completed++;
                                    if (item.getCreatedAt() != null && item.getCompletedAt() != null) {
                                        try {
                                            long start = Instant.parse(item.getCreatedAt()).getEpochSecond();
                                            long end = Instant.parse(item.getCompletedAt()).getEpochSecond();
                                            if (end >= start) {
                                                totalDurationSec += (end - start);
                                                countedDuration++;
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                    break;
                                case FAILED:
                                    failed++;
                                    break;
                                case CANCELLED:
                                    cancelled++;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }

                    res.put("completedCount", completed);
                    res.put("failedCount", failed);
                    res.put("cancelledCount", cancelled);
                    res.put("avgDurationSeconds", countedDuration > 0 ? (totalDurationSec / countedDuration) : 0);
                    break;
                }

                case "getConfig": {
                    res.put("teipEnabled", TeipExecutionManager.getInstance().isTeipEnabled());
                    res.put("trystMcpUrl", System.getProperty("evo.tryst.mcp.url", "http://localhost:3000/mcp"));
                    String token = System.getProperty("evo.teip.auth.token");
                    res.put("hasAuthToken", token != null && !token.trim().isEmpty());
                    String timeout = System.getProperty("evo.tryst.timeout.seconds", "30");
                    res.put("timeoutSeconds", Integer.parseInt(timeout));
                    break;
                }

                case "saveConfig": {
                    boolean enabled = payload.optBoolean("teipEnabled", true);
                    TeipExecutionManager.getInstance().setTeipEnabled(enabled);
                    System.setProperty("evo.teip.enabled", String.valueOf(enabled));

                    if (payload.has("trystMcpUrl")) {
                        System.setProperty("evo.tryst.mcp.url", payload.getString("trystMcpUrl").trim());
                    }
                    if (payload.has("authToken") && !payload.getString("authToken").startsWith("••")) {
                        String token = payload.getString("authToken").trim();
                        if (!token.isEmpty()) {
                            System.setProperty("evo.teip.auth.token", token);
                        }
                    }
                    if (payload.has("timeoutSeconds")) {
                        System.setProperty("evo.tryst.timeout.seconds", String.valueOf(payload.getInt("timeoutSeconds")));
                    }
                    res.put("success", true);
                    break;
                }

                case "testConnection": {
                    String url = payload.optString("trystMcpUrl", System.getProperty("evo.tryst.mcp.url", "http://localhost:3000/mcp"));
                    String token = System.getProperty("evo.teip.auth.token", "");
                    try {
                        TrystMcpClient client = new TrystMcpClient(url, token, List.of("*"));
                        JSONObject initResult = client.initialize();
                        res.put("connected", true);
                        res.put("serverInfo", initResult);
                    } catch (Exception ex) {
                        res.put("connected", false);
                        res.put("error", ex.getMessage() != null ? ex.getMessage() : "Connection failed");
                    }
                    break;
                }

                case "runDiagnostics": {
                    JSONArray probes = new JSONArray();

                    // Probe 1: Local TEIP Health
                    try {
                        TeipHealthResponse health = TeipExecutionManager.getInstance().checkHealth();
                        probes.put(new JSONObject()
                            .put("name", "Local EVO TEIP Health")
                            .put("status", health.isReady() ? "PASS" : "WARN")
                            .put("message", "Status: " + health.getStatus() + ", App: " + health.getApplication()));
                    } catch (Exception e) {
                        probes.put(new JSONObject().put("name", "Local EVO TEIP Health").put("status", "FAIL").put("message", e.getMessage()));
                    }

                    // Probe 2: Capabilities Check
                    try {
                        TeipCapabilityDescriptor cap = TeipExecutionManager.getInstance().getCapabilities();
                        probes.put(new JSONObject()
                            .put("name", "Capabilities Dynamic Catalog")
                            .put("status", "PASS")
                            .put("message", "Modes: " + cap.getSupportedModes().size() + ", Agents: " + cap.getAgents().size()));
                    } catch (Exception e) {
                        probes.put(new JSONObject().put("name", "Capabilities Dynamic Catalog").put("status", "FAIL").put("message", e.getMessage()));
                    }

                    // Probe 3: TRYST MCP Handshake
                    String url = System.getProperty("evo.tryst.mcp.url", "http://localhost:3000/mcp");
                    String token = System.getProperty("evo.teip.auth.token", "");
                    try {
                        TrystMcpClient client = new TrystMcpClient(url, token, List.of("*"));
                        JSONObject init = client.initialize();
                        probes.put(new JSONObject()
                            .put("name", "TRYST MCP Handshake")
                            .put("status", "PASS")
                            .put("message", "Handshake succeeded: " + init.optString("serverVersion", "OK")));
                    } catch (Exception e) {
                        probes.put(new JSONObject()
                            .put("name", "TRYST MCP Handshake")
                            .put("status", "WARN")
                            .put("message", "Endpoint unreachable (" + url + "): " + e.getMessage()));
                    }

                    // Probe 4: Event Store Integrity
                    try {
                        int count = TeipExecutionManager.getInstance().listAllExecutions().size();
                        probes.put(new JSONObject()
                            .put("name", "TeipEventStore & Idempotency Cache")
                            .put("status", "PASS")
                            .put("message", "Tracking " + count + " active/historical executions"));
                    } catch (Exception e) {
                        probes.put(new JSONObject().put("name", "TeipEventStore").put("status", "FAIL").put("message", e.getMessage()));
                    }

                    res.put("probes", probes);
                    break;
                }

                default:
                    res.put("error", "Unknown action: " + action);
                    break;
            }
        } catch (Exception e) {
            res.put("error", e.getMessage());
        }
        return res.toString();
    }

    @Override
    public void refreshUI() {
        if (browser == null || browser.isDisposed()) return;
        browser.execute("if(typeof refreshActiveTab === 'function') { refreshActiveTab(); }");
    }

    @Override
    public void dispose() {
        if (teipActionFunction != null) {
            teipActionFunction.dispose();
            teipActionFunction = null;
        }
        if (teipLogFunction != null) {
            teipLogFunction.dispose();
            teipLogFunction = null;
        }
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
            browser = null;
        }
        super.dispose();
    }
}
