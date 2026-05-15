/*******************************************************************************
 * Copyright (c) 2024, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.dialogs;

/**
 * Enumeration of supported dynamic field types for DynamicMapDialog.
 *
 * @author Petr Kalafatic
 */
public enum DynamicFieldType {
	TEXT,
	MULTILINE_TEXT,
	COMBO,
	CHECKBOX,
	DIRECTORY,
	FILE,
	PASSWORD,
	NUMBER
}
