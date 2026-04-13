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
package eu.kalafatic.utils.constants;

import java.util.regex.Pattern;

/**
 * The Enum enum ERegex.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum ERegex {

	/** The CID r_ address. */
	CIDR_ADDRESS("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))?"),

	IPV6("([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}"),

	SPLIT_NAME("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(_)"),

	SPLIT_NAME_SIMPLE("([A-Z]{,1})|(_)"),

	SPLIT_JAVA_NAME("((?<=\\p{Ll})(?=\\p{Lu}))|_"),

	SPLIT_RANGES("(,?)(\\d+)-(\\d+)(,?)"),

	;

	/** The regex. */
	String regex;

	/** The pattern. */
	Pattern pattern;

	/**
	 * Instantiates a new e regex.
	 * @param regex the regex
	 */
	private ERegex(String regex) {
		this.regex = regex;
	}

	/**
	 * Gets the regex.
	 * @return the regex
	 */
	public String getRegex() {
		return regex;
	}

	/**
	 * Gets the pattern.
	 * @return the pattern
	 */
	public Pattern getPattern() {
		return Pattern.compile(getRegex());
	}
}
