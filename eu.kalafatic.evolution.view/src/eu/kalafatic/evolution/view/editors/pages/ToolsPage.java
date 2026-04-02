package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ToolsPage extends ScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private boolean isUpdating = false;

    private Text gitRepoText, gitBranchText, gitLocalPathText;
    private Text mavenGoalsText, mavenProfilesText;
    private Text fileLocalPathText;
    private Text dbUrlText, dbUsernameText, dbPasswordText, dbDriverText;

    public ToolsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        createControl();
    }

    private void createControl() {
        Composite comp = new Composite(this, SWT.NONE);
        comp.setLayout(new GridLayout(1, false));

        createGitGroup(comp);
        createMavenGroup(comp);
        createFileGroup(comp);
        createDatabaseGroup(comp);

        ModifyListener ml = e -> {
            if (orchestrator != null && !isUpdating) {
                updateModelFromFields();
                editor.setDirty(true);
            }
        };

        Text[] texts = { gitRepoText, gitBranchText, gitLocalPathText, mavenGoalsText, mavenProfilesText,
                         fileLocalPathText, dbUrlText, dbUsernameText, dbPasswordText, dbDriverText };
        for (Text t : texts) {
            t.addModifyListener(ml);
        }

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateUIFromModel();
    }

    private void createGitGroup(Composite parent) {
        Group group = SWTFactory.createGroup(parent, "Git Tool Settings", 3);
        SWTFactory.createLabel(group, "Repository URL:");
        gitRepoText = new Text(group, SWT.BORDER);
        gitRepoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, gitRepoText);

        SWTFactory.createLabel(group, "Branch:");
        gitBranchText = new Text(group, SWT.BORDER);
        gitBranchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, gitBranchText);

        SWTFactory.createLabel(group, "Local Path:");
        gitLocalPathText = new Text(group, SWT.BORDER);
        gitLocalPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, gitLocalPathText);
    }

    private void createMavenGroup(Composite parent) {
        Group group = SWTFactory.createGroup(parent, "Maven Tool Settings", 3);
        SWTFactory.createLabel(group, "Goals:");
        mavenGoalsText = new Text(group, SWT.BORDER);
        mavenGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, mavenGoalsText);

        SWTFactory.createLabel(group, "Profiles:");
        mavenProfilesText = new Text(group, SWT.BORDER);
        mavenProfilesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, mavenProfilesText);
    }

    private void createFileGroup(Composite parent) {
        Group group = SWTFactory.createGroup(parent, "File Tool Settings", 3);
        SWTFactory.createLabel(group, "Project Root:");
        fileLocalPathText = new Text(group, SWT.BORDER);
        fileLocalPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, fileLocalPathText);
    }

    private void createDatabaseGroup(Composite parent) {
        Group group = SWTFactory.createGroup(parent, "Database Tool Settings", 3);
        SWTFactory.createLabel(group, "JDBC URL:");
        dbUrlText = new Text(group, SWT.BORDER);
        dbUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, dbUrlText);

        SWTFactory.createLabel(group, "Driver Class:");
        dbDriverText = new Text(group, SWT.BORDER);
        dbDriverText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, dbDriverText);

        SWTFactory.createLabel(group, "Username:");
        dbUsernameText = new Text(group, SWT.BORDER);
        dbUsernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, dbUsernameText);

        SWTFactory.createLabel(group, "Password:");
        dbPasswordText = new Text(group, SWT.BORDER | SWT.PASSWORD);
        dbPasswordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, dbPasswordText);
    }

    public void updateUIFromModel() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;

        if (orchestrator.getGit() != null) {
            gitRepoText.setText(orchestrator.getGit().getRepositoryUrl() != null ? orchestrator.getGit().getRepositoryUrl() : "");
            gitBranchText.setText(orchestrator.getGit().getBranch() != null ? orchestrator.getGit().getBranch() : "");
            gitLocalPathText.setText(orchestrator.getGit().getLocalPath() != null ? orchestrator.getGit().getLocalPath() : "");
        }

        if (orchestrator.getMaven() != null) {
            mavenGoalsText.setText(orchestrator.getMaven().getGoals().toString());
            mavenProfilesText.setText(orchestrator.getMaven().getProfiles().toString());
        }

        if (orchestrator.getFileConfig() != null) {
            fileLocalPathText.setText(orchestrator.getFileConfig().getLocalPath() != null ? orchestrator.getFileConfig().getLocalPath() : "");
        }

        if (orchestrator.getDatabase() != null) {
            dbUrlText.setText(orchestrator.getDatabase().getUrl() != null ? orchestrator.getDatabase().getUrl() : "");
            dbDriverText.setText(orchestrator.getDatabase().getDriver() != null ? orchestrator.getDatabase().getDriver() : "");
            dbUsernameText.setText(orchestrator.getDatabase().getUsername() != null ? orchestrator.getDatabase().getUsername() : "");
            dbPasswordText.setText(orchestrator.getDatabase().getPassword() != null ? orchestrator.getDatabase().getPassword() : "");
        }

        isUpdating = false;
    }

    private void updateModelFromFields() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;

        if (orchestrator.getGit() == null) {
            orchestrator.setGit(OrchestrationFactory.eINSTANCE.createGit());
        }
        orchestrator.getGit().setRepositoryUrl(gitRepoText.getText());
        orchestrator.getGit().setBranch(gitBranchText.getText());
        orchestrator.getGit().setLocalPath(gitLocalPathText.getText());

        if (orchestrator.getMaven() == null) {
            orchestrator.setMaven(OrchestrationFactory.eINSTANCE.createMaven());
        }
        orchestrator.getMaven().getGoals().clear();
        for (String goal : mavenGoalsText.getText().replace("[", "").replace("]", "").split("[,\\s]+")) {
            if (!goal.trim().isEmpty()) orchestrator.getMaven().getGoals().add(goal.trim());
        }
        orchestrator.getMaven().getProfiles().clear();
        for (String prof : mavenProfilesText.getText().replace("[", "").replace("]", "").split("[,\\s]+")) {
            if (!prof.trim().isEmpty()) orchestrator.getMaven().getProfiles().add(prof.trim());
        }

        if (orchestrator.getFileConfig() == null) {
            orchestrator.setFileConfig(OrchestrationFactory.eINSTANCE.createFileConfig());
        }
        orchestrator.getFileConfig().setLocalPath(fileLocalPathText.getText());

        if (orchestrator.getDatabase() == null) {
            orchestrator.setDatabase(OrchestrationFactory.eINSTANCE.createDatabase());
        }
        orchestrator.getDatabase().setUrl(dbUrlText.getText());
        orchestrator.getDatabase().setDriver(dbDriverText.getText());
        orchestrator.getDatabase().setUsername(dbUsernameText.getText());
        orchestrator.getDatabase().setPassword(dbPasswordText.getText());

        isUpdating = false;
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        updateUIFromModel();
    }
}
