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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * The Class class Utilities.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class Utilities {

	/** The log. */
	private static Log log = LogFactory.getLog(Utilities.class);

	/** The Constant ICONS_PATH. */
	private static final IPath ICONS_PATH = new Path("$nl$/icons/full/"); //$NON-NLS-1$

	/**
	 * Find site.
	 * 
	 * @param c
	 *            the c
	 * @return the i workbench part site
	 */
	public static IWorkbenchPartSite findSite(Control c) {
		while (c != null && !c.isDisposed()) {
			Object data = c.getData();
			if (data instanceof IWorkbenchPart) {
				return ((IWorkbenchPart) data).getSite();
			}
			c = c.getParent();
		}
		return null;
	}

	/**
	 * Sets the enable composite.
	 * 
	 * @param composite
	 *            the composite
	 * @param enable
	 *            the enable
	 */
	public static void setEnableComposite(Composite composite, boolean enable) {
		Control[] children = composite.getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].setEnabled(enable);
		}
	}

	/**
	 * Gets the boolean.
	 * 
	 * @param cc
	 *            the cc
	 * @param key
	 *            the key
	 * @param dflt
	 *            the dflt
	 * @return the boolean
	 */
	public static boolean getBoolean(CompareConfiguration cc, String key,
			boolean dflt) {
		if (cc != null) {
			Object value = cc.getProperty(key);
			if (value instanceof Boolean) {
				return ((Boolean) value).booleanValue();
			}
		}
		return dflt;
	}

	/**
	 * Fire property change.
	 * 
	 * @param listenerList
	 *            the listener list
	 * @param source
	 *            the source
	 * @param property
	 *            the property
	 * @param old
	 *            the old
	 * @param newValue
	 *            the new value
	 */
	public static void firePropertyChange(ListenerList listenerList,
			Object source, String property, Object old, Object newValue) {
		PropertyChangeEvent event = new PropertyChangeEvent(source, property,
				old, newValue);
		firePropertyChange(listenerList, event);
	}

	/**
	 * Fire property change.
	 * 
	 * @param listenerList
	 *            the listener list
	 * @param event
	 *            the event
	 */
	public static void firePropertyChange(final ListenerList listenerList,
			final PropertyChangeEvent event) {
		if (listenerList == null || listenerList.isEmpty()) {
			return;
		}
		// Legacy listeners may expect to get notified in the UI thread
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				if (listenerList != null) {
					Object[] listeners = listenerList.getListeners();
					for (int i = 0; i < listeners.length; i++) {
						final IPropertyChangeListener listener = (IPropertyChangeListener) listeners[i];
						SafeRunner.run(new ISafeRunnable() {

							@Override
							public void run() throws Exception {
								listener.propertyChange(event);
							}

							@Override
							public void handleException(Throwable exception) {
								// Logged by SafeRunner
							}
						});
					}
				}
			}
		};
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(runnable);
		} else {
			runnable.run();
		}
	}

	/**
	 * Ok to use.
	 * 
	 * @param widget
	 *            the widget
	 * @return true, if successful
	 */
	public static boolean okToUse(Widget widget) {
		return widget != null && !widget.isDisposed();
	}

	/**
	 * Internal get resources.
	 * 
	 * @param selection
	 *            the selection
	 * @param type
	 *            the type
	 * @return the array list
	 */
	private static ArrayList internalGetResources(ISelection selection,
			Class type) {
		ArrayList tmp = new ArrayList();
		if (selection instanceof IStructuredSelection) {
			Object[] s = ((IStructuredSelection) selection).toArray();

			for (int i = 0; i < s.length; i++) {
				IResource resource = null;
				Object o = s[i];
				if (type.isInstance(o)) {
					resource = (IResource) o;

				} else if (o instanceof ResourceMapping) {
					try {
						ResourceTraversal[] travs = ((ResourceMapping) o)
								.getTraversals(
										ResourceMappingContext.LOCAL_CONTEXT,
										null);
						if (travs != null) {
							for (int k = 0; k < travs.length; k++) {
								IResource[] resources = travs[k].getResources();
								for (int j = 0; j < resources.length; j++) {
									if (type.isInstance(resources[j])
											&& resources[j].isAccessible()) {
										tmp.add(resources[j]);
									}
								}
							}
						}
					} catch (CoreException ex) {
						log.error(ex.getMessage(), ex);
					}
				} else if (o instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) o;
					Object adapter = a.getAdapter(IResource.class);
					if (type.isInstance(adapter)) {
						resource = (IResource) adapter;
					}
				}

				if (resource != null && resource.isAccessible()) {
					tmp.add(resource);
				}
			}
		}
		return tmp;
	}

	/*
	 * Convenience method: extract all accessible <code>IResources</code> from
	 * given selection. Never returns null.
	 */
	/**
	 * Gets the resources.
	 * 
	 * @param selection
	 *            the selection
	 * @return the resources
	 */
	public static IResource[] getResources(ISelection selection) {
		ArrayList tmp = internalGetResources(selection, IResource.class);
		return (IResource[]) tmp.toArray(new IResource[tmp.size()]);
	}

	/*
	 * Convenience method: extract all accessible <code>IFiles</code> from given
	 * selection. Never returns null.
	 */
	/**
	 * Gets the files.
	 * 
	 * @param selection
	 *            the selection
	 * @return the files
	 */
	public static IFile[] getFiles(ISelection selection) {
		ArrayList tmp = internalGetResources(selection, IFile.class);
		return (IFile[]) tmp.toArray(new IFile[tmp.size()]);
	}

	/**
	 * Read bytes.
	 * 
	 * @param in
	 *            the in
	 * @return the byte[]
	 */
	public static byte[] readBytes(InputStream in) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			while (true) {
				int c = in.read();
				if (c == -1) {
					break;
				}
				bos.write(c);
			}

		} catch (IOException ex) {
			return null;

		} finally {
			Utilities.close(in);
			try {
				bos.close();
			} catch (IOException x) {
				// silently ignored
			}
		}

		return bos.toByteArray();
	}

	/**
	 * Gets the icon path.
	 * 
	 * @param display
	 *            the display
	 * @return the icon path
	 */
	public static IPath getIconPath(Display display) {
		return ICONS_PATH;
	}

	/*
	 * Initialize the given Action from a ResourceBundle.
	 */
	/**
	 * Inits the action.
	 * 
	 * @param a
	 *            the a
	 * @param bundle
	 *            the bundle
	 * @param prefix
	 *            the prefix
	 */
	public static void initAction(IAction a, ResourceBundle bundle,
			String prefix) {

		String labelKey = "label"; //$NON-NLS-1$
		String tooltipKey = "tooltip"; //$NON-NLS-1$
		String imageKey = "image"; //$NON-NLS-1$
		String descriptionKey = "description"; //$NON-NLS-1$

		if (prefix != null && prefix.length() > 0) {
			labelKey = prefix + labelKey;
			tooltipKey = prefix + tooltipKey;
			imageKey = prefix + imageKey;
			descriptionKey = prefix + descriptionKey;
		}

		a.setText(getString(bundle, labelKey, labelKey));
		a.setToolTipText(getString(bundle, tooltipKey, null));
		a.setDescription(getString(bundle, descriptionKey, null));

		String relPath = getString(bundle, imageKey, null);
		if (relPath != null && relPath.trim().length() > 0) {

			String dPath;
			String ePath;

			if (relPath.indexOf("/") >= 0) { //$NON-NLS-1$
				String path = relPath.substring(1);
				dPath = 'd' + path;
				ePath = 'e' + path;
			} else {
				dPath = "dlcl16/" + relPath; //$NON-NLS-1$
				ePath = "elcl16/" + relPath; //$NON-NLS-1$
			}

			// ImageDescriptor id= CompareUIPlugin.getImageDescriptor(dPath); //
			// we set the disabled image first (see PR
			// 1GDDE87)
			// if (id != null)
			// a.setDisabledImageDescriptor(id);
			// id= CompareUIPlugin.getImageDescriptor(ePath);
			// if (id != null) {
			// a.setImageDescriptor(id);
			// a.setHoverImageDescriptor(id);
			// }
		}
	}

	/**
	 * Inits the toggle action.
	 * 
	 * @param a
	 *            the a
	 * @param bundle
	 *            the bundle
	 * @param prefix
	 *            the prefix
	 * @param checked
	 *            the checked
	 */
	public static void initToggleAction(IAction a, ResourceBundle bundle,
			String prefix, boolean checked) {

		String tooltip = null;
		if (checked) {
			tooltip = getString(bundle, prefix + "tooltip.checked", null); //$NON-NLS-1$
		} else {
			tooltip = getString(bundle, prefix + "tooltip.unchecked", null); //$NON-NLS-1$
		}
		if (tooltip == null) {
			tooltip = getString(bundle, prefix + "tooltip", null); //$NON-NLS-1$
		}

		if (tooltip != null) {
			a.setToolTipText(tooltip);
		}

		String description = null;
		if (checked) {
			description = getString(bundle,
					prefix + "description.checked", null); //$NON-NLS-1$
		} else {
			description = getString(bundle,
					prefix + "description.unchecked", null); //$NON-NLS-1$
		}
		if (description == null) {
			description = getString(bundle, prefix + "description", null); //$NON-NLS-1$
		}

		if (description != null) {
			a.setDescription(description);
		}

	}

	/**
	 * Gets the string.
	 * 
	 * @param bundle
	 *            the bundle
	 * @param key
	 *            the key
	 * @param dfltValue
	 *            the dflt value
	 * @return the string
	 */
	public static String getString(ResourceBundle bundle, String key,
			String dfltValue) {

		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException x) {
				// fall through
			}
		}
		return dfltValue;
	}

	/**
	 * Gets the string.
	 * 
	 * @param key
	 *            the key
	 * @return the string
	 */
	public static String getString(String key) {
		try {
			return CompareUI.getResourceBundle().getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!"; //$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	/**
	 * Gets the formatted string.
	 * 
	 * @param key
	 *            the key
	 * @param arg
	 *            the arg
	 * @return the formatted string
	 */
	public static String getFormattedString(String key, String arg) {
		try {
			return MessageFormat.format(CompareUI.getResourceBundle()
					.getString(key), new String[] { arg });
		} catch (MissingResourceException e) {
			return "!" + key + "!"; //$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	/**
	 * Gets the formatted string.
	 * 
	 * @param key
	 *            the key
	 * @param arg0
	 *            the arg0
	 * @param arg1
	 *            the arg1
	 * @return the formatted string
	 */
	public static String getFormattedString(String key, String arg0, String arg1) {
		try {
			return MessageFormat.format(CompareUI.getResourceBundle()
					.getString(key), new String[] { arg0, arg1 });
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	/**
	 * Gets the string.
	 * 
	 * @param bundle
	 *            the bundle
	 * @param key
	 *            the key
	 * @return the string
	 */
	public static String getString(ResourceBundle bundle, String key) {
		return getString(bundle, key, key);
	}

	/** The Constant VALIDATE_EDIT_PROBLEM. */
	/*
	 * public static boolean isSelectionNonOverlapping() throws TeamException {
	 * IResource[] resources = getSelectedResources(); // allow operation for
	 * non-overlapping resource selections if(resources.length>0) { List
	 * validPaths = new ArrayList(2); for (int i = 0; i < resources.length; i++)
	 * { IResource resource = resources[i];
	 * 
	 * // only allow cvs resources to be selected
	 * if(RepositoryProvider.getProvider(resource.getProject(),
	 * CVSProviderPlugin.getTypeId()) == null) { return false; }
	 * 
	 * // check if this resource overlaps other selections IPath
	 * resourceFullPath = resource.getFullPath(); if(!validPaths.isEmpty()) {
	 * for (Iterator it = validPaths.iterator(); it.hasNext();) { IPath path =
	 * (IPath) it.next(); if(path.isPrefixOf(resourceFullPath) ||
	 * resourceFullPath.isPrefixOf(path)) { return false; } } }
	 * validPaths.add(resourceFullPath);
	 * 
	 * // ensure that resources are managed ICVSResource cvsResource =
	 * CVSWorkspaceRoot.getCVSResourceFor(resource); if(cvsResource.isFolder())
	 * { if( ! ((ICVSFolder)cvsResource).isCVSFolder()) return false; } else {
	 * if( ! cvsResource.isManaged()) return false; } } return true; } return
	 * false; }
	 */

	/* validate edit utilities */

	/**
	 * Status constant indicating that an validateEdit call has changed the
	 * content of a file on disk.
	 */
	private static final int VALIDATE_EDIT_PROBLEM = 10004;

	/** The RUNNIN g_ tests. */
	public static boolean RUNNING_TESTS = false;

	/** The TESTIN g_ flus h_ o n_ compar e_ inpu t_ change. */
	public static boolean TESTING_FLUSH_ON_COMPARE_INPUT_CHANGE = false;

	/**
	 * Display error.
	 * 
	 * @param shell
	 *            the shell
	 * @param title
	 *            the title
	 * @param status
	 *            the status
	 * @param message
	 *            the message
	 */
	private static void displayError(final Shell shell, final String title,
			final IStatus status, final String message) {
		if (Display.getCurrent() != null) {
			ErrorDialog.openError(shell, title, message, status);
		} else {
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					ErrorDialog.openError(shell, title, message, status);
				}
			});
		}
	}

	/**
	 * Gets the readonly files.
	 * 
	 * @param resources
	 *            the resources
	 * @return the readonly files
	 */
	private static List getReadonlyFiles(IResource[] resources) {
		List readOnlyFiles = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			ResourceAttributes resourceAttributes = resource
					.getResourceAttributes();
			if (resource.getType() == IResource.FILE
					&& resourceAttributes != null
					&& resourceAttributes.isReadOnly()) {
				readOnlyFiles.add(resource);
			}
		}
		return readOnlyFiles;
	}

	/**
	 * Creates the modification stamp map.
	 * 
	 * @param files
	 *            the files
	 * @return the map
	 */
	private static Map createModificationStampMap(List files) {
		Map map = new HashMap();
		for (Iterator iter = files.iterator(); iter.hasNext();) {
			IFile file = (IFile) iter.next();
			map.put(file, new Long(file.getModificationStamp()));
		}
		return map;
	}

	/*
	 * Returns null if an error occurred.
	 */
	/**
	 * Read string.
	 * 
	 * @param is
	 *            the is
	 * @param encoding
	 *            the encoding
	 * @return the string
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static String readString(InputStream is, String encoding)
			throws IOException {
		if (is == null) {
			return null;
		}
		BufferedReader reader = null;
		try {
			StringBuffer buffer = new StringBuffer();
			char[] part = new char[2048];
			int read = 0;
			reader = new BufferedReader(new InputStreamReader(is, encoding));
			while ((read = reader.read(part)) != -1) {
				buffer.append(part, 0, read);
			}

			return buffer.toString();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					// silently ignored
				}
			}
		}
	}

	/**
	 * Gets the bytes.
	 * 
	 * @param s
	 *            the s
	 * @param encoding
	 *            the encoding
	 * @return the bytes
	 */
	public static byte[] getBytes(String s, String encoding) {
		byte[] bytes = null;
		if (s != null) {
			try {
				bytes = s.getBytes(encoding);
			} catch (UnsupportedEncodingException e) {
				bytes = s.getBytes();
			}
		}
		return bytes;
	}

	/**
	 * Close.
	 * 
	 * @param is
	 *            the is
	 */
	public static void close(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException ex) {
				// silently ignored
			}
		}
	}

	/**
	 * Gets the first resource.
	 * 
	 * @param selection
	 *            the selection
	 * @return the first resource
	 */
	public static IResource getFirstResource(ISelection selection) {
		IResource[] resources = getResources(selection);
		if (resources.length > 0) {
			return resources[0];
		}
		return null;
	}

	/**
	 * Gets the adapter.
	 * 
	 * @param element
	 *            the element
	 * @param adapterType
	 *            the adapter type
	 * @param load
	 *            the load
	 * @return the adapter
	 */
	public static Object getAdapter(Object element, Class adapterType,
			boolean load) {
		if (adapterType.isInstance(element)) {
			return element;
		}
		if (element instanceof IAdaptable) {
			Object adapted = ((IAdaptable) element).getAdapter(adapterType);
			if (adapterType.isInstance(adapted)) {
				return adapted;
			}
		}
		if (load) {
			Object adapted = Platform.getAdapterManager().loadAdapter(element,
					adapterType.getName());
			if (adapterType.isInstance(adapted)) {
				return adapted;
			}
		} else {
			Object adapted = Platform.getAdapterManager().getAdapter(element,
					adapterType);
			if (adapterType.isInstance(adapted)) {
				return adapted;
			}
		}
		return null;
	}

	/**
	 * Gets the adapter.
	 * 
	 * @param element
	 *            the element
	 * @param adapterType
	 *            the adapter type
	 * @return the adapter
	 */
	public static Object getAdapter(Object element, Class adapterType) {
		return getAdapter(element, adapterType, false);
	}

	/**
	 * Schedule.
	 * 
	 * @param job
	 *            the job
	 * @param site
	 *            the site
	 */
	// public static boolean isHunk(Object input) {
	// if (input != null && input instanceof DiffNode) {
	// ITypedElement right = ((DiffNode) input).getRight();
	// if (right != null) {
	// Object element = Utilities.getAdapter(right, IHunk.class);
	// if (element instanceof IHunk)
	// return true;
	// }
	// ITypedElement left = ((DiffNode) input).getLeft();
	// if (left != null) {
	// Object element = Utilities.getAdapter(left, IHunk.class);
	// if (element instanceof IHunk)
	// return true;
	// }
	// }
	// return false;
	// }

	public static void schedule(Job job, IWorkbenchSite site) {
		if (site != null) {
			IWorkbenchSiteProgressService siteProgress = (IWorkbenchSiteProgressService) site
					.getAdapter(IWorkbenchSiteProgressService.class);
			if (siteProgress != null) {
				siteProgress.schedule(job, 0, true /* use half-busy cursor */);
				return;
			}
		}
		job.schedule();
	}

	/**
	 * Run in ui thread.
	 * 
	 * @param runnable
	 *            the runnable
	 */
	public static void runInUIThread(final Runnable runnable) {
		if (Display.getCurrent() != null) {
			BusyIndicator.showWhile(Display.getCurrent(), runnable);
		} else {
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					BusyIndicator.showWhile(Display.getCurrent(), runnable);
				}
			});
		}
	}
}
