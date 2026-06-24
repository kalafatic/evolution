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
import eu.kalafatic.evolution.view.projection.ProjectionService;
import eu.kalafatic.evolution.view.projection.RuntimeProjection;
import eu.kalafatic.utils.factories.GUIFactory;
import eu.kalafatic.evolution.view.application.Activator;
import java.util.function.Consumer;

public class InteractiveWorkflowGroup extends AEvoGroup {
    private Browser browser;
    private String sessionId;
    private GraphActionExecutor executor;
    private boolean isLoaded = false;

    private final Consumer<RuntimeProjection> projectionObserver = projection -> {
        if (browser == null || browser.isDisposed()) return;
        String activeSid = (editor != null && editor.getAiChatPage() != null) ?
                editor.getAiChatPage().getCurrentSessionName() : sessionId;
        if (projection.getSessionId().equals(activeSid)) {
            scheduleRefresh();
        }
    };

    public InteractiveWorkflowGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, String sessionId) {
        super(editor, orchestrator);
        this.sessionId = sessionId;
        this.executor = new GraphActionExecutor(editor.getCurrentContext());
        ProjectionService.getInstance().subscribe(projectionObserver);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Interactive AI Workflow", 1, true, true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 700;
        group.setLayoutData(gd);

        // Add toolbar buttons if group is a Section
        if (group.getParent() instanceof Section) {
            Section section = (Section) group.getParent();
            Composite toolbar = toolkit.createComposite(section);
            toolbar.setLayout(new org.eclipse.swt.layout.GridLayout(6, false));

            org.eclipse.swt.widgets.Button refresh = toolkit.createButton(toolbar, "Ref", SWT.PUSH);
            refresh.setToolTipText("Refresh");
            refresh.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { scheduleRefresh(); }
            });

            org.eclipse.swt.widgets.Button zoomIn = toolkit.createButton(toolbar, "+", SWT.PUSH);
            zoomIn.setToolTipText("Zoom In");
            zoomIn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { if (browser != null) browser.execute("if(window.applyZoom) window.applyZoom(1.2);"); }
            });

            org.eclipse.swt.widgets.Button zoomOut = toolkit.createButton(toolbar, "-", SWT.PUSH);
            zoomOut.setToolTipText("Zoom Out");
            zoomOut.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { if (browser != null) browser.execute("if(window.applyZoom) window.applyZoom(0.8);"); }
            });

            org.eclipse.swt.widgets.Button reset = toolkit.createButton(toolbar, "R", SWT.PUSH);
            reset.setToolTipText("Reset Zoom");
            reset.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { if (browser != null) browser.execute("if(window.resetZoom) window.resetZoom();"); }
            });

            org.eclipse.swt.widgets.Button fit = toolkit.createButton(toolbar, "Fit", SWT.PUSH);
            fit.setToolTipText("Fit to Screen");
            fit.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { if (browser != null) browser.execute("if(window.fitToScreen) window.fitToScreen();"); }
            });

            GUIFactory.INSTANCE.createMaximizeButton(toolbar, section, false);
            section.setTextClient(toolbar);
        }

        Composite browserContainer = toolkit.createComposite(group);
        browserContainer.setLayout(new FillLayout());
        browserContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        browserContainer.addListener(SWT.Resize, event -> {
            if (browser != null && !browser.isDisposed()) {
                // Trigger JS resize
                browser.execute("if(window.dispatchEvent) window.dispatchEvent(new Event('resize'));");
            }
        });

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
            String sid = (editor != null && editor.getAiChatPage() != null) ?
                editor.getAiChatPage().getCurrentSessionName() :
                (sessionId != null ? sessionId : "Default");

            this.sessionId = sid;
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
        scheduleRefresh();
    }

    @Override
    public void dispose() {
        ProjectionService.getInstance().unsubscribe(projectionObserver);
        String sid = sessionId != null ? sessionId : "Default";
        SessionContainer session = SessionManager.getInstance().getSession(sid);
        if (session != null) {
            session.getWorkflowGraphManager().removeInstance(sid);
        }
        super.dispose();
    }
}
