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

import java.util.ArrayList;
import java.util.List;

/**
 * The Class class FCMDConstants.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public final class FCMDConstants {

	/** The Constant FILE_COMMAND. */
	public static final String FILE_COMMAND = "rundll32 url.dll,FileProtocolHandler ";

	/** The Constant PDF_COMMAND. */
	public static final String PDF_COMMAND = "rundll32 url.dll,FileProtocolHandler ";

	/** The Constant PDF_ASSOC. */
	public static final List<String> PDF_ASSOC = new ArrayList<String>() {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			add(".pdf");
		}
	};

	/** The Constant TXT_COMMAND. */
	public static final String TXT_COMMAND = "c:\\Windows\\system32\\notepad.exe \"";

	/** The Constant TXT_ASSOC. */
	public static final List<String> TXT_ASSOC = new ArrayList<String>() {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			add(".txt");
			// add(EExt.TORRENT.ext);
			add(".ini");
			add(".url");
			add(".xpi");
		}
	};

	/** The Constant IMG_COMMAND. */
	public static final String IMG_COMMAND = "c:\\Windows\\system32\\mspaint.exe \"";

	/** The Constant IMG_ASSOC. */
	public static final List<String> IMG_ASSOC = new ArrayList<String>() {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			add(".jpg");
			add(".jpeg");
			add(".png");
			add(".gif");
		}
	};

	/** The Constant DOC_COMMAND. */
	public static final String DOC_COMMAND = "rundll32 SHELL32.DLL,ShellExec_RunDLL ";

	/** The Constant DOC_ASSOC. */
	public static final List<String> DOC_ASSOC = new ArrayList<String>() {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			add(".doc");
			add(".docx");
		}
	};

	// TOKENS
	// ---------------------------------------------------------------

	/** The Constant REGSTR_TOKEN. */
	public static final String REGSTR_TOKEN = "REG_SZ";

	/** The Constant REGSTREXP_TOKEN. */
	public static final String REGSTREXP_TOKEN = "REG_EXPAND_SZ";

	/** The Constant REGDWORD_TOKEN. */
	public static final String REGDWORD_TOKEN = "REG_DWORD";

	// SELECT
	// ---------------------------------------------------------------

	/** The Constant REG_QUERY. */
	public static final String REG_QUERY = "reg query ";

	/** The Constant GET_PERSONAL_FOLDER. */
	public static final String GET_PERSONAL_FOLDER = REG_QUERY + "\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\" + "Explorer\\Shell Folders\" /v Personal";

	/** The Constant GET_CPU_SPEED. */
	public static final String GET_CPU_SPEED = REG_QUERY + "\"HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0\"" + " /v ~MHz";

	/** The Constant GET_CPU_NAME. */
	public static final String GET_CPU_NAME = REG_QUERY + "\"HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0\"" + " /v ProcessorNameString";

	/** The Constant GET_USER_ENV. */
	public static final String GET_USER_ENV = REG_QUERY + "\"" + FRegistryConstants.HKCU_ENV + "\"" + " /v Path";

	/** The Constant GET_SYSTEM_ENV. */
	public static final String GET_SYSTEM_ENV = REG_QUERY + "\"" + FRegistryConstants.HKLM_ENV + "\"" + " /v Path";

	// ADD
	// ---------------------------------------------------------------

	/** The Constant REG_ADD. */
	// public static final String REG_ADD = "cmd /c reg add ";
	private static final String REG_ADD = "cmd /c reg add ";

	/** The Constant ADD_USER_ENV. */
	public static final String ADD_USER_ENV = REG_ADD + "\"HKEY_CURRENT_USER\\Environment\" /v \"{0}\" /d \"{1}\" /t REG_EXPAND_SZ /f";

	/** The Constant ADD_SYSTEM_ENV. */
	public static final String ADD_SYSTEM_ENV = REG_ADD + "\"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment\" /v \"{0}\" /d \"{1}\" /t REG_EXPAND_SZ /f";

	/** The Constant ADD_APP_PATH_1. */
	public static final String ADD_APP_PATH_1 = REG_ADD + "\"HKLM\\SOFTWARE\\{0}\" /ve /f";

	/** The Constant ADD_APP_PATH_2. */
	public static final String ADD_APP_PATH_2 = REG_ADD + "\"HKLM\\SOFTWARE\\{0}\" /t REG_SZ /v \"Path\" /d {1} /f";

	/** The Constant ADD_APP_PATH_3. */
	public static final String ADD_APP_PATH_3 = REG_ADD + "\"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\{0}\" /ve /d {1} /f";

	/** The Constant ADD_APP_PATH_4. */
	public static final String ADD_APP_PATH_4 = REG_ADD + "\"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\{0}\" /t REG_SZ /v \"Path\" /d {1} /f";

}
