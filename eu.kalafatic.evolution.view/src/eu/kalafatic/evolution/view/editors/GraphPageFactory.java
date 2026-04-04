package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.GraphPage;

public class GraphPageFactory {
    public static GraphPage createGraphPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        GraphPage page = new GraphPage(editor.getContainer(), editor, editor.getSite(), orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Graph");
        return page;
    }
}
