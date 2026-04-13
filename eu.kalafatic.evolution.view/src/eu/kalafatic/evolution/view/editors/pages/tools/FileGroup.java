package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.model.orchestration.FileConfig;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import java.io.File;

public class FileGroup extends AToolGroup {
    private Text fileLocalPathText;
    private Text filePathText, newFilePathText;

    public FileGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "File Tool Settings", 3, false);
        SWTFactory.createLabel(group, "Project Root:");
        fileLocalPathText = SWTFactory.createText(group);
        fileLocalPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createEditButton(group, fileLocalPathText);

        SWTFactory.createLabel(group, "Path:");
        filePathText = SWTFactory.createText(group);
        SWTFactory.createLabel(group, "");

        SWTFactory.createLabel(group, "New Path:");
        newFilePathText = SWTFactory.createText(group);
        SWTFactory.createLabel(group, "");

        Composite btnComp = toolkit.createComposite(group);
        btnComp.setLayout(new RowLayout());
        GridData btnGd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        btnComp.setLayoutData(btnGd);

        Button createFileBtn = SWTFactory.createButton(btnComp, "Create File");
        createFileBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("WRITE " + filePathText.getText() + "\n ", "file");
            }
        });

        Button createFolderBtn = SWTFactory.createButton(btnComp, "Create Folder");
        createFolderBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("MKDIR " + filePathText.getText(), "file");
            }
        });

        Button removeBtn = SWTFactory.createButton(btnComp, "Remove");
        removeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("DELETE " + filePathText.getText(), "file");
            }
        });

        Button renameBtn = SWTFactory.createButton(btnComp, "Rename");
        renameBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeCommand("RENAME " + filePathText.getText() + " " + newFilePathText.getText(), "file");
            }
        });

        Button testBtn = SWTFactory.createButton(btnComp, "Test File");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testFile();
            }
        });
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
            MessageDialog.openInformation(group.getShell(), "File Test", "File operations successful (Create, Write, Read, Delete).");
            if (orchestrator.getFileConfig() != null) {
                orchestrator.getFileConfig().setTestStatus("SUCCESS");
                updateGroupStatus();
            }
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "File Test Failed", e.getMessage());
            if (orchestrator.getFileConfig() != null) {
                orchestrator.getFileConfig().setTestStatus("FAILED");
                updateGroupStatus();
            }
        }
    }

    private File getWorkingDir() {
        String path = fileLocalPathText.getText();
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        if (editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
            org.eclipse.core.resources.IProject project = ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput()).getFile().getProject();
            org.eclipse.core.resources.IFolder resources = project.getFolder("resources");
            if (resources.exists()) {
                return resources.getLocation().toFile();
            }
            return project.getLocation().toFile();
        }
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Override
    protected void refreshUI() {
        if (orchestrator.getFileConfig() != null) {
            FileConfig config = orchestrator.getFileConfig();
            fileLocalPathText.setText(config.getLocalPath() != null ? config.getLocalPath() : "");
            updateGroupStatus();
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator.getFileConfig() == null) {
            orchestrator.setFileConfig(OrchestrationFactory.eINSTANCE.createFileConfig());
        }
        FileConfig config = orchestrator.getFileConfig();
        config.setLocalPath(fileLocalPathText.getText());
    }

    @Override
    protected String getTestStatus() {
        return orchestrator.getFileConfig() != null ? orchestrator.getFileConfig().getTestStatus() : null;
    }

    @Override
    protected void clearTestStatus() {
        if (orchestrator.getFileConfig() != null) {
            orchestrator.getFileConfig().setTestStatus(null);
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { fileLocalPathText };
    }
}
