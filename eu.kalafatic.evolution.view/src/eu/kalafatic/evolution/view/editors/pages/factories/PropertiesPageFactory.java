package eu.kalafatic.evolution.view.editors.pages.factories;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;

public class PropertiesPageFactory {
    public static PropertiesPage createPropertiesPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        PropertiesPage page = new PropertiesPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Properties");
        return page;
    }
}
