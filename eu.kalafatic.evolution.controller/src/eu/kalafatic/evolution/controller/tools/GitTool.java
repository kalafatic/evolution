package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Git;

/**
 * Tool for executing Git commands.
 */
public class GitTool implements ITool {

    private static List<File> cachedRepos = new ArrayList<>();
    private static boolean scanning = false;

    /**
     * Returns the cached list of local Git repositories.
     * Triggers a background scan if the cache is empty and not already scanning.
     */
    public static synchronized List<File> getCachedLocalRepositories() {
        if (cachedRepos.isEmpty() && !scanning) {
            triggerBackgroundScan();
        }
        return new ArrayList<>(cachedRepos);
    }

    /**
     * Triggers a background scan for Git repositories.
     */
    public static void triggerBackgroundScan() {
        synchronized (GitTool.class) {
            if (scanning) return;
            scanning = true;
        }

        Thread scanThread = new Thread(() -> {
            try {
                List<File> results = findAllLocalRepositories();
                synchronized (GitTool.class) {
                    cachedRepos = results;
                    scanning = false;
                }
            } catch (Exception e) {
                synchronized (GitTool.class) {
                    scanning = false;
                }
            }
        }, "GitRepoScanner");
        scanThread.setDaemon(true);
        scanThread.setPriority(Thread.MIN_PRIORITY);
        scanThread.start();
    }

    /**
     * Finds all local Git repositories in common locations.
     *
     * @return A list of directories containing a .git folder.
     */
    private static List<File> findAllLocalRepositories() {
        List<File> results = new ArrayList<>();
        String userHome = System.getProperty("user.home");

        // Priority 1: ~/projects
        File projectsDir = new File(userHome, "projects");
        if (projectsDir.exists()) {
            findRepositories(projectsDir, 0, 4, results);
        }

        // Priority 2: user home
        findRepositories(new File(userHome), 0, 3, results);

        // Priority 3: system roots
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                 findRepositories(root, 0, 2, results);
            }
        }

        return results;
    }

    private static void findRepositories(File root, int depth, int maxDepth, List<File> results) {
        if (depth > maxDepth || root == null || !root.exists() || !root.isDirectory()) {
            return;
        }

        // Avoid duplicates
        if (results.contains(root)) return;

        File gitDir = new File(root, ".git");
        if (gitDir.exists() && gitDir.isDirectory()) {
            results.add(root);
            return;
        }

        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    try {
                        findRepositories(file, depth + 1, maxDepth, results);
                    } catch (SecurityException e) {
                        // Skip inaccessible directories
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "GitTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        context.log("Tool [GitTool]: Running " + command);
        File gitWorkingDir = workingDir;
        Git gitSettings = context.getOrchestrator().getGit();
        if (gitSettings != null && gitSettings.getLocalPath() != null && !gitSettings.getLocalPath().isEmpty()) {
            File subDir = new File(workingDir, gitSettings.getLocalPath());
            if (subDir.exists() && subDir.isDirectory()) {
                gitWorkingDir = subDir;
            }
        }
        String branch = (gitSettings != null && gitSettings.getBranch() != null && !gitSettings.getBranch().isEmpty()) ? gitSettings.getBranch() : "master";

        ShellTool shell = new ShellTool();
        StringBuilder output = new StringBuilder();

        if (command.toLowerCase().contains("add") || command.toLowerCase().contains("commit")) {
            context.log("Tool [GitTool]: Staging all changes and committing.");
            output.append(shell.execute("git add .", gitWorkingDir, context)).append("\n");

            String metadata = "";
            if (context != null) {
                String iterationId = context.getOrchestrationState().getCurrentIterationId();
                String taskId = context.getCurrentTaskName(); // Task name as proxy for ID if ID not easily reachable
                metadata = String.format(" [Iteration: %s] [Task: %s]",
                    iterationId != null ? iterationId : "unknown",
                    taskId != null ? taskId : "none");
            }

            output.append(shell.execute("git commit -m \"AI Evolution step: " + command + metadata + "\"", gitWorkingDir, context)).append("\n");
        }

        if (command.toLowerCase().contains("push")) {
            context.log("Tool [GitTool]: Pushing to branch " + branch);
            output.append(shell.execute("git push origin " + branch, gitWorkingDir, context));
        }

        // Robust command handling for complex commit messages
        List<String> fullCmd = new ArrayList<>();
        fullCmd.add("git");

        // Split by space but preserve quoted segments
        if (command.contains("\"")) {
            int firstQuote = command.indexOf("\"");
            int lastQuote = command.lastIndexOf("\"");
            String before = command.substring(0, firstQuote).trim();
            String msg = command.substring(firstQuote + 1, lastQuote);
            if (!before.isEmpty()) {
                for (String p : before.split(" ")) {
                    if (!p.isEmpty()) fullCmd.add(p);
                }
            }
            fullCmd.add(msg);
        } else {
            for (String p : command.split(" ")) {
                if (!p.isEmpty()) fullCmd.add(p);
            }
        }

        if (command.toLowerCase().startsWith("status")) {
            output.append(shell.execute("git status --porcelain", gitWorkingDir, context));
        }

        if (output.length() == 0) {
            return "No git action mapped for: " + command;
        }
        return output.toString().trim();
    }

    public List<String> getBranches(File root) throws Exception {
        String output = execute("branch --format=%(refname:short)", root, null);
        List<String> branches = new ArrayList<>();
        if (output != null) {
            for (String line : output.split("\n")) {
                if (!line.trim().isEmpty()) {
                    branches.add(line.trim());
                }
            }
        }
        return branches;
    }
}
