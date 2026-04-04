package eu.kalafatic.evolution.view.editors;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;

public class TaskStackPageFactory {

    public static TaskStackPage createTaskStackPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        TaskStackPage page = new TaskStackPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Task Stack");
        return page;
    }
}
