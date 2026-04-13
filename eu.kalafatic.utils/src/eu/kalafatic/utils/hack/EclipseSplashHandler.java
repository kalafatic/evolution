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

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.internal.util.PrefUtil;

/**
 * The Class class EclipseSplashHandler.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
public class EclipseSplashHandler extends BasicSplashHandler {

	/** The message rect. */
	private Rectangle bounds, progressRect, messageRect;

	/** The location. */
	private Point location;

	/** The foreground. */
	public Color foreground;

	/** The version. */
	final String version;

	/**
	 * Instantiates a new eclipse splash handler.
	 */
	public EclipseSplashHandler() {
		version = Platform.getProduct().getProperty("version");
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.splash.AbstractSplashHandler#init(org.eclipse.swt.widgets
	 * .Shell)
	 */
	@Override
	public void init(Shell splash) {
		super.init(splash);

		setForeground(new Color(splash.getDisplay(), 128, 128, 128).getRGB());

		String progressRectString = null;
		String messageRectString = null;
		String foregroundColorString = null;
		IProduct product = Platform.getProduct();

		if (product != null) {
			progressRectString = product
					.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
			messageRectString = product
					.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
			foregroundColorString = product
					.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
		}
		progressRect = StringConverter.asRectangle(progressRectString,
				new Rectangle(10, 10, 300, 15));
		setProgressRect(progressRect);

		messageRect = StringConverter.asRectangle(messageRectString,
				new Rectangle(10, 35, 300, 15));
		setMessageRect(messageRect);

		bounds = splash.getBounds();
		location = splash.getLocation();

		int foregroundColorInteger;
		try {
			foregroundColorInteger = Integer
					.parseInt(foregroundColorString, 16);
		} catch (Exception ex) {
			foregroundColorInteger = 0xD2D7FF; // off white
		}
		RGB rgb = new RGB((foregroundColorInteger & 0xFF0000) >> 16,
				(foregroundColorInteger & 0xFF00) >> 8,
				foregroundColorInteger & 0xFF);

		setForeground(rgb);

		foreground = new Color(splash.getDisplay(), rgb);

		Composite content = getContent(); // ensure creation of the progress

		// the following code will be removed for release time
		if (PrefUtil.getInternalPreferenceStore().getBoolean(
				"SHOW_BUILDID_ON_STARTUP")) { //$NON-NLS-1$
			final String buildId = System.getProperty("eclipse.buildId",
					"Unknown Build");
			// find the specified location. Not currently API
			// hardcoded to be sensible with our current splash Graphic
			String buildIdLocString = product.getProperty("buildIdLocation");
			final Point buildIdPoint = StringConverter.asPoint(
					buildIdLocString, new Point(322, 190));

			content.addPaintListener(new PaintListener() {

				@Override
				public void paintControl(PaintEvent e) {
					e.gc.setForeground(getForeground());
					e.gc.drawText(buildId, buildIdPoint.x, buildIdPoint.y, true);
				}
			});
		} else {
			drawVersion(splash, content);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Draw version.
	 * 
	 * @param splash
	 *            the splash
	 * @param content
	 *            the content
	 */
	public void drawVersion(Shell splash, Composite content) {
		try {
			content.addPaintListener(new PaintListener() {

				int x = getBounds().width - 45;
				int y = 10;

				@Override
				public void paintControl(PaintEvent e) {
					e.gc.setForeground(foreground);
					e.gc.drawText("v " + version, x, y, true);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the progress rect.
	 * 
	 * @return the progress rect
	 */
	public Rectangle getProgressRect() {
		return progressRect;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the message rect.
	 * 
	 * @return the message rect
	 */
	public Rectangle getMessageRect() {
		return messageRect;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the bounds.
	 * 
	 * @return the bounds
	 */
	public Rectangle getBounds() {
		return bounds;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the location.
	 * 
	 * @return the location
	 */
	public Point getLocation() {
		return location;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the version.
	 * 
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
}
