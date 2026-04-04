package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.BrowserPage;

public class BrowserPageFactory {
    public static BrowserPage createBrowserPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        BrowserPage page = new BrowserPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Browser");
        return page;
    }
}
