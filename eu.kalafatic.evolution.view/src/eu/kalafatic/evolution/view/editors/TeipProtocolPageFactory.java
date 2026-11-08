package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.TeipProtocolPage;

/**
 * Factory for creating and registering the TEIP Protocol editor tab.
 */
public class TeipProtocolPageFactory {

    public static TeipProtocolPage createTeipProtocolPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        TeipProtocolPage page = new TeipProtocolPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "TEIP Protocol");
        return page;
    }
}
