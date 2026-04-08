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
import eu.kalafatic.evolution.model.orchestration.Compiler;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import java.io.File;

public class CompilerGroup extends AToolGroup {
    private Text sourceVersionText, targetVersionText, cPathText, cppPathText, makePathText, cmakePathText;

    public CompilerGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Compiler & Language Settings", 3, false);

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
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testCompiler();
            }
        });
    }

    private void testCompiler() {
        try {
            eu.kalafatic.evolution.controller.orchestration.CppTool tool = new eu.kalafatic.evolution.controller.orchestration.CppTool();
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            String result = tool.execute("TEST_CONNECTION", workingDir, context);
            MessageDialog.openInformation(group.getShell(), "Compiler Test", result);
            if (orchestrator.getCompiler() != null) {
                orchestrator.getCompiler().setTestStatus("SUCCESS");
                updateGroupStatus();
            }
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Compiler Test Failed", e.getMessage());
            if (orchestrator.getCompiler() != null) {
                orchestrator.getCompiler().setTestStatus("FAILED");
                updateGroupStatus();
            }
        }
    }

    private File getWorkingDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Override
    public void updateUI() {
        if (orchestrator.getCompiler() != null) {
            Compiler compiler = orchestrator.getCompiler();
            sourceVersionText.setText(compiler.getSourceVersion() != null ? compiler.getSourceVersion() : "");
            targetVersionText.setText(compiler.getTargetVersion() != null ? compiler.getTargetVersion() : "");
            cPathText.setText(compiler.getCPath() != null ? compiler.getCPath() : "");
            cppPathText.setText(compiler.getCppPath() != null ? compiler.getCppPath() : "");
            makePathText.setText(compiler.getMakePath() != null ? compiler.getMakePath() : "");
            cmakePathText.setText(compiler.getCmakePath() != null ? compiler.getCmakePath() : "");
            updateGroupStatus();
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator.getCompiler() == null) {
            orchestrator.setCompiler(OrchestrationFactory.eINSTANCE.createCompiler());
        }
        Compiler compiler = orchestrator.getCompiler();
        compiler.setSourceVersion(sourceVersionText.getText());
        compiler.setTargetVersion(targetVersionText.getText());
        compiler.setCPath(cPathText.getText());
        compiler.setCppPath(cppPathText.getText());
        compiler.setMakePath(makePathText.getText());
        compiler.setCmakePath(cmakePathText.getText());
    }

    @Override
    protected String getTestStatus() {
        return orchestrator.getCompiler() != null ? orchestrator.getCompiler().getTestStatus() : null;
    }

    @Override
    protected void clearTestStatus() {
        if (orchestrator.getCompiler() != null) {
            orchestrator.getCompiler().setTestStatus(null);
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { sourceVersionText, targetVersionText, cPathText, cppPathText, makePathText, cmakePathText };
    }
}
