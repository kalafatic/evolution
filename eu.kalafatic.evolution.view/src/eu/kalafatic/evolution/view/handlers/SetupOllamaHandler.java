package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.view.PropertiesView;
import eu.kalafatic.evolution.view.wizards.SetupOllamaWizard;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class SetupOllamaHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IViewPart view = page.findView(PropertiesView.ID);
                if (view instanceof PropertiesView) {
                    Orchestrator orchestrator = (Orchestrator) ((PropertiesView) view).getRootObject();
                    if (orchestrator != null) {
                        SetupOllamaWizard wizard = new SetupOllamaWizard(orchestrator);
                        WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
                        dialog.open();
                    }
                }
            }
        }
        return null;
    }
}
