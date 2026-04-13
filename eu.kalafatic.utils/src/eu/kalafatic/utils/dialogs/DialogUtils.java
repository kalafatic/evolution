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
package eu.kalafatic.utils.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * The Class class DialogUtils.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class DialogUtils {

	/** The Constant INSTANCE. */
	public static final DialogUtils INSTANCE = new DialogUtils();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Info.
	 * 
	 * @param message
	 *            the message
	 */
	public void info(String message) {
		showMessage(MessageDialog.INFORMATION, "INFORMATION", message);
	}

	// ---------------------------------------------------------------

	/**
	 * Warn.
	 * 
	 * @param message
	 *            the message
	 */
	public void warn(String message) {
		showMessage(MessageDialog.WARNING, "WARNING", message);
	}

	// ---------------------------------------------------------------

	/**
	 * Error.
	 * 
	 * @param message
	 *            the message
	 */
	public void error(String message) {
		showMessage(MessageDialog.ERROR, "ERROR", message);
	}

	// ---------------------------------------------------------------

	/**
	 * Question.
	 * 
	 * @param message
	 *            the message
	 * @return the int
	 */
	public int question(String message) {
		try {
			int style = SWT.ICON_QUESTION | SWT.YES | SWT.NO;

			Shell shell = new Shell(Display.getDefault(), SWT.PRIMARY_MODAL | SWT.SHEET);
			MessageBox messageBox = new MessageBox(shell, style);

			messageBox.setMessage(message);
			return messageBox.open();

		} catch (Exception e) {
			// Log.log(ECorePreferences.MODULE, e);
		}
		return SWT.CANCEL;
	}

	// ---------------------------------------------------------------

	/**
	 * Show message.
	 * 
	 * @param style
	 *            the style
	 * @param title
	 *            the title
	 * @param message
	 *            the message
	 */
	public void showMessage(final int style, final String title, final String message) {

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					MessageDialog.open(style, Display.getDefault().getActiveShell(), title,
							message, SWT.SHEET);
				} catch (Exception e) {
					// Log.log(ECorePreferences.MODULE, e);
				}
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Show exception.
	 * 
	 * @param e
	 *            the e
	 */
	public void showException(Exception e) {
		showException(e, 10);
	}

	// ---------------------------------------------------------------

	/**
	 * Show exception.
	 * 
	 * @param e
	 *            the e
	 * @param level
	 *            the level
	 */
	public void showException(Exception e, int level) {
		showException(e, "EXCEPTION", level);
	}

	// ---------------------------------------------------------------

	/**
	 * Show exception.
	 * 
	 * @param e
	 *            the e
	 * @param title
	 *            the title
	 * @param level
	 *            the level
	 */
	public void showException(final Exception e, final String title, final int level) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					MessageDialog.openWarning(Display.getDefault().getActiveShell(), title,
							getTrace(e, level));
				} catch (Exception e) {
					// Log.log(ECorePreferences.MODULE, e);
				}
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the trace.
	 * 
	 * @param e
	 *            the e
	 * @param level
	 *            the level
	 * @return the trace
	 */
	public String getTrace(Exception e, int level) {
		StringBuffer sb = new StringBuffer();
		StackTraceElement[] stackTrace = e.getStackTrace();

		for (int i = 0; i < level && i < stackTrace.length; i++) {
			sb.append(stackTrace[i] + "\n");
		}
		return sb.toString();
	}
}
