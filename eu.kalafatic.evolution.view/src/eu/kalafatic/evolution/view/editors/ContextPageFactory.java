package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.ContextPage;

public class ContextPageFactory {
    public static ContextPage createContextPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        ContextPage page = new ContextPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Context");
        return page;
    }
}
