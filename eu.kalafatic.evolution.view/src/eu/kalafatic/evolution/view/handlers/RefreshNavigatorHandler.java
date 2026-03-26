package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import eu.kalafatic.evolution.view.views.EvoNavigator;

/**
 * Handler for the 'Refresh Evo Navigator' command.
 * It finds the active EvoNavigator instance and triggers a full refresh.
 */
public class RefreshNavigatorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            IViewPart view = page.findView("eu.kalafatic.views.EvoNavigator");
            if (view instanceof EvoNavigator) {
                ((EvoNavigator) view).refresh();
            }
        }
        return null;
    }
}
