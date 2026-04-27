package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.ArchitecturePage;

/**
 * @evo:18:A reason=architecture-page
 */
public class ArchitecturePageFactory {
    public static ArchitecturePage createArchitecturePage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        ArchitecturePage page = new ArchitecturePage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Architecture");
        return page;
    }
}
