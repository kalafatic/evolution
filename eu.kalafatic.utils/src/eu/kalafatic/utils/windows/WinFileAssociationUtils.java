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
package eu.kalafatic.utils.windows;


/**
 * The Class class WinFileAssociationUtils.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class WinFileAssociationUtils {

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the assoc.
	 * 
	 * @param ext
	 *            the ext
	 * @return the assoc
	 */
	public static String getAssoc(String ext) {
		Process process = WinCMDUtils.getInstance().processCommand("cmd.exe",
				"/c", "assoc", ext);
		return WinCMDUtils.getInstance().getProcessOutput(process);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the ftype.
	 * 
	 * @param ftype
	 *            the ftype
	 * @return the ftype
	 */
	public static String getFtype(String ftype) {
		Process process = WinCMDUtils.getInstance().processCommand("cmd.exe",
				"/c", "ftype", ftype);
		return WinCMDUtils.getInstance().getProcessOutput(process);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the ftype programs.
	 * 
	 * @param ftype
	 *            the ftype
	 * @return the ftype programs
	 */
	public static String getFtypePrograms(String ftype) {
		Process process = WinCMDUtils.getInstance().processCommand("cmd.exe",
				"/c", "ftype", ftype);
		return WinCMDUtils.getInstance().getProcessOutput(process);
	}
}
