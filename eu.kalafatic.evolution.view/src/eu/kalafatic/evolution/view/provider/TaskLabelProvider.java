package eu.kalafatic.evolution.view.provider;

import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import eu.kalafatic.evolution.model.orchestration.Task;



public class TaskLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public String getColumnText(Object element, int columnIndex) {
        if (columnIndex == 0) {
            if (element instanceof Task) {
                Task task = (Task) element;
                return (task.getName() != null ? task.getName() : (task.getId() != null ? task.getId() : "Task"));
            }
        } else if (columnIndex == 1) {
            if (element instanceof Task) {
                Task task = (Task) element;
                return task.getStatus() != null ? task.getStatus().toString() : "PENDING";
            }
        }
        return null;
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        if (columnIndex == 0) {
            return getImage(element);
        }
        return null;
    }

    @Override
    public String getText(Object element) {
        if (element instanceof Task) {
            Task task = (Task) element;
            String status = task.getStatus() != null ? "[" + task.getStatus().toString() + "] " : "";
            return status + (task.getName() != null ? task.getName() : (task.getId() != null ? task.getId() : "Task"));
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof Task) {
            Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.view");
            if (bundle != null) {
                URL url = bundle.getEntry("icons/evo_task.svg");
                if (url != null) {
                    return ImageDescriptor.createFromURL(url).createImage();
                }
            }
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
        }
        return super.getImage(element);
    }
}
