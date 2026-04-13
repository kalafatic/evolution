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
package eu.kalafatic.utils.ui;

import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;

/**
 * The Class class UIUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class UIUtils {

	/**
	 * Gets the string width.
	 * @param nChars the n chars
	 * @param control the control
	 * @return the string width
	 */
	public static int getStringWidth(int nChars, Control control) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		return nChars * fontMetrics.getAverageCharWidth();
	}

	/**
	 * Gets the string height.
	 * @param control the control
	 * @return the string height
	 */
	public static int getStringHeight(Control control) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		return fontMetrics.getHeight();
	}

	/**
	 * Gets the widget height.
	 * @param control the control
	 * @param width the width
	 * @param text the text
	 * @return the widget height
	 */
	public static int getWidgetHeight(Control control, int width, String text) {
		int stringWidth = getStringWidth(text.length(), control);

		if (stringWidth > width) {
			int stringHeight = getStringHeight(control);
			int lines = (stringWidth / width);
			lines = (width % stringWidth > 0) ? (lines + 1) : lines;
			return lines * (stringHeight + 2);
		}
		return control.getBounds().height;
	}

}
