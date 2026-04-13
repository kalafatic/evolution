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

import java.io.IOException;

import eu.kalafatic.utils.lib.ECmd;

/**
 * The Class class OpenFileUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class OpenFileUtils {

	/** The INSTANCE. */
	private volatile static OpenFileUtils INSTANCE;

	/**
	 * Gets the single instance of OpenFileUtils.
	 * @return single instance of OpenFileUtils
	 */
	public static OpenFileUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (OpenFileUtils.class) {
				INSTANCE = new OpenFileUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Open file.
	 * @param path the path
	 */
	@SuppressWarnings("unused")
	public void openFile(String path) {
		Process process = null;
		try {
			boolean tried = false;
			String extension = getExtension(path);

			for (ECmd ecmd : ECmd.values()) {
				if ((ecmd.assoc != null) && ecmd.assoc.contains(extension)) {
					tried = true;
					process = Runtime.getRuntime().exec(ecmd.command + path);
					// process.waitFor();
					break;
				}
			}
			if (!tried) {
				process = Runtime.getRuntime().exec("cmd /c /b start & \"" + path + "\"");
				// process = Runtime.getRuntime().exec("cmd /c start & pause");
				// process = Runtime.getRuntime().exec("cmd.exe /k");
				// process = Runtime.getRuntime().exec("cmd.exe /k & pause");
				// Scanner sc = new Scanner(process.getInputStream());
				// while (sc.hasNext()) System.out.println(sc.nextLine());
				// process.waitFor();
			}
		} catch (IOException e) {
			e.printStackTrace();
			// } catch (InterruptedException e) {
			// e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the extension.
	 * @param path the path
	 * @return the extension
	 */
	private String getExtension(String path) {
		// Find the position of the last dot.
		int dotPos = path.lastIndexOf(".");
		// Get extension.
		return path.substring(dotPos);
	}

}
