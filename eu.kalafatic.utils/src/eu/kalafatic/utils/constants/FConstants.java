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
package eu.kalafatic.utils.constants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.osgi.service.prefs.Preferences;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.model.LogElement;
import eu.kalafatic.utils.model.NetInterface;

/**
 * The Class class FConstants.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
public final class FConstants {

	/** The Constant PREFERENCES. */
	// public static final Preferences PREFERENCES = Platform
	// .getPreferencesService().getRootNode().node(Activator.PLUGIN_ID);

	public static final Preferences PREFERENCES = Platform.getPreferencesService().getRootNode().node(Plugin.PLUGIN_PREFERENCE_SCOPE).node(Activator.PLUGIN_ID);

	/** The Constant EQUINOX_BUNDLE. */
	public static EquinoxBundle EQUINOX_BUNDLE = (EquinoxBundle) Activator.getDefault().getBundle();

	/** The Constant GENERATION. */
	public static Generation GENERATION = (Generation) EQUINOX_BUNDLE.getModule().getCurrentRevision().getRevisionInfo();

	/** The Constant BUNDLE_FILE. */
	public static BundleFile BUNDLE_FILE = GENERATION.getBundleFile();

	/** The Constant SOUNDS_LOC. */
	public static final String SOUNDS_LOC = BUNDLE_FILE.toString().concat(File.separator).concat("sounds");

	/** The Constant ICONS_LOC. */
	public static final String ICONS_LOC = BUNDLE_FILE.toString().concat(File.separator).concat("icons");

	/** The Constant CERT_LOC. */
	public static final String CERT_LOC = BUNDLE_FILE.toString().concat(File.separator).concat("certificates");

	/** The Constant INTERFACES. */
	public static final List<NetInterface> INTERFACES = new ArrayList<NetInterface>();

	/** The Constant LOGS. */
	public static final List<LogElement> LOGS = new ArrayList<LogElement>();

	/** The Constant COMMAND_STACK. */
	public static final Map<String, List<Object>> COMMAND_STACK = new HashMap<String, List<Object>>();

	// ---------------------------------------------------------------

	/** The Constant NUMBERS. */
	public static final String[] NUMBERS = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24",
			"25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55",
			"56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86",
			"87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", };

	/** The Constant TEN_NUMBERS. */
	public static final String[] TEN_NUMBERS = new String[] { "0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100", "110", "120", "130", "140", "150", "160", "170", "180", "190", "200",
			"210", "220", "230", "240", "250", "260", "270", "280", "290", "300", "310", "320", "330", "340", "350", "360", "370", "380", "390", "400", "410", "420", "430", "440", "450", "460",
			"470", "480", "490", "500", "510", "520", "530", "540", "550", "560", "570", "580", "590", "600", "610", "620", "630", "640", "650", "660", "670", "680", "690", "700", "710", "720",
			"730", "740", "750", "760", "770", "780", "790", "800", "810", "820", "830", "840", "850", "860", "870", "880", "890", "900", "910", "920", "930", "940", "950", "960", "970", "980", "990" };

	/** The Constant HUNDRED_NUMBERS. */
	public static final String[] HUNDRED_NUMBERS = new String[] { "0", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000", "1100", "1200", "1300", "1400", "1500", "1600", "1700",
			"1800", "1900", "2000", "2100", "2200", "2300", "2400", "2500", "2600", "2700", "2800", "2900", "3000", "3100", "3200", "3300", "3400", "3500", "3600", "3700", "3800", "3900", "4000",
			"4100", "4200", "4300", "4400", "4500", "4600", "4700", "4800", "4900", "5000", "5100", "5200", "5300", "5400", "5500", "5600", "5700", "5800", "5900", "6000", "6100", "6200", "6300",
			"6400", "6500", "6600", "6700", "6800", "6900", "7000", "7100", "7200", "7300", "7400", "7500", "7600", "7700", "7800", "7900", "8000", "8100", "8200", "8300", "8400", "8500", "8600",
			"8700", "8800", "8900", "9000", "9100", "9200", "9300", "9400", "9500", "9600", "9700", "9800", "9900" };

	// ---------------------------------------------------------------

	/** The Constant BUFFER_SIZE. */
	public static final int BUFFER_SIZE = 1 << 10; // 1024

	/** The Constant MEMORY_SIZES. */
	public static final String[] MEMORY_SIZES = new String[] { "8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384", "32768", "65536" };

	// ---------------------------------------------------------------

	/** The Constant WELL_KNOWN_PORTS. */
	public static final Object[] WELL_KNOWN_PORTS = new Object[] { "Well-known ports", 0, 1023 };

	/** The Constant REGISTERED_PORTS. */
	public static final Object[] REGISTERED_PORTS = new Object[] { "Registered ports", 1024, 49151 };

	/** The Constant DYNAMIC_PORTS. */
	public static final Object[] DYNAMIC_PORTS = new Object[] { "Dynamic ports", 49152, 65535 };
}
