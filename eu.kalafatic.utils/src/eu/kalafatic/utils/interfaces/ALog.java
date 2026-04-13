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
package eu.kalafatic.utils.interfaces;

import static eu.kalafatic.utils.constants.FConstants.PREFERENCES;

import java.util.logging.Logger;

import org.eclipse.swt.SWTException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.kalafatic.utils.lib.EView;
import eu.kalafatic.utils.model.LogElement;
import eu.kalafatic.utils.preferences.ECorePreferences;
import eu.kalafatic.utils.views.ConsoleView;

/**
 * The Class class ALog.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public abstract class ALog implements ILog {

	/** The logger. */
	protected static Logger logger;

	/** The console. */
	private static ConsoleView console;

	/** The LO g_ enabled. */
	public static boolean LOG_ENABLED = PREFERENCES.getBoolean(ECorePreferences.LOG_ENABLED.getName(), (Boolean) ECorePreferences.LOG_ENABLED.getValue());

	// ---------------------------------------------------------------.
	// ---------------------------------------------------------------

	/**
	 * Prints the console.
	 * @param logElement the log element
	 */
	// public static LogElement getLogElement(String type, String name, String
	// msg) {
	//
	// return createLog(type, name, msg);
	// }
	//
	// /**
	// * @param type
	// * @param name
	// * @param msg
	// * @return
	// */
	// private static LogElement createLog(String type, String name, String msg)
	// {
	// String date = new SimpleDateFormat("HH.mm.ss")
	// .format(System.nanoTime());
	// String fullClassName = Thread.currentThread().getStackTrace()[4]
	// .getClassName();
	// String className = fullClassName.substring(fullClassName
	// .lastIndexOf(".") + 1);
	// String methodName = Thread.currentThread().getStackTrace()[4]
	// .getMethodName();
	// int lineNumber = Thread.currentThread().getStackTrace()[4]
	// .getLineNumber();
	//
	// return new LogElement(type, date, name, msg, className + "."
	// + methodName + "():" + lineNumber);
	// }

	// ---------------------------------------------------------------

	/**
	 * Prints the console.
	 * @param logElement the log element
	 */
	protected static void printConsole(LogElement logElement) {
		IWorkbenchWindow workbenchWindow;

		try {
			workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		} catch (SWTException e) {
			// e.printStackTrace();
			return;
		}

		// LOGS.add(0, logElement);

		if (workbenchWindow != null && workbenchWindow.getActivePage() != null && console == null) {
			try {
				console = (ConsoleView) workbenchWindow.getActivePage().showView(EView.CONSOLE.ID);

			} catch (PartInitException e) {
				e.printStackTrace();
			} catch (NullPointerException e1) {
				e1.printStackTrace();
			}
		}
		if (console != null) {
			console.refresh();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Clear logs.
	 */
	// protected static void clearLogs() {
	// while (LOGS.size() >= MAX_LOGS) {
	// synchronized (LOGS) {
	// LOGS.remove(LOGS.size() - 1);
	// // LogElement logElement = LOGS.remove(LOGS.size()-1);
	// // logElement=null;
	// }
	// }
	// }
}
