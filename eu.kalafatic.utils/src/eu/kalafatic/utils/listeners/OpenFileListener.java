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
package eu.kalafatic.utils.listeners;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.IHandlerService;

import eu.kalafatic.utils.lib.EHandler;

/**
 * The listener interface for receiving openFile events. The class that is interested in processing a openFile event implements this interface, and
 * the object created with that class is registered with a component using the component's <code>addOpenFileListener<code> method. When
 * the openFile event occurs, that object's appropriate
 * method is invoked.
 * @see OpenFileEvent
 */
public class OpenFileListener implements SelectionListener, MouseListener {

	/** The site. */
	private IWorkbenchPartSite site;

	/**
	 * Instantiates a new open file listener.
	 * @param site the site
	 */
	public OpenFileListener(IWorkbenchPartSite site) {
		this.site = site;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Execute command.
	 */
	private void executeCommand() {
		try {
			IHandlerService handlerService = (IHandlerService) site.getService(IHandlerService.class);

			handlerService.executeCommand(EHandler.OPEN.ID, null);

		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (NotDefinedException e) {
			e.printStackTrace();
		} catch (NotEnabledException e) {
			e.printStackTrace();
		} catch (NotHandledException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt .events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		executeCommand();
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse .swt.events.SelectionEvent)
	 */
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt .events.MouseEvent)
	 */
	@Override
	public void mouseDoubleClick(MouseEvent e) {
		executeCommand();
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events .MouseEvent)
	 */
	@Override
	public void mouseDown(MouseEvent e) {}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events. MouseEvent)
	 */
	@Override
	public void mouseUp(MouseEvent e) {}
}
