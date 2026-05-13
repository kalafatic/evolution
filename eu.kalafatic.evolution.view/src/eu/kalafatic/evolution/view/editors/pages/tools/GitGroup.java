package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

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
    private Text gitRepoText, gitBranchText, gitUsernameText, gitPasswordText, gitLocalPathText;
    private Text branchNameText, commitMsgText;

    public GitGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Git Tool Settings", 3, true);
        SWTFactory.createLabel(group, "Repository URL:");
        gitRepoText = SWTFactory.createText(group);
        gitRepoText.setText(orchestrator.getGit() != null && orchestrator.getGit().getRepositoryUrl() != null ? orchestrator.getGit().getRepositoryUrl() : "");
        SWTFactory.createEditButton(group, gitRepoText);

        SWTFactory.createLabel(group, "Branch:");
        gitBranchText = SWTFactory.createText(group);
        gitBranchText.setText(orchestrator.getGit() != null && orchestrator.getGit().getBranch() != null ? orchestrator.getGit().getBranch() : "");
        SWTFactory.createEditButton(group, gitBranchText);

        SWTFactory.createLabel(group, "Username:");
        gitUsernameText = SWTFactory.createText(group);
        gitUsernameText.setText(orchestrator.getGit() != null && orchestrator.getGit().getUsername() != null ? orchestrator.getGit().getUsername() : "");
        SWTFactory.createEditButton(group, gitUsernameText);

        SWTFactory.createLabel(group, "Password:");
        gitPasswordText = SWTFactory.createPasswordText(group);
        gitPasswordText.setText(orchestrator.getGit() != null && orchestrator.getGit().getPassword() != null ? orchestrator.getGit().getPassword() : "");
        SWTFactory.createEditButton(group, gitPasswordText);

        SWTFactory.createLabel(group, "Local Path:");
        gitLocalPathText = SWTFactory.createText(group);
        gitLocalPathText.setText(orchestrator.getGit() != null && orchestrator.getGit().getLocalPath() != null ? orchestrator.getGit().getLocalPath() : "");
        SWTFactory.createEditButton(group, gitLocalPathText);

        SWTFactory.createLabel(group, "Branch Name:");
        branchNameText = SWTFactory.createText(group);
        branchNameText.setText(orchestrator.getGit() != null && orchestrator.getGit().getBranchName() != null ? orchestrator.getGit().getBranchName() : "");
        SWTFactory.createLabel(group, "");

        SWTFactory.createLabel(group, "Commit Msg:");
        commitMsgText = SWTFactory.createText(group);
        commitMsgText.setText(orchestrator.getGit() != null && orchestrator.getGit().getCommitMsg() != null ? orchestrator.getGit().getCommitMsg() : "");
        SWTFactory.createLabel(group, "");

        Composite btnComp = toolkit.createComposite(group);
        btnComp.setLayout(new GridLayout(5, false));
        GridData btnGd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        btnComp.setLayoutData(btnGd);

        Button branchBtn = SWTFactory.createButton(btnComp, "New Branch");
        branchBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("BRANCH " + branchNameText.getText(), "git");
            }
        });

        Button commitBtn = SWTFactory.createButton(btnComp, "Commit");
        commitBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("COMMIT " + commitMsgText.getText(), "git");
            }
        });

        Button pullBtn = SWTFactory.createButton(btnComp, "Pull");
        pullBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("PULL", "git");
            }
        });

        Button pushBtn = SWTFactory.createButton(btnComp, "Push");
        pushBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("PUSH", "git");
            }
        });

        Button testBtn = SWTFactory.createButton(btnComp, "Test Git");
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
            eu.kalafatic.evolution.controller.tools.ShellTool shell = new eu.kalafatic.evolution.controller.tools.ShellTool();

            String gitVersion = shell.execute("git --version", workingDir, context);
            StringBuilder statusMsg = new StringBuilder("Git is available: " + gitVersion + "\n");

            String url = gitRepoText.getText();
            if (url != null && !url.isEmpty()) {
                String user = gitUsernameText.getText();
                String pass = gitPasswordText.getText();
                String remoteUrl = url;
                if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
                    if (url.startsWith("https://")) {
                        remoteUrl = "https://" + java.net.URLEncoder.encode(user, "UTF-8") + ":" +
                                    java.net.URLEncoder.encode(pass, "UTF-8") + "@" + url.substring(8);
                    }
                }

                statusMsg.append("Testing remote connectivity...\n");
                String remoteResult = shell.execute("git ls-remote " + remoteUrl + " HEAD", workingDir, context);
                statusMsg.append("Remote connection successful!");
            }

            MessageDialog.openInformation(group.getShell(), "Git Test", statusMsg.toString());
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
    protected void refreshUI() {
        if (orchestrator.getGit() != null) {
            Git git = orchestrator.getGit();
            gitRepoText.setText(git.getRepositoryUrl() != null ? git.getRepositoryUrl() : "");
            gitBranchText.setText(git.getBranch() != null ? git.getBranch() : "");
            gitUsernameText.setText(git.getUsername() != null ? git.getUsername() : "");
            gitPasswordText.setText(git.getPassword() != null ? git.getPassword() : "");
            gitLocalPathText.setText(git.getLocalPath() != null ? git.getLocalPath() : "");
            branchNameText.setText(git.getBranchName() != null ? git.getBranchName() : "");
            commitMsgText.setText(git.getCommitMsg() != null ? git.getCommitMsg() : "");
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
        git.setUsername(gitUsernameText.getText());
        git.setPassword(gitPasswordText.getText());
        git.setLocalPath(gitLocalPathText.getText());
        git.setBranchName(branchNameText.getText());
        git.setCommitMsg(commitMsgText.getText());
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
        return new Text[] { gitRepoText, gitBranchText, gitUsernameText, gitPasswordText, gitLocalPathText };
    }
}
