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
package eu.kalafatic.utils.model;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * The Class class PolygonObject.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class PolygonObject {

	/** The p4. */
	private Point p1, p2, p3, p4;

	/**
	 * Instantiates a new polygon object.
	 * 
	 * @param image
	 *            the image
	 * @param data
	 *            the data
	 */
	public PolygonObject(Image image, int data) {

		Rectangle rect = image.getBounds();		

		p1 = new Point(10, rect.height - 8 - data);
		p2 = new Point(10, rect.height - 14);
		p4 = new Point(30, rect.height - 8- data);
		p3 = new Point(30, rect.height - 14);
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Shift polygon.
	 * 
	 * @param shift
	 *            the shift
	 * @param indexOf
	 *            the index of
	 */
	public synchronized void shiftPolygon(int shift, int indexOf) {
		p1.x = 30 + shift + (indexOf * 20);
		p2.x = 30 + shift + (indexOf * 20);
		p3.x = 10 + shift + (indexOf * 20);
		p4.x = 10 + shift + (indexOf * 20);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the polygon.
	 * 
	 * @return the polygon
	 */
	public int[] getPolygon() {
		return new int[] { p1.x, p1.y, p2.x, p3.y, p3.x, p3.y, p4.x, p4.y };
	}

}
