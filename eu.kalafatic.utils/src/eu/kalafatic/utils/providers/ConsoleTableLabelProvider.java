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
package eu.kalafatic.utils.providers;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.utils.model.LogElement;

/**
 * The Class class ConsoleTableLabelProvider.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ConsoleTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider, ITableFontProvider {

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang .Object, int)
	 */
	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof LogElement) {
			LogElement logElement = (LogElement) element;

			switch (columnIndex) {
			case 0:
				return logElement.getType();
			case 1:
				return logElement.getDate();
			case 2:
				return logElement.getName();
			case 3:
				return logElement.getMsg();
			case 4:
				return logElement.getTrace();

			default:
				break;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang .Object, int)
	 */
	@Override
	public Image getColumnImage(Object arg0, int arg1) {

		return null;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableFontProvider#getFont(java.lang.Object, int)
	 */
	@Override
	public Font getFont(Object element, int columnIndex) {
		return FUIConstants.ARIAL_8_FONT;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang .Object, int)
	 */
	@Override
	public Color getBackground(Object element, int columnIndex) {
		return null;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang .Object, int)
	 */
	@Override
	public Color getForeground(Object element, int columnIndex) {

		if (element instanceof LogElement) {
			LogElement logElement = (LogElement) element;

			if (logElement.getType() == null) {
				return null;
			}

			if (logElement.getType().startsWith("ERR")) {
				return FUIConstants.RED;
			} else if (logElement.getName().startsWith("PROCESSING")) {
				return FUIConstants.BLUE;
			} else if (logElement.getName().startsWith("INCOMING")) {
				return FUIConstants.DARK_GRAY;
			} else if (logElement.getName().startsWith("SHEDULER")) {
				return FUIConstants.GREEN;
			} else {
				return FUIConstants.BLACK;
			}
		}
		return null;
	}
}
