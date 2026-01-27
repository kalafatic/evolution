package eu.kalafatic.evolution.controller;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public abstract class AbstractOrchestratorHandler extends AbstractHandler {

    protected Orchestrator getOrchestrator(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        Orchestrator orchestrator = findOrchestrator(selection);

        if (orchestrator == null) {
            IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
            if (window != null) {
                ISelection serviceSelection = window.getSelectionService().getSelection("eu.kalafatic.evolution.view.propertiesView");
                orchestrator = findOrchestrator(serviceSelection);
            }
        }
        return orchestrator;
    }

    private Orchestrator findOrchestrator(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof Orchestrator) {
                return (Orchestrator) first;
            } else if (first instanceof EObject) {
                EObject eObj = (EObject) first;
                while (eObj != null && !(eObj instanceof Orchestrator)) {
                    eObj = eObj.eContainer();
                }
                if (eObj instanceof Orchestrator) {
                    return (Orchestrator) eObj;
                }
            }
        }
        return null;
    }
}
