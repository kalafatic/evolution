package eu.kalafatic.evolution.view.editors;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.PeerReviewPage;

public class PeerReviewPageFactory {
    public static PeerReviewPage createPeerReviewPage(MultiPageEditor editor, Orchestrator orchestrator) {
        PeerReviewPage page = new PeerReviewPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "Peer Review");
        return page;
    }
}
