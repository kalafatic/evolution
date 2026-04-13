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

import java.util.Collection;

import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.ISelectionListener;

/**
 * The Interface interface IViewer.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public interface IViewer extends ISelectionListener, IPerspectiveListener {

	/**
	 * Inits the columns.
	 */
	void initColumns();

	/**
	 * Inits the listeners.
	 */
	void initListeners();

	/**
	 * Inits the drag and drop.
	 */
	void initDragAndDrop();

	/**
	 * Inits the adapters.
	 * 
	 * @param collection
	 *            the collection
	 */
	void initAdapters(Collection<?> collection);

	/**
	 * Hook context menu.
	 */
	void hookContextMenu();

	/**
	 * Gets the viewer.
	 * 
	 * @return the viewer
	 */
	public Object getViewer();
}
