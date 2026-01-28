package eu.kalafatic.evolution.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.OrchestratorEditorInput;

public class OrchestrationEditorActionProvider extends CommonActionProvider {

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
    }

    @Override
    public void fillActionBars(org.eclipse.ui.IActionBars actionBars) {
        ISelection selection = getContext().getSelection();
        if (selection instanceof IStructuredSelection) {
            final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            if (firstElement instanceof Orchestrator) {
                Action openAction = new Action("Open Orchestration") {
                    @Override
                    public void run() {
                        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                        try {
                            page.openEditor(new OrchestratorEditorInput((Orchestrator) firstElement), "eu.kalafatic.evolution.view.editors.MultiPageEditor");
                        } catch (PartInitException e) {
                            e.printStackTrace();
                        }
                    }
                };
                actionBars.setGlobalActionHandler(org.eclipse.ui.IWorkbenchActionConstants.OPEN, openAction);
            }
        }
    }
}
