package eu.kalafatic.evolution.view.provider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jface.viewers.ITreeContentProvider;

import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.Compiler;
import eu.kalafatic.evolution.model.orchestration.Database;
import eu.kalafatic.evolution.model.orchestration.Eclipse;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.FileConfig;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.nature.EvolutionNature;

public class OrchestrationNavigatorContentProvider implements ITreeContentProvider {

    public static class ModelProperty {
        public final EObject owner;
        public final EAttribute attribute;
        public final String label;

        public ModelProperty(EObject owner, EAttribute attribute, String label) {
            this.owner = owner;
            this.attribute = attribute;
            this.label = label;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ModelProperty other = (ModelProperty) obj;
            return owner.equals(other.owner) && attribute.equals(other.attribute);
        }

        @Override
        public int hashCode() {
            return 31 * owner.hashCode() + attribute.hashCode();
        }
    }

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
                        IFile file = (IFile) member;
                        String ext = file.getFileExtension();
                        if (isCommonExtension(ext)) {
                            children.add(member);
                        } else {
                            EvoProject ep = loadEvoProject(file);
                            if (ep != null) {
                                children.add(member);
                            }
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
            children.add(new ModelProperty(orch, OrchestrationPackage.Literals.ORCHESTRATOR__AI_MODE, "AI Mode"));
            children.add(new ModelProperty(orch, OrchestrationPackage.Literals.ORCHESTRATOR__REMOTE_MODEL, "Remote Model"));
            children.add(new ModelProperty(orch, OrchestrationPackage.Literals.ORCHESTRATOR__LOCAL_MODEL, "Local Model"));
            children.add(new ModelProperty(orch, OrchestrationPackage.Literals.ORCHESTRATOR__HYBRID_MODEL, "Hybrid Model"));
            children.add(new ModelProperty(orch, OrchestrationPackage.Literals.ORCHESTRATOR__DARWIN_MODE, "Darwin Mode"));
            children.add(new ModelProperty(orch, OrchestrationPackage.Literals.ORCHESTRATOR__OFFLINE_MODE, "Offline Mode"));

            children.addAll(orch.getAgents());
            children.addAll(orch.getTasks());
            if (orch.getGit() != null) children.add(orch.getGit());
            if (orch.getMaven() != null) children.add(orch.getMaven());
            if (orch.getLlm() != null) children.add(orch.getLlm());
            if (orch.getCompiler() != null) children.add(orch.getCompiler());
            if (orch.getOllama() != null) children.add(orch.getOllama());
            if (orch.getAiChat() != null) children.add(orch.getAiChat());
            if (orch.getNeuronAI() != null) children.add(orch.getNeuronAI());
            if (orch.getSupervisorSettings() != null) children.add(orch.getSupervisorSettings());
            return children.toArray();
        } else if (parentElement instanceof Task) {
            return ((Task) parentElement).getSubTasks().toArray();
        } else if (parentElement instanceof Git) {
            Git g = (Git) parentElement;
            return new Object[] {
                new ModelProperty(g, OrchestrationPackage.Literals.GIT__REPOSITORY_URL, "URL"),
                new ModelProperty(g, OrchestrationPackage.Literals.GIT__BRANCH, "Branch"),
                new ModelProperty(g, OrchestrationPackage.Literals.GIT__USERNAME, "User"),
                new ModelProperty(g, OrchestrationPackage.Literals.GIT__LOCAL_PATH, "Path")
            };
        } else if (parentElement instanceof LLM) {
            LLM l = (LLM) parentElement;
            return new Object[] {
                new ModelProperty(l, OrchestrationPackage.Literals.LLM__MODEL, "Model"),
                new ModelProperty(l, OrchestrationPackage.Literals.LLM__TEMPERATURE, "Temp")
            };
        } else if (parentElement instanceof SupervisorSettings) {
            SupervisorSettings s = (SupervisorSettings) parentElement;
            return new Object[] {
                new ModelProperty(s, OrchestrationPackage.Literals.SUPERVISOR_SETTINGS__EXECUTABLE_PATH, "Executable"),
                new ModelProperty(s, OrchestrationPackage.Literals.SUPERVISOR_SETTINGS__SOURCE_PATH, "Source"),
                new ModelProperty(s, OrchestrationPackage.Literals.SUPERVISOR_SETTINGS__COMMANDS, "Commands"),
                new ModelProperty(s, OrchestrationPackage.Literals.SUPERVISOR_SETTINGS__SETTINGS, "Settings"),
                new ModelProperty(s, OrchestrationPackage.Literals.SUPERVISOR_SETTINGS__DEPLOYED, "Deployed")
            };
        } else if (parentElement instanceof Ollama) {
            Ollama o = (Ollama) parentElement;
            return new Object[] {
                new ModelProperty(o, OrchestrationPackage.Literals.OLLAMA__URL, "URL"),
                new ModelProperty(o, OrchestrationPackage.Literals.OLLAMA__MODEL, "Model"),
                new ModelProperty(o, OrchestrationPackage.Literals.OLLAMA__PATH, "Path")
            };
        } else if (parentElement instanceof AiChat) {
            AiChat a = (AiChat) parentElement;
            return new Object[] {
                new ModelProperty(a, OrchestrationPackage.Literals.AI_CHAT__URL, "URL"),
                new ModelProperty(a, OrchestrationPackage.Literals.AI_CHAT__TOKEN, "Token"),
                new ModelProperty(a, OrchestrationPackage.Literals.AI_CHAT__PROMPT, "Prompt")
            };
        } else if (parentElement instanceof Maven) {
            Maven m = (Maven) parentElement;
            return new Object[] {
                new ModelProperty(m, OrchestrationPackage.Literals.MAVEN__GOALS, "Goals"),
                new ModelProperty(m, OrchestrationPackage.Literals.MAVEN__PROFILES, "Profiles")
            };
        } else if (parentElement instanceof NeuronAI) {
            NeuronAI n = (NeuronAI) parentElement;
            return new Object[] {
                new ModelProperty(n, OrchestrationPackage.Literals.NEURON_AI__URL, "URL"),
                new ModelProperty(n, OrchestrationPackage.Literals.NEURON_AI__MODEL, "Model"),
                new ModelProperty(n, OrchestrationPackage.Literals.NEURON_AI__TYPE, "Type")
            };
        } else if (parentElement instanceof Compiler) {
            Compiler c = (Compiler) parentElement;
            return new Object[] {
                new ModelProperty(c, OrchestrationPackage.Literals.COMPILER__SOURCE_VERSION, "Source"),
                new ModelProperty(c, OrchestrationPackage.Literals.COMPILER__TARGET_VERSION, "Target")
            };
        } else if (parentElement instanceof Eclipse) {
            Eclipse e = (Eclipse) parentElement;
            return new Object[] {
                new ModelProperty(e, OrchestrationPackage.Literals.ECLIPSE__WORKSPACE, "Workspace"),
                new ModelProperty(e, OrchestrationPackage.Literals.ECLIPSE__INSTALLATION, "Installation")
            };
        } else if (parentElement instanceof Database) {
            Database d = (Database) parentElement;
            return new Object[] {
                new ModelProperty(d, OrchestrationPackage.Literals.DATABASE__URL, "URL"),
                new ModelProperty(d, OrchestrationPackage.Literals.DATABASE__USERNAME, "User"),
                new ModelProperty(d, OrchestrationPackage.Literals.DATABASE__PASSWORD, "Pass")
            };
        } else if (parentElement instanceof FileConfig) {
            FileConfig f = (FileConfig) parentElement;
            return new Object[] {
                new ModelProperty(f, OrchestrationPackage.Literals.FILE_CONFIG__LOCAL_PATH, "Path")
            };
        }
        return new Object[0];
    }

    private boolean isCommonExtension(String ext) {
        if (ext == null) return false;
        String[] common = {"txt", "pdf", "java", "c", "properties", "gcode", "ktml", "xml", "xhtml", "kt", "py", "js", "ts", "html", "css", "md", "json", "yaml", "yml"};
        for (String c : common) {
            if (c.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    private EvoProject loadEvoProject(IFile file) {
        String ext = file.getFileExtension();
        if ("xml".equals(ext) || "evo".equals(ext)) {
            try {
                return eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().loadProject(file);
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
