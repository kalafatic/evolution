package eu.kalafatic.evolution.view.editors.pages.factories;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.TestsPage;

public class TestsPageFactory {
    public static TestsPage createTestsPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        TestsPage page = new TestsPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Tests");
        return page;
    }
}
