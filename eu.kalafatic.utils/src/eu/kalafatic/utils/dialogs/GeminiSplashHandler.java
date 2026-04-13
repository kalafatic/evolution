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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.utils.constants.FCoreImageConstants;
import eu.kalafatic.utils.hack.EclipseSplashHandler;
import eu.kalafatic.utils.lib.AppData;
import eu.kalafatic.utils.log.Log;
import eu.kalafatic.utils.preferences.ECorePreferences;
import eu.kalafatic.utils.ui.ImageUtils;

/**
 * The Class class GeminiSplashHandler.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class GeminiSplashHandler extends EclipseSplashHandler {

	/**
	 * The Class class GSHf.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	public static class GSHf {

		/** The Constant NONE. */
		public static final int NONE = 0;

		/** The Constant TASK_NEW. */
		private static final int TASK_NEW = 1 << 1;

		/** The Constant TASK_SUB_NEW. */
		private static final int TASK_SUB_NEW = 1 << 2;

		/** The Constant TASK_END. */
		public static final int TASK_END = 1 << 3;

		/** The Constant VISIBLE. */
		public static final int VISIBLE = 1 << 4;

		/** The Constant DONE. */
		public static final int DONE = 1 << 5;

		/** The FLAG. */
		public static int FLAG = TASK_NEW;
	}

	/** The alpha. */
	private int alpha = 255;

	/** The animated. */
	private Image[] animated;

	/** The app monitor. */
	private AbsolutePositionProgressMonitorPart appMonitor;

	/** The display. */
	private Display display;

	/** The total work. */
	private int totalWork;

	/** The pending image. */
	private Image pendingImage;

	/** The pending index. */
	private int pendingIndex = 0;

	/** The pending label. */
	private Label pendingLabel;

	/** The progress indicator. */
	private ProgressIndicator progressIndicator;

	/** The progress monitor. */
	public IProgressMonitor progressMonitor;

	/** The splash. */
	private Shell splash;

	/** The text. */
	private String text = "Progress ... ";

	/** The text label. */
	private Label textLabel;

	/** The ending. */
	private AtomicBoolean ending = new AtomicBoolean();

	/** The sub progress monitor. */
	private SubProgressMonitor subProgressMonitor;

	/**
	 * Instantiates a new gemini splash handler.
	 */
	public GeminiSplashHandler() {
		super();
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Creates the pending.
	 * @param rectangle the rectangle
	 */
	private void createPending(final Rectangle rectangle) {
		try {
			animated = ImageUtils.loadGif("icons/actions/pending.gif");

			pendingLabel = new Label(splash, SWT.NONE);
			pendingLabel.setBounds(rectangle);

			pendingImage = animated[pendingIndex++];
			pendingLabel.setBackgroundImage(pendingImage);

		} catch (Exception e) {
			Log.log(ECorePreferences.MODULE, e);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the ui.
	 * @param display the display
	 * @return the shell
	 */
	public Shell createUI(Display display) {
		splash = new Shell(this.display = display, SWT.ON_TOP | SWT.APPLICATION_MODAL | SWT.INHERIT_DEFAULT);
		splash.setBackgroundImage(FCoreImageConstants.SPLASH_IMG);
		splash.setBounds(getBounds());
		splash.setLocation(getLocation());

		String splashLoc = System.getProperty("org.eclipse.equinox.launcher.splash.location");
		final Image background = ImageUtils.loadImage(splashLoc);

		splash.setBackgroundImage(background);

		Rectangle rect = getProgressRect();
		createPending(new Rectangle(rect.x, rect.y - 5, rect.width, 3));

		return splash;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.splash.BasicSplashHandler#dispose()
	 */
	@Override
	public void dispose() {
		AppData.getInstance().setSplashHandler(this);
		super.dispose();
	}

	// ---------------------------------------------------------------

	/**
	 * Done.
	 */
	public void done() {
		if (splash != null) {
			splash.dispose();
			splash = null;
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the alpha.
	 * @return the alpha
	 */
	public int getAlpha() {
		return alpha;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the display.
	 * @return the display
	 */
	public Display getDisplay() {
		return display;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.splash.AbstractSplashHandler#getSplash()
	 */
	@Override
	public Shell getSplash() {
		if (super.getSplash() != null) {
			return super.getSplash();
		} else {

		}
		return splash == null ? super.getSplash() : splash;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the text.
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.views.EclipseSplashHandler#init(org.eclipse. swt.widgets.Shell)
	 */
	@Override
	public void init(Shell splash) {
		super.init(splash);
	}

	// ---------------------------------------------------------------

	/**
	 * Run pending.
	 */
	public void runPending() {
		try {
			while (splash != null && !splash.isDisposed() && !pendingLabel.isDisposed() && !((GSHf.FLAG & GSHf.DONE) != 0)) {

				pendingIndex = (pendingIndex < animated.length) ? pendingIndex : 0;
				pendingImage = animated[pendingIndex++];
				Thread.sleep(100);
			}
		} catch (Exception e) {
			Log.log(ECorePreferences.MODULE, e);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the alpha.
	 * @param alpha the new alpha
	 */
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the monitor.
	 */
	public void setMonitor() {
		progressMonitor = getBundleProgressMonitor();

		appMonitor = (AbsolutePositionProgressMonitorPart) getBundleProgressMonitor();
		appMonitor.setVisible(true);

		progressIndicator = appMonitor.getProgressIndicator();

		textLabel = appMonitor.getProgressText();

		if (foreground != null && !foreground.isDisposed()) {
			textLabel.setForeground(foreground);
			splash.setForeground(foreground);
		}
		progressIndicator.setVisible(true);
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the progress.
	 * @param progress the new progress
	 */
	public void setProgress(int progress) {}

	// ---------------------------------------------------------------

	/**
	 * Sets the text.
	 * @param text the new text
	 */
	public void setText(String text) {
		this.text = text;
	}

	// ---------------------------------------------------------------

	/**
	 * Start task.
	 * @param name the name
	 * @param totalWork the total work
	 */
	public void startTask(String name, int totalWork) {
		GSHf.FLAG |= GSHf.TASK_NEW;
		this.text = name;
		this.totalWork = totalWork;
	}

	// ---------------------------------------------------------------

	/**
	 * Start sub task.
	 * @param name the name
	 * @param flags the flags
	 */
	public void startSubTask(final String name, int flags) {
		GSHf.FLAG |= (GSHf.TASK_SUB_NEW | flags);
		text = name;
	}

	// ---------------------------------------------------------------

	/**
	 * Update.
	 */
	public void update() {
		try {
			// System.out.println("------\n"
			// + Integer.toString(GSHf.FLAG & 0xff, 2) + " " + GSHf.FLAG);
			// System.out.println((GSHf.FLAG & GSHf.VISIBLE) != 0);
			// System.err.println("progress-" + progress);

			if ((GSHf.FLAG & GSHf.DONE) != 0) {
				textLabel.setText("Done");
				textLabel.update();
				progressMonitor.done();
				return;
			}
			pendingLabel.setVisible((GSHf.FLAG & GSHf.VISIBLE) != 0);

			if ((GSHf.FLAG & GSHf.TASK_NEW) != 0) {
				appMonitor.setVisible(true);
				if (ending.compareAndSet(false, true)) {
					progressMonitor.beginTask(text, totalWork);

					subProgressMonitor = new SubProgressMonitor(progressMonitor, 100);
					subProgressMonitor.beginTask(text, 10);
				}
				GSHf.FLAG &= ~GSHf.TASK_NEW;

			} else if ((GSHf.FLAG & GSHf.TASK_SUB_NEW) != 0) {
				subProgressMonitor = new SubProgressMonitor(progressMonitor, 100);
				subProgressMonitor.beginTask(text, 10);

				GSHf.FLAG &= ~GSHf.TASK_SUB_NEW;

			} else if ((GSHf.FLAG & GSHf.TASK_END) != 0) {
				subProgressMonitor.clearBlocked();
				subProgressMonitor.done();
				subProgressMonitor.setCanceled(true);

				GSHf.FLAG &= ~GSHf.TASK_END;

			} else {
				textLabel.setText(text);
				textLabel.update();
			}

			if ((GSHf.FLAG & GSHf.VISIBLE) != 0) {
				pendingLabel.setBackgroundImage(pendingImage);
				pendingLabel.update();
			}
			progressMonitor.worked(1);
			subProgressMonitor.worked(1);
			appMonitor.worked(1);
			appMonitor.update();
			splash.setAlpha(alpha);
			splash.redraw();

		} catch (Exception e) {
			Log.log(ECorePreferences.MODULE, e);
		}
	}
}
