package eu.kalafatic.evolution.view.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;

public class EditorPageFactory {
    public static Text createEditorPage(MultiPageEditor editor, String content) throws PartInitException {
        Text text = new Text(editor.getContainer(), SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setText(content);
        int index = editor.addPage(text);
        editor.setPageText(index, "Editor");
        return text;
    }
}
