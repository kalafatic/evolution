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
package eu.kalafatic.utils.actions;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;

import eu.kalafatic.utils.model.LogElement;

/**
 * The Class class ClearAction.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ClearAction extends Action {

	/** The viewer. */
	private final TableViewer viewer;

	/** The lock. */
	private final Lock lock = new ReentrantLock(true);

	/**
	 * Instantiates a new clear action.
	 * @param viewer the viewer
	 */
	public ClearAction(TableViewer viewer) {
		this.viewer = viewer;
		this.setText("Clear");
		this.setToolTipText("Clear");
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public synchronized void run() {

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				Object input = viewer.getInput();
				if (input instanceof List<?>) {
					List<?> list = (List<?>) input;

					for (Object object : list) {
						if (object instanceof LogElement) {
							LogElement logElement = (LogElement) object;
							logElement.clear();
						}
					}
					refresh();
				}
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Refresh.
	 */
	private void refresh() {
		if (lock.tryLock()) {
			try {
				Display.getDefault().asyncExec(refresh);
			} finally {
				lock.unlock();
			}
		}
	}

	// ---------------------------------------------------------------

	/** The refresh. */
	private final Runnable refresh = new Runnable() {
		@Override
		public void run() {
			lock.lock();
			try {
				if (viewer != null && viewer.getControl() != null && !viewer.getControl().isDisposed() && viewer.getControl().isVisible()) {

					viewer.refresh();
				}
			} catch (Exception e) {
				// e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}
	};

}
