package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GitManager {
    private final File root;

    public GitManager(File root) {
        this.root = root;
    }

    private String runGit(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String arg : args) command.add(arg);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(root);
        Process process = pb.start();

        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Git command failed with exit code " + exitCode + ": git " + String.join(" ", args));
        }
        return output.toString().trim();
    }

    public void init() throws Exception {
        runGit("init");
        runGit("config", "user.email", "evolution@kalafatic.eu");
        runGit("config", "user.name", "Evolution Supervisor");
    }

    public String getCurrentBranch() throws Exception {
        return runGit("rev-parse", "--abbrev-ref", "HEAD");
    }

    public void createBranch(String branchName) throws Exception {
        runGit("checkout", "-b", branchName);
    }

    public void commit(String message) throws Exception {
        runGit("add", ".");
        runGit("commit", "--allow-empty", "-m", message);
    }

    public void rollback() throws Exception {
        runGit("reset", "--hard", "HEAD");
        runGit("clean", "-fd");
    }

    public void checkout(String branch) throws Exception {
        runGit("checkout", branch);
    }
}
