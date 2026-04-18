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
package eu.kalafatic.utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.swt.SWT;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import eu.kalafatic.utils.application.JavaUtils;
import eu.kalafatic.utils.constants.FCMDConstants;
import eu.kalafatic.utils.constants.FConstants;
import eu.kalafatic.utils.convert.ConvertUtils;
import eu.kalafatic.utils.log.Log;
import eu.kalafatic.evolution.model.orchestration.NetInterface;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.utils.os.OSUtils;
import eu.kalafatic.utils.p2.PreferenceConstants;
import eu.kalafatic.utils.preferences.ECorePreferences;
import eu.kalafatic.utils.windows.WinRegistryUtils;

/**
 * The Class class PreferenceInitializer.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/** The program path. */
	private static String programPath;

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer# initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		setUp();
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the up.
	 */
	public static void setUp() {
		try {
			setUpInterfaces();

			Preferences preferences = Platform.getPreferencesService().getRootNode().node(Activator.PLUGIN_ID);
			if (preferences == null) {
				return;
			}

			boolean set = preferences.getBoolean(ECorePreferences.SET.getName(), (Boolean) ECorePreferences.SET.getDef());

			String clientId = getClientID();
			if (clientId != null) {
				preferences.put(ECorePreferences.APP_ID.getName(), clientId);
			}
			preferences.flush();

			if (set) {
				return;
			}
			setUp(preferences);

			preferences.putBoolean(ECorePreferences.SET.getName(), true);

			// CONFIG
			preferences.put(ECorePreferences.APP_NAME.getName(), (String) ECorePreferences.APP_NAME.getDef());

			// SYSTEM
			String cpuName = WinRegistryUtils.getFromRegistry(FCMDConstants.GET_CPU_NAME, FCMDConstants.REGSTR_TOKEN);
			if (cpuName != null) {
				preferences.put(ECorePreferences.CPU_NAME.getName(), cpuName);
			}
			String cpuSpeed = WinRegistryUtils.getFromRegistry(FCMDConstants.GET_CPU_SPEED, FCMDConstants.REGDWORD_TOKEN);
			if (cpuSpeed != null) {
				preferences.put(ECorePreferences.CPU_SPEED.getName(), cpuSpeed);
			}

			// OS
			String osName = OSUtils.INSTANCE.getOSName();
			if (osName != null) {
				preferences.put(ECorePreferences.OS_NAME.getName(), osName);
			}
			String osArch = OSUtils.INSTANCE.getOSArch();
			if (osArch != null) {
				preferences.put(ECorePreferences.OS_ARCH.getName(), osArch);
			}

			// BASIC ASSOCIATION
			// AppPreferences.getInstance().putPreference("extensions", ".torrent", "torrentfile=" + programPath);

			// JAVA
			String javaVersion = JavaUtils.INSTANCE.getJavaVersion();
			if (javaVersion != null) {
				preferences.put(ECorePreferences.JAVA_VERSION.getName(), javaVersion);
			}

			// LOCATIONS
			preferences.put(ECorePreferences.WORKSPACE_LOC.getName(), (String) ECorePreferences.WORKSPACE_LOC.getDef());
			preferences.put(ECorePreferences.MODELS_LOC.getName(), (String) ECorePreferences.MODELS_LOC.getDef());
			preferences.put(ECorePreferences.LOGS_LOC.getName(), (String) ECorePreferences.LOGS_LOC.getDef());
			preferences.put(ECorePreferences.TORRENTS_LOC.getName(), (String) ECorePreferences.TORRENTS_LOC.getDef());
			preferences.put(ECorePreferences.DOWNLOADS_LOC.getName(), (String) ECorePreferences.DOWNLOADS_LOC.getDef());
			preferences.put(ECorePreferences.SITE_LOC.getName(), (String) ECorePreferences.SITE_LOC.getDef());
			preferences.put(ECorePreferences.NL_LOC.getName(), (String) ECorePreferences.NL_LOC.getDef());

			preferences.put(ECorePreferences.PROGRAM_LOC.getName(), programPath);

			// PORTS
			preferences.putInt(ECorePreferences.SINGLETON_ARGS_PORT.getName(), (Integer) ECorePreferences.SINGLETON_ARGS_PORT.getDef());
			preferences.putInt(ECorePreferences.BT_CLIENT_PORT.getName(), (Integer) ECorePreferences.BT_CLIENT_PORT.getDef());
			preferences.putInt(ECorePreferences.TRACKER_PORT.getName(), (Integer) ECorePreferences.TRACKER_PORT.getDef());
			preferences.putInt(ECorePreferences.TRACKER_UPLOAD_PORT.getName(), (Integer) ECorePreferences.TRACKER_UPLOAD_PORT.getDef());
			preferences.putInt(ECorePreferences.UPDATE_PORT.getName(), (Integer) ECorePreferences.UPDATE_PORT.getDef());
			preferences.putInt(ECorePreferences.WEB_UPLOAD_PORT.getName(), (Integer) ECorePreferences.WEB_UPLOAD_PORT.getDef());
			preferences.putInt(ECorePreferences.RC_PORT.getName(), (Integer) ECorePreferences.RC_PORT.getDef());

			// ADDRESS
			preferences.put(ECorePreferences.HOME_SITE.getName(), (String) ECorePreferences.HOME_SITE.getDef());
			preferences.put(ECorePreferences.UPDATE_SITE.getName(), (String) ECorePreferences.UPDATE_SITE.getDef());
			preferences.put(ECorePreferences.BUG_SITE.getName(), (String) ECorePreferences.BUG_SITE.getDef());
			preferences.put(ECorePreferences.MAIL_SITE.getName(), (String) ECorePreferences.MAIL_SITE.getDef());

			// CUSTOMIZE
			preferences.putBoolean(ECorePreferences.LOG_ENABLED.getName(), (Boolean) ECorePreferences.LOG_ENABLED.getDef());
			preferences.put(ECorePreferences.LOG_TYPE.getName(), (String) ECorePreferences.LOG_TYPE.getDef());
			preferences.put(ECorePreferences.LOG_EVENT.getName(), (String) ECorePreferences.LOG_EVENT.getDef());
			preferences.putInt(ECorePreferences.LOG_SIZE.getName(), (Integer) ECorePreferences.LOG_SIZE.getDef());

			preferences.putInt(ECorePreferences.CHECK_AFTER.getName(), (Integer) ECorePreferences.CHECK_AFTER.getDef());
			preferences.putInt(ECorePreferences.MAX_LOGS.getName(), (Integer) ECorePreferences.MAX_LOGS.getDef());

			preferences.putBoolean(ECorePreferences.THREAD_MANAGEMENT.getName(), (Boolean) ECorePreferences.THREAD_MANAGEMENT.getDef());
			preferences.putBoolean(ECorePreferences.CPU_MANAGEMENT.getName(), (Boolean) ECorePreferences.CPU_MANAGEMENT.getDef());
			preferences.putInt(ECorePreferences.MAX_CPU.getName(), (Integer) ECorePreferences.MAX_CPU.getDef());
			preferences.putBoolean(ECorePreferences.SOUND.getName(), (Boolean) ECorePreferences.SOUND.getDef());
			preferences.putBoolean(ECorePreferences.RUN_BT_AFTER_START.getName(), (Boolean) ECorePreferences.RUN_BT_AFTER_START.getDef());

			createWorkspace();

			// Forces the application to save the preferences
			preferences.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the up.
	 * @param preferences the new up
	 */
	private static void setUp(Preferences preferences) {
		try {
			programPath = new File(".").getCanonicalPath();

			preferences.putBoolean(PreferenceConstants.REPOSITORIES_VISIBLE, false);
			preferences.putBoolean(PreferenceConstants.SHOW_LATEST_VERSION_ONLY, true);
			preferences.putBoolean(PreferenceConstants.AVAILABLE_SHOW_ALL_BUNDLES, false);
			preferences.putBoolean(PreferenceConstants.INSTALLED_SHOW_ALL_BUNDLES, false);
			preferences.putBoolean(PreferenceConstants.AVAILABLE_GROUP_BY_CATEGORY, true);
			preferences.putBoolean(PreferenceConstants.SHOW_DRILLDOWN_REQUIREMENTS, false);
			preferences.putInt(PreferenceConstants.RESTART_POLICY, Policy.RESTART_POLICY_PROMPT_RESTART_OR_APPLY);
			// corePreferences.putInt(PreferenceConstants.UPDATE_WIZARD_STYLE,
			// Policy.UPDATE_STYLE_MULTIPLE_IUS);
			preferences.putBoolean(PreferenceConstants.FILTER_ON_ENV, false);
			preferences.putInt(PreferenceConstants.UPDATE_DETAILS_HEIGHT, SWT.DEFAULT);
			preferences.putInt(PreferenceConstants.UPDATE_DETAILS_WIDTH, SWT.DEFAULT);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the workspace.
	 */
	private static void createWorkspace() {
		File workspace = new File((String) ECorePreferences.WORKSPACE_LOC.getValue());
		if (!workspace.exists()) {
			workspace.mkdirs();
		}
		File file = new File((String) ECorePreferences.LOGS_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File((String) ECorePreferences.MODELS_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File((String) ECorePreferences.TORRENTS_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File((String) ECorePreferences.DOWNLOADS_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}

		file = new File((String) ECorePreferences.SITE_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File((String) ECorePreferences.CERT_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File((String) ECorePreferences.NL_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File((String) ECorePreferences.JAVADOC_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File((String) ECorePreferences.UPDATES_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File((String) ECorePreferences.MAINTAIN_LOC.getValue());
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the up interfaces.
	 */
	private static void setUpInterfaces() {
		try {
			Enumeration<?> e = NetworkInterface.getNetworkInterfaces();

			while (e.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface) e.nextElement();

				NetInterface netInterface = OrchestrationFactory.eINSTANCE.createNetInterface();
				netInterface.setName(ni.getName());
				netInterface.setDisplayName(ni.getDisplayName());

				netInterface.setUp(ni.isUp());
				netInterface.setVirtual(ni.isVirtual());
				netInterface.setMulticast(ni.supportsMulticast());

				byte[] hardwareAddress = ni.getHardwareAddress();

				if (hardwareAddress != null) {
					String mac = ConvertUtils.macToString(':', hardwareAddress);
					netInterface.setMac(mac);
				}

				Enumeration<?> e2 = ni.getInetAddresses();

				while (e2.hasMoreElements()) {
					InetAddress ip = (InetAddress) e2.nextElement();

					String stringIp = ip.toString();

					if (stringIp.startsWith("/")) {
						stringIp = stringIp.substring(1);
					}
					netInterface.getAddress().add(stringIp);
				}
				FConstants.INTERFACES.add(netInterface);
			}
		} catch (Exception e) {
			Log.log(ECorePreferences.MODULE, e);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the client id.
	 * @return the client id
	 */
	private static String getClientID() {
		String version = "2.6.1";
		try {
			if (Platform.getProduct() != null) {
				version = Platform.getProduct().getProperty("version");
			}
		} catch (Throwable e) {
		}

		if (version == null || "".equals(version.trim())) {
			version = "2.6.1";
		}
		version = version.replace(".", "");
		return "-GE" + version + "0-000000000000";
	}
}
