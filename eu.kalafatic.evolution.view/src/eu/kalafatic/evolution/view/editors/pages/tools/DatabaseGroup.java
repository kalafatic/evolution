package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.orchestration.DatabaseTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Database;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import java.io.File;

public class DatabaseGroup {
    private Composite group;
    private Text dbUrlText, dbUsernameText, dbPasswordText, dbDriverText;
    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private Color successColor;

    public DatabaseGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.successColor = successColor;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(parent, "Database Tool Settings", 3, false);
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
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testDatabase();
            }
        });
    }

    private void testDatabase() {
        try {
            DatabaseTool tool = new DatabaseTool();
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            String result = tool.execute("TEST_CONNECTION", workingDir, context);
            MessageDialog.openInformation(group.getShell(), "Database Test", result);
            if (orchestrator.getDatabase() != null) {
                orchestrator.getDatabase().setTestStatus("SUCCESS");
                updateGroupStatus();
            }
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Database Test Failed", e.getMessage());
            if (orchestrator.getDatabase() != null) {
                orchestrator.getDatabase().setTestStatus("FAILED");
                updateGroupStatus();
            }
        }
    }

    private File getWorkingDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public void updateUI() {
        if (orchestrator.getDatabase() != null) {
            Database db = orchestrator.getDatabase();
            dbUrlText.setText(db.getUrl() != null ? db.getUrl() : "");
            dbDriverText.setText(db.getDriver() != null ? db.getDriver() : "");
            dbUsernameText.setText(db.getUsername() != null ? db.getUsername() : "");
            dbPasswordText.setText(db.getPassword() != null ? db.getPassword() : "");
            updateGroupStatus();
        }
    }

    public void updateModel() {
        if (orchestrator.getDatabase() == null) {
            orchestrator.setDatabase(OrchestrationFactory.eINSTANCE.createDatabase());
        }
        Database db = orchestrator.getDatabase();
        db.setUrl(dbUrlText.getText());
        db.setDriver(dbDriverText.getText());
        db.setUsername(dbUsernameText.getText());
        db.setPassword(dbPasswordText.getText());
    }

    public void updateGroupStatus() {
        if (orchestrator.getDatabase() != null) {
            String status = orchestrator.getDatabase().getTestStatus();
            if ("SUCCESS".equals(status)) {
                group.setBackground(successColor);
            } else if ("FAILED".equals(status)) {
                group.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_RED));
            } else {
                group.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            }
        }
    }

    public Text[] getTextFields() {
        return new Text[] { dbUrlText, dbUsernameText, dbPasswordText, dbDriverText };
    }

    public Composite getGroup() {
        return group;
    }
}
