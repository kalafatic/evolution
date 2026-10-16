package eu.kalafatic.evolution.controller.manager;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult;
import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool;
import eu.kalafatic.evolution.controller.tools.GitRepositoryStartup;

/**
 * Centralized background discovery and process service.
 * <p>
 * Encapsulates background scanning tasks (Git repository discovery, workspace source discovery,
 * and workspace filesystem refresh) so they are fully supported and runnable as background Eclipse Jobs,
 * but NEVER executed automatically on application startup.
 * Tasks run strictly when manually triggered by the user via UI commands or explicit API calls.
 */
public class BackgroundDiscoveryService {

    private static final BackgroundDiscoveryService INSTANCE = new BackgroundDiscoveryService();

    private BackgroundDiscoveryService() {
    }

    public static BackgroundDiscoveryService getInstance() {
        return INSTANCE;
    }

    /**
     * Manually triggers all background discovery tasks (Git scan, workspace source discovery, and workspace refresh).
     */
    public synchronized void startAllScans() {
        Log.log("[BACKGROUND_SERVICE] Manual trigger: Starting all background discovery scans...");
        startGitDiscoveryJob();
        startWorkspaceSourceDiscoveryJob();
        startWorkspaceRefreshJob();
    }

    /**
     * Manually triggers background Git repository discovery and registration.
     */
    public Job startGitDiscoveryJob() {
        Job job = new Job("Manual Git Repository Discovery") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                Log.log("[BACKGROUND_SERVICE] Executing manual Git repository discovery...");
                try {
                    EclipseGitEvoTool.initializeRepositories();
                    EclipseGitEvoTool.lockMasterBranchForPush();

                    GitRepositoryStartup startupScanner = new GitRepositoryStartup();
                    startupScanner.scanAndRegisterRepositories();
                    return Status.OK_STATUS;
                } catch (Throwable t) {
                    Log.log("[BACKGROUND_SERVICE] Error in Git discovery job: " + t.getMessage());
                    return new Status(IStatus.WARNING, "eu.kalafatic.evolution.controller", "Git discovery encountered issue", t);
                }
            }
        };
        job.setPriority(Job.DECORATE);
        job.schedule();
        return job;
    }

    /**
     * Manually triggers background workspace source directory resolution.
     */
    public Job startWorkspaceSourceDiscoveryJob() {
        Job job = new Job("Manual Workspace Source Discovery") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                Log.log("[BACKGROUND_SERVICE] Executing manual workspace source discovery...");
                try {
                    SourceDiscoveryResult discovery = ProjectModelManager.getInstance().getOrDiscoverWorkspace();
                    if (discovery != null) {
                        Log.log("[BACKGROUND_SERVICE] Resolved " + discovery.getProjectRoots().size() + " projects and " + discovery.getGitRepositories().size() + " Git repos.");
                    }
                    return Status.OK_STATUS;
                } catch (Throwable t) {
                    Log.log("[BACKGROUND_SERVICE] Error in workspace discovery job: " + t.getMessage());
                    return new Status(IStatus.WARNING, "eu.kalafatic.evolution.controller", "Workspace discovery encountered issue", t);
                }
            }
        };
        job.setPriority(Job.DECORATE);
        job.schedule();
        return job;
    }

    /**
     * Manually triggers background workspace filesystem refresh.
     */
    public Job startWorkspaceRefreshJob() {
        Job job = new Job("Manual Workspace Refresh") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                Log.log("[BACKGROUND_SERVICE] Executing manual workspace filesystem refresh...");
                try {
                    ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, monitor);
                    return Status.OK_STATUS;
                } catch (Throwable t) {
                    Log.log("[BACKGROUND_SERVICE] Error in workspace refresh job: " + t.getMessage());
                    return new Status(IStatus.WARNING, "eu.kalafatic.evolution.controller", "Workspace refresh encountered issue", t);
                }
            }
        };
        job.setPriority(Job.DECORATE);
        job.schedule();
        return job;
    }
}
