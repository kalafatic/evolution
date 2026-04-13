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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.widgets.Display;

import eu.kalafatic.utils.constants.FCoreImageConstants;
import eu.kalafatic.utils.interfaces.IPreference;
import eu.kalafatic.utils.lib.ELogEvent;
import eu.kalafatic.utils.lib.ELogType;
import eu.kalafatic.utils.lib.ERemote;

/**
 * The Enum enum ECorePreferences.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum ECorePreferences implements IPreference {

	/** The SET. */
	SET("Set", false, false),

	/** The MODULE. */
	MODULE("Core", "", ""),

	// APLICATION
	/** The AP p_ name. */
	APP_NAME("Application name", "Gemini", "Gemini"),

	/** The AP p_ id. */
	APP_ID("Application ID", "-GE2010-000000000000", "-GE2010-000000000000"),
	// APP_ID(3, "Application ID", "M2-0-1--000000000000",
	// "M2-0-1--000000000000"),

	// LOCATIONS
	/** The WORKSPAC e_ loc. */
	WORKSPACE_LOC("Workspace location", "C:\\GE\\workspace", "C:\\GE\\workspace"),

	/** The MODEL s_ loc. */
	MODELS_LOC("Models location", "C:\\GE\\workspace\\models", "C:\\GE\\workspace\\models"),

	/** The LOG s_ loc. */
	LOGS_LOC("Logs location", "C:\\GE\\workspace\\logs", "C:\\GE\\workspace\\logs"),

	/** The TORRENT s_ loc. */
	TORRENTS_LOC("Torrents location", "C:\\GE\\workspace\\torrents", "C:\\GE\\workspace\\torrents"),

	/** The DOWNLOAD s_ loc. */
	DOWNLOADS_LOC("Downloads location", "C:\\GE\\workspace\\downloads", "C:\\GE\\workspace\\downloads"),

	/** The SIT e_ loc. */
	SITE_LOC("Site location", "C:\\GE\\site", "C:\\GE\\site"),

	/** The N l_ loc. */
	NL_LOC("Site location", "C:\\GE\\nl", "C:\\GE\\nl"),

	/** The CER t_ loc. */
	CERT_LOC("App certificates location", "C:\\GE\\certificates", "C:\\GE\\certificates"),

	/** The JD k_ cer t_ loc. */
	JDK_CERT_LOC("JDK cacerts certificates location", "", ""),

	/** The JR e_ cer t_ loc. */
	JRE_CERT_LOC("JRE cacerts certificates location", "", ""),

	/** The CER t_ enabled. */
	CERT_ENABLED("Certificates loaded (lazy)", false, false),

	/** The JAVADO c_ loc. */
	JAVADOC_LOC("Javadoc location", "C:\\GE\\javadoc", "C:\\GE\\javadoc"),

	/** The UPDATE s_ loc. */
	UPDATES_LOC("Updates location", "C:\\GE\\updates", "C:\\GE\\updates"),

	/** The MAINTAI n_ loc. */
	MAINTAIN_LOC("Maintain location", "C:\\GE\\maintain", "C:\\GE\\maintain"),

	// SYSTEM
	/** The CP u_ name. */
	CPU_NAME("CPU Name", "", ""),

	/** The CP u_ speed. */
	CPU_SPEED("CPU Speed", "", ""),

	/** The O s_ name. */
	OS_NAME("OS Name", "", ""),

	/** The O s_ arch. */
	OS_ARCH("OS Architecture", "", ""),

	// MEMORY
	/** The MA x_ ap p_ memory. */
	MAX_APP_MEMORY("Max memory (MB)", 5, 5),

	/** The AP p_ memory. */
	APP_MEMORY("Application memory (MB)", 32, 32),

	/** The DIS c_ buffer. */
	DISC_BUFFER("Disc write buffer (MB)", 64, 64),

	// JAVA
	/** The JAV a_ version. */
	JAVA_VERSION("Java version", "", ""),

	// NETWORK
	/** The I p_ protocol. */
	IP_PROTOCOL("IP version ", 4, 4),

	/** The SINGLETO n_ arg s_ port. */
	SINGLETON_ARGS_PORT("Singleton arguments port", 6880, 6880),

	/** The R c_ port. */
	RC_PORT("Remote control port", 6881, 6881),

	/** The B t_ clien t_ port. */
	BT_CLIENT_PORT("BT client port", 6882, 6882),

	/** The TRACKE r_ port. */
	TRACKER_PORT("Tracker port", 6883, 6883),

	/** The TRACKE r_ uploa d_ port. */
	TRACKER_UPLOAD_PORT("Tracker upload port", 6884, 6884),

	/** The UPDAT e_ port. */
	UPDATE_PORT("Update port", 6885, 6885),

	/** The WE b_ uploa d_ port. */
	WEB_UPLOAD_PORT("Web upload port", 8000, 8000),

	/** The PIN g_ address. */
	PING_ADDRESS("Ping address", "http://www.google.com:80", "http://www.google.com:80"),

	/** The WATCHDO g_ delay. */
	WATCHDOG_DELAY("Watchdog delay (s)", 10, 10),

	/** The PIN g_ enabled. */
	PING_ENABLED("Ping enabled", ERemote.DISABLED.literal, ERemote.DISABLED.literal),

	/** The PROX y_ set. */
	PROXY_SET("Proxy", false, false),

	/** The PROX y_ host. */
	PROXY_HOST("Host", "", ""),

	/** The PROX y_ port. */
	PROXY_PORT("Port", 8080, 8080),

	/** The PROX y_ user. */
	PROXY_USER("User", "", ""),

	/** The PROX y_ pass. */
	PROXY_PASS("Password", "", ""),

	// OS INTEGRATION
	/** The O s_ integration. */
	OS_INTEGRATION("OS Integration", "", ""),

	/** The EXTENSION. */
	EXTENSION("Extension", "", ""),

	/** The PROGRAM. */
	PROGRAM("Program", "", ""),

	/** The ASSOCIATED. */
	ASSOCIATED("Associated", "", ""),

	// ADDRESS
	/** The HOM e_ site. */
	HOME_SITE("Home", "http://kalafatic.eu", "http://kalafatic.eu"),

	/** The UPDAT e_ site. */
	UPDATE_SITE("Update", "http://gemini.kalafatic.eu/Pages/Update/index.html", "http://gemini.kalafatic.eu/Pages/Update/index.html"),

	/** The BU g_ site. */
	BUG_SITE("Bugs", "http://bugs.kalafatic.eu", "http://bugs.kalafatic.eu"),

	/** The MAI l_ site. */
	MAIL_SITE("Mail", "http://post.kalafatic.eu", "http://post.kalafatic.eu"),

	// CUSTOMIZE
	/** The LO g_ console. */
	LOG_CONSOLE("Enable console output", true, true),

	/** The LO g_ enabled. */
	LOG_ENABLED("Enable logging", false, false),

	/** The LO g_ type. */
	LOG_TYPE("Log type", ELogType.TXT.getValue(), ELogType.TXT.getValue()),

	/** The LO g_ event. */
	LOG_EVENT("Log event", ELogEvent.BOTH.getValue(), ELogEvent.BOTH.getValue()),

	/** The LO g_ size. */
	LOG_SIZE("Max size (kB)", 100, 100),

	/** The CHEC k_ after. */
	CHECK_AFTER("Check log size\n(after x inputs)", 30, 30),

	/** The MA x_ logs. */
	MAX_LOGS("Max logs", 64, 64),

	/** The THREA d_ management. */
	THREAD_MANAGEMENT("Deadlocks management enabled", false, false),

	/** The CP u_ management. */
	CPU_MANAGEMENT("CPU management enabled", true, true),

	/** The MA x_ cpu. */
	MAX_CPU("Max CPU usage (%)", 50, 50),

	/** The SOUND. */
	SOUND("Sound enabled", false, false),

	/** The RU n_ b t_ afte r_ start. */
	RUN_BT_AFTER_START("Run BT after application start", false, false),

	/** The R c_ enabled. */
	RC_ENABLED("Enable RC", true, true),

	/** The PROGRA m_ loc. */
	PROGRAM_LOC("Program location", "C:\\GE\\Gemini\\Gemini.exe", "C:\\GE\\Gemini\\Gemini.exe"),

	/** The D b_ torrennt s_ query. */
	DB_TORRENNTS_QUERY("DB torrents query", "7-select name from torrents", "7-select name from torrents"),

	// SYNCHRONIZATION
	/** The SYN c_ client. */
	SYNC_CLIENT("Sync with client", true, true),

	/** The SYN c_ files. */
	SYNC_FILES("Sync with local files", true, true),

	/** The SYN c_ db. */
	SYNC_DB("Sync with DB", true, true),

	/** The SUBFOLDERS. */
	SUBFOLDERS("Subfolders", true, true),

	LANGUAGE("Language", Locale.ENGLISH.getLanguage(), Locale.ENGLISH.getLanguage()),

	LANGUAGES("Languages", new ArrayList<Object[]>(), new ArrayList<Object[]>())

	;

	private static List<Object[]> locales;
	private static String[] langs = { "cs", "en", "de", "zh", "ru", "ru" };

	private List<Object[]> getLocale() {
		if (locales == null) {
			locales = new ArrayList<Object[]>();
			int i = 0;
			locales.add(new Object[] { langs[i++], FCoreImageConstants.CS_IMG });
			locales.add(new Object[] { langs[i++], FCoreImageConstants.EN_IMG });
			locales.add(new Object[] { langs[i++], FCoreImageConstants.DE_IMG });
			locales.add(new Object[] { langs[i++], FCoreImageConstants.ZH_IMG });
			locales.add(new Object[] { langs[i++], FCoreImageConstants.RU_IMG });
			locales.add(new Object[] { langs[i++], FCoreImageConstants.ES_IMG });
		}
		return locales;
	}

	public static int getLocaleIndex(String lang) {
		for (int i = 0; i < langs.length; i++) {
			if (langs[i].equals(lang)) {
				return i;
			}
		}
		return 0;
	}

	/** The name. */
	private String name;

	/** The def. */
	private Object value, def;

	/**
	 * Instantiates a new e core preferences.
	 * @param index the index
	 * @param name the name
	 * @param value the value
	 * @param def the def
	 */
	private ECorePreferences(String name, final Object value, final Object def) {
		this.name = name;
		this.value = value;
		this.def = def;

		if (name.equals("Languages")) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					ECorePreferences.LANGUAGES.value = getLocale();
					ECorePreferences.LANGUAGES.def = getLocale();
				}
			});
		}
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IPreference#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the name.
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IPreference#getValue()
	 */
	@Override
	public Object getValue() {
		return value;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the value.
	 * @param value the new value
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IPreference#getDef()
	 */
	@Override
	public Object getDef() {
		return def;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the def.
	 * @param def the new def
	 */
	public void setDef(Object def) {
		this.def = def;
	}
}
