package eu.kalafatic.evolution.view.editors.pages.development;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import java.io.File;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.workflow.WorkflowGraphManager;
import eu.kalafatic.evolution.controller.workflow.GraphActionExecutor;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;
import eu.kalafatic.evolution.view.application.Activator;

public class InteractiveWorkflowGroup extends AEvoGroup {
    private Browser browser;
    private String sessionId;
    private GraphActionExecutor executor;
    private boolean isLoaded = false;

    public InteractiveWorkflowGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, String sessionId) {
        super(editor, orchestrator);
        this.sessionId = sessionId;
        this.executor = new GraphActionExecutor(editor.getCurrentContext());
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Interactive AI Workflow", 1, true, true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 700;
        group.setLayoutData(gd);

        Composite browserContainer = toolkit.createComposite(group);
        browserContainer.setLayout(new FillLayout());
        browserContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        try {
            browser = GUIFactory.INSTANCE.createBrowser(browserContainer, 700);

            browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
                @Override
                public void completed(org.eclipse.swt.browser.ProgressEvent event) {
                    isLoaded = true;
                    setupJavaScriptBridges();
                    scheduleRefresh();
                }
            });

            setupJavaScriptBridges();
            loadWorkflowHtml();
        } catch (Exception e) {
            toolkit.createLabel(browserContainer, "Browser Error: " + e.getMessage());
        }
    }

    private void setupJavaScriptBridges() {
        new BrowserFunction(browser, "javaAction") {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length >= 2) {
                    executor.execute(arguments[0].toString(), arguments[1].toString());
                }
                return null;
            }
        };
    }

    private void loadWorkflowHtml() {
        try {
            Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.view");
            if (bundle == null) {
                bundle = FrameworkUtil.getBundle(getClass());
            }

            if (bundle != null) {
                URL bundleRoot = FileLocator.toFileURL(bundle.getEntry("/"));
                String base = bundleRoot.toString();
                String html = GUIFactory.INSTANCE.loadHtmlTemplate(getClass(), "/workflow/workflow.html");

                if (html.contains("<head>")) {
                    html = html.replace("<head>", "<head><base href=\"" + base + "workflow/\">");
                }

                browser.setText(html, true);
            } else {
                browser.setText(getFallbackHtml());
            }
        } catch (Exception e) {
            browser.setText(getFallbackHtml());
        }
    }

    private String getFallbackHtml() {
        return "<html><body style='background:#f0f2f5; font-family:sans-serif; display:flex; justify-content:center; align-items:center; height:100vh;'>"
             + "<div style='text-align:center;'><h3>Workflow Diagram</h3><p>Loading assets...</p></div>"
             + "</body></html>";
    }

    @Override
    protected void refreshUI() {
        if (browser != null && !browser.isDisposed() && isLoaded) {
            String sid = sessionId != null ? sessionId : "Default";
            executor.setSessionId(sid);
            executor.setContext(editor.getCurrentContext());

            SessionContainer session = SessionManager.getInstance().getSession(sid);
            if (session != null) {
                JSONObject graph = session.getWorkflowGraphManager().getGraphJson(sid);
                if (graph != null) {
                    browser.execute("if(window.updateGraph) window.updateGraph(" + graph.toString() + ");");
                }
            }
        }
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void dispose() {
        String sid = sessionId != null ? sessionId : "Default";
        SessionContainer session = SessionManager.getInstance().getSession(sid);
        if (session != null) {
            session.getWorkflowGraphManager().removeInstance(sid);
        }
        super.dispose();
    }
}
