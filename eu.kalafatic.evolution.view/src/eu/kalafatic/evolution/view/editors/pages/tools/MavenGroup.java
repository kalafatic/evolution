package eu.kalafatic.evolution.view.editors.pages.tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.utils.factories.GUIFactory;

public class MavenGroup extends AToolGroup {
    private Text mavenGoalsText, mavenProfilesText;
    private Text mavenOutputText;
    private Button testBtn;

    public MavenGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Maven Tool Settings", 3, false);
        GUIFactory.INSTANCE.createLabel(group, "Goals:");
        mavenGoalsText = GUIFactory.INSTANCE.createText(group);
        mavenGoalsText.setText(orchestrator.getMaven() != null ? orchestrator.getMaven().getGoals().toString() : "");
        GUIFactory.INSTANCE.createEditButton(group, mavenGoalsText);

        GUIFactory.INSTANCE.createLabel(group, "Profiles:");
        mavenProfilesText = GUIFactory.INSTANCE.createText(group);
        mavenProfilesText.setText(orchestrator.getMaven() != null ? orchestrator.getMaven().getProfiles().toString() : "");
        GUIFactory.INSTANCE.createEditButton(group, mavenProfilesText);

        GUIFactory.INSTANCE.createLabel(group, "");
        testBtn = GUIFactory.INSTANCE.createButton(group, "Test Maven");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testMaven();
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "Output Console:");
        mavenOutputText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 80;
        gd.horizontalSpan = 2;
        mavenOutputText.setLayoutData(gd);
    }

    private void testMaven() {
        if (testBtn != null && !testBtn.isDisposed()) testBtn.setEnabled(false);
        appendLog("[MAVEN] Running Maven availability check...");

        Job job = new Job("Maven Diagnostics") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    File workingDir = getWorkingDir();
                    TaskContext context = new TaskContext(orchestrator, workingDir);
                    eu.kalafatic.evolution.controller.tools.ShellTool shell = new eu.kalafatic.evolution.controller.tools.ShellTool();
                    String cmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd -version" : "mvn -version";
                    appendLog("[MAVEN] Executing: " + cmd + " (Working Dir: " + workingDir.getAbsolutePath() + ")");

                    String result = shell.execute(cmd, workingDir, context);
                    appendLog("[MAVEN][STDOUT] " + result);

                    if (orchestrator.getMaven() != null) {
                        orchestrator.getMaven().setTestStatus("SUCCESS");
                        Display.getDefault().asyncExec(() -> updateGroupStatus());
                    }
                    appendLog("[MAVEN] Maven environment verified successfully.");
                } catch (Exception e) {
                    appendLog("[MAVEN][ERROR] Maven test failed: " + e.getMessage());
                    if (orchestrator.getMaven() != null) {
                        orchestrator.getMaven().setTestStatus("FAILED");
                        Display.getDefault().asyncExec(() -> updateGroupStatus());
                    }
                } finally {
                    Display.getDefault().asyncExec(() -> {
                        if (testBtn != null && !testBtn.isDisposed()) testBtn.setEnabled(true);
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void appendLog(String msg) {
        Display.getDefault().asyncExec(() -> {
            if (mavenOutputText == null || mavenOutputText.isDisposed()) return;
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            mavenOutputText.append("[" + time + "] " + msg + "\n");
        });
    }

    private File getWorkingDir() {
        // Fallback working directory
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Override
    protected void refreshUI() {
        if (orchestrator.getMaven() != null) {
            Maven maven = orchestrator.getMaven();
            mavenGoalsText.setText(maven.getGoals().toString());
            mavenProfilesText.setText(maven.getProfiles().toString());
            updateGroupStatus();
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator.getMaven() == null) {
            orchestrator.setMaven(OrchestrationFactory.eINSTANCE.createMaven());
        }
        Maven maven = orchestrator.getMaven();
        maven.getGoals().clear();
        for (String goal : mavenGoalsText.getText().replace("[", "").replace("]", "").split("[,\\s]+")) {
            if (!goal.trim().isEmpty()) maven.getGoals().add(goal.trim());
        }
        maven.getProfiles().clear();
        for (String prof : mavenProfilesText.getText().replace("[", "").replace("]", "").split("[,\\s]+")) {
            if (!prof.trim().isEmpty()) maven.getProfiles().add(prof.trim());
        }
    }

    @Override
    protected String getTestStatus() {
        return orchestrator.getMaven() != null ? orchestrator.getMaven().getTestStatus() : null;
    }

    @Override
    protected void clearTestStatus() {
        if (orchestrator.getMaven() != null) {
            orchestrator.getMaven().setTestStatus(null);
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { mavenGoalsText, mavenProfilesText };
    }
}
