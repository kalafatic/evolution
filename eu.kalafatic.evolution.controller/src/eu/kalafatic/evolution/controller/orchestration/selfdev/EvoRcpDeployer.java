package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EvoRcpDeployer extends AbstractDeployer<BuildArtifact> {

    public EvoRcpDeployer() {
        super("evo_rcp_deployer");
    }

    @Override
    public TaskResult deploy(SelfDevContext context, BuildArtifact artifact) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("deploy_evo_rcp", "SelfDevContext is null", null);
        }
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure("deploy_evo_rcp", "Invalid or non-existent EVO RCP artifact", null);
        }

        File targetRuntimeDir = context.getRuntimeDirectory();
        if (targetRuntimeDir == null) {
            return TaskResult.failure("deploy_evo_rcp", "Target runtime directory in context is null", null);
        }

        File evoRuntimeSubDir = new File(targetRuntimeDir, "evo");
        try {
            deleteRecursively(evoRuntimeSubDir);
            evoRuntimeSubDir.mkdirs();

            File artifactFile = artifact.getPath();
            if (artifactFile.isDirectory()) {
                copyDirectory(artifactFile, evoRuntimeSubDir);
            } else if (artifactFile.getName().endsWith(".zip")) {
                unzip(artifactFile, evoRuntimeSubDir);
            } else {
                File dest = new File(evoRuntimeSubDir, artifactFile.getName());
                Files.copy(artifactFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            TaskResult valRes = validateDeployment(context, evoRuntimeSubDir);
            if (!valRes.isSuccess()) {
                return valRes;
            }

            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder("deploy_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("Successfully deployed EVO RCP product to " + evoRuntimeSubDir.getAbsolutePath())
                    .duration(duration)
                    .workingDirectory(evoRuntimeSubDir)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure("deploy_evo_rcp", "Deployment exception: " + e.getMessage(), e);
        }
    }

    @Override
    public TaskResult validateDeployment(SelfDevContext context, File deployedLocation) {
        if (deployedLocation == null || !deployedLocation.exists()) {
            return TaskResult.failure("deploy_evo_rcp_validation", "Deployed location does not exist: " + (deployedLocation != null ? deployedLocation.getAbsolutePath() : "null"), null);
        }

        File pluginsDir = findFileRecursively(deployedLocation, "plugins");
        if (pluginsDir == null || !pluginsDir.isDirectory()) {
            return TaskResult.failure("deploy_evo_rcp_validation", "Deployment validation failed: 'plugins' directory not found in " + deployedLocation.getAbsolutePath(), null);
        }

        File executable = findExecutable(deployedLocation);
        if (executable == null) {
            return TaskResult.failure("deploy_evo_rcp_validation", "Deployment validation failed: product executable not found in " + deployedLocation.getAbsolutePath(), null);
        }

        return new TaskResult.Builder("deploy_evo_rcp_validation")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP deployment validated. Executable: " + executable.getName())
                .workingDirectory(deployedLocation)
                .build();
    }

    private File findExecutable(File root) {
        File[] execs = root.listFiles((dir, name) -> name.equals("evo.exe") || name.equals("evo") || name.equals("eclipse.exe") || name.equals("eclipse"));
        if (execs != null && execs.length > 0) return execs[0];

        File[] subdirs = root.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File sub : subdirs) {
                File found = findExecutable(sub);
                if (found != null) return found;
            }
        }
        return null;
    }

    private File findFileRecursively(File dir, String targetName) {
        if (dir.getName().equalsIgnoreCase(targetName)) return dir;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    File found = findFileRecursively(child, targetName);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private void copyDirectory(File src, File dest) throws IOException {
        Path srcPath = src.toPath();
        Path destPath = dest.toPath();
        Files.walk(srcPath).forEach(source -> {
            try {
                Path destination = destPath.resolve(srcPath.relativize(source));
                if (Files.isDirectory(source)) {
                    if (!Files.exists(destination)) Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
