package eu.kalafatic.evolution.view.provider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import java.io.File;
import java.net.URI;

public class ProjectManager {
	
	private void refresh() {
		Job job = new Job("Background Refresh") {
		    @Override
		    protected IStatus run(IProgressMonitor monitor) {
		        try {
		            // Refresh everything asynchronously
		            ResourcesPlugin.getWorkspace().getRoot()
		                .refreshLocal(IResource.DEPTH_INFINITE, monitor);
		            
		            refreshOpenProjects();
		            refreshView("org.eclipse.ui.navigator.ProjectExplorer");
		            
		            return Status.OK_STATUS;
		        } catch (CoreException e) {
		            return e.getStatus();
		        }
		    }
		};

		// Priority DECORATE tells Eclipse this is a UI-update priority job
		job.setPriority(Job.DECORATE);
		job.schedule();

	}
	
	public void openProject(IWorkbench workbench, IProject project, String fileName) {		
        IFile file = project.getFile(fileName);
		 // Automatically open project in project view and open editor with project orchestration      
        if (file.exists() && workbench != null) {
            IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
            if (dw != null) {
                IWorkbenchPage page = dw.getActivePage();
                if (page != null) {
                    try {
                        // Ensure Project Explorer is visible
                        page.showView(IPageLayout.ID_PROJECT_EXPLORER);
                        BasicNewResourceWizard.selectAndReveal(file, dw);
                        
                        // Open created file with the specific MultiPageEditor ID
                        IDE.openEditor(page, file, "eu.kalafatic.evolution.view.editors.MultiPageEditor", true);
                    } catch (PartInitException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
	}

	public void refreshAllProjects(){
		// Get the root of the entire workspace
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		// Run the refresh in a Job to prevent UI freezes
		Job refreshJob = new Job("Refreshing Workspace") {
		    @Override
		    protected IStatus run(IProgressMonitor monitor) {
		        try {
		            // DEPTH_INFINITE explores all projects and all sub-folders
		            root.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		            return Status.OK_STATUS;
		        } catch (CoreException e) {
		            return e.getStatus();
		        }
		    }
		};
		refreshJob.schedule();
	}
	
	public void  refreshOpenProjects(){
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
		    if (project.isOpen()) {
		        try {
					project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		}
		
	}
	
	public void  refreshView(String id){
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart view = page.findView(id);

		if (view instanceof CommonNavigator) {
		    CommonNavigator nav = (CommonNavigator) view;
		    nav.getCommonViewer().refresh();
		}
		
	}

	/**
	 * Returns the codebase folder/repository path where the real source code is.
	 * This method attempts multiple robust strategies to locate the real source code repository,
	 * including discovering workspace source roots, querying active Eclipse project locations,
	 * searching local Git repositories, checking environment variables, and traversing up
	 * from the user directory.
	 *
	 * @return the absolute path of the codebase folder/repository, or null if it cannot be determined.
	 */
	public static String getCodebasePath() {
		return eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
	}

	/**
	 * Returns the codebase folder/repository path where the real source code is (instance method).
	 *
	 * @return the absolute path of the codebase folder/repository.
	 */
	public String getCodebaseFolderPath() {
		return getCodebasePath();
	}

	/**
	 * Returns the workspace folder path.
	 * This method returns the active Eclipse workspace root folder.
	 *
	 * @return the absolute path of the workspace folder.
	 */
	public static String getWorkspacePath() {
		return eu.kalafatic.evolution.controller.manager.ProjectModelManager.getWorkspacePath();
	}

	/**
	 * Returns the workspace folder path (instance method).
	 *
	 * @return the absolute path of the workspace folder, or null if it cannot be determined.
	 */
	public String getWorkspaceFolderPath() {
		return getWorkspacePath();
	}
}
