package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonNavigator;

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

            IWorkbenchPart part = HandlerUtil.getActivePart(event);
            if (part instanceof CommonNavigator) {
                ((CommonNavigator) part).getCommonViewer().refresh();
            }
        }
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
