package eu.kalafatic.evolution.view.editors.listeners;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class EditorSelectionListener implements ISelectionListener {
    private MultiPageEditor editor;

    public EditorSelectionListener(MultiPageEditor editor) {
        this.editor = editor;
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            if (firstElement instanceof EObject) {
                if (firstElement instanceof Orchestrator) {
                    editor.setOrchestrator((Orchestrator) firstElement);
                }
                editor.selectNode(firstElement);
            }
        }
    }
}
