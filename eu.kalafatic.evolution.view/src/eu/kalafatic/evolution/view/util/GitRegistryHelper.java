package eu.kalafatic.evolution.view.util;

import java.io.File;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Utility for registering and cloning Git repositories with EGit.
 */
public class GitRegistryHelper {

    /**
     * Registers a Git repository with EGit's Git Repositories view.
     * @param repoDir The directory containing the .git folder.
     */
    public static void registerGitRepository(File repoDir) {
        if (repoDir == null || !repoDir.exists()) return;

        File gitDir = new File(repoDir, ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
             // Maybe repoDir IS the .git dir
             if (repoDir.getName().equals(".git")) {
                 gitDir = repoDir;
             } else {
                 return;
             }
        }

        registerViaReflection(gitDir);
    }

    /**
     * Clones a repository and registers it with EGit.
     */
    public static void cloneAndRegister(String url, File localPath, String branch, String user, String password) {
        if (url == null || url.isEmpty() || localPath == null) return;

        try {
            if (!new File(localPath, ".git").exists()) {
                System.out.println("[GIT-REG] Cloning " + url + " to " + localPath.getAbsolutePath());
                var cloneCmd = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(localPath)
                    .setCloneAllBranches(true);

                if (branch != null && !branch.isEmpty()) {
                    cloneCmd.setBranch(branch);
                }

                if (user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
                    cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password));
                }

                try (Git git = cloneCmd.call()) {
                    System.out.println("[GIT-REG] Clone complete.");
                }
            } else {
                System.out.println("[GIT-REG] Repository already exists at " + localPath.getAbsolutePath());
            }

            registerGitRepository(localPath);

        } catch (Exception e) {
            System.err.println("[GIT-REG] Failed to clone/register repository: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerViaReflection(File gitDir) {
        try {
            Class<?> utilClass = null;
            try {
                utilClass = Class.forName("org.eclipse.egit.core.RepositoryUtil");
            } catch (ClassNotFoundException e) {
                try {
                    utilClass = Class.forName("org.eclipse.egit.core.internal.util.RepositoryUtil");
                } catch (ClassNotFoundException e2) {
                    // Ignore
                }
            }

            if (utilClass == null) return;

            Object repoUtil = null;
            try {
                // Try static getInstance()
                repoUtil = utilClass.getMethod("getInstance").invoke(null);
            } catch (Exception e) {
                // Try Activator.getDefault().getRepositoryUtil()
                try {
                    Class<?> activatorClass = Class.forName("org.eclipse.egit.core.Activator");
                    Object activator = activatorClass.getMethod("getDefault").invoke(null);
                    repoUtil = activator.getClass().getMethod("getRepositoryUtil").invoke(activator);
                } catch (Exception e2) {
                     // Try internal Activator
                    try {
                        Class<?> activatorClass = Class.forName("org.eclipse.egit.core.internal.Activator");
                        Object activator = activatorClass.getMethod("getDefault").invoke(null);
                        repoUtil = activator.getClass().getMethod("getRepositoryUtil").invoke(activator);
                    } catch (Exception e3) {
                        try {
                            Class<?> activatorClass = Class.forName("org.eclipse.egit.ui.Activator");
                            Object activator = activatorClass.getMethod("getDefault").invoke(null);
                            repoUtil = activator.getClass().getMethod("getRepositoryUtil").invoke(activator);
                        } catch (Exception e4) {
                            // Give up
                        }
                    }
                }
            }

            if (repoUtil != null) {
                repoUtil.getClass().getMethod("addConfiguredRepository", File.class).invoke(repoUtil, gitDir);
                System.out.println("[GIT-REG] Successfully registered Git repository (reflection): " + gitDir.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[GIT-REG] Failed to register Git repository with EGit: " + e.getMessage());
        }
    }
}
