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
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Ollama.gif");
        } else if (element instanceof AiChat) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/AiChat.gif");
        } else if (element instanceof NeuronAI) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/NeuronAI.gif");
        } else if (element instanceof Database) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Database.gif");
        } else if (element instanceof SelfDevSession) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/SelfDevSession.gif");
        } else if (element instanceof Iteration) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Iteration.gif");
        } else if (element instanceof EvaluationResult) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/EvaluationResult.gif");
        } else if (element instanceof Test) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Task.gif");
        } else if (element instanceof Eclipse) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/Eclipse.gif");
        } else if (element instanceof FileConfig) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/FileConfig.gif");
        } else if (element instanceof AccessRule) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/AccessRule.gif");
        } else if (element instanceof NetworkRule) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/NetworkRule.gif");
        } else if (element instanceof MemoryRule) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/MemoryRule.gif");
        } else if (element instanceof SecretRule) {
            return getCachedImage("eu.kalafatic.evolution.model.edit", "icons/full/obj16/SecretRule.gif");
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
            Agent a = (Agent) element;
            return "Agent: " + a.getId() + " [" + a.getType() + "]";
        } else if (element instanceof NeuronAI) {
            return "Neuron AI: " + ((NeuronAI) element).getModel();
        } else if (element instanceof Git) {
            Git g = (Git) element;
            return "Git: " + g.getBranch() + " (" + g.getRepositoryUrl() + ")";
        } else if (element instanceof Database) {
            return "DB: " + ((Database) element).getUrl();
        } else if (element instanceof SelfDevSession) {
            return "Session: " + ((SelfDevSession) element).getId();
        } else if (element instanceof Iteration) {
            Iteration i = (Iteration) element;
            return "Iteration: " + i.getId() + " (" + i.getPhase() + ")";
        } else if (element instanceof Test) {
            Test t = (Test) element;
            return "Test: " + t.getName() + " (" + t.getStatus() + ")";
        } else if (element instanceof Rule) {
            return "Rule: " + ((Rule) element).getName();
        } else if (element instanceof Eclipse) {
            return "Eclipse: " + ((Eclipse) element).getWorkspace();
        } else if (element instanceof FileConfig) {
            return "File: " + ((FileConfig) element).getLocalPath();
        } else if (element instanceof EvaluationResult) {
            EvaluationResult er = (EvaluationResult) element;
            return "Evaluation: " + (er.isSuccess() ? "PASS" : "FAIL");
        }
        return "";
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
                return getColor("TASK_RUNNING", new RGB(255, 255, 150));
            } else if (t.getStatus() == TaskStatus.DONE) {
                return getColor("TASK_DONE", new RGB(150, 255, 150));
            } else if (t.getStatus() == TaskStatus.FAILED) {
                return getColor("TASK_FAILED", new RGB(255, 150, 150));
            } else if (t.getStatus() == TaskStatus.PENDING) {
                return getColor("TASK_PENDING", new RGB(240, 240, 240));
            } else if (t.getStatus() == TaskStatus.WAITING_FOR_APPROVAL) {
                return getColor("TASK_WAITING_APPROVAL", new RGB(255, 200, 100));
            }
        } else if (entity instanceof Test) {
            Test t = (Test) entity;
            if (t.getStatus() == TestStatus.PASSED) {
                return getColor("TEST_PASSED", new RGB(150, 255, 150));
            } else if (t.getStatus() == TestStatus.FAILED) {
                return getColor("TEST_FAILED", new RGB(255, 150, 150));
            } else if (t.getStatus() == TestStatus.RUNNING) {
                return getColor("TEST_RUNNING", new RGB(255, 255, 150));
            } else {
                return getColor("TEST_PENDING", new RGB(240, 240, 240));
            }
        } else if (entity instanceof Orchestrator) {
            return getColor("ORCHESTRATOR_BG", new RGB(180, 180, 255));
        } else if (entity instanceof Agent) {
            return getColor("AGENT_BG", new RGB(220, 220, 220));
        } else if (entity instanceof NeuronAI || entity instanceof LLM || entity instanceof Ollama || entity instanceof AiChat) {
            return getColor("AI_BG", new RGB(255, 220, 180));
        } else if (entity instanceof Git || entity instanceof Maven || entity instanceof Compiler || entity instanceof Database || entity instanceof FileConfig || entity instanceof Eclipse) {
            return getColor("TOOL_BG", new RGB(220, 255, 255));
        } else if (entity instanceof SelfDevSession || entity instanceof Iteration || entity instanceof EvaluationResult) {
            return getColor("SESSION_BG", new RGB(230, 210, 255));
        } else if (entity instanceof Rule) {
            return getColor("RULE_BG", new RGB(255, 255, 200));
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
