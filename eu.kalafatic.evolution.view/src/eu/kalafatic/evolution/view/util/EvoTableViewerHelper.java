package eu.kalafatic.evolution.view.util;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableItem;

/**
 * Reusable, thread-safe SWT/JFace TableViewer utility helpers for column creation,
 * row reordering, and check selection handling across EVO workbench UI components.
 */
public final class EvoTableViewerHelper {

    private EvoTableViewerHelper() {
        // Utility class constructor
    }

    /**
     * Creates and configures a TableViewerColumn on the specified TableViewer.
     *
     * @param viewer the target TableViewer
     * @param title column header text
     * @param width column width in pixels
     * @param resizable whether the column is resizable
     * @param moveable whether the column is moveable
     * @return the created TableViewerColumn
     */
    public static TableViewerColumn createColumn(TableViewer viewer, String title, int width, boolean resizable, boolean moveable) {
        if (viewer == null || viewer.getTable() == null || viewer.getTable().isDisposed()) {
            throw new IllegalArgumentException("Target TableViewer or underlying Table cannot be null or disposed");
        }
        TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
        col.getColumn().setText(title != null ? title : "");
        col.getColumn().setWidth(width);
        col.getColumn().setResizable(resizable);
        col.getColumn().setMoveable(moveable);
        return col;
    }

    /**
     * Reorders an element in a list by a delta offset without requiring a UI TableViewer instance.
     *
     * @param <T> element type
     * @param list the backing data list
     * @param element the element to move
     * @param delta index shift offset (-1 for up, +1 for down)
     * @param postMoveCallback optional callback executed after swapping elements (e.g. to update sequential order indices)
     * @return true if the element was moved, false otherwise
     */
    public static <T> boolean moveRowInList(List<T> list, T element, int delta, Consumer<List<T>> postMoveCallback) {
        if (list == null || element == null) {
            return false;
        }

        int idx = list.indexOf(element);
        if (idx < 0) {
            return false;
        }

        int targetIdx = idx + delta;
        if (targetIdx < 0 || targetIdx >= list.size()) {
            return false;
        }

        Collections.swap(list, idx, targetIdx);
        if (postMoveCallback != null) {
            postMoveCallback.accept(list);
        }

        return true;
    }

    /**
     * Reorders an element in a list by a delta offset, updates selection, and refreshes the viewer.
     *
     * @param <T> element type
     * @param viewer the TableViewer rendering the list
     * @param list the backing data list
     * @param element the element to move
     * @param delta index shift offset (-1 for up, +1 for down)
     * @param postMoveCallback optional callback executed after swapping elements (e.g. to update sequential order indices)
     * @return true if the row was moved, false otherwise
     */
    public static <T> boolean moveRow(TableViewer viewer, List<T> list, T element, int delta, Consumer<List<T>> postMoveCallback) {
        boolean moved = moveRowInList(list, element, delta, postMoveCallback);
        if (moved && viewer != null && viewer.getTable() != null && !viewer.getTable().isDisposed()) {
            viewer.refresh();
            viewer.setSelection(new StructuredSelection(element), true);
        }
        return moved;
    }

    /**
     * Attaches a check selection listener to the TableViewer's underlying Table control.
     *
     * @param viewer the target TableViewer
     * @param checkConsumer callback receiving (element, isChecked)
     */
    public static void handleCheckSelection(TableViewer viewer, BiConsumer<Object, Boolean> checkConsumer) {
        if (viewer == null || viewer.getTable() == null || viewer.getTable().isDisposed() || checkConsumer == null) {
            return;
        }
        viewer.getTable().addListener(SWT.Selection, event -> {
            if (event.detail == SWT.CHECK && event.item instanceof TableItem item) {
                Object data = item.getData();
                if (data != null) {
                    checkConsumer.accept(data, item.getChecked());
                }
            }
        });
    }
}
