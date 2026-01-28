package eu.kalafatic.evolution.view;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.ITreeContentProvider;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import java.util.Collections;

public class OrchestrationNavigatorContentProvider implements ITreeContentProvider {

    private ResourceSet resourceSet = new ResourceSetImpl();

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IFile) {
            IFile file = (IFile) parentElement;
            if ("xml".equals(file.getFileExtension()) || "evo".equals(file.getFileExtension())) {
                try {
                    URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
                    Resource resource = resourceSet.getResource(uri, true);
                    if (!resource.getContents().isEmpty() && resource.getContents().get(0) instanceof EvoProject) {
                        return ((EvoProject) resource.getContents().get(0)).getOrchestrations().toArray();
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        } else if (parentElement instanceof Orchestrator) {
            return ((Orchestrator) parentElement).getAgents().toArray();
        }
        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof Orchestrator) {
            Orchestrator orch = (Orchestrator) element;
            if (orch.eResource() != null) {
                // This is a bit simplified, usually you'd want to return the IFile
            }
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }
}
