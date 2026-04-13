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
package eu.kalafatic.utils.factories;

/**
 * A factory for creating OSCmd objects.
 */
public class OSCmdFactory {

	/** The INSTANCE. */
	private volatile static OSCmdFactory INSTANCE;

	/**
	 * Gets the single instance of OSCmdFactory.
	 * 
	 * @return single instance of OSCmdFactory
	 */
	public static OSCmdFactory getInstance() {
		if (INSTANCE == null) {
			synchronized (OSCmdFactory.class) {
				INSTANCE = new OSCmdFactory();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------


}
