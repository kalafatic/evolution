package eu.kalafatic.evolution.view.editors;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.EcosystemPage;

/**
 * Factory for the standalone Ecosystem page (live interactive topology of the
 * whole EVO ecosystem). Follows the standard page-factory convention: create
 * the page, register it as a tab and set its title.
 */
public class EcosystemPageFactory {

    private EcosystemPageFactory() {
    }

    public static EcosystemPage createEcosystemPage(MultiPageEditor editor, Orchestrator orchestrator) {
        EcosystemPage page = new EcosystemPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Ecosystem");
        return page;
    }
}
