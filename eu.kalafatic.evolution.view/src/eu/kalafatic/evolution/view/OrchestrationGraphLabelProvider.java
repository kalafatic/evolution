package eu.kalafatic.evolution.view;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;

public class OrchestrationGraphLabelProvider extends LabelProvider implements IEntityStyleProvider {

    private ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

    private Color getColor(String key, RGB rgb) {
        if (!colorRegistry.hasValueFor(key)) {
            colorRegistry.put(key, rgb);
        }
        return colorRegistry.get(key);
    }

    @Override
    public String getText(Object element) {
        if (element instanceof Orchestrator) {
            return "Orchestrator: " + ((Orchestrator) element).getName();
        } else if (element instanceof Task) {
            Task t = (Task) element;
            return t.getName() + " (" + t.getStatus() + ")";
        } else if (element instanceof Agent) {
            return "Agent: " + ((Agent) element).getId();
        }
        return super.getText(element);
    }

    @Override
    public Color getNodeHighlightColor(Object entity) {
        return null;
    }

    @Override
    public Color getBorderColor(Object entity) {
        return null;
    }

    @Override
    public Color getBorderHighlightColor(Object entity) {
        return null;
    }

    @Override
    public int getBorderWidth(Object entity) {
        return 0;
    }

    @Override
    public Color getBackgroundColour(Object entity) {
        if (entity instanceof Task) {
            Task t = (Task) entity;
            if (t.getStatus() == TaskStatus.RUNNING) {
                return getColor("TASK_RUNNING", new RGB(255, 255, 200)); // Yellow
            } else if (t.getStatus() == TaskStatus.DONE) {
                return getColor("TASK_DONE", new RGB(200, 255, 200)); // Green
            } else if (t.getStatus() == TaskStatus.FAILED) {
                return getColor("TASK_FAILED", new RGB(255, 200, 200)); // Red
            }
        } else if (entity instanceof Orchestrator) {
            return getColor("ORCHESTRATOR_BG", new RGB(200, 200, 255)); // Blue
        } else if (entity instanceof Agent) {
            return getColor("AGENT_BG", new RGB(230, 230, 230)); // Grey
        }
        return null;
    }

    @Override
    public Color getForegroundColour(Object entity) {
        return null;
    }

    @Override
    public boolean fishEyeHighlightNode(Object entity) {
        return false;
    }
}
