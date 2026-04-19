package eu.kalafatic.evolution.view.application;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {
	
	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		boolean serverOnly = false;
		int port = 48080;
		for (int i = 0; i < args.length; i++) {
			if ("--server".equals(args[i])) {
				serverOnly = true;
			} else if ("--port".equals(args[i]) && i + 1 < args.length) {
				port = Integer.parseInt(args[++i]);
			}
		}

		final int finalPort = port;
		if (serverOnly) {
			System.out.println("Starting Evolution in SERVER-ONLY mode on port " + finalPort + "...");
			ServerManager.getInstance().start(finalPort);
			System.out.println("Server is running. Press Ctrl+C to stop.");
			while (true) {
				Thread.sleep(1000);
			}
		}

		// Start server in background for UI mode
		new Thread(() -> {
			try {
				System.out.println("Starting Evolution background server on port " + finalPort + "...");
				ServerManager.getInstance().start(finalPort);
				System.out.println("Evolution background server started on port " + finalPort);
			} catch (Exception e) {
				System.err.println("Failed to start background server on port " + finalPort + ": " + e.getMessage());
				e.printStackTrace();
			}
		}).start();

		Display display = PlatformUI.createDisplay();
		try {
			if (!checkWorkspace(display)) {
				return IApplication.EXIT_OK;
			}

			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART)
				return IApplication.EXIT_RESTART;
			else
				return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}
		
	}

	/**
	 * Checks if a workspace is selected and prompts the user if not.
	 *
	 * @param display the display
	 * @return true if a workspace is selected, false otherwise
	 */
	private boolean checkWorkspace(Display display) {
		Location instanceLoc = Platform.getInstanceLocation();

		// if workspace is already set (e.g. via -data), we're good
		if (instanceLoc.isSet()) {
			return true;
		}

		// Show workspace selection dialog
		ChooseWorkspaceData launchData = new ChooseWorkspaceData(Platform.getConfigurationLocation().getURL());
		ChooseWorkspaceDialog dialog = new ChooseWorkspaceDialog(null, launchData, false, true);
		dialog.prompt(false);

		String selection = launchData.getSelection();
		if (selection == null) {
			return false;
		}

		// Persist the selection so it is remembered on next launch
		launchData.writePersistedData();

		try {
			// Set the instance location to the selected workspace
			instanceLoc.setURL(new File(selection).toURI().toURL(), true);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void stop() {
		if (!PlatformUI.isWorkbenchRunning())
			return;
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final Display display = workbench.getDisplay();
		display.syncExec(() -> {
			if (!display.isDisposed())
				workbench.close();
		});
	}
}
