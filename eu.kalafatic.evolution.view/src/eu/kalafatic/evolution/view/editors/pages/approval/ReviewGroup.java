package eu.kalafatic.evolution.view.editors.pages.approval;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.orchestration.ShellTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ReviewGroup {
    private Composite group;
    private StyledText diffText;
    private MultiPageEditor editor;
    private Orchestrator orchestrator;

    public ReviewGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Review Changes (Git Diff)", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        diffText = new StyledText(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        diffText.setEditable(false);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        diffText.setLayoutData(gd);
        diffText.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());

        Button refreshBtn = toolkit.createButton(group, "Refresh Diff", SWT.PUSH);
        refreshBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                updateDiff();
            }
        });
    }

    public void updateUI(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        updateDiff();
    }

    private void updateDiff() {
        if (orchestrator == null || orchestrator.getSelfDevSession() == null || orchestrator.getSelfDevSession().getIterations().isEmpty()) {
            if (!diffText.getText().equals("No active iteration to show diff.")) {
                diffText.setText("No active iteration to show diff.");
            }
            return;
        }

        if (!(editor.getEditorInput() instanceof IFileEditorInput)) {
            if (!diffText.getText().equals("Editor input is not a file, cannot show diff.")) {
                diffText.setText("Editor input is not a file, cannot show diff.");
            }
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                IProject project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
                File projectRoot = project.getLocation().toFile();
                ShellTool shell = new ShellTool();
                TaskContext context = editor.getCurrentContext();
                if (context == null) {
                    context = new TaskContext(orchestrator, projectRoot);
                }

                final String diffResult = shell.execute("git diff HEAD", projectRoot, context);

                Display.getDefault().asyncExec(() -> {
                    if (!diffText.isDisposed()) {
                        String newText = (diffResult == null || diffResult.trim().isEmpty()) ?
                            "No changes detected in the current working directory (compared to HEAD)." : diffResult;

                        if (!diffText.getText().equals(newText)) {
                            diffText.setText(newText);
                        }
                    }
                });
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    if (!diffText.isDisposed()) {
                        String errorMsg = "Error retrieving git diff: " + e.getMessage();
                        if (!diffText.getText().equals(errorMsg)) {
                            diffText.setText(errorMsg);
                        }
                    }
                });
            }
        });
    }
}
