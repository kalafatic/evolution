package eu.kalafatic.evolution.view.editors.pages.development;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import eu.kalafatic.evolution.view.editors.pages.DevelopmentPage;
import eu.kalafatic.utils.factories.GUIFactory;
import eu.kalafatic.evolution.view.projection.ProjectionService;
import eu.kalafatic.evolution.view.projection.RuntimeProjection;
import java.util.function.Consumer;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class VizGroup extends AEvoGroup {
    private Browser browser;
    private final Consumer<RuntimeProjection> projectionObserver = projection -> {
        if (browser == null || browser.isDisposed()) return;
        String activeSid = (editor != null && editor.getAiChatPage() != null) ?
                editor.getAiChatPage().getCurrentSessionName() : "Default";
        if (projection.getSessionId().equals(activeSid)) {
            scheduleRefresh();
        }
    };

    public VizGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, DevelopmentPage page) {
        super(editor, orchestrator);
        ProjectionService.getInstance().subscribe(projectionObserver);
        createControl(toolkit, parent, page);
    }

    @Override
    protected void refreshUI() {
        // Handled by DevelopmentPage.refreshBrowser() via model notifications
    }

    private void createControl(FormToolkit toolkit, Composite parent, DevelopmentPage page) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Network Visualization", 1, false, true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 700;
        group.setLayoutData(gd);

        // Access the section to add toolbar buttons
        if (group.getParent() instanceof Section) {
            Section section = (Section) group.getParent();
            Composite toolbar = toolkit.createComposite(section);
            toolbar.setLayout(new GridLayout(6, true));

            Button refresh = toolkit.createButton(toolbar, "Ref", SWT.PUSH);
            refresh.setToolTipText("Refresh");
            refresh.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { page.scheduleRefresh(); }
            });

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

            Button fit = toolkit.createButton(toolbar, "Fit", SWT.PUSH);
            fit.setToolTipText("Fit to Screen");
            fit.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) { if (browser != null) browser.execute("if(window.fitToScreen) window.fitToScreen();"); }
            });

            Button export = toolkit.createButton(toolbar, "Exp", SWT.PUSH);
            export.setToolTipText("Export (Log to Console)");
            export.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                    if (browser != null) {
                        Object html = browser.evaluate("return document.documentElement.outerHTML;");
                        System.out.println("Exported Diagram HTML: " + html);
                    }
                }
            });
            
           GUIFactory.INSTANCE.createMaximizeButton(toolbar, section, false);
           
           section.setTextClient(toolbar);
        }

        Composite browserContainer = toolkit.createComposite(group);
        browserContainer.setLayout(new FillLayout());
        browserContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        browserContainer.addListener(SWT.Resize, event -> {
            if (browser != null && !browser.isDisposed()) {
                browser.execute("if(window.dispatchEvent) window.dispatchEvent(new Event('resize'));");
            }
        });

        try {
		browser = GUIFactory.INSTANCE.createBrowser(browserContainer,700);
          
            page.setupBrowserListeners(browser);
            hookContextMenu(page);
            
            
        } catch (Exception e) {
            toolkit.createLabel(browserContainer, "Browser not supported: " + e.getMessage());
        }
    }

    private void hookContextMenu(DevelopmentPage page) {
        org.eclipse.jface.action.MenuManager menuMgr = new org.eclipse.jface.action.MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            manager.add(new org.eclipse.jface.action.Action("Refresh") { @Override public void run() { page.scheduleRefresh(); } });
            manager.add(new org.eclipse.jface.action.Separator());
            manager.add(new org.eclipse.jface.action.Action("Zoom In") { @Override public void run() { if (browser != null) browser.execute("applyZoom(1.2);"); } });
            manager.add(new org.eclipse.jface.action.Action("Zoom Out") { @Override public void run() { if (browser != null) browser.execute("applyZoom(0.8);"); } });
            manager.add(new org.eclipse.jface.action.Action("Reset Zoom") { @Override public void run() { if (browser != null) browser.execute("resetZoom();"); } });
        });
        org.eclipse.swt.widgets.Menu menu = menuMgr.createContextMenu(browser);
        browser.setMenu(menu);
    }

    public Browser getBrowser() { return browser; }

    @Override
    public void dispose() {
        ProjectionService.getInstance().unsubscribe(projectionObserver);
        super.dispose();
    }
}
