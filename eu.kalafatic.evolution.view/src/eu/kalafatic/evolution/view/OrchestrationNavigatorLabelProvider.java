package eu.kalafatic.evolution.view;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Agent;

public class OrchestrationNavigatorLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof Orchestrator) {
            String name = ((Orchestrator) element).getName();
            return name != null ? name : "Unnamed Orchestration";
        } else if (element instanceof Agent) {
            Agent agent = (Agent) element;
            return agent.getId() + " (" + agent.getType() + ")";
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof Orchestrator) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
        } else if (element instanceof Agent) {
            // Using a different icon for Agents
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEF_VIEW);
        }
        return super.getImage(element);
    }
}
