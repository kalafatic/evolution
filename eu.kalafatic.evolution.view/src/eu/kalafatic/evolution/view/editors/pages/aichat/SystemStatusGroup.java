package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class SystemStatusGroup extends AEvoGroup {
    private Label ollamaStatusLabel, modelStatusLabel, statusLabel;
    private ProgressBar progressBar;

    public SystemStatusGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        // Managed by AiChatPage
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "System Status", 4, false);
        SWTFactory.createLabel(group, "Ollama Status:");
        ollamaStatusLabel = new Label(group, SWT.NONE);
        ollamaStatusLabel.setText("Unknown");
        ollamaStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTFactory.createLabel(group, "Model:");
        modelStatusLabel = new Label(group, SWT.NONE);
        modelStatusLabel.setText("Not Configured");
        modelStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        statusLabel = new Label(group, SWT.NONE);
        statusLabel.setText("Idle");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
        progressBar = new ProgressBar(group, SWT.HORIZONTAL);
        progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
    }

    public void updateOllamaStatus(String text, org.eclipse.swt.graphics.Color color) {
        if (ollamaStatusLabel.isDisposed()) return;
        ollamaStatusLabel.setText(text);
        ollamaStatusLabel.setForeground(color);
    }

    public void updateModelStatus(String text) {
        if (modelStatusLabel.isDisposed()) return;
        modelStatusLabel.setText(text);
    }

    public void updateProgress(String status, int progress) {
        if (statusLabel.isDisposed()) return;
        statusLabel.setText(status);
        progressBar.setSelection(progress);
    }
}
