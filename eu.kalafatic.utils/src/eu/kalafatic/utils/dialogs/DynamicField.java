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

import java.util.List;

/**
 * Metadata model for a dynamic field in DynamicMapDialog.
 *
 * @author Petr Kalafatic
 */
public class DynamicField {

	private String key;
	private String label;
	private Object value;
	private DynamicFieldType type;

	private List<String> comboValues;

	private boolean required;
	private boolean editable = true;
	private String tooltip;

	private int width = 200;

	/**
	 * Default constructor.
	 */
	public DynamicField() {
	}

	/**
	 * Basic constructor.
	 */
	public DynamicField(String key, String label, Object value, DynamicFieldType type) {
		this.key = key;
		this.label = label;
		this.value = value;
		this.type = type;
	}

	/**
	 * Constructor with combo values.
	 */
	public DynamicField(String key, String label, Object value, DynamicFieldType type, List<String> comboValues) {
		this(key, label, value, type);
		this.comboValues = comboValues;
	}

	// Getters and Setters

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

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

	public DynamicFieldType getType() {
		return type;
	}

	public void setType(DynamicFieldType type) {
		this.type = type;
	}

	public List<String> getComboValues() {
		return comboValues;
	}

	public void setComboValues(List<String> comboValues) {
		this.comboValues = comboValues;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}
}
