package eu.kalafatic.evolution.view.editors.pages.tests;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.TestsPage;
import eu.kalafatic.utils.factories.GUIFactory;

public class IterativeDevelopmentLifecycleGroup extends AEvoGroup {
    private Browser iterativeBrowser;
    private Button runBtn;
    private TestsPage page;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Text simulationLogConsole;

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
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Iterative Development Lifecycle", 1, false);
        group.setLayout(new GridLayout(2, false));

        runBtn = toolkit.createButton(group, "Run Lifecycle Simulation", SWT.PUSH);
        GridData btnGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        btnGd.widthHint = 180;
        runBtn.setLayoutData(btnGd);

        statusLabel = toolkit.createLabel(group, "Status: Idle");
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        progressBar = new ProgressBar(group, SWT.HORIZONTAL | SWT.SMOOTH);
        GridData pbGd = new GridData(GridData.FILL_HORIZONTAL);
        pbGd.horizontalSpan = 2;
        pbGd.exclude = true;
        progressBar.setLayoutData(pbGd);
        progressBar.setVisible(false);

        iterativeBrowser = new Browser(group, SWT.NONE);
        GridData browserGD = new GridData(GridData.FILL_BOTH);
        browserGD.heightHint = 165;
        browserGD.horizontalSpan = 2;
        iterativeBrowser.setLayoutData(browserGD);
        iterativeBrowser.setText(page.getIterativeHtmlTemplate());

        Label logLabel = toolkit.createLabel(group, "Simulation Step Logs:");
        GridData logLabelGd = new GridData(GridData.FILL_HORIZONTAL);
        logLabelGd.horizontalSpan = 2;
        logLabel.setLayoutData(logLabelGd);

        simulationLogConsole = toolkit.createText(group, "", SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData logGd = new GridData(GridData.FILL_HORIZONTAL);
        logGd.heightHint = 80;
        logGd.horizontalSpan = 2;
        simulationLogConsole.setLayoutData(logGd);

        runBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                page.runIterativeSimulation(iterativeBrowser, runBtn, null);
            }
        });
    }

    public Browser getBrowser() { return iterativeBrowser; }

    public void appendLog(String msg) {
        Display.getDefault().asyncExec(() -> {
            if (simulationLogConsole == null || simulationLogConsole.isDisposed()) return;
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            simulationLogConsole.append("[" + time + "] " + msg + "\n");
        });
    }

    public void updateStatus(String status) {
        Display.getDefault().asyncExec(() -> {
            if (statusLabel != null && !statusLabel.isDisposed()) {
                statusLabel.setText(status);
            }
        });
    }

    public void updateProgress(int percent) {
        Display.getDefault().asyncExec(() -> {
            if (progressBar != null && !progressBar.isDisposed()) {
                progressBar.setSelection(percent);
            }
        });
    }

    public void setProgressBarVisible(boolean visible) {
        Display.getDefault().asyncExec(() -> {
            if (progressBar != null && !progressBar.isDisposed()) {
                progressBar.setVisible(visible);
                GridData gd = (GridData) progressBar.getLayoutData();
                gd.exclude = !visible;
                group.layout(true, true);
            }
        });
    }
}
