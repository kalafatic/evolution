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
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.workflow.WorkflowGraphManager;
import eu.kalafatic.evolution.controller.workflow.GraphActionExecutor;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;
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
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Interactive AI Workflow", 1, true, true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 700;
        group.setLayoutData(gd);

        Composite browserContainer = toolkit.createComposite(group);
        browserContainer.setLayout(new FillLayout());
        browserContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        try {
            browser = SWTFactory.createBrowser(browserContainer, 700);

            browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
                @Override
                public void completed(org.eclipse.swt.browser.ProgressEvent event) {
                    isLoaded = true;
                    scheduleRefresh();
                }
            });

            new BrowserFunction(browser, "javaAction") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length >= 2) {
                        executor.execute(arguments[0].toString(), arguments[1].toString());
                    }
                    return null;
                }
            };

            loadWorkflowHtml();
        } catch (Exception e) {
            toolkit.createLabel(browserContainer, "Browser Error: " + e.getMessage());
        }
    }

    private void loadWorkflowHtml() {
        try {
            if (Activator.getDefault() == null) {
                System.err.println("[WORKFLOW] Activator default is null");
                return;
            }
            URL url = FileLocator.find(Activator.getDefault().getBundle(), new Path("workflow/workflow.html"), null);
            if (url != null) {
                URL fileUrl = FileLocator.toFileURL(url);
                String base = fileUrl.toString();
                base = base.substring(0, base.lastIndexOf("/") + 1);

                java.io.InputStream is = fileUrl.openStream();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                String html = sb.toString();
                html = html.replace("<head>", "<head><base href=\"" + base + "\">");

                System.out.println("[WORKFLOW] Loading workflow HTML with base: " + base);
                browser.setText(html, true);
            } else {
                System.err.println("[WORKFLOW] Could not find workflow/workflow.html in bundle");
            }
        } catch (Exception e) {
            System.err.println("[WORKFLOW] Error loading workflow HTML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void refreshUI() {
        if (browser != null && !browser.isDisposed() && isLoaded) {
            JSONObject graph = WorkflowGraphManager.getInstance(sessionId).getGraphJson(sessionId);
            browser.execute("if(window.updateGraph) window.updateGraph(" + graph.toString() + ");");
        }
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void dispose() {
        WorkflowGraphManager.removeInstance(sessionId);
        super.dispose();
    }
}
