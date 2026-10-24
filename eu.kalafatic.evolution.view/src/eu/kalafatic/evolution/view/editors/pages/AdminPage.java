package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import eu.kalafatic.evolution.controller.orchestration.AdminResourceDiscoveryManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class AdminPage extends AEvoPage {

    private Browser browser;
    private boolean bridgesInstalled = false;

    public AdminPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        createControl();
    }

    private void createControl() {
        setLayout(new GridLayout(1, false));

        try {
            this.browser = new Browser(this, SWT.NONE);
            this.browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            initBrowser();
            setupBridges();

            AdminResourceDiscoveryManager.getInstance().discoverAndSyncResources();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initBrowser() {
        try {
            org.osgi.framework.Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.controller");
            if (bundle != null) {
                java.net.URL entry = bundle.getEntry("eu/kalafatic/evolution/controller/orchestration/admin.html");
                if (entry == null) {
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

    private void setupBridges() {
        if (browser == null || browser.isDisposed() || bridgesInstalled) return;

        try {
            new BrowserFunction(browser, "adminAction") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length >= 1) {
                        String action = String.valueOf(arguments[0]);
                        if ("refreshResources".equalsIgnoreCase(action)) {
                            AdminResourceDiscoveryManager.getInstance().discoverAndSyncResources();
                            return "SUCCESS";
                        }
                    }
                    return "UNKNOWN_ACTION";
                }
            };
            bridgesInstalled = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void refreshUI() {
        if (browser != null && !browser.isDisposed()) {
            browser.execute("if (typeof loadOverview === 'function') loadOverview();");
        }
    }

    @Override
    public void dispose() {
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }
        super.dispose();
    }
}
