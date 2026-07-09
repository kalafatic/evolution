package eu.kalafatic.evolution.view.editors;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * A safe text editor subclass that guarantees getAdapter(IUndoContext.class)
 * never returns null, preventing AssertionFailedException in global undo/redo action handlers.
 */
public class SafeTextEditor extends TextEditor {
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (IUndoContext.class.equals(adapter)) {
            T context = super.getAdapter(adapter);
            if (context == null) {
                context = adapter.cast(new ObjectUndoContext(this));
            }
            return context;
        }
        return super.getAdapter(adapter);
    }
}
