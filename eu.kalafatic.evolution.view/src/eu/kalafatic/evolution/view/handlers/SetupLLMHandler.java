package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.view.wizards.SetupLLMWizard;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public class SetupLLMHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window != null) {
            Orchestrator orchestrator = getOrchestrator(event, window);
            if (orchestrator != null) {
                SetupLLMWizard wizard = new SetupLLMWizard(orchestrator);
                WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
                dialog.open();
            }
        }
        return null;
    }

    private Orchestrator getOrchestrator(ExecutionEvent event, IWorkbenchWindow window) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        Orchestrator orchestrator = findOrchestrator(selection);
        if (orchestrator == null) {
            ISelection serviceSelection = window.getSelectionService().getSelection("eu.kalafatic.evolution.view.propertiesView");
            orchestrator = findOrchestrator(serviceSelection);
        }
        return orchestrator;
    }

    private Orchestrator findOrchestrator(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof Orchestrator) {
                return (Orchestrator) first;
            }
        }
        return null;
    }
}
