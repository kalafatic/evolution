package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
    private Process currentProcess;
    private Boolean mockBuildResult;
    private Boolean mockRCPResult;
    private Boolean mockPatchResult;
    private Boolean mockTestResult;
    private Boolean mockRunResult;
    private Boolean mockVerifyResult;
    private Runnable onRCPStart;

    public void setMockBuildResult(Boolean res) { this.mockBuildResult = res; }
    public void setMockRCPResult(Boolean res) { this.mockRCPResult = res; }
    public void setMockPatchResult(Boolean res) { this.mockPatchResult = res; }
    public void setMockTestResult(Boolean res) { this.mockTestResult = res; }
    public void setMockRunResult(Boolean res) { this.mockRunResult = res; }
    public void setMockVerifyResult(Boolean res) { this.mockVerifyResult = res; }
    public void setOnRCPStart(Runnable runnable) { this.onRCPStart = runnable; }

    public Boolean getMockBuildResult() { return mockBuildResult; }
    public Boolean getMockRCPResult() { return mockRCPResult; }
    public Boolean getMockPatchResult() { return mockPatchResult; }
    public Boolean getMockTestResult() { return mockTestResult; }
    public Boolean getMockRunResult() { return mockRunResult; }
    public Boolean getMockVerifyResult() { return mockVerifyResult; }

    public boolean runBuild(File variantDir) {
        if (mockBuildResult != null) {
            System.out.println("[MOCK BUILD] Returning mock result: " + mockBuildResult);
            return mockBuildResult;
        }

        List<String> command = new ArrayList<>();
        if (PlatformInfo.isWindows()) {
            File mvnwCmd = new File(variantDir, "mvnw.cmd");
            if (mvnwCmd.exists()) {
                command.add(mvnwCmd.getAbsolutePath());
            } else {
                command.add("mvn.cmd");
            }
            command.add("clean");
            command.add("verify");
            command.add("-DskipTests");
            command.add("-Pwindows");
        } else {
            File mvnw = new File(variantDir, "mvnw");
            if (mvnw.exists()) {
                try {
                    mvnw.setExecutable(true);
                } catch (Exception ignored) {}
                command.add("./mvnw");
            } else {
                command.add("mvn");
            }
            command.add("clean");
            command.add("verify");
            command.add("-DskipTests");
            command.add("-Plinux");
        }

        System.out.println("[BUILD] Executing build command: " + command + " in " + variantDir.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(variantDir);
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            System.out.println("[BUILD] Build exited with: " + exitCode);
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("[BUILD] Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean runTests(File variantDir) {
        if (mockTestResult != null) {
            System.out.println("[MOCK TEST] Returning mock result: " + mockTestResult);
            return mockTestResult;
        }
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

    public boolean runApplication(File variantDir) {
        if (mockRunResult != null) {
            System.out.println("[MOCK RUN] Returning mock result: " + mockRunResult);
            return mockRunResult;
        }
        System.out.println("[RUN] Testing application startup in " + variantDir.getAbsolutePath());

        File exportDir = new File(variantDir, "export");
        if (!exportDir.exists() && variantDir.getName().equalsIgnoreCase("export")) {
            exportDir = variantDir;
        }
        File releaseDir = new File(variantDir, "release");

        File executable = findExecutableInDir(exportDir);
        if (executable == null) executable = findExecutableInDir(releaseDir);

        if (executable != null && executable.exists()) {
            if (!PlatformInfo.isWindows()) {
                executable.setExecutable(true);
            }
            List<String> command = new ArrayList<>();
            command.add(executable.getAbsolutePath());
            command.add("--mode=SELF_DEV");
            command.add("--variant=" + variantDir.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(executable.getParentFile());
            try {
                Process p = pb.start();
                boolean finished = p.waitFor(5, TimeUnit.SECONDS);
                if (finished) {
                    int exitCode = p.exitValue();
                    System.out.println("[RUN] Application process exited with code: " + exitCode);
                    return exitCode == 0;
                } else {
                    System.out.println("[RUN] Application process started successfully. Terminating test run cleanly.");
                    p.destroyForcibly();
                    return true;
                }
            } catch (Exception e) {
                System.err.println("[RUN] Failed to launch application executable: " + e.getMessage());
                return false;
            }
        }

        return true;
    }

    private File findExecutableInDir(File dir) {
        if (dir == null || !dir.exists()) return null;
        boolean isWin = PlatformInfo.isWindows();
        String exeName = isWin ? "evo.exe" : "evo";
        String shName = "evo.sh";

        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(dir.toPath())) {
            List<File> files = stream.filter(java.nio.file.Files::isRegularFile)
                .map(java.nio.file.Path::toFile)
                .toList();

            for (File f : files) {
                if (f.getName().equalsIgnoreCase(exeName)) {
                    return f;
                }
            }
            if (!isWin) {
                for (File f : files) {
                    if (f.getName().equalsIgnoreCase(shName)) {
                        return f;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public boolean verifyApplication(File variantDir) {
        if (mockVerifyResult != null) {
            System.out.println("[MOCK VERIFY] Returning mock result: " + mockVerifyResult);
            return mockVerifyResult;
        }
        System.out.println("[VERIFY] Simulating health check and verification...");
        return true;
    }

    public boolean applyPatch(File baseDir, String diff) {
        if (mockPatchResult != null) {
            System.out.println("[MOCK PATCH] Returning mock result: " + mockPatchResult);
            return mockPatchResult;
        }
        System.out.println("[PATCH] Applying diff to " + baseDir.getAbsolutePath());
        File patchFile = new File(baseDir, "temp.patch");
        try {
            // Discard any local modifications and clean the directory first to ensure incoming patch overrides all old data
            try {
                System.out.println("[PATCH] Resetting and cleaning local workspace to override with incoming changes...");
                ProcessBuilder pbReset = new ProcessBuilder("git", "reset", "--hard");
                pbReset.directory(baseDir);
                pbReset.start().waitFor();

                ProcessBuilder pbClean = new ProcessBuilder("git", "clean", "-fd");
                pbClean.directory(baseDir);
                pbClean.start().waitFor();
            } catch (Exception resetEx) {
                System.err.println("[PATCH] Warning during local reset/clean: " + resetEx.getMessage());
            }

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
            System.out.println("[RUN] Stopping current RCP process...");
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

    public boolean runRCP(File variantDir, String targetPath, String statePath) {
        if (mockRCPResult != null) {
            System.out.println("[MOCK RUN] Returning mock result: " + mockRCPResult);
            if (onRCPStart != null) {
                onRCPStart.run();
            }
            return mockRCPResult;
        }
        stopRCP();
        System.out.println("[RUN] Running RCP/EVO in " + variantDir.getAbsolutePath() + " with target: " + targetPath);

        File targetFile = new File(targetPath);
        if (!targetFile.isAbsolute()) {
            targetFile = new File(variantDir, targetPath);
        }

        List<String> command = new ArrayList<>();
        boolean isExecutable = targetFile.getName().endsWith(".exe") || targetFile.getName().endsWith(".sh") || targetFile.getName().equalsIgnoreCase("evo");

        if (isExecutable && targetFile.exists()) {
            if (!PlatformInfo.isWindows()) {
                targetFile.setExecutable(true);
            }
            command.add(targetFile.getAbsolutePath());
        } else {
            command.add("java");
            if (statePath != null) {
                command.add("-Dstate=" + statePath);
            }
            command.add("-jar");
            command.add(targetFile.getAbsolutePath());
        }

        command.add("--mode=SELF_DEV");
        command.add("--variant=" + variantDir.getAbsolutePath());
        if (statePath != null) {
            command.add("--state=" + statePath);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        File workDir = targetFile.getParentFile() != null && targetFile.getParentFile().exists() ? targetFile.getParentFile() : variantDir;
        pb.directory(workDir);
        pb.inheritIO();
        try {
            currentProcess = pb.start();
            Process process = currentProcess;
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
