package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.ComparePage;

public class ComparePageFactory {
    public static ComparePage createComparePage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        ComparePage page = new ComparePage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Compare");
        return page;
    }
}
