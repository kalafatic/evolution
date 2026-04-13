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
package eu.kalafatic.utils.os;

import java.util.Properties;

/**
 * The Class class OSUtils.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class OSUtils {

	/** The properties. */
	private Properties properties = System.getProperties();

	/** The Constant INSTANCE. */
	public static final OSUtils INSTANCE = new OSUtils();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the oS name.
	 * 
	 * @return the oS name
	 */
	public String getOSName() {
		return properties.getProperty("os.name");
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the oS arch.
	 * 
	 * @return the oS arch
	 */
	public String getOSArch() {
		return properties.getProperty("os.arch");
	}
}
