package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.utils.factories.GUIFactory;
import java.io.File;
import java.util.List;

public class GitGroup extends AToolGroup {
    private Text gitRepoText, gitBranchText, gitUsernameText, gitPasswordText;
    private Combo gitLocalPathText;
    private Text branchNameText, commitMsgText;
    private Git git;
    
    private Text gitRepoTextEvo, gitBranchTextEvo, gitUsernameTextEvo, gitPasswordTextEvo;
    private Combo gitLocalPathTextEvo;
    private Text branchNameTextEvo, commitMsgTextEvo;
    private Git gitEvo;

    public GitGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Git Tool Settings", 2, true);      
        
        createGitControllsEvolution(GUIFactory.INSTANCE.createGroup(group, "Evolution (Codebase)", 3));
        createGitControllsEvo(GUIFactory.INSTANCE.createGroup(group, "Evo (Self Development)", 3));
    }

	private void createGitControllsEvolution(Composite composite) {
		GUIFactory.INSTANCE.createLabel(composite, "Repository URL:");
        gitRepoText = GUIFactory.INSTANCE.createText(composite);
        gitRepoText.setText(orchestrator.getGit() != null && orchestrator.getGit().getRepositoryUrl() != null ? orchestrator.getGit().getRepositoryUrl() : "");
        GUIFactory.INSTANCE.createEditButton(composite, gitRepoText);

        GUIFactory.INSTANCE.createLabel(composite, "Branch:");
        gitBranchText = GUIFactory.INSTANCE.createText(composite);
        gitBranchText.setText(orchestrator.getGit() != null && orchestrator.getGit().getBranch() != null ? orchestrator.getGit().getBranch() : "");
        GUIFactory.INSTANCE.createEditButton(composite, gitBranchText);

        GUIFactory.INSTANCE.createLabel(composite, "Username:");
        gitUsernameText = GUIFactory.INSTANCE.createText(composite);
        gitUsernameText.setText(orchestrator.getGit() != null && orchestrator.getGit().getUsername() != null ? orchestrator.getGit().getUsername() : "");
        GUIFactory.INSTANCE.createEditButton(composite, gitUsernameText);

        GUIFactory.INSTANCE.createLabel(composite, "Password:");
        gitPasswordText = GUIFactory.INSTANCE.createPasswordText(composite);
        gitPasswordText.setText(orchestrator.getGit() != null && orchestrator.getGit().getPassword() != null ? orchestrator.getGit().getPassword() : "");
        GUIFactory.INSTANCE.createEditButton(composite, gitPasswordText);

        GUIFactory.INSTANCE.createLabel(composite, "Local Path:");
        gitLocalPathText = GUIFactory.INSTANCE.createCombo(composite);
        List<String> repos = ProjectModelManager.getInstance().getAvailableLocalRepositories();
        for (String r : repos) {
            gitLocalPathText.add(r);
        }

        String initialLocalPath = orchestrator.getGit() != null && orchestrator.getGit().getLocalPath() != null ? orchestrator.getGit().getLocalPath() : "";
        if (!initialLocalPath.isEmpty()) {
            if (gitLocalPathText.indexOf(initialLocalPath) < 0) {
                gitLocalPathText.add(initialLocalPath);
            }
            gitLocalPathText.setText(initialLocalPath);
        } else if (!repos.isEmpty()) {
            boolean found = false;
            for (int i = 0; i < gitLocalPathText.getItemCount(); i++) {
                String item = gitLocalPathText.getItem(i).toLowerCase();
                if (item.contains("evolution") || item.contains("/evo")) {
                    gitLocalPathText.select(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                gitLocalPathText.select(0);
            }
        }
        GUIFactory.INSTANCE.createLabel(composite, "");

        GUIFactory.INSTANCE.createLabel(composite, "Branch Name:");
        branchNameText = GUIFactory.INSTANCE.createText(composite);
        branchNameText.setText(orchestrator.getGit() != null && orchestrator.getGit().getBranchName() != null ? orchestrator.getGit().getBranchName() : "");
        GUIFactory.INSTANCE.createLabel(composite, "");

        GUIFactory.INSTANCE.createLabel(composite, "Commit Msg:");
        commitMsgText = GUIFactory.INSTANCE.createText(composite);
        commitMsgText.setText(orchestrator.getGit() != null && orchestrator.getGit().getCommitMsg() != null ? orchestrator.getGit().getCommitMsg() : "");
        GUIFactory.INSTANCE.createLabel(composite, "");

        Composite btnComp = GUIFactory.INSTANCE.createComposite(composite);
        btnComp.setLayout(new GridLayout(5, false));
        GridData btnGd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        btnComp.setLayoutData(btnGd);

        Button branchBtn = GUIFactory.INSTANCE.createButton(btnComp, "New Branch");
        branchBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("BRANCH " + branchNameText.getText(), "git");
            }
        });

        Button commitBtn = GUIFactory.INSTANCE.createButton(btnComp, "Commit");
        commitBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("COMMIT " + commitMsgText.getText(), "git");
            }
        });

        Button pullBtn = GUIFactory.INSTANCE.createButton(btnComp, "Pull");
        pullBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("PULL", "git");
            }
        });

        Button pushBtn = GUIFactory.INSTANCE.createButton(btnComp, "Push");
        pushBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("PUSH", "git");
            }
        });

        Button testBtn = GUIFactory.INSTANCE.createButton(btnComp, "Test Git");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testGit(git);
            }
        });
	}
	
	private void createGitControllsEvo(Composite composite) {		
		gitEvo = orchestrator.getSupervisorSettings().getGit();
		
		if (gitEvo==null) {
			git = OrchestrationFactory.eINSTANCE.createGit();
        	gitEvo.setRepositoryUrl("https://github.com/kalafatic/evo/");
        	gitEvo.setBranch("master");
            orchestrator.getSupervisorSettings().setGit(gitEvo);
		}
		
		GUIFactory.INSTANCE.createLabel(composite, "Repository URL:");
        gitRepoTextEvo = GUIFactory.INSTANCE.createText(composite);
        gitRepoTextEvo.setText(orchestrator.getGit() != null && orchestrator.getGit().getRepositoryUrl() != null ? gitEvo.getRepositoryUrl() : "");
        GUIFactory.INSTANCE.createEditButton(composite, gitRepoTextEvo);

        GUIFactory.INSTANCE.createLabel(composite, "Branch:");
        gitBranchTextEvo = GUIFactory.INSTANCE.createText(composite);
        gitBranchTextEvo.setText(gitEvo != null && gitEvo.getBranch() != null ? gitEvo.getBranch() : "");
        GUIFactory.INSTANCE.createEditButton(composite, gitBranchText);

        GUIFactory.INSTANCE.createLabel(composite, "Username:");
        gitUsernameTextEvo = GUIFactory.INSTANCE.createText(composite);
        gitUsernameTextEvo.setText(gitEvo != null && gitEvo.getUsername() != null ? gitEvo.getUsername() : "");
        GUIFactory.INSTANCE.createEditButton(composite, gitUsernameText);

        GUIFactory.INSTANCE.createLabel(composite, "Password:");
        gitPasswordTextEvo = GUIFactory.INSTANCE.createPasswordText(composite);
        gitPasswordTextEvo.setText(gitEvo != null && gitEvo.getPassword() != null ? gitEvo.getPassword() : "");
        GUIFactory.INSTANCE.createEditButton(composite, gitPasswordText);

        GUIFactory.INSTANCE.createLabel(composite, "Local Path:");
        gitLocalPathTextEvo = GUIFactory.INSTANCE.createCombo(composite);
        List<String> repos = ProjectModelManager.getInstance().getAvailableLocalRepositories();
        for (String r : repos) {
            gitLocalPathTextEvo.add(r);
        }

        String initialLocalPath = gitEvo != null && gitEvo.getLocalPath() != null ? gitEvo.getLocalPath() : "";
        if (!initialLocalPath.isEmpty()) {
            if (gitLocalPathTextEvo.indexOf(initialLocalPath) < 0) {
                gitLocalPathTextEvo.add(initialLocalPath);
            }
            gitLocalPathTextEvo.setText(initialLocalPath);
        } else if (!repos.isEmpty()) {
            boolean found = false;
            for (int i = 0; i < gitLocalPathTextEvo.getItemCount(); i++) {
                String item = gitLocalPathTextEvo.getItem(i).toLowerCase();
                if (item.contains("evolution") || item.contains("/evo")) {
                    gitLocalPathTextEvo.select(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                gitLocalPathText.select(0);
            }
        }
        GUIFactory.INSTANCE.createLabel(composite, "");

        GUIFactory.INSTANCE.createLabel(composite, "Branch Name:");
        branchNameTextEvo = GUIFactory.INSTANCE.createText(composite);
        branchNameTextEvo.setText(gitEvo != null && gitEvo.getBranchName() != null ? gitEvo.getBranchName() : "");
        GUIFactory.INSTANCE.createLabel(composite, "");

        GUIFactory.INSTANCE.createLabel(composite, "Commit Msg:");
        commitMsgTextEvo = GUIFactory.INSTANCE.createText(composite);
        commitMsgTextEvo.setText(gitEvo != null && gitEvo.getCommitMsg() != null ? gitEvo.getCommitMsg() : "");
        GUIFactory.INSTANCE.createLabel(composite, "");

        Composite btnComp = GUIFactory.INSTANCE.createComposite(composite);
        btnComp.setLayout(new GridLayout(5, false));
        GridData btnGd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        btnComp.setLayoutData(btnGd);

        Button branchBtn = GUIFactory.INSTANCE.createButton(btnComp, "New Branch");
        branchBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("BRANCH " + branchNameTextEvo.getText(), "git");
            }
        });

        Button commitBtn = GUIFactory.INSTANCE.createButton(btnComp, "Commit");
        commitBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("COMMIT " + commitMsgTextEvo.getText(), "git");
            }
        });

        Button pullBtn = GUIFactory.INSTANCE.createButton(btnComp, "Pull");
        pullBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("PULL", "git");
            }
        });

        Button pushBtn = GUIFactory.INSTANCE.createButton(btnComp, "Push");
        pushBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("PUSH", "git");
            }
        });

        Button testBtn = GUIFactory.INSTANCE.createButton(btnComp, "Test Git");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testGit(gitEvo);
            }
        });
	}

    private void testGit(Git git) {
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
            if (git != null) {
            	git.setTestStatus("SUCCESS");
                updateGroupStatus();
            }
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Git Test Failed", e.getMessage());
            if (git != null) {
            	git.setTestStatus("FAILED");
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
            setTextSafe(gitRepoText, git.getRepositoryUrl());
            setTextSafe(gitBranchText, git.getBranch());
            setTextSafe(gitUsernameText, git.getUsername());
            setTextSafe(gitPasswordText, git.getPassword());
            selectSafe(gitLocalPathText, git.getLocalPath());
            setTextSafe(branchNameText, git.getBranchName());
            setTextSafe(commitMsgText, git.getCommitMsg());
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
        return new Text[] { gitRepoText, gitBranchText, gitUsernameText, gitPasswordText };
    }

    @Override
    public Control[] getControls() {
        return new Control[] { gitRepoText, gitBranchText, gitUsernameText, gitPasswordText, gitLocalPathText };
    }
}
