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
 * The Enum enum ESync.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum ESync {

	/** The SYN c_ db. */
	SYNC_DB("Sync DB", true), 
 
 /** The SYN c_ files. */
 SYNC_FILES("Sync Files", false);

	/** The key. */
	public String key;
	
	/** The value. */
	public boolean value;

	/**
	 * Instantiates a new e sync.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
	private ESync(String key, boolean value) {
		this.key = key;
		this.value = value;
	}

}
