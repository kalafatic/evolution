# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/project/

## Domain: general

## Components
* `ProjectCreator.java`: package eu.kalafatic.evolution.view.project; import org.eclipse.core.resources.IProject; import org.eclipse.core.resources.IProjectDescription; import org.eclipse.core.resources.IWorkspaceRoot; import org.eclipse.core.resources.ResourcesPlugin; import org.eclipse.core.runtime.CoreException; import org.eclipse.core.runtime.IProgressMonitor; import org.eclipse.core.runtime.NullProgressMonitor; import org.eclipse.jface.viewers.ISelection; import org.eclipse.jface.viewers.ISelectionProvider; import org.eclipse.jface.viewers.StructuredSelection; import org.eclipse.ui.IWorkbenchPage; import org.eclipse.ui.IWorkbenchWindow; import org.eclipse.ui.PlatformUI; import org.eclipse.ui.navigator.resources.ProjectExplorer; public class ProjectCreator { public static void createAndShowProject(String projectName) { IProgressMonitor monitor = new NullProgressMonitor(); try { IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
