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
package eu.kalafatic.utils.net;

import eu.kalafatic.utils.windows.WinCMDUtils;

/**
 * The Class class NetstatUtils.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class NetstatUtils {

	/** The INSTANCE. */
	private volatile static NetstatUtils INSTANCE;

	/**
	 * Gets the single instance of NetstatUtils.
	 * 
	 * @return single instance of NetstatUtils
	 */
	public static NetstatUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (NetstatUtils.class) {
				INSTANCE = new NetstatUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the users of port.
	 * 
	 * @param port
	 *            the port
	 * @return the users of port
	 */
	public String getUsersOfPort(String port) {
		Process process = WinCMDUtils.getInstance().processCommand("cmd.exe",
				"/c", "netstat", "-a", "-n", "-o", "|", "findstr", ":" + port);
		return WinCMDUtils.getInstance().getProcessOutput(process);
	}
}
