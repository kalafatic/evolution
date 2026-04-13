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
package eu.kalafatic.utils.application;

import java.util.Properties;

/**
 * The Class class JavaUtils.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class JavaUtils {

	/** The properties. */
	private Properties properties = System.getProperties();

	/** The Constant INSTANCE. */
	public static final JavaUtils INSTANCE = new JavaUtils();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the java version.
	 * 
	 * @return the java version
	 */
	public String getJavaVersion() {
		return properties.getProperty("java.version");
	}

}
