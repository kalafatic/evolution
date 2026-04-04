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

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.EditorSelectionDialog;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

import eu.kalafatic.utils.application.AppUtils;

/**
 * The Class class OpenWithMenu.
 *
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings({ "rawtypes", "restriction", "unchecked" })
public class OpenWithMenu extends ContributionItem {

	/** The page. */
	private IWorkbenchPage page;

	/** The file. */
	private File file;

	/** The registry. */
	private IEditorRegistry registry = PlatformUI.getWorkbench()
			.getEditorRegistry();

	/** The Constant ID. */
	public static final String ID = PlatformUI.PLUGIN_ID + ".OpenWithMenu";

	/*
	 * Compares the labels from two IEditorDescriptor objects
	 */
	/** The Constant comparer. */
//	private static final Comparator comparer = new Comparator() {
//		private Collator collator = Collator.getInstance();
//
//		@Override
//		public int compare(Object arg0, Object arg1) {
//			String s1 = ((IEditorDescriptor) arg0).getLabel();
//			String s2 = ((IEditorDescriptor) arg1).getLabel();
//			return collator.compare(s1, s2);
//		}
//	};

	/**
	 * Instantiates a new open with menu.
	 *
	 * @param page
	 *            the page
	 * @param file
	 *            the file
	 */
	public OpenWithMenu(IWorkbenchPage page, File file) {
		super(ID);
		this.page = page;
		this.file = file;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------
	/**
	 * Gets the image.
	 *
	 * @param editorDesc
	 *            the editor desc
	 * @return the image
	 */
	private Image getImage(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = getImageDescriptor(editorDesc);
		if (imageDesc == null) {
			return null;
		}
		return IDEWorkbenchPlugin.getDefault().getResourceManager()
				.createImage(imageDesc);
	}

	// ---------------------------------------------------------------
	/**
	 * Gets the image descriptor.
	 *
	 * @param editorDesc
	 *            the editor desc
	 * @return the image descriptor
	 */
	private ImageDescriptor getImageDescriptor(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = null;
		if (editorDesc == null) {
			imageDesc = registry.getImageDescriptor(file.getName());
			// TODO: is this case valid, and if so, what are the implications
			// for content-type editor bindings?
		} else {
			imageDesc = editorDesc.getImageDescriptor();
		}
		if (imageDesc == null) {
			if (editorDesc.getId().equals(
					IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID)) {
				imageDesc = registry
						.getSystemExternalEditorImageDescriptor(file.getName());
			}
		}
		return imageDesc;
	}

	// ---------------------------------------------------------------
	/**
	 * Creates the menu item.
	 *
	 * @param menu
	 *            the menu
	 * @param descriptor
	 *            the descriptor
	 * @param preferredEditor
	 *            the preferred editor
	 */
	private void createMenuItem(Menu menu, final IEditorDescriptor descriptor,
			final IEditorDescriptor preferredEditor) {

		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		boolean isPreferred = preferredEditor != null
				&& descriptor.getId().equals(preferredEditor.getId());
		menuItem.setSelection(isPreferred);
		menuItem.setText(descriptor.getLabel());
		Image image = getImage(descriptor);
		if (image != null) {
			menuItem.setImage(image);
		}
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						AppUtils.openEditor(descriptor, file);
					}
					break;
				}
			}
		};
		menuItem.addListener(SWT.Selection, listener);
	}

	// ---------------------------------------------------------------
	/**
	 * Creates the other menu item.
	 *
	 * @param menu
	 *            the menu
	 */
	private void createOtherMenuItem(final Menu menu) {
		new MenuItem(menu, SWT.SEPARATOR);
		final MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(IDEWorkbenchMessages.OpenWithMenu_Other);
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					EditorSelectionDialog dialog = new EditorSelectionDialog(
							menu.getShell());
					dialog.setMessage(NLS
							.bind(IDEWorkbenchMessages.OpenWithMenu_OtherDialogDescription,
									file.getName()));
					if (dialog.open() == Window.OK) {
						IEditorDescriptor iEditorDesc = dialog
								.getSelectedEditor();
						if (iEditorDesc != null) {
							AppUtils.openEditor(iEditorDesc, file);
						}
					}
					break;
				}
			}
		};
		menuItem.addListener(SWT.Selection, listener);
	}

	// ---------------------------------------------------------------

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#fill(org.eclipse.swt.widgets.Menu, int)
	 */
	@Override
	public void fill(Menu menu, int index) {
		IEditorDescriptor defaultEditor = registry
				.findEditor(IDEWorkbenchPlugin.DEFAULT_TEXT_EDITOR_ID);

		IEditorDescriptor iEditorDescriptor = AppUtils.getDefaultEditorDesc();

		IContentType iContentType = Platform.getContentTypeManager()
				.findContentTypeFor(file.getName());

		Object[] editors = registry.getEditors(file.getName(), iContentType);
//		Collections.sort(Arrays.asList(editors), comparer);

		boolean defaultFound = false;

		// Check that we don't add it twice. This is possible
		// if the same editor goes to two mappings.
		ArrayList alreadyMapped = new ArrayList();

		for (int i = 0; i < editors.length; i++) {
			IEditorDescriptor editor = (IEditorDescriptor) editors[i];
			if (!alreadyMapped.contains(editor)) {
				createMenuItem(menu, editor, iEditorDescriptor);
				if (defaultEditor != null
						&& editor.getId().equals(defaultEditor.getId())) {
					defaultFound = true;
				}
				alreadyMapped.add(editor);
			}
		}

		// Only add a separator if there is something to separate
		if (editors.length > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}

		// Add default editor. Check it if it is saved as the preference.
		if (!defaultFound && defaultEditor != null) {
			createMenuItem(menu, defaultEditor, iEditorDescriptor);
		}

		// Add system editor (should never be null)
		IEditorDescriptor descriptor = registry
				.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		createMenuItem(menu, descriptor, iEditorDescriptor);

		// Add system in-place editor (can be null)
		descriptor = registry
				.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
		if (descriptor != null) {
			createMenuItem(menu, descriptor, iEditorDescriptor);
		}
		createDefaultMenuItem(menu, file);

		// add Other... menu item
		createOtherMenuItem(menu);
	}

	// ---------------------------------------------------------------
	/**
	 * Creates the default menu item.
	 *
	 * @param menu
	 *            the menu
	 * @param file
	 *            the file
	 */
	private void createDefaultMenuItem(Menu menu, final File file) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		menuItem.setSelection(true);
		menuItem.setText("&Default Editor");

		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {

						try {
							AppUtils.openEditor(file);
						} catch (Exception e) {

						}
					}
					break;
				}
			}
		};
		menuItem.addListener(SWT.Selection, listener);
	}

	// ---------------------------------------------------------------

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#isDynamic()
	 */
	@Override
	public boolean isDynamic() {
		return true;
	}
}
