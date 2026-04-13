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
 * The Enum enum EDateFormat.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum EDateFormat {

	/** The SIMPL e_1. */
	SIMPLE_1(0, "dd MMMMM yyyy"),

	/** The SIMPL e_2. */
	SIMPLE_2(1, "EEE, MMM d"),

	/** The BASIC. */
	BASIC(3, "dd.MM.yy"),

	/** The NIC e_1. */
	NICE_1(4, "yyyy.MM.dd 'at' hh:mm:ss z"),

	/** The NIC e_2. */
	NICE_2(5, "yyyy.MM.dd G 'at' hh:mm:ss z"),

	/** The NIC e_3. */
	NICE_3(6, "yyyy.MMMMM.dd GGG hh:mm aaa"),

	/** The TIM e_1. */
	TIME_1(7, "H:mm:ss:SSS"),

	/** The TIM e_2. */
	TIME_2(8, "K:mm a,z"),

	;

	/** The index. */
	public int index;

	/** The literal. */
	public String literal;

	/**
	 * Instantiates a new e date format.
	 * @param index the index
	 * @param literal the literal
	 */
	EDateFormat(int index, String literal) {
		this.index = index;
		this.literal = literal;
	}

	/**
	 * Gets the index.
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the literal.
	 * @return the literal
	 */
	public String getLiteral() {
		return literal;
	}

}
