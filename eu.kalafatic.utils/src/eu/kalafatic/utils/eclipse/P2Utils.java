/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3 
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt  
 * 
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.eclipse;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.hack.AccumulatingProgressMonitor;

/**
 * The Class class P2Utils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class P2Utils {

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Check for updates.
	 */
	public static void checkForUpdates() {
		try {
			ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(null);
			progressDialog.run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					doCheckForUpdates(new AccumulatingProgressMonitor(monitor, Display.getDefault()));
				}

			});
		} catch (InvocationTargetException e) {

		} catch (InterruptedException e) {

		}
	}

	// ---------------------------------------------------------------

	/**
	 * Do check for updates.
	 * @param monitor the monitor
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void doCheckForUpdates(IProgressMonitor monitor) {
		BundleContext bundleContext = Activator.getDefault().getBundle().getBundleContext();
		ServiceReference reference = bundleContext.getServiceReference(IProvisioningAgent.SERVICE_NAME);
		if (reference == null) {
			// Activator
			// .log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
			// "No provisioning agent found.  This application is not set up for updates."));
			return;
		}

		final IProvisioningAgent agent = (IProvisioningAgent) bundleContext.getService(reference);
		try {
			IStatus updateStatus = P2Utils.checkForUpdates(agent, monitor);
			// Activator.log(updateStatus);
			if (updateStatus.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
				return;
			}
			if (updateStatus.getSeverity() != IStatus.ERROR) {
				PlatformUI.getWorkbench().restart();
			}
		} finally {
			bundleContext.ungetService(reference);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Check for updates.
	 * @param agent the agent
	 * @param monitor the monitor
	 * @return the i status
	 * @throws OperationCanceledException the operation canceled exception
	 */
	static IStatus checkForUpdates(IProvisioningAgent agent, IProgressMonitor monitor) throws OperationCanceledException {
		ProvisioningSession session = new ProvisioningSession(agent);
		// the default update operation looks for updates to the currently
		// running profile, using the default profile root marker. To change
		// which installable units are being updated, use the more detailed
		// constructors.
		UpdateOperation operation = new UpdateOperation(session);
		SubMonitor sub = SubMonitor.convert(monitor, "Checking for application updates...", 200);
		IStatus status = operation.resolveModal(sub.newChild(100));
		if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			return status;
		}
		if (status.getSeverity() == IStatus.CANCEL) {
			throw new OperationCanceledException();
		}

		if (status.getSeverity() != IStatus.ERROR) {
			// More complex status handling might include showing the user what
			// updates are available if there are multiples, differentiating
			// patches vs. updates, etc. In this example, we simply update as
			// suggested by the operation.
			ProvisioningJob job = operation.getProvisioningJob(monitor);
			if (job == null) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "ProvisioningJob could not be created - does this application support p2 software installation?");
			}
			status = job.runModal(sub.newChild(100));
			if (status.getSeverity() == IStatus.CANCEL) {
				throw new OperationCanceledException();
			}
		}
		return status;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the i installable units.
	 * @return the i installable units
	 */
	public static IQueryResult<IInstallableUnit> getIInstallableUnits() {
		try {
			ProvisioningUI provisioningUI = ProvisioningUI.getDefaultUI();

			if (null == provisioningUI) {
				return null;
			}

			String profileId = provisioningUI.getProfileId();

			ProvisioningSession provisioningSession = provisioningUI.getSession();

			if (null == provisioningSession) {
				return null;
			}

			IQueryable<IInstallableUnit> queryable = ((IProfileRegistry) provisioningSession.getProvisioningAgent().getService(IProfileRegistry.SERVICE_NAME)).getProfile(profileId);

			if (null == queryable) {
				return null;
			}

			if (null != queryable) {
				return queryable.query(QueryUtil.createIUAnyQuery(), null);
			}
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}
		return null;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the i installable unit.
	 * @param id the id
	 * @return the i installable unit
	 */
	public static IInstallableUnit getIInstallableUnit(String id) {
		IQueryResult<IInstallableUnit> iqr;

		if ((iqr = getIInstallableUnits()) != null) {

			Iterator<IInstallableUnit> ius = iqr.iterator();
			if (ius.hasNext()) {
				IInstallableUnit iu = ius.next();
				// Version v = iu.getVersion();

				if (iu.getId().equals(id)) {
					return iu;
				}
			}
		}
		return null;
	}
}
