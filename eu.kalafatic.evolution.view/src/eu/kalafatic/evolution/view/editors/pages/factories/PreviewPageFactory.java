package eu.kalafatic.evolution.view.editors.pages.factories;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.PreviewPage;

public class PreviewPageFactory {
    public static PreviewPage createPreviewPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        PreviewPage page = new PreviewPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Preview");
        return page;
    }
}
