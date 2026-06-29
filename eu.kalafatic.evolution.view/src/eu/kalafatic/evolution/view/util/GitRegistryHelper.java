package eu.kalafatic.evolution.view.util;

import java.io.File;

/**
 * Utility for registering Git repositories with EGit using reflection to avoid direct dependency issues.
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
                        // Try UI Activator
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
                System.out.println("[GIT-REG] Successfully registered Git repository: " + gitDir.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[GIT-REG] Failed to register Git repository with EGit: " + e.getMessage());
        }
    }
}
