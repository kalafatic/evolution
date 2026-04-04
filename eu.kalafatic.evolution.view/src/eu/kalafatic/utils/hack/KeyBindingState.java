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
package eu.kalafatic.utils.hack;

/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.WorkbenchWindow;

/**
 * The Class class KeyBindingState.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
public class KeyBindingState {

	/** The associated window. */
	private IWorkbenchWindow associatedWindow;

	/** The current sequence. */
	private KeySequence currentSequence;

	/** The workbench. */
	private final IWorkbench workbench;

	/**
	 * Instantiates a new key binding state.
	 * 
	 * @param workbenchToNotify
	 *            the workbench to notify
	 */
	public KeyBindingState(IWorkbench workbenchToNotify) {
		currentSequence = KeySequence.getInstance();
		workbench = workbenchToNotify;
		associatedWindow = workbench.getActiveWorkbenchWindow();
	}

	/**
	 * Gets the associated window.
	 * 
	 * @return the associated window
	 */
	IWorkbenchWindow getAssociatedWindow() {
		return associatedWindow;
	}

	/**
	 * Gets the current sequence.
	 * 
	 * @return the current sequence
	 */
	KeySequence getCurrentSequence() {
		return currentSequence;
	}

	/**
	 * Gets the status line.
	 * 
	 * @return the status line
	 */
	StatusLineContributionItem getStatusLine() {
		if (associatedWindow instanceof WorkbenchWindow) {
			WorkbenchWindow window = (WorkbenchWindow) associatedWindow;
			IStatusLineManager statusLine = window.getStatusLineManager();
			// TODO implicit dependency on IDE's action builder
			// @issue implicit dependency on IDE's action builder
			if (statusLine != null) { // this can be null if we're exiting
				IContributionItem item = statusLine
						.find("ModeContributionItem"); //$NON-NLS-1$
				if (item instanceof StatusLineContributionItem) {
					return ((StatusLineContributionItem) item);
				}
			}
		}

		return null;
	}

	/**
	 * Reset.
	 */
	void reset() {
		currentSequence = KeySequence.getInstance();
		updateStatusLines();
	}

	/**
	 * Sets the associated window.
	 * 
	 * @param window
	 *            the new associated window
	 */
	void setAssociatedWindow(IWorkbenchWindow window) {
		associatedWindow = window;
	}

	/**
	 * Sets the current sequence.
	 * 
	 * @param sequence
	 *            the new current sequence
	 */
	void setCurrentSequence(KeySequence sequence) {
		currentSequence = sequence;
		updateStatusLines();
	}

	/**
	 * Update status lines.
	 */
	private void updateStatusLines() {
		StatusLineContributionItem statusLine = getStatusLine();
		if (statusLine != null) {
			statusLine.setText(getCurrentSequence().format());
		}
	}
}
