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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.orchestration.DatabaseTool;
import eu.kalafatic.evolution.controller.orchestration.FileTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import java.io.File;

public class ToolsPage extends ScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private boolean isUpdating = false;

    private Text gitRepoText, gitBranchText, gitLocalPathText;
    private Text mavenGoalsText, mavenProfilesText;
    private Text fileLocalPathText;
    private Text dbUrlText, dbUsernameText, dbPasswordText, dbDriverText;
    private Text sourceVersionText, targetVersionText, cPathText, cppPathText, makePathText, cmakePathText;

    private Group gitGroup, mavenGroup, fileGroup, dbGroup, compilerGroup;
    private Color successColor;

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
                         sourceVersionText, targetVersionText, cPathText, cppPathText, makePathText, cmakePathText };
        for (Text t : texts) {
            t.addModifyListener(ml);
        }

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateUIFromModel();
    }

    private void createGitGroup(Composite parent) {
        gitGroup = SWTFactory.createGroup(parent, "Git Tool Settings", 3);
        Group group = gitGroup;
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

        Button testBtn = SWTFactory.createButton(group, "Test Git");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testGit();
            }
        });
    }

    private void createMavenGroup(Composite parent) {
        mavenGroup = SWTFactory.createGroup(parent, "Maven Tool Settings", 3);
        Group group = mavenGroup;
        SWTFactory.createLabel(group, "Goals:");
        mavenGoalsText = new Text(group, SWT.BORDER);
        mavenGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, mavenGoalsText);

        SWTFactory.createLabel(group, "Profiles:");
        mavenProfilesText = new Text(group, SWT.BORDER);
        mavenProfilesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, mavenProfilesText);

        Button testBtn = SWTFactory.createButton(group, "Test Maven");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testMaven();
            }
        });
    }

    private void createFileGroup(Composite parent) {
        fileGroup = SWTFactory.createGroup(parent, "File Tool Settings", 3);
        Group group = fileGroup;
        SWTFactory.createLabel(group, "Project Root:");
        fileLocalPathText = new Text(group, SWT.BORDER);
        fileLocalPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, fileLocalPathText);

        Button testBtn = SWTFactory.createButton(group, "Test File");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testFile();
            }
        });
    }

    private void createDatabaseGroup(Composite parent) {
        dbGroup = SWTFactory.createGroup(parent, "Database Tool Settings", 3);
        Group group = dbGroup;
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

        Button testBtn = SWTFactory.createButton(group, "Test DB");
        testBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                testDatabase();
            }
        });
    }

    private void createCompilerGroup(Composite parent) {
        compilerGroup = SWTFactory.createGroup(parent, "Compiler & Language Settings", 3);
        Group group = compilerGroup;

        SWTFactory.createLabel(group, "Java Source Version:");
        sourceVersionText = new Text(group, SWT.BORDER);
        sourceVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, sourceVersionText);

        SWTFactory.createLabel(group, "Java Target Version:");
        targetVersionText = new Text(group, SWT.BORDER);
        targetVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, targetVersionText);

        SWTFactory.createLabel(group, "C Path (gcc):");
        cPathText = new Text(group, SWT.BORDER);
        cPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, cPathText);

        SWTFactory.createLabel(group, "C++ Path (g++):");
        cppPathText = new Text(group, SWT.BORDER);
        cppPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, cppPathText);

        SWTFactory.createLabel(group, "Make Path:");
        makePathText = new Text(group, SWT.BORDER);
        makePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, makePathText);

        SWTFactory.createLabel(group, "CMake Path:");
        cmakePathText = new Text(group, SWT.BORDER);
        cmakePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, cmakePathText);

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

        isUpdating = false;
    }

    private void updateGroupStatus(Group group, String status) {
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
        super.dispose();
    }

    private void resetBackgrounds() {
        Color defaultColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        gitGroup.setBackground(defaultColor);
        mavenGroup.setBackground(defaultColor);
        fileGroup.setBackground(defaultColor);
        dbGroup.setBackground(defaultColor);
        compilerGroup.setBackground(defaultColor);

        if (orchestrator.getGit() != null) orchestrator.getGit().setTestStatus(null);
        if (orchestrator.getMaven() != null) orchestrator.getMaven().setTestStatus(null);
        if (orchestrator.getFileConfig() != null) orchestrator.getFileConfig().setTestStatus(null);
        if (orchestrator.getDatabase() != null) orchestrator.getDatabase().setTestStatus(null);
        if (orchestrator.getCompiler() != null) orchestrator.getCompiler().setTestStatus(null);
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
