package eu.kalafatic.evolution.view.provider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.ITreeContentProvider;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.view.nature.EvolutionNature;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrchestrationNavigatorContentProvider implements ITreeContentProvider {

    private ResourceSet resourceSet = new ResourceSetImpl();

    public OrchestrationNavigatorContentProvider() {
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IProject) {
            IProject project = (IProject) parentElement;
            try {
                if (project.isOpen() && project.hasNature(EvolutionNature.NATURE_ID)) {
                    List<EvoProject> results = new ArrayList<>();
                    // Only scan root level for performance
                    for (IResource res : project.members()) {
                        if (res instanceof IFile) {
                            EvoProject ep = loadEvoProject((IFile) res);
                            if (ep != null) {
                                results.add(ep);
                            }
                        }
                    }
                    return results.toArray();
                }
            } catch (CoreException e) {
                // Ignore
            }
        } else if (parentElement instanceof EvoProject) {
            return ((EvoProject) parentElement).getOrchestrations().toArray();
        } else if (parentElement instanceof Orchestrator) {
            return ((Orchestrator) parentElement).getAgents().toArray();
        }
        return new Object[0];
    }

    private EvoProject loadEvoProject(IFile file) {
        String ext = file.getFileExtension();
        if ("xml".equals(ext) || "evo".equals(ext)) {
            try {
                URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
                Resource resource = resourceSet.getResource(uri, true);
                if (!resource.getContents().isEmpty() && resource.getContents().get(0) instanceof EvoProject) {
                    return (EvoProject) resource.getContents().get(0);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof Agent) {
            return ((Agent) element).eContainer();
        } else if (element instanceof Orchestrator) {
            return ((Orchestrator) element).eContainer();
        } else if (element instanceof EvoProject) {
            EvoProject ep = (EvoProject) element;
            if (ep.eResource() != null) {
                URI uri = ep.eResource().getURI();
                if (uri.isPlatformResource()) {
                    String path = uri.toPlatformString(true);
                    IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(path));
                    if (res != null) {
                        return res.getProject();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof IProject) {
            IProject project = (IProject) element;
            try {
                return project.isOpen() && project.hasNature(EvolutionNature.NATURE_ID);
            } catch (CoreException e) {
                return false;
            }
        }
        return getChildren(element).length > 0;
    }
}
