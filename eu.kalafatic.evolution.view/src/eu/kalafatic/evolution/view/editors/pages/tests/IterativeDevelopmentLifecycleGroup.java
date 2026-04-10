package eu.kalafatic.evolution.view.editors.pages.tests;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.view.editors.pages.TestsPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class IterativeDevelopmentLifecycleGroup extends AEvoGroup {
    private Browser iterativeBrowser;
    private Button runBtn;
    private TestsPage page;

    public IterativeDevelopmentLifecycleGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, TestsPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        // Managed by TestsPage
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Iterative Development Lifecycle", 1, false);
        group.setLayout(new GridLayout(2, false));

        runBtn = toolkit.createButton(group, "Run Lifecycle Simulation", SWT.PUSH);
        GridData btnGd = new GridData();
        btnGd.widthHint = 180;
        runBtn.setLayoutData(btnGd);

        iterativeBrowser = new Browser(group, SWT.NONE);
        GridData browserGD = new GridData(GridData.FILL_BOTH);
        browserGD.heightHint = 250;
        browserGD.horizontalSpan = 2;
        iterativeBrowser.setLayoutData(browserGD);
        iterativeBrowser.setText(page.getIterativeHtmlTemplate());

        runBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                page.runIterativeSimulation(iterativeBrowser, runBtn, null);
            }
        });
    }

    public Browser getBrowser() { return iterativeBrowser; }
}
