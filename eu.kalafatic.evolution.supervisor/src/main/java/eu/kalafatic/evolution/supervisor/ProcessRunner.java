package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {

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

    public boolean runRCP(File variantDir, String jarName) {
        System.out.println("[RUN] Running RCP in " + variantDir.getAbsolutePath());
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarName);
        command.add("--mode=SELF_DEV");
        command.add("--variant=" + variantDir.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(variantDir);
        pb.inheritIO();
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                System.err.println("[RUN] Timeout reached. Killing process.");
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("[RUN] Failed: " + e.getMessage());
            return false;
        }
    }
}
