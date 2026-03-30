package eu.kalafatic.evolution.view.editors.pages.factories;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AiFlowPage;

public class AiFlowPageFactory {
    public static AiFlowPage createAiFlowPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        AiFlowPage page = new AiFlowPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "AI Flow");
        return page;
    }
}
