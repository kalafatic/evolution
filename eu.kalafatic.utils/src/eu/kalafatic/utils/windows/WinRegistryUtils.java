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
package eu.kalafatic.utils.windows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import eu.kalafatic.utils.constants.FCMDConstants;
import eu.kalafatic.utils.model.Association;

/**
 * The Class class WinRegistryUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class WinRegistryUtils {

	/** The associations. */
	public static Map<String, Association> associations = new HashMap<String, Association>();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the from registry.
	 * @param cmd the cmd
	 * @param token the token
	 * @return the from registry
	 */
	public static String getFromRegistry(String cmd, String token) {
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			StreamReader reader = new StreamReader(process.getInputStream());

			reader.start();
			process.waitFor();
			reader.join();

			String result = reader.getResult();
			int p = result.indexOf(token);

			if (p == -1) {
				return null;
			}

			return result.substring(p + token.length()).trim();
		} catch (Exception e) {
			return null;
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Change registry.
	 * @param cmd the cmd
	 * @param args the args
	 * @return true, if successful
	 */
	public static boolean changeRegistry(String cmd, Object[] args) {
		try {
			String formatedCmd = cmd;
			if ((args != null) && (args.length > 0)) {
				formatedCmd = MessageFormat.format(cmd, args);
			}
			Process process = Runtime.getRuntime().exec(formatedCmd);
			process.waitFor();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Associate extension.
	 * @param ext the ext
	 * @param ftype the ftype
	 */
	public static void associateExtension(String ext, String ftype) {
		changeRegistry("reg delete \"HKCR\\{0}\" /ve /f", new String[] { ext });

		// .extension=filetype
		changeRegistry("reg add \"HKCR\\{0}\" /ve /d \"{1}\" /f", new String[] { ext, ftype });
	}

	// ---------------------------------------------------------------

	/**
	 * Integrate context menu.
	 * @param ext the ext
	 * @param ftype the ftype
	 * @param path the path
	 * @param contexts the contexts
	 */
	public static void integrateContextMenu(String ext, String ftype, String path, boolean[] contexts) {
		String programName = new File(path).getName();

		String pathWithArgs = "\\\"\"" + path + "\\\" \\\"%1\\\"\"";
		path = "\\\"\"" + path + "\\\"\"";

		String prefix = "reg delete \"HKCR\\*\\shell\\GeminiMenu";

		if (contexts[0]) {
			changeRegistry(prefix + "\" /ve /f", null);

			prefix = "reg add \"HKCR\\*\\shell\\GeminiMenu";
			changeRegistry(prefix + "\" /ve /f", null);
			changeRegistry(prefix + "\" /t REG_SZ /v \"MUIVerb\" /d \"{0}\" /f", new String[] { "Gemini" });
			changeRegistry(prefix + "\" /t REG_SZ /v \"Icon\" /d \"{0}\" /f", new String[] { path });

			prefix = "reg delete \"HKCR\\*\\shellex\\ContextMenuHandlers\\GeminiMenu";
			changeRegistry(prefix + "\" /ve /f", null);

			prefix = "reg add \"HKCR\\*\\shellex\\ContextMenuHandlers\\GeminiMenu";
			changeRegistry(prefix + "\" /ve /f", null);
			changeRegistry(prefix + "\" /t REG_SZ /v \"MUIVerb\" /d \"{0}\" /f", new String[] { "Gemini" });
			changeRegistry(prefix + "\" /t REG_SZ /v \"Icon\" /d \"{0}\" /f", new String[] { path });
			return;
		}

		// filetype=program
		if (contexts[1]) {
			prefix = "reg delete \"HKCR\\{0}";
			changeRegistry(prefix + "\" /ve /f", new String[] { ftype });
			changeRegistry(prefix + "\\DefaultIcon\" /ve /f", new String[] { ftype });
			changeRegistry(prefix + "\\shell\" /ve /f", new String[] { ftype });
			changeRegistry(prefix + "\\shell\\open\" /ve /f", new String[] { ftype });
			changeRegistry(prefix + "\\shell\\open\\command\" /ve /f", new String[] { ftype });
			changeRegistry(prefix + "\\shellex\" /ve /f", new String[] { ftype });
			changeRegistry(prefix + "\\shellex\\ContextMenuHandlers\" /ve /f", new String[] { ftype });

			prefix = "reg add \"HKCR\\{0}";
			changeRegistry(prefix + "\" /ve /f", new String[] { ftype });
			changeRegistry(prefix + "\\DefaultIcon\" /ve /d {1} /f", new String[] { ftype, path });

			prefix += "\\shell";
			changeRegistry(prefix + "\" /ve /f", new String[] { ftype });
			prefix += "\\open";
			changeRegistry(prefix + "\" /ve /d \"&Open\" /f", new String[] { ftype });
			changeRegistry(prefix + "\" /t REG_SZ /v \"MUIVerb\" /d Gemini /f", new String[] { ftype });

			prefix += "\\command";
			changeRegistry(prefix + "\" /ve /d {1} /f", new String[] { ftype, pathWithArgs });

			prefix = "reg add \"HKCR\\{0}\\shellex";
			changeRegistry(prefix + "\" /ve /f", new String[] { ftype });
			prefix += "\\ContextMenuHandlers";
			changeRegistry(prefix + "\" /ve /f", new String[] { ftype });
			prefix += "\\" + programName.substring(0, programName.indexOf("."));
			changeRegistry(prefix + "\" /ve /d \"{1}\" /f", new String[] { ftype, programName });

			prefix = "reg delete \"HKCR\\Gemini";
			changeRegistry(prefix + "\" /ve /f", null);
			changeRegistry(prefix + "\\shell\" /ve /f", null);
			changeRegistry(prefix + "\\shell\\Open\" /ve /f", null);
			changeRegistry(prefix + "\\shell\\Open\\command\" /ve /f", null);

			prefix = "reg add \"HKCR\\Gemini";
			changeRegistry(prefix + "\" /ve /f", null);
			changeRegistry(prefix + "\\shell\" /ve /f", null);
			changeRegistry(prefix + "\\shell\\Open\" /ve /f", null);
			changeRegistry(prefix + "\\shell\\Open\" /t REG_SZ /v \"MUIVerb\" /d \"&Open with Gemini\" /f", null);
			changeRegistry(prefix + "\\shell\\Open\\command\" /ve /d {0} /f", new String[] { pathWithArgs });
		}

		// extension=program
		if (contexts[2]) {
			prefix = "reg delete \"HKCR\\{0}";
			changeRegistry(prefix + "\\OpenWithList\" /ve /f", new String[] { ext });
			changeRegistry(prefix + "\\OpenWithList\\{1}\" /ve /f", new String[] { ext, ftype });

			prefix = "reg add \"HKCR\\{0}";
			changeRegistry(prefix + "\\OpenWithList\" /ve /f", new String[] { ext });
			changeRegistry(prefix + "\\OpenWithList\\{1}\" /ve /f", new String[] { ext, programName });
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Adds the program to path.
	 * @param dir the dir
	 * @param path the path
	 */
	public static void addProgramToPath(String dir, String path) {
		String programName1 = new File(path).getName();
		String programName2 = programName1.substring(0, programName1.indexOf("."));

		dir = "\\\"\"" + dir + "\\\"\"";
		path = "\\\"\"" + path + "\\\"\"";

		String prefix = "reg delete \"HKLM\\SOFTWARE\\{0}";
		changeRegistry(prefix + "\" /ve /f", new String[] { programName2 });

		prefix = "reg add \"HKLM\\SOFTWARE\\{0}";
		changeRegistry(prefix + "\" /ve /f", new String[] { programName2 });

		changeRegistry(prefix + "\" /t REG_SZ /v \"Path\" /d \"{1}\" /f", new String[] { "Gemini", dir });
		changeRegistry(prefix + "\" /t REG_SZ /v \"exec\" /d \"{1}\" /f", new String[] { "Gemini", path });

		prefix = "reg delete \"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\{0}";
		changeRegistry(prefix + "\" /ve /f", new String[] { programName1 });

		prefix = "reg add \"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\{0}";
		changeRegistry(prefix + "\" /ve /d \"{1}\" /f", new String[] { programName1, path });
		changeRegistry(prefix + "\" /t REG_SZ /v \"Path\" /d \"{1}\" /f", new String[] { programName1, dir });

	}

	// ---------------------------------------------------------------

	/**
	 * Adds the program to user enviroment.
	 * @param dir the dir
	 */
	public static void addProgramToUserEnviroment(String dir) {
		String prefix = "reg add HKCU\\Environment";
		changeRegistry(prefix + " /t REG_EXPAND_SZ /v \"Path\" /d \"{0}\" /f", new String[] { dir });
	}

	// ---------------------------------------------------------------

	/**
	 * Adds the program to system enviroment.
	 * @param dir the dir
	 */
	public static void addProgramToSystemEnviroment(String dir) {
		// dir = "\"" + dir + "\";\"%Path%\"";
		String prefix = "reg add \"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment\"";

		changeRegistry(prefix + " /t REG_EXPAND_SZ /v \"Path\" /d \"{0}\" /f", new String[] { dir });
	}

	// ---------------------------------------------------------------

	/**
	 * Reload registry.
	 */
	public static void reloadRegistry() {
		try {
			Process process = Runtime.getRuntime().exec("taskkill /f /IM explorer.exe");
			process.waitFor();
			Thread.sleep(2000);
			Runtime.getRuntime().exec("explorer.exe");
			Thread.sleep(2000);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * The Class class StreamReader.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	static class StreamReader extends Thread {

		/** The is. */
		private InputStream is;

		/** The sw. */
		private StringWriter sw;

		/**
		 * Instantiates a new stream reader.
		 * @param is the is
		 */
		StreamReader(InputStream is) {
			this.is = is;
			sw = new StringWriter();
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try {
				int c;
				while ((c = is.read()) != -1) {
					sw.write(c);
				}
			} catch (IOException e) {
				;
			}
		}

		/**
		 * Gets the result.
		 * @return the result
		 */
		String getResult() {
			return sw.toString();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * The main method.
	 * @param s the arguments
	 */
	public static void main(String s[]) {
		// System.out.println("Personal directory : "
		// + getCurrentUserPersonalFolderPath());
		// System.out.println("CPU Name : " + getCPUName());
		// System.out.println("CPU Speed : " + getCPUSpeed() + " Mhz");
		//

		String string = getFromRegistry(FCMDConstants.GET_SYSTEM_ENV, FCMDConstants.REGSTREXP_TOKEN);
		System.out.println(string);

		System.err.println();
	}

}
