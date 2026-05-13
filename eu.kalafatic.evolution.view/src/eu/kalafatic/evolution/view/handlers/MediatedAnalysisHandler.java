package eu.kalafatic.evolution.view.handlers;

import java.io.File;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorService;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.dialogs.MediatedTargetDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Handler for triggering General Mediated Analysis.
 */
public class MediatedAnalysisHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        MediatedTargetDialog dialog = new MediatedTargetDialog(HandlerUtil.getActiveShell(event));
        if (dialog.open() == Window.OK) {
            String path = dialog.getSelectedPath();
            if (path == null || path.isEmpty()) return null;

            Orchestrator orchestrator = findSelectedOrchestrator(event);
            if (orchestrator == null) return null;

            // Switch to Mediated Mode
            orchestrator.setAiMode(AiMode.MEDIATED);

            OrchestratorService service = new OrchestratorServiceImpl();
            TaskRequest request = new TaskRequest("Analyze target: " + path, new File(path));
            request.getContext().put("orchestrator", orchestrator);
            service.handle(request);
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
