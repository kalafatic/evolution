package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;

/**
 * @evo:17:A reason=new-thread-wizard
 */
public class NewAiSessionWizard extends Wizard implements INewWizard {
    private NewAiSessionWizardPage page;

    public NewAiSessionWizard() {
        setWindowTitle("New AI Session");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
    public void addPages() {
        page = new NewAiSessionWizardPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        String sessionName = page.getSessionName();
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage workbenchPage = window.getActivePage();
            if (workbenchPage != null && workbenchPage.getActiveEditor() instanceof MultiPageEditor) {
                MultiPageEditor editor = (MultiPageEditor) workbenchPage.getActiveEditor();
                AiChatPage aiChatPage = editor.getAiChatPage();
                if (aiChatPage != null) {
                    aiChatPage.createNewSession(sessionName);
                    return true;
                }
            }
        }
        return false;
    }
}
