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
public class NewAiThreadWizard extends Wizard implements INewWizard {
    private NewAiThreadWizardPage page;

    public NewAiThreadWizard() {
        setWindowTitle("New AI Thread");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
    public void addPages() {
        page = new NewAiThreadWizardPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        String threadName = page.getThreadName();
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage workbenchPage = window.getActivePage();
            if (workbenchPage != null && workbenchPage.getActiveEditor() instanceof MultiPageEditor) {
                MultiPageEditor editor = (MultiPageEditor) workbenchPage.getActiveEditor();
                AiChatPage aiChatPage = editor.getAiChatPage();
                if (aiChatPage != null) {
                    aiChatPage.createNewThread(threadName);
                    return true;
                }
            }
        }
        return false;
    }
}
