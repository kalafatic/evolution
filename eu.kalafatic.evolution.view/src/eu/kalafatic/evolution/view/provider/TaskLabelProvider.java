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
        if (!(element instanceof Task)) return null;
        Task task = (Task) element;
        switch (columnIndex) {
            case 0: return "\u25B6"; // Run
            case 1: return task.getScheduledTime();
            case 2: return task.getId();
            case 3: return task.getType();
            case 4: return task.getName();
            case 5: return task.getStatus() != null ? task.getStatus().toString() : "PENDING";
            case 6: return task.getResultSummary();
            case 7: return task.getPrompt();
            case 8: return task.getAttachments() != null ? String.join(", ", task.getAttachments()) : "";
            case 9: return "\u270E"; // Edit
            default: return null;
        }
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
