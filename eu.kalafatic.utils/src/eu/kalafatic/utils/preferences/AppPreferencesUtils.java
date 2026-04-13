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
package eu.kalafatic.utils.preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import eu.kalafatic.utils.Activator;

/**
 * The Class class AppPreferencesUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class AppPreferencesUtils {

	/** The preferences. */
	private Preferences preferences;

	/** The INSTANCE. */
	private volatile static AppPreferencesUtils INSTANCE;

	/**
	 * Instantiates a new app preferences utils.
	 */
	public AppPreferencesUtils() {
		preferences = Platform.getPreferencesService().getRootNode().node(Plugin.PLUGIN_PREFERENCE_SCOPE).node(Activator.PLUGIN_ID);

		preferences.put("extensions", "");
	}

	/**
	 * Gets the single instance of AppPreferencesUtils.
	 * @return single instance of AppPreferencesUtils
	 */
	public static AppPreferencesUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (AppPreferencesUtils.class) {
				INSTANCE = new AppPreferencesUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the preference.
	 * @param nodeName the node name
	 * @param key the key
	 * @return the preference
	 */
	public String getPreference(String nodeName, String key) {
		return preferences.node(nodeName).get(key, null);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the preference.
	 * @param nodeName the node name
	 * @return the preference
	 */
	public Preferences getPreference(String nodeName) {
		return preferences.node(nodeName);
	}

	// ---------------------------------------------------------------

	/**
	 * Put preference.
	 * @param nodeName the node name
	 * @param key the key
	 * @param value the value
	 */
	public void putPreference(String nodeName, String key, String value) {
		preferences.node(nodeName).put(key, value);
		try {
			preferences.node(nodeName).flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}
}
