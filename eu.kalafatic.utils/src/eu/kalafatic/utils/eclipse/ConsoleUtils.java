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
package eu.kalafatic.utils.eclipse;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class ConsoleUtils {

	public static final ConsoleUtils INSTANCE = new ConsoleUtils();

	// public static MessageConsole findConsole() {
	//
	// }

	public MessageConsole findConsole(String consoleName) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (consoleName.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(consoleName, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	// public void displayConsole(String consoleName) {
	// try {
	// IConsole myConsole = findConsole(consoleName);// your console instance
	// IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();// obtain the active page
	// String id = IConsoleConstants.ID_CONSOLE_VIEW;
	// IConsoleView view = (IConsoleView) page.showView(id);
	// view.display(myConsole);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public void printToConsole(final String consoleName, final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageConsole console = findConsole(consoleName);
				// displayConsole(consoleName);
				MessageConsoleStream out = console.newMessageStream();
				out.println(message);
			}
		});

	}

}
