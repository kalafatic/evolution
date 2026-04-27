package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.view.wizards.NewAiThreadWizard;

/**
 * @evo:17:A reason=new-thread-command-handler
 */
public class NewAiThreadHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        NewAiThreadWizard wizard = new NewAiThreadWizard();
        WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
        dialog.open();
        return null;
    }
}
