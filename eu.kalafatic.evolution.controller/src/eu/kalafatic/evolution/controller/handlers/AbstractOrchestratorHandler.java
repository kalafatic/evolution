package eu.kalafatic.evolution.controller.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

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

    protected IProject getProject(Orchestrator orchestrator) {
        if (orchestrator == null) return null;
        Resource res = orchestrator.eResource();
        if (res != null) {
            URI uri = res.getURI();
            if (uri.isPlatformResource()) {
                String path = uri.toPlatformString(true);
                return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path)).getProject();
            }
        }
        return null;
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
