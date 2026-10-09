package eu.kalafatic.evolution.view.editors.listeners;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import eu.kalafatic.evolution.controller.orchestration.ContextualHelpService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class EditorSelectionListener implements ISelectionListener {
    private MultiPageEditor editor;

    public EditorSelectionListener(MultiPageEditor editor) {
        this.editor = editor;
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        String partName = (part != null && part.getTitle() != null) ? part.getTitle() : "";
        String selectedName = "";
        String selectedType = "";
        String resourcePath = "";
        String projectName = "";
        String selectionText = "";

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection) selection;
            Object firstElement = ss.getFirstElement();
            if (firstElement != null) {
                selectedType = firstElement.getClass().getSimpleName();
                if (firstElement instanceof EObject) {
                    EObject eObj = (EObject) firstElement;
                    selectedName = eObj.eClass().getName();
                    if (firstElement instanceof Orchestrator) {
                        editor.setOrchestrator((Orchestrator) firstElement);
                        selectedName = ((Orchestrator) firstElement).getName() != null ? ((Orchestrator) firstElement).getName() : "Orchestrator";
                    } else if (firstElement instanceof Task) {
                        Task t = (Task) firstElement;
                        selectedName = t.getName() != null ? t.getName() : "Task";
                    }
                    editor.selectNode(firstElement);
                } else if (firstElement instanceof IResource) {
                    IResource res = (IResource) firstElement;
                    selectedName = res.getName();
                    resourcePath = res.getFullPath().toString();
                    if (res.getProject() != null) {
                        projectName = res.getProject().getName();
                    }
                } else {
                    selectedName = firstElement.toString();
                }
            }
        } else if (selection instanceof ITextSelection) {
            ITextSelection textSel = (ITextSelection) selection;
            editor.setLastTextSelection(textSel);
            selectionText = textSel.getText() != null ? textSel.getText().trim() : "";
            selectedType = "TextSelection";
            selectedName = selectionText.isEmpty() ? "Cursor Position" : "Selected Text (" + selectionText.length() + " chars)";
            if (editor != null && editor.getEditorInput() != null) {
                projectName = editor.getEditorInput().getName();
            }
        }

        String sid = (editor != null && editor.getOrchestrator() != null) ? editor.getOrchestrator().getId() : "GLOBAL";

        ContextualHelpService.getInstance().requestAssistanceDebounced(
                partName, selectedName, selectedType, resourcePath, projectName, selectionText, sid, 250,
                result -> {
                    Display.getDefault().asyncExec(() -> {
                        if (editor != null && editor.getContainer() != null && !editor.getContainer().isDisposed()) {
                            editor.updateContextualHelp(result);
                        }
                    });
                }
        );
    }
}
