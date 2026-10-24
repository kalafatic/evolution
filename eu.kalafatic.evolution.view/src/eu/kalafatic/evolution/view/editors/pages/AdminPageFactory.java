package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.widgets.Composite;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class AdminPageFactory {

    public static AdminPage createAdminPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        return new AdminPage(parent, editor, orchestrator);
    }
}
