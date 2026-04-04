package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;

public class AiChatPageFactory {
    public static AiChatPage createAiChatPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        AiChatPage page = new AiChatPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "AI Chat");
        return page;
    }
}
