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
        if (context != null) context.log("Tool [GitTool]: Running " + command);
        File gitWorkingDir = workingDir;
        String branch = "master";

        if (context != null && context.getOrchestrator() != null) {
            Git gitSettings = context.getOrchestrator().getGit();
            if (gitSettings != null) {
                if (gitSettings.getLocalPath() != null && !gitSettings.getLocalPath().isEmpty()) {
                    File subDir = new File(workingDir, gitSettings.getLocalPath());
                    if (subDir.exists() && subDir.isDirectory()) {
                        gitWorkingDir = subDir;
                    }
                }
                if (gitSettings.getBranch() != null && !gitSettings.getBranch().isEmpty()) {
                    branch = gitSettings.getBranch();
                }
            }
        }

        ShellTool shell = new ShellTool();

        // SPECIALIZED LOGIC: Commit metadata injection
        if (command.startsWith("commit")) {
            executeWithRetry(shell, "git add .", gitWorkingDir, context);

            StringBuilder metadata = new StringBuilder();
            if (context != null && context.getOrchestrationState() != null) {
                String iterationId = context.getOrchestrationState().getCurrentIterationId();
                String taskId = context.getCurrentTaskId();
                metadata.append(String.format("[EVO-META] [Iteration: %s] [Task: %s]",
                    iterationId != null ? iterationId : "unknown",
                    taskId != null ? taskId : "none"));

                // Evolutionary Lineage Injection
                eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
                if (tree != null && tree.getCurrentWinnerId() != null) {
                    eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionNode node = tree.getWinnerNode();
                    if (node != null) {
                        metadata.append(String.format("\n[EVO-LINEAGE] [Node: %s] [Parent: %s] [Mutation: %s] [Fitness: %.2f]",
                            node.getId(),
                            node.getParentId() != null ? node.getParentId() : "root",
                            node.getStrategy(),
                            node.getFitnessScore()));

                        // Inject affected dimensions if available
                        if (node.getMutationRecord() != null && !node.getMutationRecord().getEngineeringDimensions().isEmpty()) {
                            String dims = node.getMutationRecord().getEngineeringDimensions().entrySet().stream()
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(java.util.stream.Collectors.joining(", "));
                            metadata.append("\n[EVO-DIMENSIONS] ").append(dims);
                        }
                    }
                }
            }

            String commitMsg = command.contains("-m") ? "" : " -m \"Darwin evolution step\"";
            String metadataArg = metadata.length() > 0 ? " -m \"" + metadata.toString().replace("\"", "\\\"") + "\"" : "";
            String fullCommand = "git " + command + commitMsg + metadataArg;
            return executeWithRetry(shell, fullCommand, gitWorkingDir, context);
        }

        // SPECIALIZED LOGIC: Push handling
        if (command.startsWith("push")) {
            String fullCommand = "git push origin " + branch;
            return executeWithRetry(shell, fullCommand, gitWorkingDir, context);
        }

        // Generic pass-through for other commands (init, checkout, branch, etc.)
        String fullCommand = command.startsWith("git ") ? command : "git " + command;
        return executeWithRetry(shell, fullCommand, gitWorkingDir, context);
    }

    private String executeWithRetry(ShellTool shell, String command, File workingDir, TaskContext context) throws Exception {
        try {
            return shell.execute(command, workingDir, context);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("index.lock")) {
                if (context != null) context.log("Git lock detected. Attempting to clear stale index.lock.");
                // Try to find .git/index.lock in workingDir or its parents
                File current = workingDir;
                while (current != null) {
                    File gitLock = new File(current, ".git/index.lock");
                    if (gitLock.exists()) {
                        if (gitLock.delete()) {
                            if (context != null) context.log("Stale index.lock removed from " + current.getAbsolutePath() + ". Retrying command.");
                            return shell.execute(command, workingDir, context);
                        }
                    }
                    current = current.getParentFile();
                }
            }
            throw e;
        }
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
