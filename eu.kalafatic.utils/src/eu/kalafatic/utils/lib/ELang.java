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

import org.eclipse.swt.graphics.Image;

import eu.kalafatic.utils.constants.FCoreImageConstants;

/**
 * The Enum enum ELang.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum ELang {

	/** The CS. */
	CS(0, "CS", FCoreImageConstants.CS_IMG),

	/** The EN. */
	EN(1, "EN", FCoreImageConstants.EN_IMG),

	/** The DE. */
	DE(2, "DE", FCoreImageConstants.DE_IMG),

	/** The ZH. */
	ZH(3, "ZH", FCoreImageConstants.ZH_IMG),

	/** The RU. */
	RU(4, "RU", FCoreImageConstants.RU_IMG),

	/** The ES. */
	ES(5, "ES", FCoreImageConstants.ES_IMG);

	/** The index. */
	public int index;

	/** The literal. */
	public String literal;

	/** The image. */
	public Image image;

	/**
	 * Instantiates a new e lang.
	 * @param index the index
	 * @param literal the literal
	 * @param image the image
	 */
	ELang(int index, String literal, Image image) {
		this.index = index;
		this.literal = literal;
		this.image = image;
	}
}
