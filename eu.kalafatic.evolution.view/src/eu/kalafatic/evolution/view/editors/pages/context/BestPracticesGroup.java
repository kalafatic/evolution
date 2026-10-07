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

import eu.kalafatic.evolution.controller.services.BestPracticesService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class BestPracticesGroup extends AEvoGroup {

    private final File projectRoot;
    private Text logConsole;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button syncBtn;

    public BestPracticesGroup(Composite parent, MultiPageEditor editor, Orchestrator orchestrator, File projectRoot) {
        super(editor, orchestrator);
        this.projectRoot = projectRoot;

        Group g = new Group(parent, SWT.NONE);
        g.setText("Best Practices Management");
        g.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        this.group = g;
        createContent(g);
    }

    private void createContent(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        syncBtn = new Button(parent, SWT.PUSH);
        syncBtn.setText("Initialize Defaults / Sync from Filesystem");
        syncBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        progressBar = new ProgressBar(parent, SWT.HORIZONTAL | SWT.SMOOTH);
        GridData pbGd = new GridData(GridData.FILL_HORIZONTAL);
        pbGd.exclude = true;
        progressBar.setLayoutData(pbGd);
        progressBar.setVisible(false);

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setText("Status: Idle");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(parent, SWT.NONE).setText("Sync Log Console:");
        logConsole = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData logGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        logGd.heightHint = 100;
        logConsole.setLayoutData(logGd);

        syncBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                syncBestPractices();
            }
        });

        Label hint = new Label(parent, SWT.WRAP);
        hint.setText("Markdown files in 'orchestrator/best_practices/' subdirectories will be automatically injected into agent prompts.");
        hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void syncBestPractices() {
        if (orchestrator == null || projectRoot == null) {
            appendLog("[BEST_PRACTICES][ERROR] Cannot sync: Orchestrator or project root is null.");
            return;
        }

        syncBtn.setEnabled(false);
        setProgressBarVisible(true);
        updateStatus("Syncing best practices from filesystem...");
        appendLog("[BEST_PRACTICES] Starting synchronization...");

        Job job = new Job("Sync Best Practices") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    BestPracticesService service = new BestPracticesService(orchestrator, projectRoot);
                    String path = service.getInstructionsPath();
                    appendLog("[BEST_PRACTICES] Instructions path: " + path);

                    updateProgress(25);
                    String[] roles = { "architect", "planner", "agent", "tools" };
                    for (int i = 0; i < roles.length; i++) {
                        String role = roles[i];
                        String practices = service.getPracticesForRole(role);
                        int count = practices.isEmpty() ? 0 : practices.split("\n").length;
                        appendLog("[BEST_PRACTICES] Role '" + role.toUpperCase() + "': " + count + " lines loaded.");
                        updateProgress(25 + ((i + 1) * 15));
                    }

                    updateProgress(90);
                    String combined = service.getCombinedPractices();
                    appendLog("[BEST_PRACTICES] Total combined guidelines length: " + combined.length() + " chars.");
                    updateProgress(100);
                    updateStatus("Status: Synchronization Complete");
                    appendLog("[BEST_PRACTICES] Best practices synchronized successfully.");
                } catch (Exception ex) {
                    appendLog("[BEST_PRACTICES][ERROR] Sync failed: " + ex.getMessage());
                    updateStatus("Status: Sync Failed");
                } finally {
                    Display.getDefault().asyncExec(() -> {
                        if (syncBtn != null && !syncBtn.isDisposed()) syncBtn.setEnabled(true);
                        setProgressBarVisible(false);
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
    }
}
