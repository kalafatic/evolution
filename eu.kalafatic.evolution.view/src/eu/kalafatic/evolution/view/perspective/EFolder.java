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
package eu.kalafatic.evolution.view.perspective;

/**
 * The Enum enum EFolder.
 * @author Petr Kalafatic
 * @version 3.0.0
 * @project Gemini
 */
public enum EFolder {

	/** The top left. */
	TOP_LEFT(0, "TOP_LEFT"),

	/** The top right. */
	TOP_RIGHT(1, "TOP_RIGHT"),

	/** The bottom left. */
	BOTTOM_LEFT(2, "BOTTOM_LEFT"),

	/** The bottom right. */
	BOTTOM_RIGHT(3, "BOTTOM_RIGHT"),

	/** The bottom bottom right. */
	BOTTOM_BOTTOM_RIGHT(4, "BOTTOM_BOTTOM_RIGHT"),

	/** The center. */
	CENTER(5, "CENTER"),

	/** The left right. */
	LEFT_RIGHT(6, "LEFT_RIGHT"),

	/** The multi view. */
	MULTI_VIEW(7, ":*");

	/** The index. */
	public int index;

	/** The id. */
	public String ID;

	/**
	 * Instantiates a new e folder.
	 * @param index the index
	 * @param id the id
	 */
	private EFolder(int index, String id) {
		this.index = index;
		this.ID = id;
	}
}
