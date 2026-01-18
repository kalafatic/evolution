package eu.kalafatic.evolution.controller;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import eu.kalafatic.evolution.view.PropertiesView;

public class MavenBuildCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IViewPart view = page.findView(PropertiesView.ID);
                if (view instanceof PropertiesView) {
                    EObject orchestrator = ((PropertiesView) view).getRootObject();
                    if (orchestrator != null) {
                        EObject maven = (EObject) orchestrator.eGet(orchestrator.eClass().getEStructuralFeature("maven"));
                        if (maven != null) {
                            @SuppressWarnings("unchecked")
                            EList<String> goals = (EList<String>) maven.eGet(maven.eClass().getEStructuralFeature("goals"));
                            @SuppressWarnings("unchecked")
                            EList<String> profiles = (EList<String>) maven.eGet(maven.eClass().getEStructuralFeature("profiles"));

                            if (!goals.isEmpty()) {
                                IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
                                if (activePage != null && activePage.getActiveEditor() != null) {
                                    IProject project = activePage.getActiveEditor().getEditorInput().getAdapter(IProject.class);
                                    if(project == null) {
                                        project = activePage.getActiveEditor().getEditorInput().getAdapter(IFile.class).getProject();
                                    }
                                    launchMavenBuild(String.join(" ", goals), String.join(",", profiles), project);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void launchMavenBuild(String goals, String profiles, IProject project) {
        try {
            ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
            ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType("org.eclipse.m2e.Maven2LaunchConfigurationType");

            ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, "Maven Build");
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, project.getLocation().toOSString());
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goals);
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES, profiles);
            workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());

            ILaunchConfiguration launchConfig = workingCopy.doSave();
            launchConfig.launch(ILaunchManager.RUN_MODE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}
