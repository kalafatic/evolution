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
package eu.kalafatic.utils.interfaces;

import static eu.kalafatic.utils.constants.FConstants.PREFERENCES;
import eu.kalafatic.utils.preferences.ECorePreferences;

/**
 * The Interface interface ILog.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public interface ILog {

	/** The MA x_ logs. */
	int MAX_LOGS = PREFERENCES.getInt(ECorePreferences.MAX_LOGS.getName(), (Integer) ECorePreferences.MAX_LOGS.getDef());

	/** The LO g_ console. */
	boolean LOG_CONSOLE = PREFERENCES.getBoolean(ECorePreferences.LOG_CONSOLE.getName(), (Boolean) ECorePreferences.LOG_CONSOLE.getValue());

	/** The LO g_ event. */
	String LOG_EVENT = PREFERENCES.get(ECorePreferences.LOG_EVENT.getName(), (String) ECorePreferences.LOG_EVENT.getDef());

	/**
	 * Log.
	 * @param logName the log name
	 * @param msg the msg
	 */
	void log(String logName, String msg);

	/**
	 * Log.
	 * @param logName the log name
	 * @param exception the exception
	 */
	void log(String logName, Exception exception);

	/**
	 * Log.
	 * @param logName the log name
	 * @param msg the msg
	 * @param exception the exception
	 */
	void log(String logName, String msg, Exception exception);
}
