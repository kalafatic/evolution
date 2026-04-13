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
package eu.kalafatic.utils.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.utils.constants.ERegex;

/**
 * The Class class StringUtils.
 * @author Petr Kalafatic
 * @version 3.0.0
 * @project Gemini
 */
public class StringUtils {

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Split.
	 * @param name the name
	 * @return the string
	 */
	public static String split(String name) {
		return split(name, ERegex.SPLIT_NAME.getRegex());
	}

	// ---------------------------------------------------------------

	/**
	 * Split.
	 * @param name the name
	 * @param regex the regex
	 * @return the string
	 */
	public static String split(String name, String regex) {
		StringBuffer stringBuffer = new StringBuffer();
		String[] splitArray = name.split(regex);

		for (int i = 0; i < splitArray.length; i++) {
			stringBuffer.append(splitArray[i] + " ");
		}
		return stringBuffer.toString();
	}

	// ---------------------------------------------------------------

	/**
	 * Split java names.
	 * @param javaName the java name
	 * @return the string[]
	 */
	public static String[] splitJavaNames(String javaName) {
		return javaName.split(ERegex.SPLIT_JAVA_NAME.getRegex());
	}

	// ---------------------------------------------------------------

	/**
	 * Capitalize first characters.
	 * @param strings the strings
	 * @return the string
	 */
	public static String capitalizeFirstCharacters(boolean addBlanks, String... strings) {
		StringBuilder buffer = new StringBuilder();
		for (String string : strings) {
			if (string.trim().length() > 0) {
				buffer.append(string.substring(0, 1).toUpperCase());
			}
			if (string.trim().length() > 1) {
				buffer.append(string.substring(1).toLowerCase());
			}
			if (addBlanks) {
				buffer.append(" ");
			}
		}
		return buffer.toString();
	}

	// ---------------------------------------------------------------

	/**
	 * Parses the integers.
	 * @param text the text
	 * @return the list
	 */
	public static List<Integer> parseIntegers(String text) {
		List<Integer> integers = new ArrayList<Integer>();
		StringBuilder buffer = new StringBuilder(text);
		replaceIntegerRanges(buffer);

		String[] splitArray = buffer.toString().split(",");

		for (int i = 0; i < splitArray.length; i++) {
			if (!splitArray[i].trim().isEmpty()) {
				integers.add(Integer.parseInt(splitArray[i]));
			}
		}
		return integers;

	}

	// ---------------------------------------------------------------

	/**
	 * Replace integer ranges.
	 * @param buffer the buffer
	 */
	public static void replaceIntegerRanges(StringBuilder buffer) {
		List<Integer> integers = new ArrayList<Integer>();

		Pattern pattern = ERegex.SPLIT_RANGES.getPattern();
		Matcher matcher = pattern.matcher(buffer);
		while (matcher.find()) {
			String fromGroup = matcher.group(2);
			String toGroup = matcher.group(3);

			int from = Integer.parseInt(fromGroup);
			int to = Integer.parseInt(toGroup);

			String s = new String();

			for (int i = from; i <= to; i++) {
				integers.add(i);
				s += Integer.toString(i) + ",";
			}
			buffer.replace(matcher.start(2), matcher.end(3), s);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * The main method.
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		String text = "1,2,3-5,6";

		List<Integer> parseIntegerRanges = parseIntegers(text);
		System.err.println(parseIntegerRanges);
	}
}
