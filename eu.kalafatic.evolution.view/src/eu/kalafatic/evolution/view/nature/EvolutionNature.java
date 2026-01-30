package eu.kalafatic.evolution.view.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class EvolutionNature implements IProjectNature {

    public static final String NATURE_ID = "eu.kalafatic.evolution.view.evolutionNature";

    private IProject project;

    @Override
    public void configure() throws CoreException {
        // Add builder if needed
    }

    @Override
    public void deconfigure() throws CoreException {
        // Remove builder if needed
    }

    @Override
    public IProject getProject() {
        return project;
    }

    @Override
    public void setProject(IProject project) {
        this.project = project;
    }
}
