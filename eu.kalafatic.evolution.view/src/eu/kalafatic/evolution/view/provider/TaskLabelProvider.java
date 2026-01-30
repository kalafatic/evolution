package eu.kalafatic.evolution.view.provider;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import eu.kalafatic.evolution.model.orchestration.Task;



public class TaskLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof Task) {
            Task task = (Task) element;
            return task.getName();
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof Task) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
        }
        return super.getImage(element);
    }
}
