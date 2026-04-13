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
package eu.kalafatic.utils.providers;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.IViewDescriptor;

import eu.kalafatic.utils.constants.FCoreImageConstants;

/**
 * The Class class ViewLabelProvider.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ViewLabelProvider extends ColumnLabelProvider {

	/** The images. */
	private HashMap<ImageDescriptor, Image> images;

	/** The window. */
	private final IWorkbenchWindow window;

	/** The dimmed foreground. */
	private final Color dimmedForeground;

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Instantiates a new view label provider.
	 * @param window the window
	 * @param dimmedForeground the dimmed foreground
	 */
	public ViewLabelProvider(IWorkbenchWindow window, Color dimmedForeground) {
		this.window = window;
		this.dimmedForeground = dimmedForeground;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CellLabelProvider#initialize(org.eclipse.jface .viewers.ColumnViewer, org.eclipse.jface.viewers.ViewerColumn)
	 */
	@Override
	protected void initialize(ColumnViewer viewer, ViewerColumn column) {
		super.initialize(viewer, column);
	}

	// ---------------------------------------------------------------

	/**
	 * Cache image.
	 * @param desc the desc
	 * @return the image
	 */
	private Image cacheImage(ImageDescriptor desc) {
		if (images == null) {
			images = new HashMap<ImageDescriptor, Image>(21);
		}
		Image image = images.get(desc);
		if (image == null) {
			image = desc.createImage();
			images.put(desc, image);
		}
		return image;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		if (images != null) {
			for (Iterator<Image> i = images.values().iterator(); i.hasNext();) {
				i.next().dispose();
			}
			images = null;
		}
		super.dispose();
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof IViewDescriptor) {
			ImageDescriptor desc = ((IViewDescriptor) element).getImageDescriptor();
			if (desc != null) {
				return cacheImage(desc);
			}
		}
		// else if (element instanceof Category) {
		// return FCoreImageConstants.FOLDER_IMG;
		// }
		return FCoreImageConstants.FILE_IMG;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		// if (element instanceof Category) {
		// return ((Category) element).getLabel();
		// } else
		if (element instanceof IViewDescriptor) {
			return ((IViewDescriptor) element).getLabel();
		}
		return "";
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getBackground(java.lang .Object)
	 */
	@Override
	public Color getBackground(Object element) {
		return null;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getForeground(java.lang .Object)
	 */
	@Override
	public Color getForeground(Object element) {
		if (element instanceof IViewDescriptor) {
			IWorkbenchPage activePage = window.getActivePage();
			if (activePage != null) {
				if (activePage.findViewReference(((IViewDescriptor) element).getId()) != null) {
					return dimmedForeground;
				}
			}
		}
		return null;
	}
}
