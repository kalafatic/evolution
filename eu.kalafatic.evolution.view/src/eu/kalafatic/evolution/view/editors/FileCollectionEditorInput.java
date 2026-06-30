package eu.kalafatic.evolution.view.editors;

import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class FileCollectionEditorInput implements IEditorInput {

    private final List<IFile> files;
    private final String name;

    public FileCollectionEditorInput(List<IFile> files, String name) {
        this.files = files;
        this.name = name;
    }

    public List<IFile> getFiles() {
        return files;
    }

    @Override
    public boolean exists() {
        return !files.isEmpty();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return name;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }
}
