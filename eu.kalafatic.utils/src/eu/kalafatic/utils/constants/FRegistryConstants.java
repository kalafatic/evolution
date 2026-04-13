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

/**
 * The Class class FRegistryConstants.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public final class FRegistryConstants {

	/** The Constant REG_ADD. */
	public static final String REG_ADD = "REG ADD {0} /f";

	/** The Constant REG_DEL. */
	public static final String REG_DEL = "REG DELETE {0} /f";

	// DIRECTORIES
	// ---------------------------------------------------------------
	/** The Constant SOFTWARE. */
	public static final String SOFTWARE = "\"HKLM\\SOFTWARE\\";

	/** The Constant APP_PATHS. */
	public static final String APP_PATHS = "\"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\";

	/** The Constant HKCU_ENV. */
	public static final String HKCU_ENV = "HKCU\\Environment";

	/** The Constant HKLM_ENV. */
	public static final String HKLM_ENV = "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";

	/** The Constant TASK_KILL. */
	public static final String TASK_KILL = "taskkill /f /IM {0}";

	/** The Constant TASK_START. */
	public static final String TASK_START = "{0}";

	/** The Constant PAUSE_CMD. */
	public static final String PAUSE_CMD = "1.1.1.1 -n 1 -w 5000 >NUL";

}
