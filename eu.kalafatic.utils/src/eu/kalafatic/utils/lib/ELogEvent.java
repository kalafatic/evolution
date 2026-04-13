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
 * The Enum enum ELogEvent.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum ELogEvent {

	/** The BOTH. */
	BOTH(0, "Both"),

	/** The EVENTS. */
	EVENTS(1, "Events"),

	/** The EXCEPTIONS. */
	EXCEPTIONS(2, "Exceptions");

	/** The index. */
	private int index;

	/** The value. */
	private String value;

	/**
	 * Instantiates a new e log event.
	 * 
	 * @param index
	 *            the index
	 * @param value
	 *            the value
	 */
	private ELogEvent(int index, String value) {
		this.index = index;
		this.value = value;
	}

	/**
	 * Gets the index.
	 * 
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the value.
	 * 
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
}
