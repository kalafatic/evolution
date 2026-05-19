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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Metadata model for a dynamic field in DynamicMapDialog using bitmask flags.
 *
 * @author Petr Kalafatic
 */
public class DynamicField {

	public static final int TYPE_TEXT     = 1;
	public static final int TYPE_COMBO    = 1 << 1;
	public static final int TYPE_CHECKBOX = 1 << 2;
	public static final int TYPE_NUMBER   = 1 << 3;
	public static final int TYPE_SPINNER  = 1 << 4;

	public static final int REQUIRED      = 1 << 10;
	public static final int READ_ONLY     = 1 << 11;
	public static final int MULTILINE     = 1 << 12;
	public static final int PASSWORD      = 1 << 13;
	public static final int FILE          = 1 << 14;
	public static final int DIRECTORY     = 1 << 15;

	private String label;
	private Object value;
	private int flags;
	private String tooltip;
	private List<String> comboValues;
	private int width = 200;

	/**
	 * Default constructor.
	 */
	public DynamicField() {
	}

	/**
	 * Flexible constructor for dynamic fields.
	 *
	 * @param label the field label
	 * @param flags the bitmask flags
	 * @param values optional values: values[0] is default value, values[1..n] are combo values
	 */
	public DynamicField(String label, int flags, Object... values) {
		this.label = label;
		this.flags = flags;

		if (values != null && values.length > 0) {
			this.value = values[0];

			if (values.length > 1) {
				this.comboValues = new ArrayList<>();
				for (int i = 1; i < values.length; i++) {
					if (values[i] instanceof List) {
						for (Object o : (List<?>) values[i]) {
							this.comboValues.add(o.toString());
						}
					} else if (values[i] instanceof Object[]) {
						for (Object o : (Object[]) values[i]) {
							this.comboValues.add(o.toString());
						}
					} else {
						this.comboValues.add(values[i].toString());
					}
				}
			}
		}
	}

	/**
	 * Checks if a specific flag is set.
	 * @param flag the flag to check
	 * @return true if set
	 */
	public boolean has(int flag) {
		return (flags & flag) != 0;
	}

	// Getters and Setters

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public List<String> getComboValues() {
		return comboValues;
	}

	public void setComboValues(List<String> comboValues) {
		this.comboValues = comboValues;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}
}
