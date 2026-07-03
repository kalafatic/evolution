package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import java.io.File;
import java.io.IOException;

public class EclipseGitRepositoryManager {
    
    private static final String RUNTIME_WORKSPACE_PATH = 
        "${workspace_loc}/../runtime-eu.kalafatic.evolution.view.product";
    
    /**
     * Resolves the Eclipse workspace location and returns the runtime workspace path
     */
    public static String getRuntimeWorkspacePath() {
        // Get the current workspace location
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath workspacePath = workspace.getRoot().getLocation();
        
        // Resolve ${workspace_loc}/../runtime-eu.kalafatic.evolution.view.product
        // Go up one directory from workspace and then into the runtime folder
//        IPath runtimePath = workspacePath.removeLastSegments(1)
//            .append("runtime-eu.kalafatic.evolution.view.product");
        
        IPath runtimePath = workspacePath.removeLastSegments(1);
        
        return runtimePath.toFile().getAbsolutePath() + File.separator;
    }
    
    /**
     * Gets the Git repository path inside the runtime workspace
     */
    public static String getGitRepositoryPath() {
        String runtimePath = getRuntimeWorkspacePath();
        return runtimePath + File.separator + ".git-repo";
    }
    
    /**
     * Creates or opens a Git repository in the runtime workspace
     */
    public static Git createOrOpenRepository() throws GitAPIException, IOException {
        String repoPath = getGitRepositoryPath();
        File repoDir = new File(repoPath);
        
        // Ensure the runtime workspace directory exists
        String runtimePath = getRuntimeWorkspacePath();
        File runtimeDir = new File(runtimePath);
        if (!runtimeDir.exists()) {
            boolean created = runtimeDir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create runtime workspace directory: " + runtimePath);
            }
            System.out.println("✓ Created runtime workspace directory: " + runtimePath);
        }
        
        // Check if Git repository already exists
        if (isGitRepository(repoDir)) {
            System.out.println("✓ Git repository already exists at: " + repoPath);
            return openExistingRepository(repoPath);
        } else {
            System.out.println("✓ Creating new Git repository at: " + repoPath);
            return Git.init().setDirectory(repoDir).call();
        }
    }
    
    /**
     * Checks if a directory contains a Git repository
     */
    public static boolean isGitRepository(File directory) {
        File gitDir = new File(directory, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }
    
    /**
     * Opens an existing Git repository
     */
    public static Git openExistingRepository(String path) throws IOException {
        File repoDir = new File(path);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder
                .setGitDir(new File(repoDir, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
        return new Git(repository);
    }
    
    /**
     * Gets the runtime workspace absolute path as a string
     */
    public static String getRuntimeWorkspaceAbsolutePath() {
        return getRuntimeWorkspacePath();
    }
}