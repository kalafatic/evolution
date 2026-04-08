package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import java.io.File;

public class GitGroup extends AToolGroup {
    private Text gitRepoText, gitBranchText, gitLocalPathText;

    public GitGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Git Tool Settings", 3, true);
        SWTFactory.createLabel(group, "Repository URL:");
        gitRepoText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, gitRepoText);

        SWTFactory.createLabel(group, "Branch:");
        gitBranchText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, gitBranchText);

        SWTFactory.createLabel(group, "Local Path:");
        gitLocalPathText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, gitLocalPathText);

        SWTFactory.createLabel(group, "");
        Button testBtn = SWTFactory.createButton(group, "Test Git");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testGit();
            }
        });
    }

    private void testGit() {
        try {
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            eu.kalafatic.evolution.controller.orchestration.ShellTool shell = new eu.kalafatic.evolution.controller.orchestration.ShellTool();
            String result = shell.execute("git --version", workingDir, context);
            MessageDialog.openInformation(group.getShell(), "Git Test", "Git is available:\n" + result);
            if (orchestrator.getGit() != null) {
                orchestrator.getGit().setTestStatus("SUCCESS");
                updateGroupStatus();
            }
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Git Test Failed", e.getMessage());
            if (orchestrator.getGit() != null) {
                orchestrator.getGit().setTestStatus("FAILED");
                updateGroupStatus();
            }
        }
    }

    private File getWorkingDir() {
        String path = gitLocalPathText.getText();
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Override
    public void updateUI() {
        if (orchestrator.getGit() != null) {
            Git git = orchestrator.getGit();
            gitRepoText.setText(git.getRepositoryUrl() != null ? git.getRepositoryUrl() : "");
            gitBranchText.setText(git.getBranch() != null ? git.getBranch() : "");
            gitLocalPathText.setText(git.getLocalPath() != null ? git.getLocalPath() : "");
            updateGroupStatus();
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator.getGit() == null) {
            orchestrator.setGit(OrchestrationFactory.eINSTANCE.createGit());
        }
        Git git = orchestrator.getGit();
        git.setRepositoryUrl(gitRepoText.getText());
        git.setBranch(gitBranchText.getText());
        git.setLocalPath(gitLocalPathText.getText());
    }

    @Override
    protected String getTestStatus() {
        return orchestrator.getGit() != null ? orchestrator.getGit().getTestStatus() : null;
    }

    @Override
    protected void clearTestStatus() {
        if (orchestrator.getGit() != null) {
            orchestrator.getGit().setTestStatus(null);
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { gitRepoText, gitBranchText, gitLocalPathText };
    }
}
