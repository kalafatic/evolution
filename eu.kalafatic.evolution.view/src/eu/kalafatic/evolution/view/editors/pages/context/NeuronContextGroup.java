package eu.kalafatic.evolution.view.editors.pages.context;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.services.NeuronContextService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class NeuronContextGroup extends AEvoGroup {

    private final NeuronContextService service;
    private Text contextPreview;
    private Text logConsole;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button learnBtn;

    public NeuronContextGroup(Composite parent, MultiPageEditor editor, Orchestrator orchestrator, File projectRoot) {
        super(editor, orchestrator);
        this.service = new NeuronContextService(orchestrator, projectRoot);

        Group g = new Group(parent, SWT.NONE);
        g.setText("Neuron Context (Learned Behavior)");
        g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.group = g;
        createContent(g);
    }

    private void createContent(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        learnBtn = new Button(parent, SWT.PUSH);
        learnBtn.setText("Learn from History (Analyze Iterations)");
        learnBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        progressBar = new ProgressBar(parent, SWT.HORIZONTAL | SWT.SMOOTH);
        GridData pbGd = new GridData(GridData.FILL_HORIZONTAL);
        pbGd.exclude = true;
        progressBar.setLayoutData(pbGd);
        progressBar.setVisible(false);

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setText("Status: Idle");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(parent, SWT.NONE).setText("Analysis Log Console:");
        logConsole = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData logGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        logGd.heightHint = 80;
        logConsole.setLayoutData(logGd);

        new Label(parent, SWT.NONE).setText("Current Learned Insights:");
        contextPreview = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 120;
        contextPreview.setLayoutData(gd);

        learnBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                learnFromHistoryAsync();
            }
        });

        refreshUI();
    }

    private void learnFromHistoryAsync() {
        learnBtn.setEnabled(false);
        setProgressBarVisible(true);
        updateStatus("Status: Analyzing iteration history...");
        appendLog("[NEURON_CONTEXT] Starting learning process from iteration history...");

        Job job = new Job("Neuron Context Learning") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    updateProgress(20);
                    appendLog("[NEURON_CONTEXT] Scanning 'orchestrator/memory/' for iteration records...");

                    updateProgress(50);
                    service.learnFromHistory();
                    appendLog("[NEURON_CONTEXT] Iteration history analysis completed.");

                    updateProgress(80);
                    appendLog("[NEURON_CONTEXT] Updating learned context model and UI...");

                    updateProgress(100);
                    updateStatus("Status: Learning Analysis Completed");
                    appendLog("[NEURON_CONTEXT] Insights saved successfully.");
                } catch (Exception ex) {
                    appendLog("[NEURON_CONTEXT][ERROR] Failed during learning process: " + ex.getMessage());
                    updateStatus("Status: Learning Analysis Failed");
                } finally {
                    Display.getDefault().asyncExec(() -> {
                        if (learnBtn != null && !learnBtn.isDisposed()) learnBtn.setEnabled(true);
                        setProgressBarVisible(false);
                        refreshUI();
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void appendLog(String msg) {
        Display.getDefault().asyncExec(() -> {
            if (logConsole == null || logConsole.isDisposed()) return;
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logConsole.append("[" + time + "] " + msg + "\n");
        });
    }

    private void updateStatus(String status) {
        Display.getDefault().asyncExec(() -> {
            if (statusLabel != null && !statusLabel.isDisposed()) {
                statusLabel.setText(status);
            }
        });
    }

    private void updateProgress(int percent) {
        Display.getDefault().asyncExec(() -> {
            if (progressBar != null && !progressBar.isDisposed()) {
                progressBar.setSelection(percent);
            }
        });
    }

    private void setProgressBarVisible(boolean visible) {
        Display.getDefault().asyncExec(() -> {
            if (progressBar != null && !progressBar.isDisposed()) {
                progressBar.setVisible(visible);
                GridData gd = (GridData) progressBar.getLayoutData();
                gd.exclude = !visible;
                group.layout(true, true);
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (contextPreview != null && !contextPreview.isDisposed()) {
            setTextSafe(contextPreview, service.getLearnedContext());
        }
    }
}
