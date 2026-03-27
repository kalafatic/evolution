package eu.kalafatic.evolution.view.provider;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.osgi.framework.Bundle;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Compiler;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;

public class OrchestrationNavigatorLabelProvider extends LabelProvider {

    private final Map<String, Image> imageCache = new HashMap<>();

    @Override
    public String getText(Object element) {
        if (element instanceof IResource) {
            return ((IResource) element).getName();
        } else if (element instanceof EvoProject) {
            String name = ((EvoProject) element).getName();
            return name != null ? name : "Evo Project";
        } else if (element instanceof Orchestrator) {
            String name = ((Orchestrator) element).getName();
            return name != null ? name : "Unnamed Orchestration";
        } else if (element instanceof Agent) {
            Agent agent = (Agent) element;
            return (agent.getId() != null ? agent.getId() : "Agent") + " (" + (agent.getType() != null ? agent.getType() : "unknown") + ")";
        } else if (element instanceof Task) {
            Task task = (Task) element;
            String status = task.getStatus() != null ? "[" + task.getStatus().toString() + "] " : "";
            return status + (task.getName() != null ? task.getName() : (task.getId() != null ? task.getId() : "Task"));
        } else if (element instanceof Git) {
            return "Git: " + ((Git) element).getBranch();
        } else if (element instanceof Maven) {
            return "Maven";
        } else if (element instanceof LLM) {
            return "LLM: " + ((LLM) element).getModel();
        } else if (element instanceof Compiler) {
            return "Compiler";
        } else if (element instanceof Ollama) {
            return "Ollama: " + ((Ollama) element).getModel();
        } else if (element instanceof AiChat) {
            return "AI Chat";
        } else if (element instanceof NeuronAI) {
            return "Neuron AI: " + ((NeuronAI) element).getModel();
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IProject) {
            IProject project = (IProject) element;
            try {
                if (project.isOpen() && project.hasNature("eu.kalafatic.evolution.view.evolutionNature")) {
                    return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_project.png");
                }
            } catch (CoreException e) {}
            return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
        } else if (element instanceof IFolder) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
        } else if (element instanceof IFile) {
            String ext = ((IFile) element).getFileExtension();
            if ("evo".equals(ext) || "xml".equals(ext)) {
                return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_project.png");
            }
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
        } else if (element instanceof EvoProject) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/evo_project.png");
        } else if (element instanceof Orchestrator) {
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
            return getCachedImage("eu.kalafatic.evolution.view", "icons/sample.png");
        } else if (element instanceof AiChat) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/sample.png");
        } else if (element instanceof NeuronAI) {
            return getCachedImage("eu.kalafatic.evolution.view", "icons/sample.png");
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
