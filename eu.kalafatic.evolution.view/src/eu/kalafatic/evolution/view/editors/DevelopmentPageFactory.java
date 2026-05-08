package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.DevelopmentPage;

public class DevelopmentPageFactory {
    public static DevelopmentPage createDevelopmentPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        DevelopmentPage page = new DevelopmentPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Development");
        return page;
    }
}
