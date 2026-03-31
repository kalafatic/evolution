package eu.kalafatic.evolution.view.provider;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Compiler;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.AiChat;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;
import org.osgi.framework.Bundle;

public class OrchestrationGraphLabelProvider extends LabelProvider implements IEntityStyleProvider {

    private ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
    private final Map<String, Image> imageCache = new HashMap<>();

    private Color getColor(String key, RGB rgb) {
        if (!colorRegistry.hasValueFor(key)) {
            colorRegistry.put(key, rgb);
        }
        return colorRegistry.get(key);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof Orchestrator) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Orchestrator.gif");
        } else if (element instanceof Agent) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Agent.gif");
        } else if (element instanceof Task) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Task.gif");
        } else if (element instanceof Git) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Git.gif");
        } else if (element instanceof Maven) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Maven.gif");
        } else if (element instanceof LLM) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/LLM.gif");
        } else if (element instanceof Compiler) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Compiler.gif");
        } else if (element instanceof Ollama) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/orchestrator.png");
        } else if (element instanceof AiChat) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/orchestrator.png");
        } else if (element instanceof NeuronAI) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/orchestrator.png");
        }
        return super.getImage(element);
    }

    private Image getCachedImage(String bundleId, String path) {
        String cacheKey = bundleId + "/" + path;
        Image image = imageCache.get(cacheKey);
        if (image == null) {
            Bundle bundle = Platform.getBundle(bundleId);
            if (bundle != null) {
                URL url = bundle.getEntry(path);
                if (url != null) {
                    image = ImageDescriptor.createFromURL(url).createImage();
                    imageCache.put(cacheKey, image);
                }
            }
        }
        return image;
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
        } else if (element instanceof NeuronAI) {
            return "Neuron AI: " + ((NeuronAI) element).getModel();
        }
        return "";//super.getText(element);
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
                return getColor("TASK_RUNNING", new RGB(255, 255, 150)); // More saturated Yellow
            } else if (t.getStatus() == TaskStatus.DONE) {
                return getColor("TASK_DONE", new RGB(150, 255, 150)); // More saturated Green
            } else if (t.getStatus() == TaskStatus.FAILED) {
                return getColor("TASK_FAILED", new RGB(255, 150, 150)); // More saturated Red
            } else if (t.getStatus() == TaskStatus.PENDING) {
                return getColor("TASK_PENDING", new RGB(240, 240, 240)); // Light Gray
            } else if (t.getStatus() == TaskStatus.WAITING_FOR_APPROVAL) {
                return getColor("TASK_WAITING_APPROVAL", new RGB(255, 200, 100)); // Orange
            }
        } else if (entity instanceof Orchestrator) {
            return getColor("ORCHESTRATOR_BG", new RGB(180, 180, 255)); // Light Blue
        } else if (entity instanceof Agent) {
            return getColor("AGENT_BG", new RGB(220, 220, 220)); // Grayish
        } else if (entity instanceof NeuronAI) {
            return getColor("NEURON_AI_BG", new RGB(255, 180, 255)); // Pinkish
        } else if (entity instanceof Git || entity instanceof Maven || entity instanceof Compiler) {
            return getColor("TOOL_BG", new RGB(220, 255, 255)); // Cyan-ish
        } else if (entity instanceof LLM || entity instanceof Ollama || entity instanceof AiChat) {
            return getColor("AI_BG", new RGB(255, 220, 180)); // Orange-ish
        }
        return null;
    }

    @Override
    public Color getForegroundColour(Object entity) {
        return null;
    }

    public boolean fishEyeHighlightNode(Object entity) {
        return false;
    }

	@Override
	public IFigure getTooltip(Object entity) {
		if (entity instanceof Task) {
			Task t = (Task) entity;
			Figure tooltipFigure = new Figure();
			tooltipFigure.setLayoutManager(new ToolbarLayout());

			tooltipFigure.add(new Label("Status: " + t.getStatus()));
			if (t.getResponse() != null && !t.getResponse().isEmpty()) {
				tooltipFigure.add(new Label("Response: " + truncate(t.getResponse(), 200)));
			}
			if (t.getFeedback() != null && !t.getFeedback().isEmpty()) {
				tooltipFigure.add(new Label("Feedback: " + truncate(t.getFeedback(), 200)));
			}
			return tooltipFigure;
		}
		return null;
	}

	private String truncate(String text, int maxLength) {
		if (text == null || text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength) + "...";
	}

	@Override
	public boolean fisheyeNode(Object entity) {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public void dispose() {
        for (Image image : imageCache.values()) {
            if (image != null && !image.isDisposed()) {
                image.dispose();
            }
        }
        imageCache.clear();
        super.dispose();
    }
}
