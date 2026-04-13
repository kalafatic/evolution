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
package eu.kalafatic.utils.dnd;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.EditorPart;

import eu.kalafatic.utils.lib.EEncoding;

/**
 * The listener interface for receiving fileTreeViewerDrop events. The class that is interested in processing a fileTreeViewerDrop event implements
 * this interface, and the object created with that class is registered with a component using the component's
 * <code>addFileTreeViewerDropListener<code> method. When
 * the fileTreeViewerDrop event occurs, that object's appropriate
 * method is invoked.
 * @see FileTreeViewerDropEvent
 */
public class FileTreeViewerDropListener {

	/** The torrent editor. */
	private EditorPart editorPart;

	/** The viewer. */
	private Viewer viewer;

	/** The ext torrent. */
	// private ExtTorrent extTorrent;

	/** The transfer types. */
	private final Transfer[] transferTypes = new Transfer[] { FileTransfer.getInstance() };

	/** The operations. */
	private final int operations = DND.DROP_COPY | DND.DROP_LINK;

	/** The right encoding. */
	private boolean rightEncoding = true;

	/** The lock. */
	public final Lock lock = new ReentrantLock(true);

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Instantiates a new file tree viewer drop listener.
	 * @param torrentEditor the torrent editor
	 * @param viewer the viewer
	 */
	public FileTreeViewerDropListener(EditorPart editorPart, Viewer viewer) {
		this.editorPart = editorPart;
		this.viewer = viewer;
		// extTorrent = editorPart.getEditorInput();

		initDropTarget();
	}

	// ---------------------------------------------------------------

	/**
	 * Inits the drop target.
	 */
	private void initDropTarget() {
		DropTarget dropTarget = new DropTarget(viewer.getControl(), operations);
		dropTarget.setTransfer(transferTypes);

		dropTarget.addDropListener(new DropTargetAdapter() {

			@Override
			public void dragEnter(DropTargetEvent event) {

				rightEncoding = true;

				String[] data = (String[]) FileTransfer.getInstance().nativeToJava(event.dataTypes[0]);

				for (int i = 0; i < data.length; i++) {
					File file = new File(data[i]);

					isEncodingOK(file);

					if (!rightEncoding) {

						MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "   Warning",
								"File encoding problem.\nRename files.\n\n" + "Default Gemini charset is " + EEncoding.ISO_1.getLiteral());

						event.detail = DND.DROP_NONE;
						return;
					}

					if (file.exists()) {
						event.detail = DND.DROP_COPY;
					} else {
						event.detail = DND.DROP_NONE;
					}
				}
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
				// String[] filePaths = (String[]) FileTransfer.getInstance().nativeToJava(event.dataTypes[0]);
				// ModelActions.getInstance().addFileToModel(extTorrent, viewer, filePaths);
				//
				// editorPart.setDirty(true);
				// ValidationUtils.INSTANCE.showDirtyDecoration(viewer.getControl());
				super.dropAccept(event);
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Checks if is encoding ok.
	 * @param file the file
	 */
	public void isEncodingOK(File file) {

		if (!rightEncoding) {
			return;
		}

		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				isEncodingOK(child);
			}
		} else {
			try {
				byte[] bytes = file.getAbsolutePath().getBytes();

				String path = new String(bytes, EEncoding.ISO_1.getLiteral());
				file = new File(path);
				if (!file.exists()) {
					rightEncoding = false;
				}
			} catch (UnsupportedEncodingException e) {
				// Log.log(ETMPreferences.MODULE, e);
			}
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Refresh.
	 */
	public void refresh() {
		if (lock.tryLock()) {
			try {
				Display.getDefault().asyncExec(refresh);
			} finally {
				lock.unlock();
			}
		}
	}

	// ---------------------------------------------------------------

	/** The refresh. */
	private final Runnable refresh = new Runnable() {
		@Override
		public void run() {
			lock.lock();
			try {
				if (viewer != null && viewer.getControl() != null && !viewer.getControl().isDisposed() && viewer.getControl().isVisible()) {

					viewer.refresh();
				}
			} catch (Exception e) {
				// e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}
	};
}
