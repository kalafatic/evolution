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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.FreeformFigure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Display;
//import org.eclipse.zest.core.viewers.internal.ZoomListener;

/**
 * The Class ZoomManager.
 */

@SuppressWarnings("restriction")
public class ZoomManager {

	/**
	 * The Class SharedMessages.
	 */
	class SharedMessages {

		/** The Constant FitAllAction_Label. */
		public static final String FitAllAction_Label = "Page"; // GEFMessages.FitAllAction_Label;

		/** The Constant FitWidthAction_Label. */
		public static final String FitWidthAction_Label = "Width"; // GEFMessages.FitWidthAction_Label;

		/** The Constant FitHeightAction_Label. */
		public static final String FitHeightAction_Label = "Height"; // GEFMessages.FitHeightAction_Label;

	}

	/** The Constant ANIMATE_NEVER. */
	public static final int ANIMATE_NEVER = 0;

	/** The Constant ANIMATE_ZOOM_IN_OUT. */
	public static final int ANIMATE_ZOOM_IN_OUT = 1;

	/** The listeners. */

//	private List<ZoomListener> listeners = new ArrayList<ZoomListener>();

	/** The multiplier. */
	private double multiplier = 1.0;

	/** The pane. */
	private ScalableFigure pane;

	/** The viewport. */
	private Viewport viewport;

	/** The zoom. */
	private double zoom = 1.0;
	// private int zoomAnimationStyle = ANIMATE_NEVER;
	/** The current zoom contant. */
	private String currentZoomContant = null;

	/** The zoom levels. */
	private double[] zoomLevels = { .5, .75, 1.0, 1.5, 2.0, 2.5, 3, 4 };

	/** The Constant FIT_HEIGHT. */

	public static final String FIT_HEIGHT = SharedMessages.FitHeightAction_Label;

	/** The Constant FIT_WIDTH. */
	public static final String FIT_WIDTH = SharedMessages.FitWidthAction_Label;

	/** The Constant FIT_ALL. */
	public static final String FIT_ALL = SharedMessages.FitAllAction_Label;

	/** The zoom level contributions. */
	@SuppressWarnings("unchecked")
	private List<String> zoomLevelContributions = Collections.EMPTY_LIST;

	//DecimalFormat format = new DecimalFormat("####%"); //$NON-NLS-1$

	/**
	 * Instantiates a new zoom manager.
	 * @param pane the pane
	 * @param viewport the viewport
	 */
	public ZoomManager(ScalableFigure pane, Viewport viewport) {
		this.pane = pane;
		this.viewport = viewport;
		zoomLevelContributions = new ArrayList<String>();
		zoomLevelContributions.add(FIT_ALL);
	}

	/**
	 * Instantiates a new zoom manager.
	 * @param pane the pane
	 * @param viewport the viewport
	 */
	@Deprecated
	public ZoomManager(ScalableFreeformLayeredPane pane, Viewport viewport) {
		this.pane = pane;
		this.viewport = viewport;
	}

	/**
	 * Adds the zoom listener.
	 * @param listener the listener
	 */
//	public void addZoomListener(ZoomListener listener) {
//		listeners.add(listener);
//	}

	/**
	 * Can zoom in.
	 * @return true, if successful
	 */
	public boolean canZoomIn() {
		return getZoom() < getMaxZoom();
	}

	/**
	 * Can zoom out.
	 * @return true, if successful
	 */
	public boolean canZoomOut() {
		return getZoom() > getMinZoom();
	}

	/**
	 * Fire zoom changed.
	 */
//	protected void fireZoomChanged() {
//		Iterator<ZoomListener> iter = listeners.iterator();
//		while (iter.hasNext()) {
//			iter.next().zoomChanged(zoom);
//		}
//	}

	/**
	 * Gets the fit x zoom level.
	 * @param which the which
	 * @return the fit x zoom level
	 */
	private double getFitXZoomLevel(int which) {
		IFigure fig = getScalableFigure();

		Dimension available = getViewport().getClientArea().getSize();
		Dimension desired;
		if (fig instanceof FreeformFigure) {
			desired = ((FreeformFigure) fig).getFreeformExtent().getCopy().union(0, 0).getSize();
		} else {
			desired = fig.getPreferredSize().getCopy();
		}

		desired.width -= fig.getInsets().getWidth();
		desired.height -= fig.getInsets().getHeight();

		while (fig != getViewport()) {
			available.width -= fig.getInsets().getWidth();
			available.height -= fig.getInsets().getHeight();
			fig = fig.getParent();
		}

		double scaleX = Math.min(available.width * zoom / desired.width, getMaxZoom());
		double scaleY = Math.min(available.height * zoom / desired.height, getMaxZoom());
		if (which == 0) {
			return scaleX;
		}
		if (which == 1) {
			return scaleY;
		}
		return Math.min(scaleX, scaleY);
	}

	/**
	 * Gets the fit height zoom level.
	 * @return the fit height zoom level
	 */
	protected double getFitHeightZoomLevel() {
		return getFitXZoomLevel(1);
	}

	/**
	 * Gets the fit page zoom level.
	 * @return the fit page zoom level
	 */
	protected double getFitPageZoomLevel() {
		return getFitXZoomLevel(2);
	}

	/**
	 * Gets the fit width zoom level.
	 * @return the fit width zoom level
	 */
	protected double getFitWidthZoomLevel() {
		return getFitXZoomLevel(0);
	}

	/**
	 * Gets the max zoom.
	 * @return the max zoom
	 */
	public double getMaxZoom() {
		return getZoomLevels()[getZoomLevels().length - 1];
	}

	/**
	 * Gets the min zoom.
	 * @return the min zoom
	 */
	public double getMinZoom() {
		return getZoomLevels()[0];
	}

	/**
	 * Gets the uI multiplier.
	 * @return the uI multiplier
	 */
	public double getUIMultiplier() {
		return multiplier;
	}

	/**
	 * Gets the next zoom level.
	 * @return the next zoom level
	 */
	public double getNextZoomLevel() {
		for (int i = 0; i < zoomLevels.length; i++) {
			if (zoomLevels[i] > zoom) {
				return zoomLevels[i];
			}
		}
		return getMaxZoom();
	}

	/**
	 * Gets the previous zoom level.
	 * @return the previous zoom level
	 */
	public double getPreviousZoomLevel() {
		for (int i = 1; i < zoomLevels.length; i++) {
			if (zoomLevels[i] >= zoom) {
				return zoomLevels[i - 1];
			}
		}
		return getMinZoom();
	}

	/**
	 * Gets the scalable figure.
	 * @return the scalable figure
	 */
	public ScalableFigure getScalableFigure() {
		return pane;
	}

	/**
	 * Gets the viewport.
	 * @return the viewport
	 */
	public Viewport getViewport() {
		return viewport;
	}

	/**
	 * Gets the zoom.
	 * @return the zoom
	 */
	public double getZoom() {
		return zoom;
	}

	/**
	 * Format.
	 * @param d the d
	 * @return the string
	 */
	private String format(double d) {
		return "" + ((int) (d * 100)) + "%";
	}

	/**
	 * Gets the zoom as text.
	 * @return the zoom as text
	 */
	public String getZoomAsText() {
		if (currentZoomContant != null) {
			return currentZoomContant;
		}

		// String newItem = format.format(zoom * multiplier);
		String newItem = format(zoom * multiplier);
		return newItem;
	}

	/**
	 * Gets the zoom level contributions.
	 * @return the zoom level contributions
	 */
	@SuppressWarnings("rawtypes")
	public List getZoomLevelContributions() {
		return zoomLevelContributions;
	}

	/**
	 * Gets the zoom levels.
	 * @return the zoom levels
	 */
	public double[] getZoomLevels() {
		return zoomLevels;
	}

	/**
	 * Gets the zoom levels as text.
	 * @return the zoom levels as text
	 */
	public String[] getZoomLevelsAsText() {
		String[] zoomLevelStrings = new String[zoomLevels.length + zoomLevelContributions.size()];

		if (zoomLevelContributions != null) {
			for (int i = 0; i < zoomLevelContributions.size(); i++) {
				zoomLevelStrings[i] = zoomLevelContributions.get(i);
			}
		}
		for (int i = 0; i < zoomLevels.length; i++) {
			// zoomLevelStrings[i + zoomLevelContributions.size()] =
			// format.format(zoomLevels[i] * multiplier);
			zoomLevelStrings[i + zoomLevelContributions.size()] = format(zoomLevels[i] * multiplier);
		}

		return zoomLevelStrings;
	}

	/**
	 * Prim set zoom.
	 * @param zoom the zoom
	 */
	protected void primSetZoom(double zoom) {
		Point p1 = getViewport().getClientArea().getCenter();
		Point p2 = p1.getCopy();
		Point p = getViewport().getViewLocation();
		double prevZoom = this.zoom;
		this.zoom = zoom;
		pane.setScale(zoom);
//		fireZoomChanged();
		getViewport().validate();

		p2.scale(zoom / prevZoom);
		Dimension dif = p2.getDifference(p1);
		p.x += dif.width;
		p.y += dif.height;
		setViewLocation(p);
	}

	/**
	 * Removes the zoom listener.
	 * @param listener the listener
	 */
//	public void removeZoomListener(ZoomListener listener) {
//		listeners.remove(listener);
//	}

	/**
	 * Sets the uI multiplier.
	 * @param multiplier the new uI multiplier
	 */
	public void setUIMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Sets the view location.
	 * @param p the new view location
	 */
	public void setViewLocation(Point p) {
		viewport.setViewLocation(p.x, p.y);

	}

	/**
	 * Sets the zoom.
	 * @param zoom the new zoom
	 */
	public void setZoom(double zoom) {
		currentZoomContant = null;
		zoom = Math.min(getMaxZoom(), zoom);
		zoom = Math.max(getMinZoom(), zoom);
		if (this.zoom != zoom) {
			primSetZoom(zoom);
		}
	}

	/**
	 * Sets the zoom animation style.
	 * @param style the new zoom animation style
	 */
	public void setZoomAnimationStyle(int style) {
		// zoomAnimationStyle = style;
	}

	/**
	 * Sets the zoom as text.
	 * @param zoomString the new zoom as text
	 */
	public void setZoomAsText(String zoomString) {
		currentZoomContant = null;
		if (zoomString.equalsIgnoreCase(FIT_HEIGHT)) {
			currentZoomContant = FIT_HEIGHT;
			primSetZoom(getFitHeightZoomLevel());
			viewport.getUpdateManager().performUpdate();
			viewport.setViewLocation(viewport.getHorizontalRangeModel().getValue(), viewport.getVerticalRangeModel().getMinimum());
		} else if (zoomString.equalsIgnoreCase(FIT_ALL)) {
			currentZoomContant = FIT_ALL;
			primSetZoom(getFitPageZoomLevel());
			viewport.getUpdateManager().performUpdate();
			viewport.setViewLocation(viewport.getHorizontalRangeModel().getMinimum(), viewport.getVerticalRangeModel().getMinimum());
		} else if (zoomString.equalsIgnoreCase(FIT_WIDTH)) {
			currentZoomContant = FIT_WIDTH;
			primSetZoom(getFitWidthZoomLevel());
			viewport.getUpdateManager().performUpdate();
			viewport.setViewLocation(viewport.getHorizontalRangeModel().getMinimum(), viewport.getVerticalRangeModel().getValue());
		} else {
			try {
				// Trim off the '%'
				if (zoomString.charAt(zoomString.length() - 1) == '%') {
					zoomString = zoomString.substring(0, zoomString.length() - 1);
				}
				double newZoom = Double.parseDouble(zoomString) / 100;
				setZoom(newZoom / multiplier);
			} catch (Exception e) {
				Display.getCurrent().beep();
			}
		}
	}

	/**
	 * Sets the zoom level contributions.
	 * @param contributions the new zoom level contributions
	 */
	public void setZoomLevelContributions(List<String> contributions) {
		zoomLevelContributions = contributions;
	}

	/**
	 * Sets the zoom levels.
	 * @param zoomLevels the new zoom levels
	 */
	public void setZoomLevels(double[] zoomLevels) {
		this.zoomLevels = zoomLevels;
	}

	/**
	 * Zoom in.
	 */
	public void zoomIn() {
		setZoom(getNextZoomLevel());
	}

	/**
	 * Zoom to.
	 * @param rect the rect
	 */
	public void zoomTo(Rectangle rect) {}

	// private void performAnimatedZoom(Rectangle rect, boolean zoomIn, int
	// iterationCount) {
	// double finalRatio;
	// double zoomIncrement;
	//
	// if (zoomIn) {
	// finalRatio = zoom / getNextZoomLevel();
	// zoomIncrement = (getNextZoomLevel() - zoom) / iterationCount;
	// } else {
	// finalRatio = zoom / getPreviousZoomLevel();
	// zoomIncrement = (getPreviousZoomLevel() - zoom) / iterationCount;
	// }
	//
	// getScalableFigure().translateToRelative(rect);
	// Point originalViewLocation = getViewport().getViewLocation();
	// Point finalViewLocation = calculateViewLocation(rect, finalRatio);
	//
	// double xIncrement =
	// (double) (finalViewLocation.x - originalViewLocation.x) / iterationCount;
	// double yIncrement =
	// (double) (finalViewLocation.y - originalViewLocation.y) / iterationCount;
	//
	// double originalZoom = zoom;
	// Point currentViewLocation = new Point();
	// for (int i = 1; i < iterationCount; i++) {
	// currentViewLocation.x = (int)(originalViewLocation.x + (xIncrement * i));
	// currentViewLocation.y = (int)(originalViewLocation.y + (yIncrement * i));
	// setZoom(originalZoom + zoomIncrement * i);
	// getViewport().validate();
	// setViewLocation(currentViewLocation);
	// getViewport().getUpdateManager().performUpdate();
	// }
	//
	// if (zoomIn)
	// setZoom(getNextZoomLevel());
	// else
	// setZoom(getPreviousZoomLevel());
	//
	// getViewport().validate();
	// setViewLocation(finalViewLocation);
	// }
	//
	// private Point calculateViewLocation(Rectangle zoomRect, double ratio) {
	// Point viewLocation = new Point();
	// viewLocation.x = (int)(zoomRect.x / ratio);
	// viewLocation.y = (int)(zoomRect.y / ratio);
	// return viewLocation;
	// }

	/**
	 * Zoom out.
	 */
	public void zoomOut() {
		setZoom(getPreviousZoomLevel());
	}

}
