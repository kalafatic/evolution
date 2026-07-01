package eu.kalafatic.evolution.view.handlers;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.dialogs.MediatedTargetDialog;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Handler for triggering General Mediated Analysis.
 */
public class MediatedAnalysisHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
		if (!(activeEditor instanceof MultiPageEditor)) {
			return null;
		}

		MultiPageEditor editor = (MultiPageEditor) activeEditor;
		AiChatPage chatPage = editor.getAiChatPage();
		if (chatPage == null) {
			return null;
		}

		Orchestrator orchestrator = chatPage.getOrchestrator();
		if (orchestrator == null) {
			return null;
		}

		eu.kalafatic.evolution.model.orchestration.ChatSession session = chatPage.getCurrentSession();
		if (session == null) {
			return null;
		}

		MediatedTargetDialog dialog = new MediatedTargetDialog(
				HandlerUtil.getActiveShell(event), session, chatPage.getProjectRoot(), editor);

		if (dialog.open() == Window.OK) {
			String path = dialog.getSelectedPath();
			if (path == null || path.isEmpty()) {
				return null;
			}

			// Switch to Mediated Mode
			orchestrator.setAiMode(AiMode.MEDIATED);
			chatPage.updateModeDisplay();

			// Trigger mediated analysis through the standard orchestrator flow
			try {
				chatPage.handleExecuteProposal("Analyze target: " + path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	private Orchestrator findSelectedOrchestrator(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			if (first instanceof Orchestrator) {
				return (Orchestrator) first;
			}
		}
		return null;
	}
}
