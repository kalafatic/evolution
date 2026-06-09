package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
    private Process currentProcess;

    public boolean runBuild(File variantDir) {
        String os = System.getProperty("os.name").toLowerCase();
        String mvnCmd = os.contains("win") ? "mvn.cmd" : "mvn";
        System.out.println("[BUILD] Running " + mvnCmd + " clean package -DskipTests in " + variantDir.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(mvnCmd, "clean", "package", "-DskipTests");
        pb.directory(variantDir);
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("[BUILD] Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean runTests(File variantDir) {
        String os = System.getProperty("os.name").toLowerCase();
        String mvnCmd = os.contains("win") ? "mvn.cmd" : "mvn";
        System.out.println("[TEST] Running " + mvnCmd + " test in " + variantDir.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(mvnCmd, "test");
        pb.directory(variantDir);
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("[TEST] Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean applyPatch(File baseDir, String diff) {
        System.out.println("[PATCH] Applying diff to " + baseDir.getAbsolutePath());
        File patchFile = new File(baseDir, "temp.patch");
        try {
            try (FileOutputStream fos = new FileOutputStream(patchFile)) {
                fos.write(diff.getBytes());
            }
            ProcessBuilder pb = new ProcessBuilder("git", "apply", "temp.patch");
            pb.directory(baseDir);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            patchFile.delete();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("[PATCH] Failed: " + e.getMessage());
            return false;
        }
    }

    public void stopRCP() {
        if (currentProcess != null && currentProcess.isAlive()) {
            System.out.println("[RUN] Stopping current process...");
            currentProcess.destroy();
            try {
                if (!currentProcess.waitFor(5, TimeUnit.SECONDS)) {
                    currentProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                currentProcess.destroyForcibly();
            }
        }
    }

    public boolean runApplication(File dir, String mainClass, String... args) {
        stopRCP();
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-cp");
        String cpSep = System.getProperty("path.separator");
        command.add("target/classes" + cpSep + "target/dependency/*"); // Assuming dependencies are here
        command.add(mainClass);
        for (String arg : args) command.add(arg);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir);
        pb.inheritIO();
        try {
            currentProcess = pb.start();
            return true;
        } catch (IOException e) {
            System.err.println("[RUN] Failed: " + e.getMessage());
            return false;
        }
    }
}
