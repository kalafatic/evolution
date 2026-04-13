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

/**
 * The Enum enum ESyncType.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum ESyncType {

	/** The DEF. */
	DEF(0, "Default"),
	
	/** The FILE. */
	FILE(1, "File"),
	
	/** The DB. */
	DB(2, "Database");

	/** The value. */
	public int value;

	/** The literal. */
	public String literal;

	/**
	 * Instantiates a new e sync type.
	 * 
	 * @param value
	 *            the value
	 * @param literal
	 *            the literal
	 */
	private ESyncType(int value, String literal) {
		this.value = value;
		this.literal = literal;
	}
}
