package eu.kalafatic.evolution.view;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class OrchestrationNavigatorLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof Orchestrator) {
            String name = ((Orchestrator) element).getName();
            return name != null ? name : "Unnamed Orchestration";
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof Orchestrator) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
        }
        return super.getImage(element);
    }
}
