# PACKAGE CONTEXT

## Directory: eu.kalafatic.utils/src/eu/kalafatic/utils/dnd/

## Domain: general

## Components
* `DragProvider.java`: package eu.kalafatic.utils.dnd; import java.io.File; import java.util.concurrent.locks.Lock; import java.util.concurrent.locks.ReentrantLock; import org.eclipse.jface.viewers.ISelection; import org.eclipse.jface.viewers.TreeSelection; import org.eclipse.jface.viewers.Viewer; import org.eclipse.swt.dnd.DND; import org.eclipse.swt.dnd.DragSourceEvent; import org.eclipse.swt.dnd.DragSourceListener; import org.eclipse.swt.dnd.FileTransfer; import org.eclipse.swt.widgets.Display; import eu.kalafatic.utils.interfaces.IViewer; public class DragProvider implements DragSourceListener { private final Viewer viewer; public final Lock lock = new ReentrantLock(true); public DragProvider(IViewer iViewer) { this.viewer = (Viewer) iViewer.getViewer(); }
* `FileTreeViewerDropListener.java`: package eu.kalafatic.utils.dnd; import java.io.File; import java.io.UnsupportedEncodingException; import java.util.concurrent.locks.Lock; import java.util.concurrent.locks.ReentrantLock; import org.eclipse.jface.dialogs.MessageDialog; import org.eclipse.jface.viewers.Viewer; import org.eclipse.swt.dnd.DND; import org.eclipse.swt.dnd.DropTarget; import org.eclipse.swt.dnd.DropTargetAdapter; import org.eclipse.swt.dnd.DropTargetEvent; import org.eclipse.swt.dnd.FileTransfer; import org.eclipse.swt.dnd.Transfer; import org.eclipse.swt.widgets.Display; import org.eclipse.ui.part.EditorPart; import eu.kalafatic.utils.lib.EEncoding; public class FileTreeViewerDropListener { private EditorPart editorPart; private Viewer viewer;
