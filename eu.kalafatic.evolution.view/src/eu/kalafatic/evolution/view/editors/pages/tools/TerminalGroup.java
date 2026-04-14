package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import java.io.File;

public class TerminalGroup extends AToolGroup {

    private Text outputText;
    private Text inputText;

    public TerminalGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Terminal", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        ((GridData)group.getLayoutData()).heightHint = 300;

        outputText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        outputText.setLayoutData(new GridData(GridData.FILL_BOTH));
        outputText.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_BLACK));
        outputText.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_GREEN));
        outputText.setFont(org.eclipse.jface.resource.JFaceResources.getFont(org.eclipse.jface.resource.JFaceResources.TEXT_FONT));

        inputText = new Text(group, SWT.BORDER);
        inputText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputText.setMessage("Enter command and press Enter...");
        inputText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    executeCommand(inputText.getText(), "shell");
                    inputText.setText("");
                }
            }
        });
    }

    private void appendOutput(String text) {
        Display.getDefault().asyncExec(() -> {
            if (outputText.isDisposed()) return;
            outputText.append(text);
            outputText.setSelection(outputText.getCharCount());
        });
    }

    @Override
    protected void refreshUI() {
        // Nothing to refresh from model for terminal state yet
    }

    @Override
    public void updateModel() {
        // Nothing to update in model from terminal yet
    }

    @Override
    protected String getTestStatus() {
        return null;
    }

    @Override
    protected void clearTestStatus() {
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { inputText };
    }
}
