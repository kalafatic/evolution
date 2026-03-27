package eu.kalafatic.evolution.view.provider;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.ITreeContentProvider;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.nature.EvolutionNature;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Compiler;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;

import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrchestrationNavigatorContentProvider implements ITreeContentProvider {

    private ResourceSet resourceSet = new ResourceSetImpl();

    public OrchestrationNavigatorContentProvider() {
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
    }

    @Override
    public Object[] getElements(Object inputElement) {
        // Clear resource set on full refresh to ensure we reload from disk
        if (inputElement instanceof org.eclipse.core.resources.IWorkspaceRoot) {
            for (Resource res : new ArrayList<>(resourceSet.getResources())) {
                res.unload();
            }
            resourceSet.getResources().clear();
        }
        return getChildren(inputElement);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IWorkspaceRoot) {
            IProject[] projects = ((IWorkspaceRoot) parentElement).getProjects();
           List<IProject> evolutionProjects = new ArrayList<>();
            for (IProject project : projects) {
                try {
                    if (project.isOpen() && project.hasNature(EvolutionNature.NATURE_ID)) {
                        evolutionProjects.add(project);
                    }
                } catch (CoreException e) {
                    // Ignore
                }
            }
            return evolutionProjects.toArray();
        } else if (parentElement instanceof IContainer) {
            IContainer container = (IContainer) parentElement;
            try {
                IResource[] members = container.members();
                List<Object> children = new ArrayList<>();
                for (IResource member : members) {
                    if (member instanceof IFile) {
                        EvoProject ep = loadEvoProject((IFile) member);
                        if (ep != null) {
                            children.add(member); // Add the IFile, which will then have the EvoProject as its child
                        }
                    } else if (member instanceof IContainer) {
                        children.add(member);
                    }
                }
                return children.toArray();
            } catch (CoreException e) {
                // Ignore
            }
        } else if (parentElement instanceof IFile) {
            EvoProject ep = loadEvoProject((IFile) parentElement);
            if (ep != null) {
                return new Object[] { ep };
            }
        } else if (parentElement instanceof EvoProject) {
            return ((EvoProject) parentElement).getOrchestrations().toArray();
        } else if (parentElement instanceof Orchestrator) {
            Orchestrator orch = (Orchestrator) parentElement;
            List<Object> children = new ArrayList<>();
            children.addAll(orch.getAgents());
            children.addAll(orch.getTasks());
            if (orch.getGit() != null) children.add(orch.getGit());
            if (orch.getMaven() != null) children.add(orch.getMaven());
            if (orch.getLlm() != null) children.add(orch.getLlm());
            if (orch.getCompiler() != null) children.add(orch.getCompiler());
            if (orch.getOllama() != null) children.add(orch.getOllama());
            if (orch.getAiChat() != null) children.add(orch.getAiChat());
            if (orch.getNeuronAI() != null) children.add(orch.getNeuronAI());
            return children.toArray();
        } else if (parentElement instanceof Task) {
            return ((Task) parentElement).getSubTasks().toArray();
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
        if (element instanceof EvoProject) {
            EvoProject ep = (EvoProject) element;
            if (ep.eResource() != null) {
                URI uri = ep.eResource().getURI();
                if (uri.isPlatformResource()) {
                    String path = uri.toPlatformString(true);
                    return org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().findMember(path);
                }
            }
        }
        if (element instanceof EObject) {
            return ((EObject) element).eContainer();
        } else if (element instanceof IResource) {
            return ((IResource) element).getParent();
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof IWorkspaceRoot) {
            return getChildren(element).length > 0;
        }
        if (element instanceof IFile) {
            return loadEvoProject((IFile) element) != null;
        }
        if (element instanceof IProject) {
            IProject project = (IProject) element;
            try {
                return project.isOpen() && project.hasNature(EvolutionNature.NATURE_ID);
            } catch (CoreException e) {
                return false;
            }
        }
        if (element instanceof Task) {
            return !((Task) element).getSubTasks().isEmpty();
        }
        return getChildren(element).length > 0;
    }
}
