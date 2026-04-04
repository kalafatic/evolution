package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.controller.orchestration.DatabaseTool;
import eu.kalafatic.evolution.controller.orchestration.FileTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Eclipse;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import java.io.File;

public class ToolsPage extends SharedScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private boolean isUpdating = false;

    private Text gitRepoText, gitBranchText, gitLocalPathText;
    private Text mavenGoalsText, mavenProfilesText;
    private Text fileLocalPathText;
    private Text dbUrlText, dbUsernameText, dbPasswordText, dbDriverText;
    private Text sourceVersionText, targetVersionText, cPathText, cppPathText, makePathText, cmakePathText;
    private Text eclipseWorkspaceText, eclipseInstallationText, eclipseTargetPlatformText;

    private FormToolkit toolkit;
    private Composite gitGroup, mavenGroup, fileGroup, dbGroup, compilerGroup, eclipseGroup;
    private Color successColor;

    public ToolsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.toolkit = new FormToolkit(parent.getDisplay());
        createControl();
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new GridLayout(1, false));

        createGitGroup(comp);
        createMavenGroup(comp);
        createFileGroup(comp);
        createDatabaseGroup(comp);
        createEclipseGroup(comp);
        createCompilerGroup(comp);

        successColor = new Color(getDisplay(), 200, 240, 200); // Light cool green

        ModifyListener ml = e -> {
            if (orchestrator != null && !isUpdating) {
                updateModelFromFields();
                editor.setDirty(true);
            }
        };

        Text[] texts = { gitRepoText, gitBranchText, gitLocalPathText, mavenGoalsText, mavenProfilesText,
                         fileLocalPathText, dbUrlText, dbUsernameText, dbPasswordText, dbDriverText,
                         sourceVersionText, targetVersionText, cPathText, cppPathText, makePathText, cmakePathText,
                         eclipseWorkspaceText, eclipseInstallationText, eclipseTargetPlatformText };
        for (Text t : texts) {
            t.addModifyListener(ml);
        }

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateUIFromModel();
    }

    private void createGitGroup(Composite parent) {
        gitGroup = SWTFactory.createExpandableGroup(toolkit, parent, "Git Tool Settings", 3);
        Composite group = gitGroup;
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
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testGit();
            }
        });
    }

    private void createMavenGroup(Composite parent) {
        mavenGroup = SWTFactory.createExpandableGroup(toolkit, parent, "Maven Tool Settings", 3);
        Composite group = mavenGroup;
        SWTFactory.createLabel(group, "Goals:");
        mavenGoalsText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, mavenGoalsText);

        SWTFactory.createLabel(group, "Profiles:");
        mavenProfilesText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, mavenProfilesText);

        SWTFactory.createLabel(group, "");
        Button testBtn = SWTFactory.createButton(group, "Test Maven");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testMaven();
            }
        });
    }

    private void createFileGroup(Composite parent) {
        fileGroup = SWTFactory.createExpandableGroup(toolkit, parent, "File Tool Settings", 3);
        Composite group = fileGroup;
        SWTFactory.createLabel(group, "Project Root:");
        fileLocalPathText = SWTFactory.createText(group);
        fileLocalPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, fileLocalPathText);

        SWTFactory.createLabel(group, "");
        Button testBtn = SWTFactory.createButton(group, "Test File");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testFile();
            }
        });
    }

    private void createDatabaseGroup(Composite parent) {
        dbGroup = SWTFactory.createExpandableGroup(toolkit, parent, "Database Tool Settings", 3);
        Composite group = dbGroup;
        SWTFactory.createLabel(group, "JDBC URL:");
        dbUrlText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, dbUrlText);

        SWTFactory.createLabel(group, "Driver Class:");
        dbDriverText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, dbDriverText);

        SWTFactory.createLabel(group, "Username:");
        dbUsernameText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, dbUsernameText);

        SWTFactory.createLabel(group, "Password:");
        dbPasswordText = SWTFactory.createPasswordText(group);
        SWTFactory.createEditButton(group, dbPasswordText);

        SWTFactory.createLabel(group, "");
        Button testBtn = SWTFactory.createButton(group, "Test DB");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testDatabase();
            }
        });
    }

    private void createEclipseGroup(Composite parent) {
        eclipseGroup = SWTFactory.createExpandableGroup(toolkit, parent, "Eclipse Development Settings", 3);
        Composite group = eclipseGroup;

        SWTFactory.createLabel(group, "Workspace Path:");
        eclipseWorkspaceText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, eclipseWorkspaceText);

        SWTFactory.createLabel(group, "Eclipse Installation:");
        eclipseInstallationText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, eclipseInstallationText);

        SWTFactory.createLabel(group, "Target Platform:");
        eclipseTargetPlatformText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, eclipseTargetPlatformText);

        SWTFactory.createLabel(group, "");
        Button testBtn = SWTFactory.createButton(group, "Test Eclipse");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testEclipse();
            }
        });
    }

    private void createCompilerGroup(Composite parent) {
        compilerGroup = SWTFactory.createExpandableGroup(toolkit, parent, "Compiler & Language Settings", 3);
        Composite group = compilerGroup;

        SWTFactory.createLabel(group, "Java Source Version:");
        sourceVersionText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, sourceVersionText);

        SWTFactory.createLabel(group, "Java Target Version:");
        targetVersionText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, targetVersionText);

        SWTFactory.createLabel(group, "C Path (gcc):");
        cPathText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, cPathText);

        SWTFactory.createLabel(group, "C++ Path (g++):");
        cppPathText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, cppPathText);

        SWTFactory.createLabel(group, "Make Path:");
        makePathText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, makePathText);

        SWTFactory.createLabel(group, "CMake Path:");
        cmakePathText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, cmakePathText);

        SWTFactory.createLabel(group, "");
        Button testBtn = SWTFactory.createButton(group, "Test Compilers");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testCompiler();
            }
        });
    }

    public void updateUIFromModel() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;

        if (orchestrator.getGit() != null) {
            gitRepoText.setText(orchestrator.getGit().getRepositoryUrl() != null ? orchestrator.getGit().getRepositoryUrl() : "");
            gitBranchText.setText(orchestrator.getGit().getBranch() != null ? orchestrator.getGit().getBranch() : "");
            gitLocalPathText.setText(orchestrator.getGit().getLocalPath() != null ? orchestrator.getGit().getLocalPath() : "");
            updateGroupStatus(gitGroup, orchestrator.getGit().getTestStatus());
        }

        if (orchestrator.getMaven() != null) {
            mavenGoalsText.setText(orchestrator.getMaven().getGoals().toString());
            mavenProfilesText.setText(orchestrator.getMaven().getProfiles().toString());
            updateGroupStatus(mavenGroup, orchestrator.getMaven().getTestStatus());
        }

        if (orchestrator.getFileConfig() != null) {
            fileLocalPathText.setText(orchestrator.getFileConfig().getLocalPath() != null ? orchestrator.getFileConfig().getLocalPath() : "");
            updateGroupStatus(fileGroup, orchestrator.getFileConfig().getTestStatus());
        }

        if (orchestrator.getDatabase() != null) {
            dbUrlText.setText(orchestrator.getDatabase().getUrl() != null ? orchestrator.getDatabase().getUrl() : "");
            dbDriverText.setText(orchestrator.getDatabase().getDriver() != null ? orchestrator.getDatabase().getDriver() : "");
            dbUsernameText.setText(orchestrator.getDatabase().getUsername() != null ? orchestrator.getDatabase().getUsername() : "");
            dbPasswordText.setText(orchestrator.getDatabase().getPassword() != null ? orchestrator.getDatabase().getPassword() : "");
            updateGroupStatus(dbGroup, orchestrator.getDatabase().getTestStatus());
        }

        if (orchestrator.getCompiler() != null) {
            sourceVersionText.setText(orchestrator.getCompiler().getSourceVersion() != null ? orchestrator.getCompiler().getSourceVersion() : "");
            targetVersionText.setText(orchestrator.getCompiler().getTargetVersion() != null ? orchestrator.getCompiler().getTargetVersion() : "");
            cPathText.setText(orchestrator.getCompiler().getCPath() != null ? orchestrator.getCompiler().getCPath() : "");
            cppPathText.setText(orchestrator.getCompiler().getCppPath() != null ? orchestrator.getCompiler().getCppPath() : "");
            makePathText.setText(orchestrator.getCompiler().getMakePath() != null ? orchestrator.getCompiler().getMakePath() : "");
            cmakePathText.setText(orchestrator.getCompiler().getCmakePath() != null ? orchestrator.getCompiler().getCmakePath() : "");
            updateGroupStatus(compilerGroup, orchestrator.getCompiler().getTestStatus());
        }

        if (orchestrator.getEclipse() != null) {
            eclipseWorkspaceText.setText(orchestrator.getEclipse().getWorkspace() != null ? orchestrator.getEclipse().getWorkspace() : "");
            eclipseInstallationText.setText(orchestrator.getEclipse().getInstallation() != null ? orchestrator.getEclipse().getInstallation() : "");
            eclipseTargetPlatformText.setText(orchestrator.getEclipse().getTargetPlatform() != null ? orchestrator.getEclipse().getTargetPlatform() : "");
            updateGroupStatus(eclipseGroup, orchestrator.getEclipse().getTestStatus());
        }

        isUpdating = false;
    }

    private void updateGroupStatus(Composite group, String status) {
        if ("SUCCESS".equals(status)) {
            group.setBackground(successColor);
        } else if ("FAILED".equals(status)) {
            group.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
        } else {
            group.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }
    }

    private void updateModelFromFields() {
        resetBackgrounds();
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

        if (orchestrator.getCompiler() == null) {
            orchestrator.setCompiler(OrchestrationFactory.eINSTANCE.createCompiler());
        }
        orchestrator.getCompiler().setSourceVersion(sourceVersionText.getText());
        orchestrator.getCompiler().setTargetVersion(targetVersionText.getText());
        orchestrator.getCompiler().setCPath(cPathText.getText());
        orchestrator.getCompiler().setCppPath(cppPathText.getText());
        orchestrator.getCompiler().setMakePath(makePathText.getText());
        orchestrator.getCompiler().setCmakePath(cmakePathText.getText());

        if (orchestrator.getEclipse() == null) {
            orchestrator.setEclipse(OrchestrationFactory.eINSTANCE.createEclipse());
        }
        orchestrator.getEclipse().setWorkspace(eclipseWorkspaceText.getText());
        orchestrator.getEclipse().setInstallation(eclipseInstallationText.getText());
        orchestrator.getEclipse().setTargetPlatform(eclipseTargetPlatformText.getText());

        isUpdating = false;
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        updateUIFromModel();
    }

    @Override
    public void dispose() {
        if (successColor != null && !successColor.isDisposed()) {
            successColor.dispose();
        }
        if (toolkit != null) {
            toolkit.dispose();
        }
        super.dispose();
    }

    private void resetBackgrounds() {
        Color defaultColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        gitGroup.setBackground(defaultColor);
        mavenGroup.setBackground(defaultColor);
        fileGroup.setBackground(defaultColor);
        dbGroup.setBackground(defaultColor);
        compilerGroup.setBackground(defaultColor);
        eclipseGroup.setBackground(defaultColor);

        if (orchestrator.getGit() != null) orchestrator.getGit().setTestStatus(null);
        if (orchestrator.getMaven() != null) orchestrator.getMaven().setTestStatus(null);
        if (orchestrator.getFileConfig() != null) orchestrator.getFileConfig().setTestStatus(null);
        if (orchestrator.getDatabase() != null) orchestrator.getDatabase().setTestStatus(null);
        if (orchestrator.getCompiler() != null) orchestrator.getCompiler().setTestStatus(null);
        if (orchestrator.getEclipse() != null) orchestrator.getEclipse().setTestStatus(null);
    }

    private void testGit() {
        try {
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            // GitTool execute handles specific commands, let's use a shell check for version as a basic test
            eu.kalafatic.evolution.controller.orchestration.ShellTool shell = new eu.kalafatic.evolution.controller.orchestration.ShellTool();
            String result = shell.execute("git --version", workingDir, context);
            MessageDialog.openInformation(getShell(), "Git Test", "Git is available:\n" + result);
            orchestrator.getGit().setTestStatus("SUCCESS");
            gitGroup.setBackground(successColor);
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Git Test Failed", e.getMessage());
            orchestrator.getGit().setTestStatus("FAILED");
            gitGroup.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
        }
    }

    private void testMaven() {
        try {
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            eu.kalafatic.evolution.controller.orchestration.ShellTool shell = new eu.kalafatic.evolution.controller.orchestration.ShellTool();
            String result = shell.execute(System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd -version" : "mvn -version", workingDir, context);
            MessageDialog.openInformation(getShell(), "Maven Test", "Maven is available:\n" + result);
            orchestrator.getMaven().setTestStatus("SUCCESS");
            mavenGroup.setBackground(successColor);
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Maven Test Failed", e.getMessage());
            orchestrator.getMaven().setTestStatus("FAILED");
            mavenGroup.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
        }
    }

    private void testFile() {
        try {
            FileTool tool = new FileTool();
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            String testDir = "test_tool_dir_" + System.currentTimeMillis();
            tool.execute("MKDIR " + testDir, workingDir, context);
            tool.execute("WRITE " + testDir + "/test.txt\nTest Content", workingDir, context);
            String content = tool.execute("READ " + testDir + "/test.txt", workingDir, context);
            if (!"Test Content".equals(content)) throw new Exception("File content mismatch");
            tool.execute("DELETE " + testDir, workingDir, context);
            MessageDialog.openInformation(getShell(), "File Test", "File operations successful (Create, Write, Read, Delete).");
            orchestrator.getFileConfig().setTestStatus("SUCCESS");
            fileGroup.setBackground(successColor);
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "File Test Failed", e.getMessage());
            orchestrator.getFileConfig().setTestStatus("FAILED");
            fileGroup.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
        }
    }

    private void testDatabase() {
        try {
            DatabaseTool tool = new DatabaseTool();
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            String result = tool.execute("TEST_CONNECTION", workingDir, context);
            MessageDialog.openInformation(getShell(), "Database Test", result);
            orchestrator.getDatabase().setTestStatus("SUCCESS");
            dbGroup.setBackground(successColor);
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Database Test Failed", e.getMessage());
            orchestrator.getDatabase().setTestStatus("FAILED");
            dbGroup.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
        }
    }

    private void testEclipse() {
        if (orchestrator.getEclipse() == null) {
            updateModelFromFields();
        }
        try {
            eu.kalafatic.evolution.controller.orchestration.EclipseTool tool = new eu.kalafatic.evolution.controller.orchestration.EclipseTool();
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            String result = tool.execute("TEST_CONNECTION", workingDir, context);
            MessageDialog.openInformation(getShell(), "Eclipse Tool Test", result);
            orchestrator.getEclipse().setTestStatus("SUCCESS");
            eclipseGroup.setBackground(successColor);
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Eclipse Tool Test Failed", e.getMessage());
            orchestrator.getEclipse().setTestStatus("FAILED");
            eclipseGroup.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
        }
    }

    private void testCompiler() {
        try {
            eu.kalafatic.evolution.controller.orchestration.CppTool tool = new eu.kalafatic.evolution.controller.orchestration.CppTool();
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            String result = tool.execute("TEST_CONNECTION", workingDir, context);
            MessageDialog.openInformation(getShell(), "Compiler Test", result);
            orchestrator.getCompiler().setTestStatus("SUCCESS");
            compilerGroup.setBackground(successColor);
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Compiler Test Failed", e.getMessage());
            orchestrator.getCompiler().setTestStatus("FAILED");
            compilerGroup.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
        }
    }

    private File getWorkingDir() {
        // Use a temp directory or project root if available
        String path = fileLocalPathText.getText();
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        return new File(System.getProperty("java.io.tmpdir"));
    }
}
