package eu.kalafatic.evolution.view.editors.pages.development;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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

import eu.kalafatic.evolution.controller.workflow.WorkflowGraphManager;
import eu.kalafatic.evolution.controller.workflow.GraphActionExecutor;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import eu.kalafatic.evolution.view.application.Activator;

public class InteractiveWorkflowGroup2 extends AEvoGroup {
    private Browser browser;
    private String sessionId;
    private GraphActionExecutor executor;
    private boolean isLoaded = false;

    public InteractiveWorkflowGroup2(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, String sessionId) {
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
            browser = SWTFactory.createBrowser(browserContainer, 300);
          
            browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
                @Override
                public void completed(org.eclipse.swt.browser.ProgressEvent event) {
                    isLoaded = true;
                    scheduleRefresh();
                }
            });

           

            //loadWorkflowHtml();
            
            setupBrowser(browser);
            
        } catch (Exception e) {
            toolkit.createLabel(browserContainer, "Browser Error: " + e.getMessage());
        }
    }
    
    private void setupBrowser(Browser browser) {
        setupJavaScriptBridges();
        
        try {
            Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.view");
            if (bundle == null) {
                bundle = FrameworkUtil.getBundle(getClass());
            }

            if (bundle != null) {
                // We use setUrl because it's the most reliable way for SWT Browser to handle ES modules
                // and resolve relative paths (./js/...) correctly.
                // We MUST use FileLocator.toFileURL on the BUNDLE ROOT to ensure all JS/CSS files are extracted.
                URL bundleRoot = FileLocator.toFileURL(bundle.getEntry("/"));
                URL chatUrl = new URL(bundleRoot, "/workflow/workflow.html");
                //browser.setUrl(chatUrl.toString());
                
                String html = SWTFactory.loadHtmlTemplate("/workflow/workflow.html");

             // critical: inject base path so relative JS works
             //URL bundleRoot = FileLocator.toFileURL(bundle.getEntry("/"));
             String base = bundleRoot.toString();

             html = html.replace(
                 "<head>",
                 "<head><base href=\"" + base + "\">"
             );

             browser.setText(html, true); // trusted = allow scripts
            } else {
                throw new Exception("Bundle not found");
            }
        } catch (Exception e) {
            System.err.println("Failed to load chat.html via setUrl: " + e.getMessage());
            String html =  SWTFactory.loadHtmlTemplate("/workflow/workflow.html");
            browser.setText(html);
        }
    }

    private void setupJavaScriptBridges() {
        // Logging bridge
//        new BrowserFunction(browser, "JavaLog") {
//            @Override
//            public Object function(Object[] args) {
//                if (args.length > 0) {
//                    System.out.println("[Chat Browser] " + args[0]);
//                }
//                return null;
//            }
//        };
        
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

//    private void loadWorkflowHtml() {
//        try {
//            if (Activator.getDefault() == null) return;
//            URL url = FileLocator.find(Activator.getDefault().getBundle(), new Path("workflow/workflow.html"), null);
//            if (url != null) {
//                browser.setUrl(FileLocator.toFileURL(url).toExternalForm());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

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
