package eu.kalafatic.evolution.view.provider;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class OrchestrationNavigatorLabelProvider extends LabelProvider {

    private final Map<String, Image> imageCache = new HashMap<>();

    @Override
    public String getText(Object element) {
        if (element instanceof EvoProject) {
            String name = ((EvoProject) element).getName();
            return name != null ? name : "Evo Project";
        } else if (element instanceof Orchestrator) {
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
        if (element instanceof EvoProject) {
            return getCachedImage("icons/evo_project.png");
        } else if (element instanceof Orchestrator) {
            return getCachedImage("icons/orchestrator.png");
        } else if (element instanceof Agent) {
            return getCachedImage("icons/agent.png");
        }
        return super.getImage(element);
    }

    private Image getCachedImage(String path) {
        Image image = imageCache.get(path);
        if (image == null) {
            Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.view");
            if (bundle != null) {
                URL url = bundle.getEntry(path);
                if (url != null) {
                    image = ImageDescriptor.createFromURL(url).createImage();
                    imageCache.put(path, image);
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
