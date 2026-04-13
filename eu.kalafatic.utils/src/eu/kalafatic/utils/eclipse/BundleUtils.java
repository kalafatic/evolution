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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The Class class BundleUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
public class BundleUtils {

	/** The G e_ prefix. */
	private final String GE_PREFIX = "eu.kalafatic";

	/** The INSTANCE. */
	private volatile static BundleUtils INSTANCE;

	/**
	 * Gets the single instance of BundleUtils.
	 * @return single instance of BundleUtils
	 */
	public static BundleUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (BundleUtils.class) {
				INSTANCE = new BundleUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	public EquinoxBundle getEquinoxBundle(AbstractUIPlugin activator) {
		return (EquinoxBundle) activator.getBundle();
	}

	// ---------------------------------------------------------------
	public Generation getGeneration(AbstractUIPlugin activator) {
		EquinoxBundle equinoxBundle = getEquinoxBundle(activator);
		return (Generation) equinoxBundle.getModule().getCurrentRevision().getRevisionInfo();
	}

	// ---------------------------------------------------------------
	public BundleFile getBundleFile(AbstractUIPlugin activator) {
		Generation generation = getGeneration(activator);
		return generation.getBundleFile();
	}

	// ---------------------------------------------------------------

	/**
	 * Checks if is bundle installed.
	 * @param installedBundles the installed bundles
	 * @param prefix the prefix
	 * @return true, if is bundle installed
	 */
	public boolean isBundleInstalled(Plugin plugin, Map<Long, Bundle> installedBundles, String prefix) {
		if (installedBundles == null) {
			installedBundles = getPlugins(plugin, GE_PREFIX);
		}
		for (Bundle bundle : installedBundles.values()) {
			if (bundle.getSymbolicName().startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the gemini plugins.
	 * @return the gemini plugins
	 */
	public Map<Long, Bundle> getPlugins(Plugin plugin, String prefix) {
		Map<Long, IBundleGroup> bundlesMap = getInstalledBundles();
		Map<Long, Bundle> geminiBundlesMap = new HashMap<Long, Bundle>();

		BundleContext bundleContext = plugin.getBundle().getBundleContext();

		if (bundleContext instanceof BundleContextImpl) {

			Bundle[] bundles = ((BundleContextImpl) bundleContext).getBundles();

			for (int i = 0; i < bundles.length; i++) {
				if (!bundlesMap.containsKey(bundles[i].getBundleId())) {

					if (bundles[i].getSymbolicName().startsWith(prefix)) {
						geminiBundlesMap.put(bundles[i].getBundleId(), bundles[i]);
					}
				}
			}
		}
		return geminiBundlesMap;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the installed bundles.
	 * @return the installed bundles
	 */
	public Map<Long, IBundleGroup> getInstalledBundles() {
		// get all the plugins that belong to features
		IBundleGroupProvider[] providers = Platform.getBundleGroupProviders();

		Map<Long, IBundleGroup> bundlesMap = new HashMap<Long, IBundleGroup>();

		if (providers != null) {
			for (int i = 0; i < providers.length; i++) {
				IBundleGroup[] bundleGroups = providers[i].getBundleGroups();

				for (int j = 0; j < bundleGroups.length; j++) {
					Bundle[] bundles = bundleGroups[j] == null ? new Bundle[0] : bundleGroups[j].getBundles();

					for (int k = 0; k < bundles.length; k++) {
						bundlesMap.put(bundles[k].getBundleId(), bundleGroups[j]);
					}
				}
			}
		}
		return bundlesMap;
	}

	// ---------------------------------------------------------------

	/**
	 * Checks if is plugin installed.
	 * @param pluginId the plugin id
	 * @return true, if is plugin installed
	 */
	public boolean isPluginInstalled(Plugin plugin, String pluginId) {
		Map<Long, IBundleGroup> bundlesMap = getInstalledBundles();

		BundleContext bundleContext = plugin.getBundle().getBundleContext();

		if (bundleContext instanceof BundleContextImpl) {
			Bundle[] bundles = ((BundleContextImpl) bundleContext).getBundles();

			for (int i = 0; i < bundles.length; i++) {
				if (!bundlesMap.containsKey(bundles[i].getBundleId())) {

					if (bundles[i].getSymbolicName().contains(pluginId)) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
