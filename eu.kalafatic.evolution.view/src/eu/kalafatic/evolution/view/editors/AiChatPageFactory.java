package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.AiCommanderPage;

public class AiChatPageFactory {
    public static AiCommanderPage createAiChatPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        AiCommanderPage page = new AiCommanderPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "AI Commander");
        return page;
    }
}
