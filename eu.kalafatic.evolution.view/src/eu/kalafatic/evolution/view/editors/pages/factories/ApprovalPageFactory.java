package eu.kalafatic.evolution.view.editors.pages.factories;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;

public class ApprovalPageFactory {
    public static ApprovalPage createApprovalPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        ApprovalPage page = new ApprovalPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Approval");
        return page;
    }
}
