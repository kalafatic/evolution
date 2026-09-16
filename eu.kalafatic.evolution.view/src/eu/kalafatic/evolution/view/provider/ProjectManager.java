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
		// 1. Try discovering via ProjectModelManager and its active WorkspaceSourceResolver
		try {
			eu.kalafatic.evolution.controller.manager.ProjectModelManager pmm =
				eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance();
			if (pmm != null) {
				eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult result = pmm.getOrDiscoverWorkspace();
				if (result != null && result.getPrimaryRepository() != null) {
					return result.getPrimaryRepository().getAbsolutePath();
				}
			}
		} catch (Throwable t) {
			// Ignore if not available
		}

		// 2. Check standard EclipseGitEvoTool configurations
		try {
			String workspaceRepo = eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getWorkspaceRepository();
			if (workspaceRepo != null && !workspaceRepo.isEmpty() && new File(workspaceRepo).exists()) {
				return new File(workspaceRepo).getAbsolutePath();
			}
		} catch (Throwable t) {
		}
		try {
			String evoRepo = eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.getEvolutionRepository();
			if (evoRepo != null && !evoRepo.isEmpty() && new File(evoRepo).exists()) {
				return new File(evoRepo).getAbsolutePath();
			}
		} catch (Throwable t) {
		}

		// 3. Check cached local repositories in GitTool
		try {
			java.util.List<File> repos = eu.kalafatic.evolution.controller.tools.GitTool.getCachedLocalRepositories();
			for (File repo : repos) {
				String name = repo.getName().toLowerCase();
				if (name.equals("evolution") || name.equals("evo")) {
					return repo.getAbsolutePath();
				}
			}
		} catch (Throwable t) {
		}

		// 4. Check system properties and environment variables
		String[] envVars = {"EVOLUTION_CODEBASE", "EVOLUTION_HOME", "EVO_HOME"};
		for (String var : envVars) {
			String val = System.getenv(var);
			if (val != null && !val.trim().isEmpty() && new File(val).exists()) {
				return new File(val).getAbsolutePath();
			}
			val = System.getProperty(var);
			if (val != null && !val.trim().isEmpty() && new File(val).exists()) {
				return new File(val).getAbsolutePath();
			}
		}

		// 5. Check active open projects in the workspace
		try {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				if (project.isOpen() && project.getLocation() != null) {
					File location = project.getLocation().toFile();
					File current = location;
					while (current != null) {
						if (new File(current, ".git").exists() ||
							new File(current, "eu.kalafatic.evolution.controller").exists() ||
							new File(current, "pom.xml").exists() && new File(current, "eu.kalafatic.evolution.view").exists()) {
							return current.getAbsolutePath();
						}
						current = current.getParentFile();
					}
				}
			}
		} catch (Throwable t) {
		}

		// 6. Traverse up from user.dir
		try {
			File current = new File(System.getProperty("user.dir"));
			while (current != null) {
				if (new File(current, "eu.kalafatic.evolution.controller").exists() ||
					new File(current, "eu.kalafatic.evolution.view").exists() ||
					new File(current, ".git").exists() ||
					new File(current, "pom.xml").exists() && new File(current, "eu.kalafatic.evolution.model").exists()) {
					return current.getAbsolutePath();
				}
				current = current.getParentFile();
			}
		} catch (Throwable t) {
		}

		// 7. Fallback to user.dir if exists
		try {
			File userDir = new File(System.getProperty("user.dir"));
			if (userDir.exists()) {
				return userDir.getAbsolutePath();
			}
		} catch (Throwable t) {
		}

		return null;
	}

	/**
	 * Returns the codebase folder/repository path where the real source code is (instance method).
	 *
	 * @return the absolute path of the codebase folder/repository, or null if it cannot be determined.
	 */
	public String getCodebaseFolderPath() {
		return getCodebasePath();
	}

	/**
	 * Returns the workspace folder path.
	 * This method returns the active Eclipse workspace root folder.
	 *
	 * @return the absolute path of the workspace folder, or null if it cannot be determined.
	 */
	public static String getWorkspacePath() {
		// 1. Try to get it from Eclipse ResourcesPlugin
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			if (root != null && root.getLocation() != null) {
				return root.getLocation().toFile().getAbsolutePath();
			}
		} catch (Throwable t) {
			// Ignore if ResourcesPlugin/workspace is not active
		}

		// 2. Try OSGi system properties or platform instance area
		try {
			String osgiInstance = System.getProperty("osgi.instance.area");
			if (osgiInstance != null && !osgiInstance.trim().isEmpty()) {
				if (osgiInstance.startsWith("file:")) {
					if (osgiInstance.startsWith("file://") && !osgiInstance.startsWith("file:///")) {
						osgiInstance = osgiInstance.replaceFirst("file://", "file:///");
					}
					osgiInstance = new URI(osgiInstance).getPath();
					if (osgiInstance.startsWith("/") && osgiInstance.length() > 2 && osgiInstance.charAt(2) == ':') {
						osgiInstance = osgiInstance.substring(1);
					}
				}
				File f = new File(osgiInstance);
				if (f.exists()) {
					return f.getAbsolutePath();
				}
			}
		} catch (Throwable t) {
		}

		// 3. Check environment or system properties
		String[] envVars = {"WORKSPACE", "ECLIPSE_WORKSPACE"};
		for (String var : envVars) {
			String val = System.getenv(var);
			if (val != null && !val.trim().isEmpty() && new File(val).exists()) {
				return new File(val).getAbsolutePath();
			}
			val = System.getProperty(var);
			if (val != null && !val.trim().isEmpty() && new File(val).exists()) {
				return new File(val).getAbsolutePath();
			}
		}

		// 4. Try traversing relative to user.dir
		try {
			File userDir = new File(System.getProperty("user.dir"));
			if (userDir.getName().contains("workspace") || new File(userDir, ".metadata").exists()) {
				return userDir.getAbsolutePath();
			}
			File parent = userDir.getParentFile();
			if (parent != null && (parent.getName().contains("workspace") || new File(parent, ".metadata").exists())) {
				return parent.getAbsolutePath();
			}
		} catch (Throwable t) {
		}

		// 5. Ultimate fallback to user.dir
		try {
			File userDir = new File(System.getProperty("user.dir"));
			if (userDir.exists()) {
				return userDir.getAbsolutePath();
			}
		} catch (Throwable t) {
		}

		return null;
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
