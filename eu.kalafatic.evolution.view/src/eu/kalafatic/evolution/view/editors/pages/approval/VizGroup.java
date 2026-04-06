package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class VizGroup {
    private Composite group;
    private Browser browser;

    public VizGroup(FormToolkit toolkit, Composite parent, ApprovalPage page) {
        createControl(toolkit, parent, page);
    }

    private void createControl(FormToolkit toolkit, Composite parent, ApprovalPage page) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Network Visualization", 1, true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 400;
        group.setLayoutData(gd);

        // Access the section to add toolbar buttons
        if (group.getParent() instanceof Section) {
            Section section = (Section) group.getParent();
            Composite toolbar = toolkit.createComposite(section);
            toolbar.setLayout(new GridLayout(3, true));

            Button zoomIn = toolkit.createButton(toolbar, "+", SWT.PUSH);
            zoomIn.setToolTipText("Zoom In");
            zoomIn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { if (browser != null) browser.execute("applyZoom(1.2);"); }
            });

            Button zoomOut = toolkit.createButton(toolbar, "-", SWT.PUSH);
            zoomOut.setToolTipText("Zoom Out");
            zoomOut.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { if (browser != null) browser.execute("applyZoom(0.8);"); }
            });

            Button reset = toolkit.createButton(toolbar, "R", SWT.PUSH);
            reset.setToolTipText("Reset Zoom");
            reset.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { if (browser != null) browser.execute("resetZoom();"); }
            });

            section.setTextClient(toolbar);
        }

        Composite browserContainer = toolkit.createComposite(group);
        browserContainer.setLayout(new FillLayout());
        browserContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        try {
            browser = new Browser(browserContainer, SWT.NONE);
            page.setupBrowserListeners(browser);
        } catch (Exception e) {
            toolkit.createLabel(browserContainer, "Browser not supported: " + e.getMessage());
        }
    }

    public Browser getBrowser() { return browser; }
}
