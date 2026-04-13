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
package eu.kalafatic.utils.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The Class class CMDParser.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class CMDParser {

	/** The st. */
	private static StringTokenizer st;
	
	/** The value. */
	private static String token, key = "app", value = "";
	
	/** The args. */
	private static List<String> args = new ArrayList<String>();
	
	/** The command map. */
	private static Map<String, Object> commandMap = new LinkedHashMap<String, Object>();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Parses the.
	 */
	private static void parse() {
		switch (token.charAt(0)) {

		case '"':
			value = getWord(st, token, "\"");
			commandMap.put(key, value);
			break;

		case '%':
			if (token.endsWith("%")) {
				value = getWord(st, token, " ");
				commandMap.put(key, value);
				args.add(value);
			} else if (token.contains("=")) {
				String[] split = token.substring(1).split("=");
				commandMap.put(split[0], split[1]);
				parse();
			} else {
				args.add(token.substring(1));
			}
			break;

		case '-':
			if (token.contains("=")) {
				String[] split = token.substring(1).split("=");
				key = split[0];
				token = split[1];
				parse();
			} else {
				args.add(token.substring(1));
			}
			break;

		case '/':
			args.add(token.substring(1));
			break;

		default:
			args.add(token);
			break;
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Parses the.
	 * 
	 * @param cmd
	 *            the cmd
	 * @return the map
	 */
	public static Map<String, Object> parse(String cmd) {
		commandMap = new LinkedHashMap<String, Object>();
		args = new ArrayList<String>();
		commandMap.put("args", args);
		st = new StringTokenizer(in, " ");

		while (st.hasMoreTokens()) {
			token = st.nextToken();
//			System.out.println(token + "\t");
			parse();
		}
		return commandMap;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the word.
	 * 
	 * @param st
	 *            the st
	 * @param word
	 *            the word
	 * @param endChar
	 *            the end char
	 * @return the word
	 */
	private static String getWord(StringTokenizer st, String word,
			String endChar) {
		while (!word.endsWith(endChar)) {
			word += st.nextToken();
		}
		return word;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * The main method.
	 * 
	 * @param arg
	 *            the arguments
	 */
	public static void main(String arg[]) {
		System.err.println(parse(in));
	}

	// static String in = "title=Java-Samples;" + "author=Emiley J;"
	// + "publisher=java-samples.com;" + "copyright=2007;" +
	// "c:\\Users\\kalafaticp\\Desktop\\JOB\\Documents\\Web\\Bugtracker\\";

	/** The in. */
	static String in = "%SystemRoot%\\system32\\cmd.exe -1=\"c:\\Users\\kalafaticp\" /n %0";

	// static String in =
	// "\"c:\\Use rs\\kalaf aticp\\cmd.exe\" -1=\"c:\\Users\\kalafaticp\" /n %0";


}
