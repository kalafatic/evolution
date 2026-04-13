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

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;

/**
 * The Class class LanguageUtils.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class LanguageUtils {

	/** The Constant INSTANCE. */
	public static final LanguageUtils INSTANCE = new LanguageUtils();

	/** The Constant INI_FILE. */
	private static final String INI_FILE = "Gemini.ini";

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Switch language.
	 * 
	 * @param locale
	 *            the locale
	 * @return the int
	 */
	public int switchLanguage(String locale) {
		System.setProperty("user.language", locale);
		System.setProperty("osgi.nl", locale);
		System.setProperty("eclipse.exitdata", "\n-nl \n" + locale + "\n-osgi.nl \n" + locale);

		Location installArea = Platform.getInstallLocation();
		try {
			if (installArea != null) {
				String path = installArea.getURL().getFile().concat(INI_FILE);
				String text = FileUtils.getInstance().getTextFromFile(path, true);

				if (!text.isEmpty()) {
					FileUtils.getInstance().writeFile(path + ".bak", text);
					String replacedLocale = replaceLocale(text, locale);
					FileUtils.getInstance().writeFile(path, replacedLocale);

					return SWT.YES;
				}
			}
		} catch (Exception e) {
			return SWT.CANCEL;
		}
		return SWT.NO;
	}

	// ---------------------------------------------------------------

	/**
	 * Replace locale.
	 * 
	 * @param text
	 *            the text
	 * @param locale
	 *            the locale
	 * @return the string
	 */
	private static String replaceLocale(String text, String locale) {
		boolean nl = false, osgiNl = false;
		StringBuffer sb = new StringBuffer();
		String[] lines = text.split(FileUtils.NEW_LINE);
		for (int i = 0; i < lines.length; i++) {

			if (lines[i].startsWith("-nl=")) {
				sb.append("-nl" + FileUtils.NEW_LINE);
				sb.append(locale + FileUtils.NEW_LINE);
				nl = true;
			} else if (lines[i].startsWith("-osgi.nl=")) {
				sb.append("-osgi.nl" + FileUtils.NEW_LINE);
				sb.append(locale + FileUtils.NEW_LINE);
				osgiNl = true;
			} else {
				sb.append(lines[i] + FileUtils.NEW_LINE);
			}
		}
		if (!nl) {
			sb.append("-nl" + FileUtils.NEW_LINE);
			sb.append(locale + FileUtils.NEW_LINE);
		}
		if (!osgiNl) {
			sb.append("-osgi.nl" + FileUtils.NEW_LINE);
			sb.append(locale + FileUtils.NEW_LINE);
		}
		return sb.toString();
	}
}
