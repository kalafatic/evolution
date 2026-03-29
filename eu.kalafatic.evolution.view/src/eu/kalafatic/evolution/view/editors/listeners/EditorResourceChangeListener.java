package eu.kalafatic.evolution.view.editors.listeners;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.swt.widgets.Display;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class EditorResourceChangeListener implements IResourceChangeListener {
    private MultiPageEditor editor;

    public EditorResourceChangeListener(MultiPageEditor editor) {
        this.editor = editor;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
            Display.getDefault().asyncExec(() -> {
                if (editor.getSite() != null && editor.getSite().getPage() != null) {
                    editor.getSite().getPage().closeEditor(editor, false);
                }
            });
        }
    }
}
