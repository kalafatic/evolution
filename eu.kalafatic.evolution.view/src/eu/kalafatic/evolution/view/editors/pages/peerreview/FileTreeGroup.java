package eu.kalafatic.evolution.view.editors.pages.peerreview;

import java.io.File;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class FileTreeGroup extends AEvoGroup {
    private TreeViewer treeViewer;

    public FileTreeGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Changed Files", 1, true);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.widthHint = 200;
        group.setLayoutData(gd);

        treeViewer = new TreeViewer(group, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
        treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        treeViewer.setContentProvider(new FileTreeContentProvider());
        treeViewer.setLabelProvider(new FileTreeLabelProvider());
    }

    @Override
    public void refreshUI() {
        if (treeViewer != null && !treeViewer.getControl().isDisposed()) {
            treeViewer.refresh();
        }
    }

    public TreeViewer getTreeViewer() {
        return treeViewer;
    }

    private class FileTreeContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof java.util.List) return ((java.util.List<?>) inputElement).toArray();
            if (inputElement instanceof File[]) return (File[]) inputElement;
            return new Object[0];
        }
        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof File && ((File) parentElement).isDirectory()) return ((File) parentElement).listFiles();
            return new Object[0];
        }
        @Override public Object getParent(Object element) { return ((File) element).getParentFile(); }
        @Override public boolean hasChildren(Object element) { return element instanceof File && ((File) element).isDirectory(); }
    }

    private class FileTreeLabelProvider extends LabelProvider {
        @Override
        public String getText(Object element) {
            if (element instanceof String) return (String) element;
            return ((File) element).getName();
        }
        @Override
        public Image getImage(Object element) {
            if (element instanceof String) return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
            String imageKey = ((File) element).isDirectory() ? ISharedImages.IMG_OBJ_FOLDER : ISharedImages.IMG_OBJ_FILE;
            return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        }
    }
}
