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
import eu.kalafatic.utils.factories.GUIFactory;
import java.io.File;

public class CompilerGroup extends AToolGroup {
    private Text sourceVersionText, targetVersionText, cPathText, cppPathText, makePathText, cmakePathText;

    public CompilerGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Compiler & Language Settings", 3, false);

        GUIFactory.INSTANCE.createLabel(group, "Java Source Version:");
        sourceVersionText = GUIFactory.INSTANCE.createText(group);
        sourceVersionText.setText(orchestrator.getCompiler() != null && orchestrator.getCompiler().getSourceVersion() != null ? orchestrator.getCompiler().getSourceVersion() : "");
        GUIFactory.INSTANCE.createEditButton(group, sourceVersionText);

        GUIFactory.INSTANCE.createLabel(group, "Java Target Version:");
        targetVersionText = GUIFactory.INSTANCE.createText(group);
        targetVersionText.setText(orchestrator.getCompiler() != null && orchestrator.getCompiler().getTargetVersion() != null ? orchestrator.getCompiler().getTargetVersion() : "");
        GUIFactory.INSTANCE.createEditButton(group, targetVersionText);

        GUIFactory.INSTANCE.createLabel(group, "C Path (gcc):");
        cPathText = GUIFactory.INSTANCE.createText(group);
        cPathText.setText(orchestrator.getCompiler() != null && orchestrator.getCompiler().getCPath() != null ? orchestrator.getCompiler().getCPath() : "");
        GUIFactory.INSTANCE.createEditButton(group, cPathText);

        GUIFactory.INSTANCE.createLabel(group, "C++ Path (g++):");
        cppPathText = GUIFactory.INSTANCE.createText(group);
        cppPathText.setText(orchestrator.getCompiler() != null && orchestrator.getCompiler().getCppPath() != null ? orchestrator.getCompiler().getCppPath() : "");
        GUIFactory.INSTANCE.createEditButton(group, cppPathText);

        GUIFactory.INSTANCE.createLabel(group, "Make Path:");
        makePathText = GUIFactory.INSTANCE.createText(group);
        makePathText.setText(orchestrator.getCompiler() != null && orchestrator.getCompiler().getMakePath() != null ? orchestrator.getCompiler().getMakePath() : "");
        GUIFactory.INSTANCE.createEditButton(group, makePathText);

        GUIFactory.INSTANCE.createLabel(group, "CMake Path:");
        cmakePathText = GUIFactory.INSTANCE.createText(group);
        cmakePathText.setText(orchestrator.getCompiler() != null && orchestrator.getCompiler().getCmakePath() != null ? orchestrator.getCompiler().getCmakePath() : "");
        GUIFactory.INSTANCE.createEditButton(group, cmakePathText);

        GUIFactory.INSTANCE.createLabel(group, "");
        Button testBtn = GUIFactory.INSTANCE.createButton(group, "Test Compilers");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testCompiler();
            }
        });
    }

    private void testCompiler() {
        try {
            eu.kalafatic.evolution.controller.tools.CppTool tool = new eu.kalafatic.evolution.controller.tools.CppTool();
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
    protected void refreshUI() {
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
