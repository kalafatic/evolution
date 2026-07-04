package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool;
import eu.kalafatic.evolution.view.util.GitRegistryHelper;
import eu.kalafatic.utils.factories.GUIFactory;
import java.io.File;
import java.util.List;

public class GitGroup extends AToolGroup {
    private Combo repoSelector;
    private Text gitRepoText, gitBranchText, gitUsernameText, gitPasswordText;
    private Combo gitLocalPathText;
    private Text branchNameText, commitMsgText;
    private ControlDecoration urlDecorator, pathDecorator;
    
    private String currentRepoId = EclipseGitEvoTool.REPO_EVOLUTION;

    public GitGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Git Tool Settings", 1, true);
        
        Group selectorGroup = GUIFactory.INSTANCE.createGroup(group, "Repository Selection", 2);
        GUIFactory.INSTANCE.createLabel(selectorGroup, "Select Repository:");
        repoSelector = new Combo(selectorGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
        repoSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String id : EclipseGitEvoTool.getRegisteredRepositoryIds()) {
            repoSelector.add(id);
        }
        repoSelector.select(0);
        repoSelector.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateModel();
                currentRepoId = repoSelector.getText();
                refreshUI();
            }
        });

        createGitControls(GUIFactory.INSTANCE.createGroup(group, "Configuration", 3));
    }

	private void createGitControls(Composite composite) {
		GUIFactory.INSTANCE.createLabel(composite, "Repository URL:");
        gitRepoText = GUIFactory.INSTANCE.createText(composite);
        urlDecorator = createErrorDecorator(gitRepoText, "URL cannot be empty");
        GUIFactory.INSTANCE.createEditButton(composite, gitRepoText);

        GUIFactory.INSTANCE.createLabel(composite, "Branch:");
        gitBranchText = GUIFactory.INSTANCE.createText(composite);
        GUIFactory.INSTANCE.createEditButton(composite, gitBranchText);

        GUIFactory.INSTANCE.createLabel(composite, "Username:");
        gitUsernameText = GUIFactory.INSTANCE.createText(composite);
        GUIFactory.INSTANCE.createEditButton(composite, gitUsernameText);

        GUIFactory.INSTANCE.createLabel(composite, "Password:");
        gitPasswordText = GUIFactory.INSTANCE.createPasswordText(composite);
        GUIFactory.INSTANCE.createEditButton(composite, gitPasswordText);

        GUIFactory.INSTANCE.createLabel(composite, "Local Path:");
        gitLocalPathText = GUIFactory.INSTANCE.createCombo(composite);
        pathDecorator = createErrorDecorator(gitLocalPathText, "Path cannot be empty");

        List<String> repos = ProjectModelManager.getInstance().getAvailableLocalRepositories();
        for (String r : repos) {
            gitLocalPathText.add(r);
        }

        Button browseBtn = GUIFactory.INSTANCE.createButton(composite, "Browse");
        browseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(group.getShell());
                dialog.setText("Select Git Repository Folder");
                String path = dialog.open();
                if (path != null) {
                    gitLocalPathText.setText(path);
                    updateModel();
                }
            }
        });

        GUIFactory.INSTANCE.createLabel(composite, "Branch Name:");
        branchNameText = GUIFactory.INSTANCE.createText(composite);
        GUIFactory.INSTANCE.createLabel(composite, "");

        GUIFactory.INSTANCE.createLabel(composite, "Commit Msg:");
        commitMsgText = GUIFactory.INSTANCE.createText(composite);
        GUIFactory.INSTANCE.createLabel(composite, "");

        Composite btnComp = GUIFactory.INSTANCE.createComposite(composite);
        btnComp.setLayout(new GridLayout(6, false));
        btnComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        Button createBtn = GUIFactory.INSTANCE.createButton(btnComp, "Create");
        createBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateModel();
                String localPath = gitLocalPathText.getText();
                if (localPath != null && !localPath.isEmpty()) {
                    File f = new File(localPath);
                    if (!f.exists()) f.mkdirs();
                    executeCommand("init", "git");
                    GitRegistryHelper.registerGitRepository(f);
                }
            }
        });

        GUIFactory.INSTANCE.createButton(btnComp, "New Branch").addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) { executeCommand("BRANCH " + branchNameText.getText(), "git"); }
        });

        GUIFactory.INSTANCE.createButton(btnComp, "Commit").addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) { executeCommand("COMMIT " + commitMsgText.getText(), "git"); }
        });

        GUIFactory.INSTANCE.createButton(btnComp, "Pull").addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) { executeCommand("PULL", "git"); }
        });

        GUIFactory.INSTANCE.createButton(btnComp, "Push").addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) { executeCommand("PUSH", "git"); }
        });

        GUIFactory.INSTANCE.createButton(btnComp, "Test Git").addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) { testGit(); }
        });
	}

    private ControlDecoration createErrorDecorator(Control control, String description) {
        ControlDecoration dec = new ControlDecoration(control, SWT.TOP | SWT.LEFT);
        dec.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        dec.setDescriptionText(description);
        dec.hide();
        return dec;
    }

    private void testGit() {
        try {
            String path = gitLocalPathText.getText();
            File workingDir = (path != null && !path.isEmpty()) ? new File(path) : new File(System.getProperty("java.io.tmpdir"));

            TaskContext context = new TaskContext(orchestrator, workingDir);
            eu.kalafatic.evolution.controller.tools.ShellTool shell = new eu.kalafatic.evolution.controller.tools.ShellTool();

            String gitVersion = shell.execute("git --version", workingDir, context);
            StringBuilder statusMsg = new StringBuilder("Git available: " + gitVersion + "\n");

            String url = gitRepoText.getText();
            if (!url.isEmpty()) {
                String user = gitUsernameText.getText();
                String pass = gitPasswordText.getText();
                String remoteUrl = url;
                if (!user.isEmpty() && !pass.isEmpty() && url.startsWith("http")) {
                    String proto = url.startsWith("https") ? "https://" : "http://";
                    String rest = url.substring(proto.length());
                    remoteUrl = proto + java.net.URLEncoder.encode(user, "UTF-8") + ":" + java.net.URLEncoder.encode(pass, "UTF-8") + "@" + rest;
                }
                statusMsg.append("Testing remote connectivity...\n");
                shell.execute("git ls-remote " + remoteUrl + " HEAD", workingDir, context);
                statusMsg.append("Remote connection successful!");
            }

            MessageDialog.openInformation(group.getShell(), "Git Test", statusMsg.toString());
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Git Test Failed", e.getMessage());
        }
    }

    @Override
    protected void refreshUI() {
        setTextSafe(gitRepoText, EclipseGitEvoTool.getRepositoryRemote(currentRepoId));
        setTextSafe(gitBranchText, EclipseGitEvoTool.getRepositoryBranch(currentRepoId));
        setTextSafe(gitUsernameText, EclipseGitEvoTool.getRepositoryUsername(currentRepoId));
        setTextSafe(gitPasswordText, EclipseGitEvoTool.getRepositoryPassword(currentRepoId));
        selectSafe(gitLocalPathText, EclipseGitEvoTool.getRepositoryPath(currentRepoId));
        
        // Branch name and commit msg are transient/contextual for operations, not persisted in Tool
    }

    @Override
    public void updateModel() {
        EclipseGitEvoTool.changeRemoteUrl(currentRepoId, gitRepoText.getText());
        EclipseGitEvoTool.changeBranch(currentRepoId, gitBranchText.getText());
        EclipseGitEvoTool.changeCredentials(currentRepoId, gitUsernameText.getText(), gitPasswordText.getText());
        EclipseGitEvoTool.changeRepositoryLocation(currentRepoId, gitLocalPathText.getText());
        
        // Also update the active orchestrator model if it matches
        if (currentRepoId.equals(EclipseGitEvoTool.REPO_EVOLUTION)) {
            ProjectModelManager.getInstance().updateGitSettings(orchestrator, gitRepoText.getText(), gitBranchText.getText(), gitUsernameText.getText(), gitPasswordText.getText(), gitLocalPathText.getText());
        } else if (currentRepoId.equals(EclipseGitEvoTool.REPO_WORKSPACE)) {
            if (orchestrator.getSupervisorSettings() != null) {
                ProjectModelManager.getInstance().updateGitSettings(orchestrator.getSupervisorSettings().getGit(), gitRepoText.getText(), gitBranchText.getText(), gitUsernameText.getText(), gitPasswordText.getText(), gitLocalPathText.getText());
            }
        } else if (currentRepoId.equals(EclipseGitEvoTool.REPO_LLM)) {
            // Update LLM repo settings in any active ForgeSession if appropriate,
            // or just rely on the central tool registry which ForgeSessionManager uses.
            for (eu.kalafatic.evolution.model.orchestration.ForgeSession session : orchestrator.getForgeSessions()) {
                if (session.getGit() != null) {
                    ProjectModelManager.getInstance().updateGitSettings(session.getGit(), gitRepoText.getText(), gitBranchText.getText(), gitUsernameText.getText(), gitPasswordText.getText(), gitLocalPathText.getText());
                }
            }
        }
    }

    @Override
    protected String getTestStatus() { return null; }

    @Override
    protected void clearTestStatus() {}

    @Override
    public Text[] getTextFields() {
        return new Text[] { gitRepoText, gitBranchText, gitUsernameText, gitPasswordText, branchNameText, commitMsgText };
    }

    @Override
    public Control[] getControls() {
        return new Control[] { repoSelector, gitRepoText, gitBranchText, gitUsernameText, gitPasswordText, gitLocalPathText, branchNameText, commitMsgText };
    }
}
