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

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import eu.kalafatic.utils.preferences.AppPreferences;
import eu.kalafatic.utils.windows.WinFileAssociationUtils;

/**
 * The Class class Association.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class Association {

	/** The extension. */
	private String extension;

	/** The file type. */
	private String fileType;

	/** The programs. */
	public static Map<String, Program> programs = new HashMap<String, Program>();

	/**
	 * Instantiates a new association.
	 * @param extension the extension
	 */
	private Association(String extension) {
		this.extension = extension;

		queryExtension(extension);
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Query extension.
	 * @param extension the extension
	 */
	public void queryExtension(String extension) {
		String assoc = WinFileAssociationUtils.getAssoc(extension);
		String[] split = assoc.split("=");

		if (split.length > 1) {
			setFileType(split[1]);
			String ftype = WinFileAssociationUtils.getFtype(getFileType());
			parseFtype(ftype);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Parses the ftype.
	 * @param ftype the ftype
	 */
	private void parseFtype(String ftype) {
		String[] split = ftype.split("=");

		if (split.length > 1) {
			String programWithArgs = split[1];
			Program program = new Program(programWithArgs);

			programs.put(getExtension(), program);

			AppPreferences.getInstance().putPreference("extensions", extension, ftype);
		}
		// Scanner s = new Scanner(ftype).useDelimiter("");
		// Scanner s = new Scanner(ftype).useDelimiter("\\s*fish\\s*");

	}

	// ---------------------------------------------------------------

	/**
	 * Gets the extension.
	 * @return the extension
	 */
	public String getExtension() {
		return extension;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the extension.
	 * @param extension the new extension
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the file type.
	 * @return the file type
	 */
	public String getFileType() {
		return fileType;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the file type.
	 * @param fileType the new file type
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * The Class class Program.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	class Program {

		/** The program. */
		public String program;

		/** The program path. */
		public String programPath;

		/** The program with args. */
		public String programWithArgs;

		/** The args. */
		public Map<String, String> args = new HashMap<String, String>();

		/**
		 * Instantiates a new program.
		 * @param programWithArgs the program with args
		 */
		private Program(String programWithArgs) {
			this.programWithArgs = programWithArgs;

			parseProgram();
		}

		/**
		 * Parses the program.
		 */
		private void parseProgram() {
			if (programWithArgs.contains("\"")) {
				String[] split = programWithArgs.split("\"");

				if (split.length > 1) {

					String programArgs = split[1];

					if (programArgs.contains("%")) {
						Scanner scanner = new Scanner(programArgs).useDelimiter("%");
						while (scanner.hasNext()) {

						}
					}
				}
			}
		}
	}

}
