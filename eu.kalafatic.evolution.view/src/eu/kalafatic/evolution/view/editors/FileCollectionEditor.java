package eu.kalafatic.evolution.view.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.core.resources.IFile;

/**
 * Multi-page editor that opens a collection of files, each in its own tab.
 */
public class FileCollectionEditor extends MultiPageEditorPart {

    public static final String ID = "eu.kalafatic.evolution.view.editors.FileCollectionEditor";

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (!(input instanceof FileCollectionEditorInput)) {
            throw new PartInitException("Invalid Input: Must be FileCollectionEditorInput");
        }
        super.init(site, input);
        setPartName(input.getName());
    }

    @Override
    protected void createPages() {
        FileCollectionEditorInput input = (FileCollectionEditorInput) getEditorInput();
        for (IFile file : input.getFiles()) {
            try {
                TextEditor editor = new SafeTextEditor();
                int index = addPage(editor, new FileEditorInput(file));
                setPageText(index, file.getName());
            } catch (PartInitException e) {
                // Log error
            }
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        for (int i = 0; i < getPageCount(); i++) {
            if (getEditor(i) != null) {
                getEditor(i).doSave(monitor);
            }
        }
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }
}
