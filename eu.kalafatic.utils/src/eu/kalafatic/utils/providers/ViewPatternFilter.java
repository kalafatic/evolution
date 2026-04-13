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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.views.IViewCategory;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * The Class class ViewPatternFilter.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ViewPatternFilter extends PatternFilter {

	/**
	 * Instantiates a new view pattern filter.
	 */
	public ViewPatternFilter() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.internal.dialogs.PatternFilter#isElementSelectable(java
	 * .lang.Object)
	 */
	@Override
	public boolean isElementSelectable(Object element) {
		return element instanceof IViewDescriptor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.PatternFilter#isElementMatch(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object)
	 */
	@Override
	protected boolean isLeafMatch(Viewer viewer, Object element) {
		if (element instanceof IViewCategory) {
			return false;
		}

		String text = null;
		if (element instanceof IViewDescriptor) {
			IViewDescriptor desc = (IViewDescriptor) element;
			text = desc.getLabel();
			if (wordMatches(text)) {
				return true;
			}
		}

		return false;
	}
}
