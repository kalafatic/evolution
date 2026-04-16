package eu.kalafatic.evolution.view.editors;

import org.eclipse.swt.widgets.Composite;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.ServerPage;

public class ServerPageFactory {

    public static ServerPage createServerPage(MultiPageEditor editor, Orchestrator orchestrator) {
        ServerPage page = new ServerPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Server");
        return page;
    }
}
