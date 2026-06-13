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

    public ForgePage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        ForgeSessionManager.getInstance().initialize(orchestrator);
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

        initBrowser();
    }

    private void initBrowser() {
        String html = eu.kalafatic.evolution.controller.tools.FileTool.readResource("/eu/kalafatic/evolution/controller/orchestration/forge.html");
        if (html != null) {
            browser.setText(html);
        } else {
            browser.setText("<html><body><h1>Forge UI Template Not Found</h1></body></html>");
        }
    }

    private void setupJavaScriptBridges() {
        if (browser.isDisposed()) return;

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
                        ForgeSessionManager.getInstance().updateModel(json.getString("sessionId"), json.getString("modelGraph"));
                    }
                    break;
                case "UPDATE_STATUS":
                    if (data instanceof String) {
                        JSONObject json = new JSONObject((String) data);
                        String sid = json.getString("sessionId");
                        String statusStr = json.getString("status");
                        eu.kalafatic.evolution.model.orchestration.ForgeStatus status = eu.kalafatic.evolution.model.orchestration.ForgeStatus.getByName(statusStr);
                        ForgeSessionManager.getInstance().updateStatus(sid, status);
                    }
                    break;
                case "CREATE_SNAPSHOT":
                     if (data instanceof String) {
                        JSONObject json = new JSONObject((String) data);
                        ForgeSessionManager.getInstance().createSnapshot(json.getString("sessionId"), json.optString("genomeId", "auto-milestone"));
                    }
                    break;
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (!isJsReady || browser.isDisposed()) return;

        // Pass port to JS
        int port = 48080;
        if (orchestrator.getServerSettings() != null) port = orchestrator.getServerSettings().getPort();
        browser.execute("if(window.initializeForge) { window.initializeForge(" + port + "); }");
    }
}
