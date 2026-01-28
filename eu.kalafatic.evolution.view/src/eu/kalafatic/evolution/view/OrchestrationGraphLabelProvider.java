package eu.kalafatic.evolution.view;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;

public class OrchestrationGraphLabelProvider extends LabelProvider implements IEntityStyleProvider {

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
                return new Color(Display.getDefault(), 255, 255, 200); // Yellow
            } else if (t.getStatus() == TaskStatus.DONE) {
                return new Color(Display.getDefault(), 200, 255, 200); // Green
            } else if (t.getStatus() == TaskStatus.FAILED) {
                return new Color(Display.getDefault(), 255, 200, 200); // Red
            }
        } else if (entity instanceof Orchestrator) {
            return new Color(Display.getDefault(), 200, 200, 255); // Blue
        } else if (entity instanceof Agent) {
            return new Color(Display.getDefault(), 230, 230, 230); // Grey
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
