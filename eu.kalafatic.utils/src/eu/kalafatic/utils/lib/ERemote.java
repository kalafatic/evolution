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
package eu.kalafatic.utils.lib;

/**
 * The Enum enum ERemote.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum ERemote {

	/** The DISABLED. */
	DISABLED(0, "Disabled"),

	/** The AUTO. */
	AUTO(1, "Auto"),

	/** The ENABLED. */
	ENABLED(2, "Enabled");

	/** The index. */
	public int index;

	/** The literal. */
	public String literal;

	/**
	 * Instantiates a new e remote.
	 * 
	 * @param index
	 *            the index
	 * @param literal
	 *            the literal
	 */
	ERemote(int index, String literal) {
		this.index = index;
		this.literal = literal;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * As string array.
	 * 
	 * @return the string[]
	 */
	public static String[] asStringArray() {
		String[] stringArray = new String[values().length];
		int i = 0;
		for (ERemote en : values()) {
			stringArray[i++] = en.literal;
		}
		return stringArray;
	}
}
