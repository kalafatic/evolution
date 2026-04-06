package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class VizGroup {
    private Group group;
    private Browser browser;
    private ScrolledComposite vizScrolled;
    private Composite browserContainer;
    private int browserWidth = 1000;
    private int browserHeight = 800;
    private ApprovalPage page;

    public VizGroup(Composite parent, ApprovalPage page) {
        this.page = page;
        createControl(parent);
    }

    private void createControl(Composite parent) {
        group = SWTFactory.createGroup(parent, "AI Network & Process Flow", 1);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setLayout(new GridLayout(1, false));

        ToolBarManager toolbarManager = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        toolbarManager.add(new Action("Zoom In") { @Override public void run() { browserWidth *= 1.2; browserHeight *= 1.2; updateScrolledContent(); } });
        toolbarManager.add(new Action("Zoom Out") { @Override public void run() { browserWidth *= 0.8; browserHeight *= 0.8; updateScrolledContent(); } });
        toolbarManager.add(new Action("Reset Zoom") { @Override public void run() { browserWidth = 1000; browserHeight = 800; updateScrolledContent(); } });
        toolbarManager.createControl(group);

        vizScrolled = new ScrolledComposite(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        vizScrolled.setLayoutData(new GridData(GridData.FILL_BOTH));
        vizScrolled.setExpandHorizontal(true); vizScrolled.setExpandVertical(true);

        browserContainer = new Composite(vizScrolled, SWT.NONE);
        browserContainer.setLayout(new GridLayout(1, false));
        vizScrolled.setContent(browserContainer);

        browser = new Browser(browserContainer, SWT.NONE);
        GridData browserGD = new GridData(SWT.LEFT, SWT.TOP, false, false);
        browserGD.widthHint = browserWidth; browserGD.heightHint = browserHeight;
        browser.setLayoutData(browserGD);

        new BrowserFunction(browser, "javaZoom") {
            @Override public Object function(Object[] arguments) {
                if (arguments.length > 0 && arguments[0] instanceof Number) {
                    double factor = ((Number) arguments[0]).doubleValue();
                    browserWidth *= factor; browserHeight *= factor; updateScrolledContent();
                }
                return null;
            }
        };
        page.setupBrowserListeners(browser);
        updateScrolledContent();
    }

    public void updateScrolledContent() {
        if (vizScrolled == null || vizScrolled.isDisposed()) return;
        if (browser != null && !browser.isDisposed()) {
            GridData gd = (GridData) browser.getLayoutData();
            gd.widthHint = browserWidth; gd.heightHint = browserHeight;
        }
        browserContainer.layout(true, true);
        vizScrolled.setMinSize(browserContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    public Browser getBrowser() { return browser; }
}
