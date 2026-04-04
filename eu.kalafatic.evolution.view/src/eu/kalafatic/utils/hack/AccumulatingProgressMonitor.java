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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;

/**
 * The Class class AccumulatingProgressMonitor.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class AccumulatingProgressMonitor extends ProgressMonitorWrapper {

	/** The display. */
	private Display display;

	/** The collector. */
	private Collector collector;

	/** The current task. */
	private String currentTask = ""; //$NON-NLS-1$

	/**
	 * The Class class Collector.
	 * 
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	private class Collector implements Runnable {
		
		/** The sub task. */
		private String subTask;

		/** The worked. */
		private double worked;

		/** The monitor. */
		private IProgressMonitor monitor;

		/**
		 * Instantiates a new collector.
		 * 
		 * @param subTask
		 *            the sub task
		 * @param work
		 *            the work
		 * @param monitor
		 *            the monitor
		 */
		public Collector(String subTask, double work, IProgressMonitor monitor) {
			this.subTask = subTask;
			this.worked = work;
			this.monitor = monitor;
		}

		/**
		 * Worked.
		 * 
		 * @param workedIncrement
		 *            the worked increment
		 */
		public void worked(double workedIncrement) {
			this.worked = this.worked + workedIncrement;
		}

		/**
		 * Sub task.
		 * 
		 * @param subTaskName
		 *            the sub task name
		 */
		public void subTask(String subTaskName) {
			this.subTask = subTaskName;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			clearCollector(this);
			if (subTask != null) {
				monitor.subTask(subTask);
			}
			if (worked > 0) {
				monitor.internalWorked(worked);
			}
		}
	}

	/**
	 * Instantiates a new accumulating progress monitor.
	 * 
	 * @param monitor
	 *            the monitor
	 * @param display
	 *            the display
	 */
	public AccumulatingProgressMonitor(IProgressMonitor monitor, Display display) {
		super(monitor);
		Assert.isNotNull(display);
		this.display = display;
	}

	/*
	 * (non-Javadoc) Method declared on IProgressMonitor.
	 */
	@Override
	public void beginTask(final String name, final int totalWork) {
		synchronized (this) {
			collector = null;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				currentTask = name;
				getWrappedProgressMonitor().beginTask(name, totalWork);
			}
		});
	}

	/**
	 * Clear collector.
	 * 
	 * @param collectorToClear
	 *            the collector to clear
	 */
	private synchronized void clearCollector(Collector collectorToClear) {
		// Check if the accumulator is still using the given collector.
		// If not, don't clear it.
		if (this.collector == collectorToClear) {
			this.collector = null;
		}
	}

	/**
	 * Creates the collector.
	 * 
	 * @param subTask
	 *            the sub task
	 * @param work
	 *            the work
	 */
	private void createCollector(String subTask, double work) {
		collector = new Collector(subTask, work, getWrappedProgressMonitor());
		display.asyncExec(collector);
	}

	/*
	 * (non-Javadoc) Method declared on IProgressMonitor.
	 */
	@Override
	public void done() {
		synchronized (this) {
			collector = null;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				getWrappedProgressMonitor().done();
			}
		});
	}

	/*
	 * (non-Javadoc) Method declared on IProgressMonitor.
	 */
	@Override
	public synchronized void internalWorked(final double work) {
		if (collector == null) {
			createCollector(null, work);
		} else {
			collector.worked(work);
		}
	}

	/*
	 * (non-Javadoc) Method declared on IProgressMonitor.
	 */
	@Override
	public void setTaskName(final String name) {
		synchronized (this) {
			collector = null;
		}
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				currentTask = name;
				getWrappedProgressMonitor().setTaskName(name);
			}
		});
	}

	/*
	 * (non-Javadoc) Method declared on IProgressMonitor.
	 */
	@Override
	public synchronized void subTask(final String name) {
		if (collector == null) {
			createCollector(name, 0);
		} else {
			collector.subTask(name);
		}
	}

	/*
	 * (non-Javadoc) Method declared on IProgressMonitor.
	 */
	@Override
	public synchronized void worked(int work) {
		internalWorked(work);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#clearBlocked()
	 */
	@Override
	public void clearBlocked() {

		// If this is a monitor that can report blocking do so.
		// Don't bother with a collector as this should only ever
		// happen once and prevent any more progress.
		final IProgressMonitor pm = getWrappedProgressMonitor();
		if (!(pm instanceof IProgressMonitorWithBlocking)) {
			return;
		}

		display.asyncExec(new Runnable() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				((IProgressMonitorWithBlocking) pm).clearBlocked();
				Dialog.getBlockedHandler().clearBlocked();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.ProgressMonitorWrapper#setBlocked(org.eclipse
	 * .core.runtime.IStatus)
	 */
	@Override
	public void setBlocked(final IStatus reason) {
		// If this is a monitor that can report blocking do so.
		// Don't bother with a collector as this should only ever
		// happen once and prevent any more progress.
		final IProgressMonitor pm = getWrappedProgressMonitor();
		if (!(pm instanceof IProgressMonitorWithBlocking)) {
			return;
		}

		display.asyncExec(new Runnable() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				((IProgressMonitorWithBlocking) pm).setBlocked(reason);
				// Do not give a shell as we want it to block until it opens.
				Dialog.getBlockedHandler().showBlocked(pm, reason, currentTask);
			}
		});
	}
}
