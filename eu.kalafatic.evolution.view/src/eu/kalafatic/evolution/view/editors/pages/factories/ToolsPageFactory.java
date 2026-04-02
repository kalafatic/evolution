package eu.kalafatic.evolution.view.editors.pages.factories;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.ToolsPage;

public class ToolsPageFactory {
    public static ToolsPage createToolsPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        ToolsPage page = new ToolsPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Tools");
        return page;
    }
}
