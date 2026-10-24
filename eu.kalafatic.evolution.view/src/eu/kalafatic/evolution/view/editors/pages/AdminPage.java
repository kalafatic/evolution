package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import eu.kalafatic.evolution.controller.orchestration.AdminResourceDiscoveryManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class AdminPage extends AEvoPage {

    private Browser browser;
    private boolean isJsReady = false;
    private boolean bridgesInstalled = false;

    public AdminPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
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

        // Set up JS bridges before loading HTML page
        setupJavaScriptBridges();
        initBrowser();

        AdminResourceDiscoveryManager.getInstance().discoverAndSyncResources();
    }

    private void initBrowser() {
        try {
            org.osgi.framework.Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.controller");
            if (bundle != null) {
                java.net.URL entry = bundle.getEntry("eu/kalafatic/evolution/controller/orchestration/admin.html");
                if (entry == null) {
                    // Fallback for development where it might be in src
                    entry = bundle.getEntry("src/eu/kalafatic/evolution/controller/orchestration/admin.html");
                }

                if (entry != null) {
                    java.net.URL adminUrl = FileLocator.toFileURL(entry);
                    browser.setUrl(adminUrl.toString());
                } else {
                    browser.setText("<html><body><h1>admin.html not found in bundle</h1></body></html>");
                }
            } else {
                browser.setText("<html><body><h1>Controller bundle not found</h1></body></html>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            browser.setText("<html><body><h1>Error loading admin.html</h1></body></html>");
        }
    }

    private void setupJavaScriptBridges() {
        if (browser == null || browser.isDisposed() || bridgesInstalled) return;

        try {
            new BrowserFunction(browser, "adminAction") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length >= 1) {
                        String action = String.valueOf(arguments[0]);
                        if ("refreshResources".equalsIgnoreCase(action)) {
                            AdminResourceDiscoveryManager.getInstance().discoverAndSyncResources();
                            refreshUI();
                            return "SUCCESS";
                        }
                    }
                    return "UNKNOWN_ACTION";
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
                            System.out.println("[ADMIN_JS] " + msg);
                        }
                    }
                    return null;
                }
            };

            bridgesInstalled = true;
        } catch (Throwable t) {
            System.err.println("[ADMIN_PAGE] Error setting up JavaScript bridges: " + t.getMessage());
        }
    }

    @Override
    protected void refreshUI() {
        if (browser == null || browser.isDisposed()) return;

        int port = 48081;
        if (orchestrator != null && orchestrator.getServerSettings() != null) {
            port = orchestrator.getServerSettings().getPort();
        }

        String jsCall = String.format("if(window.initializeAdmin) { window.initializeAdmin(%d); } else if(typeof loadOverview === 'function') { loadOverview(); }", port);
        browser.execute(jsCall);
    }

    @Override
    public void dispose() {
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }
        super.dispose();
    }
}
