package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class ForgePage extends AEvoPage {
    private Browser browser;
    private boolean isJsReady = false;
    private boolean bridgesInstalled = false;
    private boolean forgeInitialized = false;
    /** Last session id propagated to the JS bridge; used to avoid redundant updates. */
    private volatile String lastSyncedSessionId = null;

    public ForgePage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        ForgeSessionManager.getInstance().initialize(orchestrator);
        createControl();
    }

    private void createControl() {
        Composite content = toolkit.createComposite(this);
        content.setLayout(new GridLayout(1, false));
        this.setContent(content);

        // Live hardware monitoring (GPU / CPU / RAM + historical charts) in one
        // expandable/collapsible group. No fixed height: when collapsed it
        // occupies a single compact summary row and the Forge browser below
        // receives the released vertical space.
        eu.kalafatic.evolution.view.monitor.HardwareMonitorGroup hardwareMonitor =
                new eu.kalafatic.evolution.view.monitor.HardwareMonitorGroup(content);
        hardwareMonitor.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // GPU diagnostics (CUDA backend + real Forge GPU training verification),
        // shared with the AI Commander Forge panel and the Settings Hardware panel.
        eu.kalafatic.evolution.view.editors.pages.aichat.GpuDiagnosticsGroup gpuDiagnostics =
                new eu.kalafatic.evolution.view.editors.pages.aichat.GpuDiagnosticsGroup(
                        toolkit, content, editor, orchestrator);

        this.browser = new Browser(content, SWT.NONE);
        // The browser is the primary workspace: it grabs all remaining space.
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
            @Override
            public void completed(org.eclipse.swt.browser.ProgressEvent event) {
                setupJavaScriptBridges();
                isJsReady = true;
                Display.getDefault().asyncExec(() -> {
                    refreshUI();
                });
            }
        });

        initBrowser();

        // Propagate AI Commander session selection changes into the Forge page. The AI
        // Commander owns the current session selection; a lightweight periodic re-resolve
        // keeps the Forge Models page and Training Monitor bound to the same session
        // without requiring a duplicate manual selection. refreshUI() no-ops when the
        // resolved session is unchanged.
        Runnable sessionSyncTimer = new Runnable() {
            @Override
            public void run() {
                Display.getDefault().asyncExec(() -> {
                    if (browser == null || browser.isDisposed() || !isJsReady) {
                        if (browser != null && !browser.isDisposed()) {
                            Display.getDefault().timerExec(1000, this);
                        }
                        return;
                    }
                    refreshUI();
                    Display.getDefault().timerExec(1000, this);
                });
            }
        };
        Display.getDefault().asyncExec(() -> {
            if (!browser.isDisposed()) {
                Display.getDefault().timerExec(1000, sessionSyncTimer);
            }
        });
    }

    private void initBrowser() {
        try {
            org.osgi.framework.Bundle bundle = org.eclipse.core.runtime.Platform.getBundle("eu.kalafatic.evolution.controller");
            if (bundle != null) {
                java.net.URL entry = bundle.getEntry("eu/kalafatic/evolution/controller/orchestration/forge.html");
                if (entry == null) {
                    // Fallback for development where it might be in src
                    entry = bundle.getEntry("src/eu/kalafatic/evolution/controller/orchestration/forge.html");
                }
                
                if (entry != null) {
                    java.net.URL forgeUrl = org.eclipse.core.runtime.FileLocator.toFileURL(entry);
                    browser.setUrl(forgeUrl.toString());
                } else {
                     browser.setText("<html><body><h1>forge.html not found in bundle</h1></body></html>");
                }
            } else {
                 browser.setText("<html><body><h1>Controller bundle not found</h1></body></html>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            browser.setText("<html><body><h1>Error loading forge.html</h1></body></html>");
        }
    }

    private void setupJavaScriptBridges() {
        if (browser == null || browser.isDisposed() || bridgesInstalled) return;

        try {
            new BrowserFunction(browser, "forgeAction") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length >= 1) {
                        String action = (String) arguments[0];
                        handleForgeAction(action, arguments.length > 1 ? arguments[1] : null);
                    }
                    return null;
                }
            };

            new BrowserFunction(browser, "browseDirectory") {
                @Override
                public Object function(Object[] arguments) {
                    if (browser.isDisposed()) return null;
                    final String[] result = new String[1];
                    Display.getDefault().syncExec(() -> {
                        org.eclipse.swt.widgets.DirectoryDialog dialog = new org.eclipse.swt.widgets.DirectoryDialog(browser.getShell());
                        dialog.setText("Select Data Source Directory");
                        eu.kalafatic.utils.application.FileUtils.configureDialogFilterPath(dialog, null);
                        result[0] = dialog.open();
                    });
                    return result[0];
                }
            };

            new BrowserFunction(browser, "browseFile") {
                @Override
                public Object function(Object[] arguments) {
                    if (browser.isDisposed()) return null;
                    final String[] result = new String[1];
                    Display.getDefault().syncExec(() -> {
                        org.eclipse.swt.widgets.FileDialog dialog = new org.eclipse.swt.widgets.FileDialog(browser.getShell(), org.eclipse.swt.SWT.OPEN);
                        dialog.setText("Select Data Source File");
                        eu.kalafatic.utils.application.FileUtils.configureDialogFilterPath(dialog, null);
                        result[0] = dialog.open();
                    });
                    return result[0];
                }
            };

            new BrowserFunction(browser, "logFunction") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length >= 1) {
                        String msg = (String) arguments[0];
                        if ("ready".equals(msg)) {
                            isJsReady = true;
                            refreshUI();
                        } else {
                            System.out.println("[FORGE_JS] " + msg);
                        }
                    }
                    return null;
                }
            };
            bridgesInstalled = true;
        } catch (Throwable t) {
            System.err.println("[FORGE_PAGE] Error setting up JavaScript bridges: " + t.getMessage());
        }
    }

    private void handleForgeAction(String action, Object data) {
        Display.getDefault().asyncExec(() -> {
            switch (action) {
                case "REFRESH":
                    refreshUI();
                    break;
                case "CREATE_SESSION":
                    if (data instanceof String) {
                        JSONObject json = new JSONObject((String) data);
                        ForgeSessionManager.getInstance().createSession(json.getString("name"), json.getString("modelType"));
                        refreshUI();
                    }
                    break;
                case "DELETE_SESSION":
                    if (data instanceof String) {
                        ForgeSessionManager.getInstance().deleteSession((String) data);
                        refreshUI();
                    }
                    break;
                case "UPDATE_MODEL":
                    if (data instanceof String) {
                        JSONObject json = new JSONObject((String) data);
                        String sid = json.optString("sessionId");
                        if (sid != null && !sid.isEmpty()) {
                            ForgeSessionManager.getInstance().updateModel(sid, json.getString("modelGraph"));
                        }
                    }
                    break;
                case "UPDATE_STATUS":
                    if (data instanceof String) {
                        JSONObject json = new JSONObject((String) data);
                        String sid = json.optString("sessionId");
                        if (sid != null && !sid.isEmpty()) {
                            String statusStr = json.getString("status");
                            eu.kalafatic.evolution.model.orchestration.ForgeStatus status = eu.kalafatic.evolution.model.orchestration.ForgeStatus.getByName(statusStr);
                            ForgeSessionManager.getInstance().updateStatus(sid, status);
                        }
                    }
                    break;
                case "CREATE_SNAPSHOT":
                     if (data instanceof String) {
                        JSONObject json = new JSONObject((String) data);
                        String sid = json.optString("sessionId");
                        if (sid != null && !sid.isEmpty()) {
                            ForgeSessionManager.getInstance().createSnapshot(sid, json.optString("genomeId", "auto-milestone"));
                        }
                    }
                    break;
            }
        });
    }

    @Override
    public void refreshUI() {
        if (!isJsReady || browser.isDisposed()) return;

        int port = 28080;
        if (orchestrator != null && orchestrator.getServerSettings() != null) {
            port = orchestrator.getServerSettings().getPort();
        }

        String activeAiSession = "";
        org.json.JSONArray aiSessionArray = new org.json.JSONArray();

        // The global EVO session UUID is the single session identity shared across the EVO
        // family. The Forge page, the Training Monitor and the Darwin engine are consumers of
        // this id — they do NOT create their own session. Propagate the authoritative global
        // UUID from the AI Commander; fall back only for legacy/headless callers with no chat
        // page.
        if (editor != null && editor.getAiChatPage() != null) {
            String globalUuid = editor.getAiChatPage().getCurrentSessionId();
            if (globalUuid != null && !globalUuid.trim().isEmpty()) {
                activeAiSession = globalUuid;
                ForgeSessionManager.getInstance().setCurrentGlobalSessionId(globalUuid);
                // Ensure the global session's ForgeSession exists with the human-readable
                // chat id as its name, so the Forge combo shows "20261128-0757_task"
                // instead of the raw UUID. The UUID remains the id used by the REST API.
                ForgeSessionManager.getInstance().getOrCreateForgeSession(globalUuid);
            }
        }
        if (activeAiSession == null || activeAiSession.trim().isEmpty()) {
            String forgeSessionId = ForgeSessionManager.getInstance().getCurrentSessionId();
            if (forgeSessionId != null && !forgeSessionId.trim().isEmpty()) {
                activeAiSession = forgeSessionId;
            }
        }
        if (activeAiSession == null || activeAiSession.trim().isEmpty()) {
            // No authoritative session selected yet. Never invent the legacy "Default"
            // chat id here: it belongs to the AI Commander namespace, not to Forge
            // sessions, and requesting it produces [SESSION][ERROR] ... NOT_FOUND.
            return;
        }

        if (orchestrator != null && orchestrator.getAiChat() != null && orchestrator.getAiChat().getSessions() != null) {
            for (eu.kalafatic.evolution.model.orchestration.ChatSession cs : orchestrator.getAiChat().getSessions()) {
                if (cs != null && cs.getId() != null && !cs.getId().trim().isEmpty()) {
                    // Pass {id, name} pairs: id = the canonical global UUID (used by the REST
                    // API and telemetry), name = the human-readable chat id ("20261128-0757_task")
                    // shown in the AI Commander. The Forge combo then displays the same name
                    // instead of the raw UUID hash, while every request stays keyed by the UUID.
                    String globalId = cs.getGlobalSessionId();
                    String id = (globalId != null && !globalId.trim().isEmpty()) ? globalId : cs.getId();
                    aiSessionArray.put(new org.json.JSONObject()
                            .put("id", id)
                            .put("name", cs.getId()));
                }
            }
        }
        boolean hasActiveInList = false;
        for (int i = 0; i < aiSessionArray.length(); i++) {
            if (activeAiSession.equals(aiSessionArray.optString(i))) {
                hasActiveInList = true;
                break;
            }
        }
        if (!hasActiveInList) {
            aiSessionArray.put(activeAiSession);
        }

        String escapedActiveSession = activeAiSession.replace("'", "\\'");
        if (!forgeInitialized) {
            forgeInitialized = true;
            lastSyncedSessionId = activeAiSession;
            String jsCall = String.format("if(window.initializeForge) { window.initializeForge(%d, '%s', %s); }",
                    port,
                    escapedActiveSession,
                    aiSessionArray.toString());
            browser.execute(jsCall);
        } else {
            // No-op when the resolved session is unchanged: the periodic sync timer and
            // explicit refreshes share this path, so a stable session must not trigger a
            // redundant re-selection (which would reset the JS viz/selection state).
            if (activeAiSession.equals(lastSyncedSessionId)) {
                return;
            }
            lastSyncedSessionId = activeAiSession;
            String jsCall = String.format("if(window.updateActiveSession) { window.updateActiveSession('%s', %s); }",
                    escapedActiveSession,
                    aiSessionArray.toString());
            browser.execute(jsCall);
        }
    }
}
