package eu.kalafatic.utils.p2;

import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;

/**
 * InstallHandler invokes the install new software UI
 */
public class InstallHandler extends PreloadingRepositoryHandler {

	@Override
	protected void doExecute(LoadMetadataRepositoryJob job) {
		getProvisioningUI().openInstallWizard(null, null, job);
	}
}
