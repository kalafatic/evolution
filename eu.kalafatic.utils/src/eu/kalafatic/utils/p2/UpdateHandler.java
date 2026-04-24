package eu.kalafatic.utils.p2;

import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;

/**
 * UpdateHandler invokes the check for updates UI
 *
 * @since 3.4
 */
public class UpdateHandler extends PreloadingRepositoryHandler {

	boolean hasNoRepos = false;

	@Override
	protected void doExecute(LoadMetadataRepositoryJob job) {
		if (hasNoRepos) {
			return;
		}
		UpdateOperation operation = getProvisioningUI().getUpdateOperation(null, null);
		// check for updates
		operation.resolveModal(null);
		if (getProvisioningUI().getPolicy().continueWorkingWithOperation(operation, getShell())) {
			// Open the normal version of the update wizard
			getProvisioningUI().openUpdateWizard(false, operation, job);
		}
	}

	@Override
	protected boolean preloadRepositories() {
		hasNoRepos = false;
		RepositoryTracker repoMan = getProvisioningUI().getRepositoryTracker();
		if (repoMan.getKnownRepositories(getProvisioningUI().getSession()).length == 0) {
			hasNoRepos = true;
			return false;
		}
		return super.preloadRepositories();
	}
}
