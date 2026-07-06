package eu.kalafatic.evolution.controller.tools;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.IStartup;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GitRepositoryManager implements IStartup {
    
    private static final String GIT_VIEW_ID = "org.eclipse.egit.ui.RepositoriesView";
    private static List<File> registeredRepos = new ArrayList<>();

        
        public void scanAndRegisterRepositories() {
            try {
                IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                RepositoryCache cache = RepositoryCache.INSTANCE;
                int count = 0;
                
                for (IProject project : projects) {
                    if (project.exists()) {
                        File projectDir = project.getLocation().toFile();
                        File gitDir = new File(projectDir, ".git");
                        
                        if (gitDir.exists() && gitDir.isDirectory()) {
                            try {
                                // This will add the repository to EGit's cache
                                Repository repo = cache.lookupRepository(gitDir);
                                count++;
                                System.out.println("Registered repository: " + project.getName());
                            } catch (Exception e) {
                                System.err.println("Failed to register repo for: " + project.getName());
                                e.printStackTrace();
                            }
                        }
                    }
                }
                
                System.out.println("Registered " + count + " Git repositories");
                
                // Refresh the view
                refreshGitView();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private void refreshGitView() {
            Display.getDefault().asyncExec(() -> {
                try {
                    IViewPart view = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage()
                        .findView("org.eclipse.egit.ui.RepositoriesView");
                    
                    if (view instanceof CommonNavigator) {
                        CommonViewer viewer = ((CommonNavigator) view).getCommonViewer();
                        if (viewer != null) {
                            viewer.refresh();
                        }
                    }
                } catch (Exception e) {
                    // View might not be open yet
                }
            });
        }

		@Override
		public void earlyStartup() {
			// TODO Auto-generated method stub
			
		}
    }