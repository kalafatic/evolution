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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.StartupThreading.StartupRunnable;
import org.eclipse.ui.splash.AbstractSplashHandler;

/**
 * The Class class BasicSplashHandler.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
class BasicSplashHandler extends AbstractSplashHandler {

	/** The foreground. */
	private Color foreground = null;

	/** The monitor. */
	private AbsolutePositionProgressMonitorPart monitor;

	/** The message rect. */
	private Rectangle messageRect;

	/** The progress rect. */
	private Rectangle progressRect;

	/** The end splash. */
	private boolean endSplash = false;

	/**
	 * The Class class AbsolutePositionProgressMonitorPart.
	 * 
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	public class AbsolutePositionProgressMonitorPart extends
			ProgressMonitorPart {

		/**
		 * Instantiates a new absolute position progress monitor part.
		 * 
		 * @param parent
		 *            the parent
		 */
		private AbsolutePositionProgressMonitorPart(Composite parent) {
			super(parent, null);
			setLayout(null);
		}

		/**
		 * Gets the progress indicator.
		 * 
		 * @return the progress indicator
		 */
		public ProgressIndicator getProgressIndicator() {
			return fProgressIndicator;
		}

		/**
		 * Gets the progress text.
		 * 
		 * @return the progress text
		 */
		public Label getProgressText() {
			return fLabel;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.wizard.ProgressMonitorPart#beginTask(java.lang.
		 * String, int)
		 */
		@Override
		public void beginTask(final String name, final int totalWork) {

			updateUI(new Runnable() {

				@Override
				public void run() {
					if (isDisposed()) {
						return;
					}
					AbsolutePositionProgressMonitorPart.super.beginTask(name,
							totalWork);
				}
			});

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.wizard.ProgressMonitorPart#done()
		 */
		@Override
		public void done() {

			updateUI(new Runnable() {

				@Override
				public void run() {
					if (isDisposed()) {
						return;
					}
					AbsolutePositionProgressMonitorPart.super.done();
				}
			});

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.wizard.ProgressMonitorPart#internalWorked(double)
		 */
		@Override
		public void internalWorked(final double work) {

			updateUI(new Runnable() {

				@Override
				public void run() {
					if (isDisposed()) {
						return;
					}
					AbsolutePositionProgressMonitorPart.super
							.internalWorked(work);
				}
			});

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.wizard.ProgressMonitorPart#setFont(org.eclipse.
		 * swt.graphics.Font)
		 */
		@Override
		public void setFont(final Font font) {

			updateUI(new Runnable() {

				@Override
				public void run() {
					if (isDisposed()) {
						return;
					}
					AbsolutePositionProgressMonitorPart.super.setFont(font);
				}
			});

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.wizard.ProgressMonitorPart#updateLabel()
		 */
		@Override
		public void updateLabel() {

			updateUI(new Runnable() {

				@Override
				public void run() {
					if (isDisposed()) {
						return;
					}
					AbsolutePositionProgressMonitorPart.super.updateLabel();
				}
			});

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.splash.AbstractSplashHandler#getBundleProgressMonitor()
	 */
	@Override
	public IProgressMonitor getBundleProgressMonitor() {
		if (monitor == null || monitor.isDisposed()) {
			Composite parent = new Composite(getSplash(),
					Window.getDefaultOrientation());
			Point size = getSplash().getSize();
			parent.setBounds(new Rectangle(0, 0, size.x, size.y));
			monitor = new AbsolutePositionProgressMonitorPart(parent);
			monitor.setSize(size);

			if (progressRect != null) {
				monitor.getProgressIndicator().setBounds(progressRect);
			} else {
				monitor.getProgressIndicator().setVisible(false);
			}
			if (messageRect != null) {
				monitor.getProgressText().setBounds(messageRect);
			} else {
				monitor.getProgressText().setVisible(false);
			}
			if (foreground != null && !foreground.isDisposed()) {
				monitor.getProgressText().setForeground(foreground);
			}

			monitor.setBackgroundMode(SWT.INHERIT_FORCE);
			monitor.setBackgroundImage(getSplash().getShell()
					.getBackgroundImage());
		}
		return monitor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.splash.AbstractSplashHandler#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (foreground != null) {
			foreground.dispose();
		}
	}

	/**
	 * Sets the foreground.
	 * 
	 * @param foregroundRGB
	 *            the new foreground
	 */
	protected void setForeground(RGB foregroundRGB) {
		if (monitor != null) {
			return;
		}
		if (this.foreground != null) {
			this.foreground.dispose();
		}
		this.foreground = new Color(getSplash().getShell().getDisplay(),
				foregroundRGB);
	}

	/**
	 * Gets the foreground.
	 * 
	 * @return the foreground
	 */
	protected Color getForeground() {
		return foreground;
	}

	/**
	 * Sets the message rect.
	 * 
	 * @param messageRect
	 *            the new message rect
	 */
	protected void setMessageRect(Rectangle messageRect) {
		this.messageRect = messageRect;
	}

	/**
	 * Sets the progress rect.
	 * 
	 * @param progressRect
	 *            the new progress rect
	 */
	protected void setProgressRect(Rectangle progressRect) {
		this.progressRect = progressRect;
	}

	/**
	 * Gets the content.
	 * 
	 * @return the content
	 */
	protected Composite getContent() {
		return (Composite) getBundleProgressMonitor();
	}

	/**
	 * Update ui.
	 * 
	 * @param r
	 *            the r
	 */
	protected void updateUI(final Runnable r) {
		Shell splashShell = getSplash();
		if (splashShell == null || splashShell.isDisposed()) {
			return;
		}

		Display display = splashShell.getDisplay();

		if (Thread.currentThread() == display.getThread()) {
			r.run(); // run immediatley if we're on the UI thread
		} else {
			// wrapper with a StartupRunnable to ensure that it will run before
			// the UI is fully initialized
			StartupRunnable startupRunnable = new StartupRunnable() {

				@Override
				public void runWithException() throws Throwable {
					r.run();
				}
			};
			display.asyncExec(startupRunnable);
		}
	}

	/**
	 * Checks if is end splash.
	 * 
	 * @return true, if is end splash
	 */
	public boolean isEndSplash() {
		return endSplash;
	}

	/**
	 * Sets the end splash.
	 * 
	 * @param endSplash
	 *            the new end splash
	 */
	public void setEndSplash(boolean endSplash) {
		this.endSplash = endSplash;
	}
}