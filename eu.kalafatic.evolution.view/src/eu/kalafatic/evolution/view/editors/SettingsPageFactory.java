package eu.kalafatic.evolution.view.editors;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.SettingsPage;

public class SettingsPageFactory {
    public static SettingsPage createSettingsPage(MultiPageEditor editor, Orchestrator orchestrator) {
        SettingsPage page = new SettingsPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Settings");
        return page;
    }
}
