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

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.utils.convert.ConvertUtils;

/**
 * The Class class ComboData.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ComboData {

	/** The data. */
	public List<Element> data = new ArrayList<Element>();

	/** The default selection. */
	public int defaultSelection = 0;

	/**
	 * Instantiates a new combo data.
	 * @param defaultSelection the default selection
	 * @param items the items
	 */
	public ComboData(int defaultSelection, String... items) {
		this.defaultSelection = defaultSelection;

		for (int i = 0; i < items.length; i++) {
			data.add(new Element(i, items[i]));
		}
	}

	/**
	 * Instantiates a new combo data.
	 * @param defaultSelection the default selection
	 * @param base the base
	 * @param increment the increment
	 * @param size the size
	 */
	public ComboData(int defaultSelection, long base, int increment, int size) {
		this.defaultSelection = defaultSelection;

		for (int i = 0; i < size; i++) {
			data.add(new Element(i, base, Long.toString(base)));
			base += increment;
		}
	}

	/**
	 * Instantiates a new combo data.
	 * @param defaultSelection the default selection
	 * @param radix the radix
	 * @param isMemory the is memory
	 * @param powerFrom the power from
	 * @param powerTo the power to
	 */
	public ComboData(int defaultSelection, int radix, boolean isMemory, int powerFrom, int powerTo) {
		this(defaultSelection, radix, isMemory, powerFrom, powerTo, 1);
	}

	/**
	 * Instantiates a new combo data.
	 * @param defaultSelection the default selection
	 * @param radix the radix
	 * @param isMemory the is memory
	 * @param powerFrom the power from
	 * @param powerTo the power to
	 * @param multiple the multiple
	 */
	public ComboData(int defaultSelection, int radix, boolean isMemory, int powerFrom, int powerTo, long multiple) {

		this.defaultSelection = defaultSelection;

		int size = powerTo - powerFrom;

		for (int i = 0; i < size; i++) {
			long number = (radix << powerFrom++) * multiple;
			String literal = "";

			if (isMemory) {
				literal = ConvertUtils.getNumberAsMemorySize(number);
			}
			data.add(new Element(i, number, literal));
		}
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the items.
	 * @return the items
	 */
	public String[] getItems() {
		String[] result = new String[data.size()];

		for (int i = 0; i < data.size(); i++) {
			result[i] = data.get(i).literal;
		}
		return result;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the def literal.
	 * @return the def literal
	 */
	public String getDefLiteral() {
		return data.get(defaultSelection).literal;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the element.
	 * @return the element
	 */
	public Element getElement() {
		return data.get(defaultSelection);
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * The Class class Element.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	public class Element {

		/** The index. */
		public int index;

		/** The value. */
		public Object value;

		/** The literal. */
		public String literal;

		/**
		 * Instantiates a new element.
		 * @param index the index
		 * @param literal the literal
		 */
		private Element(int index, String literal) {
			this(index, literal, literal);
		}

		/**
		 * Instantiates a new element.
		 * @param index the index
		 * @param value the value
		 * @param literal the literal
		 */
		private Element(int index, Object value, String literal) {
			super();
			this.index = index;
			this.value = value;
			this.literal = literal;
		}

		// ---------------------------------------------------------------

		/**
		 * Gets the int value.
		 * @return the int value
		 */
		public int getIntValue() {
			if ((Long) value < (long) Integer.MAX_VALUE) {
				return ((Long) value).intValue();
			}
			return 0;
		}
	}
}
