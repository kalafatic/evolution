package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonNavigator;

import eu.kalafatic.evolution.view.provider.ProjectManager;
import eu.kalafatic.evolution.view.views.OrchestrationZestView;
import eu.kalafatic.evolution.view.views.TaskTreeView;

public class ReloadOrchestrationHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            for (Object element : ((IStructuredSelection) selection).toList()) {
                if (element instanceof EObject) {
                    reloadResource(((EObject) element).eResource());
                }
            }

            IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
            if (page != null) {
                for (IViewReference ref : page.getViewReferences()) {
                    IViewPart view = ref.getView(false);
                    if (view instanceof CommonNavigator) {
                        ((CommonNavigator) view).getCommonViewer().refresh();
                    } else if (view instanceof OrchestrationZestView) {
                        ((OrchestrationZestView) view).refreshViewer();
                    } else if (view instanceof TaskTreeView) {
                        ((TaskTreeView) view).refresh();
                    }
                }
            }
        }
        new ProjectManager().refreshView("org.eclipse.ui.navigator.ProjectExplorer");
        
        return null;
    }

    private void reloadResource(Resource resource) {
        if (resource != null) {
            resource.unload();
            try {
                resource.load(null);
            } catch (Exception e) {
                // Ignore reload errors, the refresh will show state
            }
        }
    }
}
