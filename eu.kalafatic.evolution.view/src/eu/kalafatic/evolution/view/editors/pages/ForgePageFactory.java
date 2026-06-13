package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.widgets.Composite;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class ForgePageFactory {
    public static ForgePage createForgePage(MultiPageEditor editor, Orchestrator orchestrator) {
        return new ForgePage(editor.getContainer(), editor, orchestrator);
    }
}
