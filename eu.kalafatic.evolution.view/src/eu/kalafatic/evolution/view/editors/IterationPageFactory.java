package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.IterationPage;

public class IterationPageFactory {
    public static IterationPage createIterationPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        IterationPage page = new IterationPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Iterations");
        return page;
    }
}
