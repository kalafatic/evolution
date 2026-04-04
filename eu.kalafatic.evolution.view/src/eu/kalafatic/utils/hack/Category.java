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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * The Class class Category.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Category implements IWorkbenchAdapter, IPluginContribution,
		IAdaptable {

	/** The Constant INSTANCE. */
	public static final Category INSTANCE = new Category();

	/** The categories. */
	public Map<Object, Category> categories = new HashMap<Object, Category>();

	/** The id. */
	private String id;

	/** The name. */
	private String name;

	/** The raw path. */
	private String rawPath;

	/** The parent path. */
	private String[] parentPath;

	/** The elements. */
	private ArrayList elements;

	/** The parent. */
	private Category parent;

	/** The configuration element. */
	private IConfigurationElement configurationElement;

	/** The plugin id. */
	private String pluginId;

	/**
	 * Instantiates a new category.
	 */
	public Category() {
	}

	/**
	 * Instantiates a new category.
	 * 
	 * @param parent
	 *            the parent
	 * @param id
	 *            the id
	 * @param name
	 *            the name
	 * @param rawPath
	 *            the raw path
	 */
	public Category(Category parent, String id, String name, String rawPath) {
		this.parent = parent;
		this.id = id;
		this.name = name;
		this.rawPath = rawPath;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Adds the element.
	 * 
	 * @param element
	 *            the element
	 */
	public void addElement(Object element) {
		if (elements == null) {
			elements = new ArrayList(5);
		}
		elements.add(element);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc) Method declared on IAdaptable.
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return this;
		} else if (adapter == IConfigurationElement.class) {
			return configurationElement;
		} else {
			return null;
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc) Method declared on IWorkbenchAdapter.
	 */
	@Override
	public Object[] getChildren(Object o) {
		return getElements().toArray();
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc) Method declared on IWorkbenchAdapter.
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc) Method declared on IWorkbenchAdapter.
	 */
	@Override
	public String getLabel(Object o) {
		return getLabel();
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the label.
	 * 
	 * @return the label
	 */
	public String getLabel() {
		return name;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the parent path.
	 * 
	 * @return the parent path
	 */
	public String[] getParentPath() {
		if (parentPath != null) {
			return parentPath;
		}

		String unparsedPath = getRawParentPath();
		if (unparsedPath != null) {
			StringTokenizer stok = new StringTokenizer(unparsedPath, "/"); //$NON-NLS-1$
			parentPath = new String[stok.countTokens()];
			for (int i = 0; stok.hasMoreTokens(); i++) {
				parentPath[i] = stok.nextToken();
			}
		}
		return parentPath;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the raw parent path.
	 * 
	 * @return the raw parent path
	 */
	public String getRawParentPath() {
		return rawPath;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the root path.
	 * 
	 * @return the root path
	 */
	public String getRootPath() {
		String[] path = getParentPath();
		if (path != null && path.length > 0) {
			return path[0];
		}
		return id;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the elements.
	 * 
	 * @return the elements
	 */
	public ArrayList getElements() {
		return elements;
	}

	// ---------------------------------------------------------------

	/**
	 * Checks for element.
	 * 
	 * @param o
	 *            the o
	 * @return true, if successful
	 */
	public boolean hasElement(Object o) {
		if (elements == null) {
			return false;
		}
		if (elements.isEmpty()) {
			return false;
		}
		return elements.contains(o);
	}

	// ---------------------------------------------------------------

	/**
	 * Checks for elements.
	 * 
	 * @return true, if successful
	 */
	public boolean hasElements() {
		if (elements != null) {
			return !elements.isEmpty();
		}
		return false;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object o) {
		return null;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.activities.support.IPluginContribution#getLocalId()
	 */
	@Override
	public String getLocalId() {
		return id;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.activities.support.IPluginContribution#getPluginId()
	 */
	@Override
	public String getPluginId() {
		return configurationElement == null ? pluginId : configurationElement
				.getNamespace();
	}

	// ---------------------------------------------------------------

	/**
	 * Clear.
	 */
	public void clear() {
		if (elements != null) {
			elements.clear();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the parent.
	 * 
	 * @return the parent
	 */
	public Category getParent() {
		return parent;
	}
}
