package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * Central utility for programmatically managing multiple Git repositories used
 * by the Evo project.
 */
public class EclipseGitEvoTool3 {
	
	public static final String PROTECTED_BRANCH = "master";

	// --- Repo IDs ---
	public static final String REPO_EVOLUTION = "evolution";
	public static final String REPO_WORKSPACE = "workspace";
	public static final String REPO_LLM = "llm";

	// --- Configuration Keys ---
	private static final String AUTO_CLONE_KEY = "auto.clone";
	private static final String AUTO_REGISTER_KEY = "auto.register";

	// --- Inner Classes & Enums ---

	public enum OpStatus {
		SUCCESS, WARNING, FAILED, MANUAL_ACTION_REQUIRED
	}

	public static class GitOpResult {
		private final OpStatus status;
		private final String message;

		public GitOpResult(OpStatus status, String message) {
			this.status = status;
			this.message = message;
		}

		public OpStatus getStatus() {
			return status;
		}

		public String getMessage() {
			return message;
		}

		public boolean isSuccess() {
			return status == OpStatus.SUCCESS || status == OpStatus.WARNING;
		}

		@Override
		public String toString() {
			return "[" + status + "] " + message;
		}
	}

	public static class RepoStatus {
		public String id;
		public boolean exists;
		public boolean isValid;
		public boolean hasHead;
		public boolean hasRemote;
		public String branch;
		public boolean isDirty;
		public boolean canRead;
		public boolean canWrite;
		public String remoteUrl;
		public String localPath;

		@Override
		public String toString() {
			return String.format("RepoStatus[id=%s, exists=%b, valid=%b, head=%b, dirty=%b, remote=%s]", id, exists,
					isValid, hasHead, isDirty, remoteUrl);
		}
	}

	public static class RepoConfig {
		public final String id;
		public String defaultRemote;
		public String defaultLocalPath;
		public String defaultBranch = "master";
		public String defaultUsername = "admin";
		public String defaultPassword = "";

		public RepoConfig(String id, String remote, String local) {
			this.id = id;
			this.defaultRemote = remote;
			this.defaultLocalPath = local;
		}
	}

	// --- State ---
	private static final Properties config = new Properties();
	private static final Map<String, RepoConfig> registry = new HashMap<>();
	private static boolean autoClone = true;
	private static boolean autoRegister = true;

	static {
		registerRepository(new RepoConfig(REPO_EVOLUTION, "https://github.com/kalafatic/evolution.git",
				getEvolutionDefaultPath()));
		registerRepository(new RepoConfig(REPO_WORKSPACE, "https://github.com/kalafatic/evo.git", getEvoDefaultPath()));
		registerRepository(new RepoConfig(REPO_LLM, "https://github.com/kalafatic/llm.git", getLlmDefaultPath()));
	}

	// --- Utilities ---

	private static void log(String message) {
		System.out.println("[GIT] " + message);
	}

	public static String getEvolutionDefaultPath() {
		try {
			File path = Paths.get(System.getProperty("user.home"), "git", "evolution").toFile();
			if (!path.exists()) {
				path.mkdirs();
			}
			return path.getAbsolutePath();
		} catch (Exception e) {
			return Paths.get(System.getProperty("user.home"), "git", "evolution").toString();
		}
	}

	public static String getEvoDefaultPath() {
		try {
			File workspaceDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
			File path = new File(workspaceDir, "evo");
			if (!path.exists()) {
				path.mkdirs();
			}
			return path.getAbsolutePath();
		} catch (Exception e) {
			return Paths.get(System.getProperty("user.home"), "git", "evo").toString();
		}
	}

	public static String getLlmDefaultPath() {
		try {
			File workspaceDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
			File path = new File(workspaceDir, "llm");
			if (!path.exists()) {
				path.mkdirs();
			}
			return path.getAbsolutePath();
		} catch (Exception e) {
			return Paths.get(System.getProperty("user.home"), "git", "llm").toString();
		}
	}

	private static File getConfigFile() {
		return new File(System.getProperty("user.home"), ".evo-git-tool.properties");
	}

	// --- Registry ---

	public static void registerRepository(RepoConfig repo) {
		registry.put(repo.id, repo);
		
		EclipseGitEvoTool3.changeRemoteUrl(repo.id, repo.defaultRemote);
		EclipseGitEvoTool3.changeRepositoryLocation(repo.id, repo.defaultLocalPath);
		EclipseGitEvoTool3.changeBranch(repo.id, repo.defaultBranch);
		EclipseGitEvoTool3.changeCredentials(repo.id, repo.defaultUsername, repo.defaultPassword);
	}

	public static List<String> getRegisteredRepositoryIds() {
		return new ArrayList<>(registry.keySet());
	}

	// --- Management ---

	public static String getEvolutionRepository() {
		return getRepositoryPath(REPO_EVOLUTION);
	}

	public static String getWorkspaceRepository() {
		return getRepositoryPath(REPO_WORKSPACE);
	}

	public static String getRepositoryPath(String id) {
		RepoConfig rc = registry.get(id);
		if (rc == null)
			return null;
		return config.getProperty(id + ".local", rc.defaultLocalPath);
	}

	public static String getRepositoryRemote(String id) {
		RepoConfig rc = registry.get(id);
		if (rc == null)
			return null;
		return config.getProperty(id + ".remote", rc.defaultRemote);
	}

	public static String getRepositoryBranch(String id) {
		RepoConfig rc = registry.get(id);
		if (rc == null)
			return null;
		return config.getProperty(id + ".branch", rc.defaultBranch);
	}

	public static String getRepositoryUsername(String id) {
		RepoConfig rc = registry.get(id);
		if (rc == null)
			return null;
		return config.getProperty(id + ".username", rc.defaultUsername);
	}

	public static String getRepositoryPassword(String id) {
		RepoConfig rc = registry.get(id);
		if (rc == null)
			return null;
		return config.getProperty(id + ".password", rc.defaultPassword);
	}

	public static GitOpResult changeRepositoryLocation(String id, String newPath) {
		if (!registry.containsKey(id))
			return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
		config.setProperty(id + ".local", newPath);
		saveConfiguration();
		return new GitOpResult(OpStatus.SUCCESS, "Location updated for " + id);
	}

	public static GitOpResult changeRemoteUrl(String id, String newUrl) {
		if (!registry.containsKey(id))
			return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
		config.setProperty(id + ".remote", newUrl);
		saveConfiguration();
		return new GitOpResult(OpStatus.SUCCESS, "Remote URL updated for " + id);
	}

	public static GitOpResult changeBranch(String id, String branch) {
		if (!registry.containsKey(id))
			return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
		config.setProperty(id + ".branch", branch);
		saveConfiguration();
		return new GitOpResult(OpStatus.SUCCESS, "Branch updated for " + id);
	}

	public static GitOpResult changeCredentials(String id, String user, String pass) {
		if (!registry.containsKey(id))
			return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
		config.setProperty(id + ".username", user);
		config.setProperty(id + ".password", pass);
		saveConfiguration();
		return new GitOpResult(OpStatus.SUCCESS, "Credentials updated for " + id);
	}

	public static GitOpResult removeRepository(String id) {
		String path = getRepositoryPath(id);
		if (path == null)
			return new GitOpResult(OpStatus.FAILED, "Repo not found: " + id);
		removeFromEgitView(path);
		return new GitOpResult(OpStatus.SUCCESS, "Repository removed from Eclipse view: " + id);
	}

	// --- Lifecycle ---

	public static GitOpResult initializeRepositories() {
		log("Initializing repositories...");
		loadConfiguration();
		for (String id : registry.keySet()) {
			GitOpResult checkResult = checkRepository(id);
			log(id + ": " + checkResult.getMessage());
			if (!checkResult.isSuccess() && autoClone) {
				GitOpResult cloneResult = cloneRepository(id);
				log(id + " clone: " + cloneResult.getMessage());
			}
			if (autoRegister) {
				GitOpResult registerResult = registerRepositoriesInGitView(id);
				log(id + " register: " + registerResult.getMessage());
			}
		}
		lockMasterBranchForPush();
		
		// Force multiple refresh attempts with increasing delays
		Display.getDefault().timerExec(1000, () -> forceRefreshGitView());
		Display.getDefault().timerExec(3000, () -> forceRefreshGitView());
		Display.getDefault().timerExec(5000, () -> forceRefreshGitView());
		
		
		
		log("Initialization complete.");
		return new GitOpResult(OpStatus.SUCCESS, "Repositories initialized");
	}
	
	// Call this after initializeRepositories()
	public static void lockMasterBranchForPush() {
	    log("Locking master branch for push (pull remains enabled)...");
	    
	    // Store protection flag
	    config.setProperty(REPO_EVOLUTION + ".push.protected", "true");
	    config.setProperty(REPO_EVOLUTION + ".protected.branch", PROTECTED_BRANCH);
	    saveConfiguration();
	    
	    log("Master branch is now PUSH-PROTECTED. Only PULL is allowed.");
	}

	public static GitOpResult checkRepositories() {
		log("Checking all repositories...");
		boolean allExist = true;
		for (String id : registry.keySet()) {
			if (!getRepoStatusById(id).exists)
				allExist = false;
		}
		return allExist ? new GitOpResult(OpStatus.SUCCESS, "All repositories exist")
				: new GitOpResult(OpStatus.WARNING, "Some repositories missing");
	}

	public static GitOpResult checkRepository(String id) {
		RepoStatus status = getRepoStatusById(id);
		if (status.exists) {
			return status.isValid ? new GitOpResult(OpStatus.SUCCESS, "Valid")
					: new GitOpResult(OpStatus.WARNING, "Invalid Git repo");
		}
		return new GitOpResult(OpStatus.FAILED, "Missing");
	}

	public static GitOpResult validateRepositories() {
		log("Validating all repositories...");
		boolean allValid = true;
		for (String id : registry.keySet()) {
			if (!getRepoStatusById(id).isValid)
				allValid = false;
		}
		return allValid ? new GitOpResult(OpStatus.SUCCESS, "All repositories valid")
				: new GitOpResult(OpStatus.FAILED, "Some repositories invalid");
	}

	public static GitOpResult cloneMissingRepositories() {
		log("Cloning missing repositories...");
		for (String id : registry.keySet()) {
			cloneRepository(id);
		}
		return new GitOpResult(OpStatus.SUCCESS, "Clone check complete");
	}

	public static GitOpResult cloneRepository(String id) {
		RepoConfig rc = registry.get(id);
		if (rc == null)
			return new GitOpResult(OpStatus.FAILED, "Unknown repo: " + id);
		return cloneIfMissing(id, getRepositoryRemote(id), getRepositoryPath(id));
	}

	public static GitOpResult registerRepositoriesInGitView() {
		for (String id : registry.keySet())
			registerRepositoriesInGitView(id);
		
		Display.getDefault().timerExec(3000, () -> forceRefreshGitView());
		
		return new GitOpResult(OpStatus.SUCCESS, "All repositories registered");
	}

	public static GitOpResult registerRepositoriesInGitView(String id) {
		String path = getRepositoryPath(id);
		if (path == null)
			return new GitOpResult(OpStatus.FAILED, "Repo not found: " + id);
		
		// Properly register the repository
		boolean registered = registerRepositoryWithEGit(path);
		
		if (registered) {
			return new GitOpResult(OpStatus.SUCCESS, "Registered");
		} else {
			return new GitOpResult(OpStatus.WARNING, "Registration attempted but may need manual refresh");
		}
	}
	
	private static void forceRefreshGitView() {
	    log("Forcing Git Repositories View refresh...");

	    Display.getDefault().asyncExec(() -> {
	        try {
	            IWorkbench workbench = PlatformUI.getWorkbench();
	            if (workbench == null) return;

	            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
	            if (window == null) return;

	            IWorkbenchPage page = window.getActivePage();
	            if (page == null) return;

	            // Open or find the view
	            IViewPart viewPart = page.findView("org.eclipse.egit.ui.RepositoriesView");
	            if (viewPart == null) {
	                viewPart = page.showView("org.eclipse.egit.ui.RepositoriesView", 
	                                       null, IWorkbenchPage.VIEW_VISIBLE);
	                log("Opened Git Repositories View");
	            }

	            if (viewPart instanceof CommonNavigator) {
	                CommonNavigator navigator = (CommonNavigator) viewPart;
	                CommonViewer viewer = navigator.getCommonViewer();

	                if (viewer != null) {
	                    // Critical: force full reload
	                    viewer.setInput(null);
	                    viewer.setInput(ResourcesPlugin.getWorkspace().getRoot()); // or RepositoryUtil
	                    viewer.refresh(true);
	                    viewer.expandToLevel(2);
	                    log("Successfully refreshed CommonViewer");
	                }
	            }

	            // Extra safety
	            ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);

	        } catch (Exception e) {
	            log("Refresh failed: " + e.getMessage());
	            e.printStackTrace();
	        }
	    });
	}
	/**
	 * Slightly more polished one for production:
	 */
//	private static void forceRefreshGitView() {
//	    log("Forcing Git Repositories View refresh...");
//
//	    Display.getDefault().asyncExec(() -> {
//	        try {
//	            IWorkbenchPage page = PlatformUI.getWorkbench()
//	                    .getActiveWorkbenchWindow().getActivePage();
//
//	            IViewPart view = page.findView("org.eclipse.egit.ui.RepositoriesView");
//	            if (view == null) {
//	                view = page.showView("org.eclipse.egit.ui.RepositoriesView");
//	            }
//
//	            if (view instanceof CommonNavigator) {
//	                CommonViewer viewer = ((CommonNavigator) view).getCommonViewer();
//	                if (viewer != null) {
//	                    Object oldInput = viewer.getInput();
//	                    viewer.setInput(null);                    // clear
//	                    viewer.setInput(oldInput != null ? oldInput : 
//	                                   RepositoryUtil.INSTANCE.getConfiguredRepositories());
//	                    viewer.refresh(true);
//	                    viewer.expandToLevel(2);
//	                    log("Git Repositories View refreshed successfully");
//	                }
//	            }
//	        } catch (Exception e) {
//	            log("Refresh error: " + e.getMessage());
//	        }
//	    });
//	}

	// --- FIXED: Proper Repository Registration using only public API ---

//	private static boolean registerRepositoryWithEGit(String localPath) {
//		try {
//			File gitDir = localPath.endsWith(".git") ? new File(localPath) : new File(localPath, ".git");
//			gitDir = gitDir.getCanonicalFile();
//			File repoDir = gitDir.getParentFile();
//			
//			if (!gitDir.exists() || !gitDir.isDirectory()) {
//				log("Git directory does not exist: " + gitDir.getAbsolutePath());
//				return false;
//			}
//
//			log("Registering repository: " + repoDir.getAbsolutePath());
//
//			// Use RepositoryUtil.INSTANCE (public API)
//			try {
//				RepositoryUtil repoUtil = RepositoryUtil.INSTANCE;
//				if (repoUtil != null) {
//					// Check if already registered
//					List<String> configured = repoUtil.getConfiguredRepositories();
//					String path = gitDir.getAbsolutePath();
//					
//					if (configured == null || !configured.contains(path)) {
//						repoUtil.addConfiguredRepository(gitDir);
//						log("Added repository via RepositoryUtil.INSTANCE");
//					} else {
//						log("Repository already registered in RepositoryUtil");
//					}
//				}
//			} catch (Exception e) {
//				log("RepositoryUtil.INSTANCE failed: " + e.getMessage());
//			}
//
//			// Direct preference writing (backup method using public API)
//			try {
//				String key = "GitRepositoriesView.configuredRepositories";
//				String canonicalPath = gitDir.getCanonicalPath();
//				String slashPath = canonicalPath.replace('\\', '/');
//
//				// Get existing repositories from preferences
//				org.eclipse.core.runtime.preferences.IEclipsePreferences prefs = 
//					InstanceScope.INSTANCE.getNode("org.eclipse.egit.ui");
//				
//				if (prefs != null) {
//					String existing = prefs.get(key, "");
//					List<String> paths = new ArrayList<>();
//					
//					if (existing != null && !existing.isEmpty()) {
//						for (String p : existing.split("\n")) {
//							if (!p.trim().isEmpty()) {
//								paths.add(p.trim());
//							}
//						}
//					}
//
//					// Add if not already present
//					boolean modified = false;
//					if (!paths.contains(canonicalPath)) {
//						paths.add(canonicalPath);
//						modified = true;
//					}
//					if (!paths.contains(slashPath)) {
//						paths.add(slashPath);
//						modified = true;
//					}
//
//					if (modified) {
//						StringBuilder sb = new StringBuilder();
//						for (String p : paths) {
//							if (sb.length() > 0) sb.append("\n");
//							sb.append(p);
//						}
//						prefs.put(key, sb.toString());
//						prefs.flush();
//						log("Updated preference for repository");
//					}
//				}
//			} catch (Exception e) {
//				log("Preference update failed: " + e.getMessage());
//			}
//
//			return true;
//			
//		} catch (Exception e) {
//			log("Failed to register repository: " + e.getMessage());
//			e.printStackTrace();
//			return false;
//		}
//	}

	private static boolean registerRepositoryWithEGit(String localPath) {
	    try {
	        File gitDir = localPath.endsWith(".git") ? new File(localPath) : new File(localPath, ".git");
	        gitDir = gitDir.getCanonicalFile();

	        if (!gitDir.exists() || !gitDir.isDirectory()) {
	            log("Git directory does not exist: " + gitDir);
	            return false;
	        }

	        RepositoryUtil repoUtil = RepositoryUtil.INSTANCE;
	        List<String> configured = repoUtil.getConfiguredRepositories();
	        String canonicalPath = gitDir.getCanonicalPath();

	        if (!configured.contains(canonicalPath)) {
	            boolean added = repoUtil.addConfiguredRepository(gitDir);
	            log(added ? "Added to EGit: " + canonicalPath : "Already known or failed");
	        } else {
	            log("Already registered: " + canonicalPath);
	        }

	        // Update preferences as backup (EGit reads from here)
	        updateEgitPreferences(canonicalPath);

	        return true;
	    } catch (Exception e) {
	        log("Failed to register: " + e.getMessage());
	        e.printStackTrace();
	        return false;
	    }
	}
	
	
	private static void updateEgitPreferences(String repoPath) {
	    try {
	        org.eclipse.core.runtime.preferences.IEclipsePreferences prefs = 
	            InstanceScope.INSTANCE.getNode("org.eclipse.egit.ui");
	        
	        String key = "GitRepositoriesView.configuredRepositories";
	        String existing = prefs.get(key, "");
	        
	        List<String> paths = new ArrayList<>();
	        if (!existing.isEmpty()) {
	            for (String p : existing.split("\n")) {
	                if (!p.trim().isEmpty()) paths.add(p.trim());
	            }
	        }

	        if (!paths.contains(repoPath)) {
	            paths.add(repoPath);
	            StringBuilder sb = new StringBuilder();
	            for (String p : paths) {
	                if (sb.length() > 0) sb.append("\n");
	                sb.append(p);
	            }
	            prefs.put(key, sb.toString());
	            prefs.flush();
	        }
	    } catch (Exception e) {
	        log("Preference update failed: " + e.getMessage());
	    }
	}

	private static void removeFromEgitView(String localPath) {
	    try {
	        File gitDir = new File(localPath.endsWith(".git") ? localPath : new File(localPath, ".git").getCanonicalPath());
	        RepositoryUtil repoUtil = RepositoryUtil.INSTANCE;
	        repoUtil.removeDir(gitDir);  // This is the public API method
	    } catch (Exception e) {
	        log("Remove failed: " + e.getMessage());
	    }
	}
	
//	private static void removeFromEgitView(String localPath) {
//		try {
//			File gitDir = new File(localPath, ".git");
//			if (!gitDir.exists())
//				return;
//				
//			RepositoryUtil repoUtil = RepositoryUtil.INSTANCE;
//			if (repoUtil != null) {
//				// Check if configured using getConfiguredRepositories
//				List<String> configured = repoUtil.getConfiguredRepositories();
//				String path = gitDir.getAbsolutePath();
//				if (configured != null && configured.contains(path)) {
//					repoUtil.removeDir(gitDir);
//					log("Removed from EGit view: " + gitDir.getAbsolutePath());
//				}
//			}
//		} catch (Exception e) {
//			log("Failed to remove from EGit: " + e.getMessage());
//		}
//	}

	// --- ULTRA AGGRESSIVE View Refresh ---

//	private static void forceRefreshGitView() {
//		log("Forcing Git view refresh...");
//
//		Display.getDefault().asyncExec(() -> {
//			try {
//				// 1. Re-register all repositories
//				for (String id : registry.keySet()) {
//					String path = getRepositoryPath(id);
//					if (path != null) {
//						registerRepositoryWithEGit(path);
//					}
//				}
//
//				// 2. Try to refresh the EGit view using multiple approaches
//				var workbench = PlatformUI.getWorkbench();
//				if (workbench != null) {
//					var window = workbench.getActiveWorkbenchWindow();
//					if (window != null) {
//						IWorkbenchPage page = window.getActivePage();
//						if (page != null) {
//							IViewPart view = page.findView("org.eclipse.egit.ui.RepositoriesView");
//							
//							// If view doesn't exist, create it
//							if (view == null) {
//								try {
//									view = page.showView("org.eclipse.egit.ui.RepositoriesView");
//									log("Opened RepositoriesView");
//								} catch (PartInitException e) {
//									log("Failed to open RepositoriesView: " + e.getMessage());
//								}
//							}
//							
//							if (view != null) {
//								// APPROACH 1: Try to access and refresh the internal repository model
//								try {
//									// Get the viewer
//									CommonViewer viewer = null;
//									if (view instanceof CommonNavigator) {
//										viewer = ((CommonNavigator) view).getCommonViewer();
//									} else {
//										// Try reflection
//										Method getViewer = view.getClass().getMethod("getCommonViewer");
//										viewer = (CommonViewer) getViewer.invoke(view);
//									}
//									
//									if (viewer != null) {
//										// Refresh the viewer
//										viewer.refresh();
//										log("Refreshed CommonViewer");
//										
//										// Try to expand all to force loading
//										viewer.expandAll();
//										log("Expanded all nodes");
//										
//										// Try to get the input and refresh it
//										Object input = viewer.getInput();
//										if (input != null) {
//											// Try to call refresh on the input if it has a refresh method
//											try {
//												Method refreshMethod = input.getClass().getMethod("refresh");
//												refreshMethod.invoke(input);
//												log("Refreshed viewer input");
//											} catch (Exception e) {
//												// Silent fail
//											}
//										}
//									}
//								} catch (Exception e) {
//									log("Failed to refresh CommonViewer: " + e.getMessage());
//								}
//								
//								// APPROACH 2: Try to update the view's model directly using reflection
//								try {
//									// Look for internal fields that might hold the repository list
//									Field[] fields = view.getClass().getDeclaredFields();
//									for (Field field : fields) {
//										field.setAccessible(true);
//										try {
//											Object value = field.get(view);
//											if (value != null) {
//												// Check if it's a collection or has a refresh method
//												if (value instanceof List || value instanceof Map || value instanceof java.util.Set) {
//													log("Found collection field: " + field.getName());
//													// Try to call refresh on the parent
//													try {
//														Method refreshMethod = view.getClass().getMethod("refresh");
//														refreshMethod.invoke(view);
//														log("Called refresh on view");
//													} catch (Exception e) {
//														// Silent fail
//													}
//												}
//											}
//										} catch (Exception e) {
//											// Silent fail
//										}
//									}
//								} catch (Exception e) {
//									// Silent fail
//								}
//								
//								// APPROACH 3: Close and reopen the view with a longer delay
//								try {
//									page.hideView(view);
//									Display.getDefault().timerExec(1000, () -> {
//										try {
//											IWorkbenchPage currentPage = PlatformUI.getWorkbench()
//												.getActiveWorkbenchWindow().getActivePage();
//											if (currentPage != null) {
//												currentPage.showView("org.eclipse.egit.ui.RepositoriesView");
//												log("Re-opened RepositoriesView with delay");
//											}
//										} catch (Exception e) {
//											log("Failed to re-open view: " + e.getMessage());
//										}
//									});
//								} catch (Exception e) {
//									log("Failed to hide/show view: " + e.getMessage());
//								}
//							}
//						}
//					}
//				}
//
//				// 3. Refresh workspace
//				ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
//				log("Workspace refreshed");
//
//				// 4. Try to force a UI update
//				Display.getDefault().update();
//
//			} catch (Exception e) {
//				log("Failed to refresh Git view: " + e.getMessage());
//				e.printStackTrace();
//			}
//		});
//	}

	public static void refreshGitView() {
		forceRefreshGitView();
	}

	// --- Repository Status ---

	private static RepoStatus getRepoStatusById(String id) {
		String path = getRepositoryPath(id);
		RepoStatus status = getRepoStatus(path);
		status.id = id;
		return status;
	}

	private static RepoStatus getRepoStatus(String localPath) {
		RepoStatus status = new RepoStatus();
		status.localPath = localPath;
		if (localPath == null)
			return status;
		File dir = new File(localPath);
		if (dir.getName().equals(".git")) {
			dir = dir.getParentFile();
		}
		status.exists = dir.exists();
		if (!status.exists)
			return status;
		status.canRead = dir.canRead();
		status.canWrite = dir.canWrite();
		try (Repository repo = new FileRepositoryBuilder().setGitDir(new File(dir, ".git")).setMustExist(true)
				.build()) {
			status.isValid = true;
			status.hasHead = repo.resolve("HEAD") != null;
			status.branch = repo.getBranch();
			String remote = repo.getConfig().getString("remote", "origin", "url");
			if (remote != null) {
				status.hasRemote = true;
				status.remoteUrl = remote;
			}
			try (Git git = new Git(repo)) {
				status.isDirty = !git.status().call().isClean();
			}
		} catch (Exception e) {
			status.isValid = false;
		}
		return status;
	}

	// --- Persistence ---

	private static void loadConfiguration() {
		File configFile = getConfigFile();
		if (configFile.exists()) {
			try (FileInputStream in = new FileInputStream(configFile)) {
				config.load(in);
				autoClone = Boolean.parseBoolean(config.getProperty(AUTO_CLONE_KEY, "true"));
				autoRegister = Boolean.parseBoolean(config.getProperty(AUTO_REGISTER_KEY, "true"));
			} catch (IOException e) {
				log("Failed to load configuration: " + e.getMessage());
			}
		}
	}

	private static void saveConfiguration() {
		File configFile = getConfigFile();
		config.setProperty(AUTO_CLONE_KEY, String.valueOf(autoClone));
		config.setProperty(AUTO_REGISTER_KEY, String.valueOf(autoRegister));

		try (FileOutputStream out = new FileOutputStream(configFile)) {
			config.store(out, "Evo Git Tool Settings");
		} catch (IOException e) {
			log("Failed to save configuration: " + e.getMessage());
		}
	}
	
	public static boolean isGitRepository(File repoDir) {
		try {    	
			File gitDir = repoDir.getName().equals(".git") ? repoDir : new File(repoDir, ".git");
			return FileKey.isGitRepository(gitDir, FS.DETECTED);
		} catch (Exception e) {
			return false;
		}
	}

	public static void createAndShowRepository(File repoDir) {
		Git git = null;
		try {
			File gitDir = new File(repoDir, ".git").getCanonicalFile();
			File canonicalRepoDir = gitDir.getParentFile();

			if (isGitRepository(canonicalRepoDir)) {
				log("Repository already exists at: " + canonicalRepoDir.getAbsolutePath());
				git = Git.open(canonicalRepoDir, FS.DETECTED);
			} else {
				// 1. Create the repo using JGit
				git = Git.init().setDirectory(canonicalRepoDir).call();
				log("Created new repository at: " + canonicalRepoDir.getAbsolutePath());
			}		

			// 2. Clean up
			git.close();

		} catch (Exception e) {
			log("Failed to create/open repository JGit: " + e.getMessage());
		}
	}

	// --- Git Operations ---

	private static GitOpResult cloneIfMissing(String id, String remoteUrl, String localPath) {
		File dir = new File(localPath);
		if (dir.getName().equals(".git")) {
			dir = dir.getParentFile();
		}
		if (dir.exists() && new File(dir, ".git").exists())
			return new GitOpResult(OpStatus.SUCCESS, "Already exists");
		log("Cloning " + id + " [" + remoteUrl + "] to " + dir.getAbsolutePath());
		try {
			if (!dir.exists() && !dir.mkdirs())
				return new GitOpResult(OpStatus.FAILED, "Mkdirs failed");

			CloneCommand cloneCmd = Git.cloneRepository().setURI(remoteUrl).setDirectory(dir).setCloneAllBranches(true)
					.setBare(false);

			String user = getRepositoryUsername(id);
			String pass = getRepositoryPassword(id);
			if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
				cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, pass));
			}

			try (Git git = cloneCmd.call()) {
				return new GitOpResult(OpStatus.SUCCESS, "Cloned");
			}
		} catch (Exception e) {
			log("Remote clone failed for " + id + ", initializing local repo: " + e.getMessage());
			try {
				try (Git git = Git.init().setDirectory(dir).call()) {
					return new GitOpResult(OpStatus.WARNING, "Local init fallback");
				}
			} catch (Exception e2) {
				return new GitOpResult(OpStatus.FAILED, "Init failed: " + e2.getMessage());
			}
		}
	}

	// --- Core Git Operations via JGit ---

	public static GitOpResult commit(String id, String message) {
		String path = getRepositoryPath(id);
		if (path == null) return new GitOpResult(OpStatus.FAILED, "Repo path is null");
		File repoDir = new File(path);
		if (repoDir.getName().equals(".git")) {
			repoDir = repoDir.getParentFile();
		}
		try (Git git = Git.open(repoDir)) {
			git.add().addFilepattern(".").call();
			git.commit().setMessage(message).call();
			return new GitOpResult(OpStatus.SUCCESS, "Committed successfully");
		} catch (Exception e) {
			return new GitOpResult(OpStatus.FAILED, "Commit failed: " + e.getMessage());
		}
	}

//	public static GitOpResult push(String id) {
//		String path = getRepositoryPath(id);
//		if (path == null) return new GitOpResult(OpStatus.FAILED, "Repo path is null");
//		File repoDir = new File(path);
//		if (repoDir.getName().equals(".git")) {
//			repoDir = repoDir.getParentFile();
//		}
//		try (Git git = Git.open(repoDir)) {
//			String user = getRepositoryUsername(id);
//			String pass = getRepositoryPassword(id);
//			var pushCmd = git.push();
//			if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
//				pushCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, pass));
//			}
//			pushCmd.call();
//			return new GitOpResult(OpStatus.SUCCESS, "Pushed successfully");
//		} catch (Exception e) {
//			return new GitOpResult(OpStatus.FAILED, "Push failed: " + e.getMessage());
//		}
//	}
	
	public static GitOpResult push(String id) {
	    String path = getRepositoryPath(id);
	    if (path == null) 
	        return new GitOpResult(OpStatus.FAILED, "Repo path is null");

	    // Check protection for evolution repo
	    if (REPO_EVOLUTION.equals(id)) {
	        String protectedBranch = config.getProperty(id + ".protected.branch", "master");
	        String currentBranch = getCurrentBranch(id);
	        
	        if (protectedBranch.equals(currentBranch)) {
	            String msg = "PUSH BLOCKED! Master branch is locked. Use Pull Request instead.";
	            log("[PROTECTED] " + msg);
	            return new GitOpResult(OpStatus.FAILED, msg);
	        }
	    }

	    // Normal push if not blocked
	    try (Git git = Git.open(new File(path))) {
	        String user = getRepositoryUsername(id);
	        String pass = getRepositoryPassword(id);
	        
	        var pushCmd = git.push();
	        if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
	            pushCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, pass));
	        }
	        
	        pushCmd.call();
	        return new GitOpResult(OpStatus.SUCCESS, "Pushed successfully");
	        
	    } catch (Exception e) {
	        return new GitOpResult(OpStatus.FAILED, "Push failed: " + e.getMessage());
	    }
	}

	public static GitOpResult pull(String id) {
		String path = getRepositoryPath(id);
		if (path == null) return new GitOpResult(OpStatus.FAILED, "Repo path is null");
		File repoDir = new File(path);
		if (repoDir.getName().equals(".git")) {
			repoDir = repoDir.getParentFile();
		}
		try (Git git = Git.open(repoDir)) {
			String user = getRepositoryUsername(id);
			String pass = getRepositoryPassword(id);
			var pullCmd = git.pull();
			if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
				pullCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, pass));
			}
			pullCmd.call();
			return new GitOpResult(OpStatus.SUCCESS, "Pulled successfully");
		} catch (Exception e) {
			return new GitOpResult(OpStatus.FAILED, "Pull failed: " + e.getMessage());
		}
	}

	public static GitOpResult checkout(String id, String branchName, boolean force) {
		String path = getRepositoryPath(id);
		if (path == null) return new GitOpResult(OpStatus.FAILED, "Repo path is null");
		File repoDir = new File(path);
		if (repoDir.getName().equals(".git")) {
			repoDir = repoDir.getParentFile();
		}
		try (Git git = Git.open(repoDir)) {
			git.checkout().setName(branchName).setForceRefUpdate(force).call();
			return new GitOpResult(OpStatus.SUCCESS, "Checked out to " + branchName);
		} catch (Exception e) {
			return new GitOpResult(OpStatus.FAILED, "Checkout failed: " + e.getMessage());
		}
	}

	public static GitOpResult createBranch(String id, String branchName) {
		String path = getRepositoryPath(id);
		if (path == null) return new GitOpResult(OpStatus.FAILED, "Repo path is null");
		File repoDir = new File(path);
		if (repoDir.getName().equals(".git")) {
			repoDir = repoDir.getParentFile();
		}
		try (Git git = Git.open(repoDir)) {
			git.branchCreate().setName(branchName).call();
			return new GitOpResult(OpStatus.SUCCESS, "Created branch " + branchName);
		} catch (Exception e) {
			return new GitOpResult(OpStatus.FAILED, "Branch creation failed: " + e.getMessage());
		}
	}

	public static GitOpResult rollback(String id) {
		String path = getRepositoryPath(id);
		if (path == null) return new GitOpResult(OpStatus.FAILED, "Repo path is null");
		File repoDir = new File(path);
		if (repoDir.getName().equals(".git")) {
			repoDir = repoDir.getParentFile();
		}
		try (Git git = Git.open(repoDir)) {
			git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("HEAD").call();
			git.clean().setCleanDirectories(true).setForce(true).call();
			return new GitOpResult(OpStatus.SUCCESS, "Rollback completed");
		} catch (Exception e) {
			return new GitOpResult(OpStatus.FAILED, "Rollback failed: " + e.getMessage());
		}
	}

	public static String getHeadCommit(String id) {
		String path = getRepositoryPath(id);
		if (path == null) return null;
		File repoDir = new File(path);
		if (repoDir.getName().equals(".git")) {
			repoDir = repoDir.getParentFile();
		}
		try (Repository repo = new FileRepositoryBuilder().setGitDir(new File(repoDir, ".git")).setMustExist(true).build()) {
			var resolved = repo.resolve("HEAD");
			return resolved != null ? resolved.getName() : null;
		} catch (Exception e) {
			return null;
		}
	}

	public static String getCurrentBranch(String id) {
		String path = getRepositoryPath(id);
		if (path == null) return null;
		File repoDir = new File(path);
		if (repoDir.getName().equals(".git")) {
			repoDir = repoDir.getParentFile();
		}
		try (Repository repo = new FileRepositoryBuilder().setGitDir(new File(repoDir, ".git")).setMustExist(true).build()) {
			return repo.getBranch();
		} catch (Exception e) {
			return null;
		}
	}

	// --- Workspace (REPO_WORKSPACE) Shortcuts ---
	public static GitOpResult commitWorkspace(String message) { return commit(REPO_WORKSPACE, message); }
	public static GitOpResult rollbackWorkspace() { return rollback(REPO_WORKSPACE); }
	public static GitOpResult pushWorkspace() { return push(REPO_WORKSPACE); }
	public static GitOpResult pullWorkspace() { return pull(REPO_WORKSPACE); }
	public static GitOpResult checkoutWorkspace(String branch, boolean force) { return checkout(REPO_WORKSPACE, branch, force); }
	public static GitOpResult createWorkspaceBranch(String branch) { return createBranch(REPO_WORKSPACE, branch); }

	// --- Evo (REPO_EVOLUTION) Shortcuts ---
	public static GitOpResult commitEvo(String message) { return commit(REPO_EVOLUTION, message); }
	public static GitOpResult rollbackEvo() { return rollback(REPO_EVOLUTION); }
	public static GitOpResult pushEvo() { return push(REPO_EVOLUTION); }
	public static GitOpResult pullEvo() { return pull(REPO_EVOLUTION); }
	public static GitOpResult checkoutEvo(String branch, boolean force) { return checkout(REPO_EVOLUTION, branch, force); }
	public static GitOpResult createEvoBranch(String branch) { return createBranch(REPO_EVOLUTION, branch); }

	// --- LLM (REPO_LLM) Shortcuts ---
		public static GitOpResult commitLlm(String message) { return commit(REPO_LLM, message); }
		public static GitOpResult rollbackLlm() { return rollback(REPO_LLM); }
		public static GitOpResult pushLlm() { return push(REPO_LLM); }
		public static GitOpResult pullLlm() { return pull(REPO_LLM); }
		public static GitOpResult checkoutLlm(String branch, boolean force) { return checkout(REPO_LLM, branch, force); }
		public static GitOpResult createLlmBranch(String branch) { return createBranch(REPO_LLM, branch); }
	}