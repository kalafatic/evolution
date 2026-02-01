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
package eu.kalafatic.evolution.view.application;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.PropertyResourceBundle;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

//import eu.kalafatic.gemini.core.p2.CloudPolicy;

/**
 * The Class class Activator.
 * @author Petr Kalafatic
 * @version 3.0.0
 * @project Gemini
 */
public class Activator extends AbstractUIPlugin {

	/** The Constant PLUGIN_ID. */
	public static final String PLUGIN_ID = "eu.kalafatic.evolution.view";

	/** The plugin properties. */
	private static String PLUGIN_PROPERTIES = "plugin.properties";

	/** The plugin properties. */
	private static PropertyResourceBundle pluginProperties;

	/** The plugin. */
	// private static final Preferences PREFERENCES = Platform
	// .getPreferencesService().getRootNode().node(PLUGIN_ID);

	// private static final int ARGS_PORT = PREFERENCES.getInt(
	// ECorePreferences.SINGLETON_ARGS_PORT.getName(),
	// (Integer) ECorePreferences.SINGLETON_ARGS_PORT.getDef());

	/** The plugin. */
	private static Activator plugin;

	/** The policy registration. */
	private ServiceRegistration<?> policyRegistration;

	/** The bundle context. */
	private static BundleContext bundleContext;

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		bundleContext = context;

//		PreferenceInitializer.setUp();

		registerP2Policy(bundleContext);
	}

	// ---------------------------------------------------------------

	/**
	 * Register p2 policy.
	 * @param context the context
	 */
	private void registerP2Policy(BundleContext context) {
//		policyRegistration = context.registerService(Policy.class.getName(), new CloudPolicy(), null);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;

		policyRegistration.unregister();
		policyRegistration = null;
		super.stop(context);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the default.
	 * @return the default
	 */
	// public static void processMessages() {
	// final String message = MessageFormat.format(LISTENING, "Application",
	// "to command line arguments", argsPort);
	// Log.log(ECorePreferences.MODULE, message);
	//
	// Display.getDefault().asyncExec(new Runnable() {
	// @Override
	// public void run() {
	// try {
	// TrayItem trayItem = AppData.getInstance().getTrayItem();
	//
	// if (trayItem.getToolTip().isVisible()) {
	// Thread.sleep(2000);
	// }
	//
	// ToolTip tip = new ToolTip(new Shell(Display.getCurrent()),
	// SWT.BALLOON | SWT.ICON_INFORMATION);
	// tip.setAutoHide(true);
	// trayItem.setToolTip(tip);
	// tip.setText("Info");
	// tip.setMessage(message);
	// tip.setVisible(true);
	//
	// } catch (Exception e) {
	// Log.log(ECorePreferences.MODULE, e);
	// }
	// }
	// });
	// }

	// ---------------------------------------------------------------

	/**
	 * Gets the default.
	 * @return the default
	 */
	public static Activator getDefault() {
		return plugin;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the image descriptor.
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	public static ImageDescriptor getImageDescriptor(String pluginID, String path) {
		return imageDescriptorFromPlugin(pluginID, path);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the plugin properties.
	 * @return the plugin properties
	 */
	public static PropertyResourceBundle getPluginProperties() {
		if (pluginProperties == null) {
			try {
				pluginProperties = new PropertyResourceBundle(FileLocator.openStream(getDefault().getBundle(), new Path(PLUGIN_PROPERTIES), false));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return pluginProperties;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the version.
	 * @return the version
	 */
	public String getVersion() {
		String version = System.getProperty("talend.studio.version"); //$NON-NLS-1$
		if (version == null || "".equals(version.trim())) { //$NON-NLS-1$
			version = getDefault().getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
		}
		return version;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the image file.
	 * @param imgName the img name
	 * @return the image file
	 */
	public static File getImageFile(String imgName) {
		Bundle bundle = Activator.getDefault().getBundle();
		Path path = new Path(imgName); //$NON-NLS-1$
		URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(url);
		} catch (IOException e) {
			// Will happen if the file cannot be read for some reason
			e.printStackTrace();
		}
		return new File(fileUrl.getPath());
	}

}
