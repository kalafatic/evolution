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
import eu.kalafatic.evolution.model.orchestration.Database;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Test;
import eu.kalafatic.evolution.model.orchestration.TestStatus;
import eu.kalafatic.evolution.model.orchestration.Eclipse;
import eu.kalafatic.evolution.model.orchestration.FileConfig;
import eu.kalafatic.evolution.model.orchestration.Rule;
import eu.kalafatic.evolution.model.orchestration.AccessRule;
import eu.kalafatic.evolution.model.orchestration.NetworkRule;
import eu.kalafatic.evolution.model.orchestration.MemoryRule;
import eu.kalafatic.evolution.model.orchestration.SecretRule;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_orchestrate.svg");
        } else if (element instanceof Agent) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_model.svg");
        } else if (element instanceof Task) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_task.svg");
        } else if (element instanceof Git) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof Maven) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_graph.svg");
        } else if (element instanceof LLM) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof Compiler) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_stack.svg");
        } else if (element instanceof Ollama) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof AiChat) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_chat.svg");
        } else if (element instanceof NeuronAI) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof Database) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof SelfDevSession) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_update.svg");
        } else if (element instanceof Iteration) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_diagram.svg");
        } else if (element instanceof EvaluationResult) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_task.svg");
        } else if (element instanceof Test) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_task.svg");
        } else if (element instanceof Eclipse) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof FileConfig) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_editor.svg");
        } else if (element instanceof AccessRule) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof NetworkRule) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof MemoryRule) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
        } else if (element instanceof SecretRule) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_settings.svg");
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
        StringBuilder sb = new StringBuilder();
        if (element instanceof Orchestrator) {
            Orchestrator o = (Orchestrator) element;
            sb.append("Orchestrator: ").append(o.getName());
        } else if (element instanceof Task) {
            Task t = (Task) element;
            sb.append(t.getName()).append(" (").append(t.getStatus()).append(")");
        } else if (element instanceof Agent) {
            Agent a = (Agent) element;
            sb.append("Agent: ").append(a.getId()).append(" [").append(a.getType()).append("]");
        } else if (element instanceof NeuronAI) {
            sb.append("Neuron AI: ").append(((NeuronAI) element).getModel());
        } else if (element instanceof Git) {
            Git g = (Git) element;
            sb.append("Git: ").append(g.getBranch()).append(" (").append(g.getRepositoryUrl()).append(")");
        } else if (element instanceof Database) {
            sb.append("DB: ").append(((Database) element).getUrl());
        } else if (element instanceof SelfDevSession) {
            sb.append("Session: ").append(((SelfDevSession) element).getId());
        } else if (element instanceof Iteration) {
            Iteration i = (Iteration) element;
            sb.append("Iteration: ").append(i.getId()).append(" (").append(i.getPhase()).append(")");
        } else if (element instanceof Test) {
            Test t = (Test) element;
            sb.append("Test: ").append(t.getName()).append(" (").append(t.getStatus()).append(")");
        } else if (element instanceof Rule) {
            sb.append("Rule: ").append(((Rule) element).getName());
        } else if (element instanceof Eclipse) {
            sb.append("Eclipse: ").append(((Eclipse) element).getWorkspace());
        } else if (element instanceof FileConfig) {
            sb.append("File: ").append(((FileConfig) element).getLocalPath());
        } else if (element instanceof EvaluationResult) {
            EvaluationResult er = (EvaluationResult) element;
            sb.append("Evaluation: ").append(er.isSuccess() ? "PASS" : "FAIL");
        } else {
            return "";
        }

        // Add child summary
        String childrenSummary = getChildrenSummary(element);
        if (!childrenSummary.isEmpty()) {
            sb.append("\n").append(childrenSummary);
        }

        return sb.toString();
    }

    private String getChildrenSummary(Object element) {
        List<String> summaries = new ArrayList<>();
        if (element instanceof Orchestrator) {
            Orchestrator o = (Orchestrator) element;
            if (!o.getAgents().isEmpty()) summaries.add("🤖" + o.getAgents().size());
            if (!o.getTasks().isEmpty()) summaries.add("📝" + o.getTasks().size());
            if (!o.getTests().isEmpty()) summaries.add("🧪" + o.getTests().size());
        } else if (element instanceof Agent) {
            Agent a = (Agent) element;
            if (!a.getTasks().isEmpty()) summaries.add("📝" + a.getTasks().size());
            if (!a.getRules().isEmpty()) summaries.add("⚖️" + a.getRules().size());
        } else if (element instanceof Task) {
            Task t = (Task) element;
            if (!t.getSubTasks().isEmpty()) summaries.add("📝" + t.getSubTasks().size());
            if (!t.getNext().isEmpty()) summaries.add("➡️" + t.getNext().size());
        } else if (element instanceof SelfDevSession) {
            SelfDevSession s = (SelfDevSession) element;
            if (!s.getIterations().isEmpty()) summaries.add("🔄" + s.getIterations().size());
        } else if (element instanceof Iteration) {
            Iteration i = (Iteration) element;
            if (!i.getTasks().isEmpty()) summaries.add("📝" + i.getTasks().size());
        }

        if (summaries.isEmpty()) return "";
        return String.join(" ", summaries);
    }

    private boolean hasChildren(Object element) {
        if (element instanceof Orchestrator) return true;
        if (element instanceof Agent) {
            Agent a = (Agent) element;
            return !a.getTasks().isEmpty() || !a.getRules().isEmpty();
        }
        if (element instanceof Task) {
            Task t = (Task) element;
            return !t.getSubTasks().isEmpty() || !t.getNext().isEmpty();
        }
        if (element instanceof SelfDevSession) {
            SelfDevSession s = (SelfDevSession) element;
            return !s.getIterations().isEmpty();
        }
        if (element instanceof Iteration) {
            Iteration i = (Iteration) element;
            return !i.getTasks().isEmpty() || i.getEvaluationResult() != null;
        }
        return false;
    }

    @Override
    public Color getNodeHighlightColor(Object entity) {
        return null;
    }

    @Override
    public Color getBorderColor(Object entity) {
        if (hasChildren(entity)) {
            return getColor("PARENT_BORDER", new RGB(50, 50, 50));
        }
        return null;
    }

    @Override
    public Color getBorderHighlightColor(Object entity) {
        return getColor("BORDER_HIGHLIGHT", new RGB(0, 0, 255));
    }

    @Override
    public int getBorderWidth(Object entity) {
        return hasChildren(entity) ? 2 : 1;
    }

    @Override
    public Color getBackgroundColour(Object entity) {
        Color baseColor = null;
        if (entity instanceof Task) {
            Task t = (Task) entity;
            if (t.getStatus() == TaskStatus.RUNNING) {
                baseColor = getColor("TASK_RUNNING", new RGB(255, 255, 150));
            } else if (t.getStatus() == TaskStatus.DONE) {
                baseColor = getColor("TASK_DONE", new RGB(150, 255, 150));
            } else if (t.getStatus() == TaskStatus.FAILED) {
                baseColor = getColor("TASK_FAILED", new RGB(255, 150, 150));
            } else if (t.getStatus() == TaskStatus.PENDING) {
                baseColor = getColor("TASK_PENDING", new RGB(240, 240, 240));
            } else if (t.getStatus() == TaskStatus.WAITING_FOR_APPROVAL) {
                baseColor = getColor("TASK_WAITING_APPROVAL", new RGB(255, 200, 100));
            }
        } else if (entity instanceof Test) {
            Test t = (Test) entity;
            if (t.getStatus() == TestStatus.PASSED) {
                baseColor = getColor("TEST_PASSED", new RGB(150, 255, 150));
            } else if (t.getStatus() == TestStatus.FAILED) {
                baseColor = getColor("TEST_FAILED", new RGB(255, 150, 150));
            } else if (t.getStatus() == TestStatus.RUNNING) {
                baseColor = getColor("TEST_RUNNING", new RGB(255, 255, 150));
            } else {
                baseColor = getColor("TEST_PENDING", new RGB(240, 240, 240));
            }
        } else if (entity instanceof Orchestrator) {
            baseColor = getColor("ORCHESTRATOR_BG", new RGB(180, 180, 255));
        } else if (entity instanceof Agent) {
            baseColor = getColor("AGENT_BG", new RGB(220, 220, 220));
        } else if (entity instanceof NeuronAI || entity instanceof LLM || entity instanceof Ollama || entity instanceof AiChat) {
            baseColor = getColor("AI_BG", new RGB(255, 220, 180));
        } else if (entity instanceof Git || entity instanceof Maven || entity instanceof Compiler || entity instanceof Database || entity instanceof FileConfig || entity instanceof Eclipse) {
            baseColor = getColor("TOOL_BG", new RGB(220, 255, 255));
        } else if (entity instanceof SelfDevSession || entity instanceof Iteration || entity instanceof EvaluationResult) {
            baseColor = getColor("SESSION_BG", new RGB(230, 210, 255));
        } else if (entity instanceof Rule) {
            baseColor = getColor("RULE_BG", new RGB(255, 255, 200));
        }

        if (baseColor != null && hasChildren(entity)) {
            // Darken the color slightly for parents
            RGB rgb = baseColor.getRGB();
            return getColor("PARENT_" + entity.getClass().getSimpleName(),
                new RGB(Math.max(0, rgb.red - 20), Math.max(0, rgb.green - 20), Math.max(0, rgb.blue - 20)));
        }

        return baseColor;
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
		} else if (entity instanceof Iteration) {
            Iteration i = (Iteration) entity;
            Figure tooltipFigure = new Figure();
            tooltipFigure.setLayoutManager(new ToolbarLayout());
            tooltipFigure.add(new Label("ID: " + i.getId()));
            tooltipFigure.add(new Label("Status: " + i.getStatus()));
            tooltipFigure.add(new Label("Phase: " + i.getPhase()));
            if (i.getRationale() != null && !i.getRationale().isEmpty()) {
                tooltipFigure.add(new Label("Rationale: " + truncate(i.getRationale(), 200)));
            }
            return tooltipFigure;
        } else if (entity instanceof EvaluationResult) {
            EvaluationResult er = (EvaluationResult) entity;
            Figure tooltipFigure = new Figure();
            tooltipFigure.setLayoutManager(new ToolbarLayout());
            tooltipFigure.add(new Label("Success: " + er.isSuccess()));
            tooltipFigure.add(new Label("Test Pass Rate: " + er.getTestPassRate()));
            tooltipFigure.add(new Label("Coverage Change: " + er.getCoverageChange()));
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
