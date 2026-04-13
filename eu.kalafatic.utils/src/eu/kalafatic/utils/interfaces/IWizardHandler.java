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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.wizards.IWizardRegistry;

/**
 * The Interface interface IWizardHandler.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public interface IWizardHandler {

	/**
	 * Gets the wizard id parameter id.
	 * 
	 * @return the wizard id parameter id
	 */
	String getWizardIdParameterId();

	/**
	 * Gets the wizard registry.
	 * 
	 * @return the wizard registry
	 */
	IWizardRegistry getWizardRegistry();

	/**
	 * Execute handler.
	 * 
	 * @param event
	 *            the event
	 */
	void executeHandler(ExecutionEvent event);
}
