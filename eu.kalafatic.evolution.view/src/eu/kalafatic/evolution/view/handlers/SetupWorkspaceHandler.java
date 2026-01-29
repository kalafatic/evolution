package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;

/**
 * Handler for the "Setup Workspace" command.
 * Allows users to switch the workspace and restart the application.
 */
public class SetupWorkspaceHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ChooseWorkspaceData launchData = new ChooseWorkspaceData(Platform.getConfigurationLocation().getURL());
        ChooseWorkspaceDialog dialog = new ChooseWorkspaceDialog(
                HandlerUtil.getActiveShell(event),
                launchData, false, true);
        dialog.prompt(true);

        String selection = launchData.getSelection();
        if (selection != null) {
            // Persist the selection so it is used upon restart
            launchData.writePersistedData();
            // Restart the application to apply the new workspace
            PlatformUI.getWorkbench().restart();
        }

        return null;
    }
}
