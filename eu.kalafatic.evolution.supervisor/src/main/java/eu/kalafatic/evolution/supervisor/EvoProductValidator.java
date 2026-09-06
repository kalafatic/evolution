package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EvoProductValidator {
    private final ProcessRunner runner;

    public EvoProductValidator() {
        this.runner = new ProcessRunner();
    }

    public EvoProductValidator(ProcessRunner runner) {
        this.runner = runner != null ? runner : new ProcessRunner();
    }

    public boolean validateProduct(Path archivePath) {
        if (runner != null && runner.getMockVerifyResult() != null) {
            System.out.println("[EvoProductValidator] [MOCK] Bypassing actual product validation, returning mock verify result: " + runner.getMockVerifyResult());
            return runner.getMockVerifyResult();
        }

        if (archivePath == null || !Files.exists(archivePath)) {
            System.err.println("[EvoProductValidator] Archive file does not exist!");
            return false;
        }

        File archiveFile = archivePath.toFile();
        File tempValDir = new File(archiveFile.getParentFile(), "temp_validation_" + System.currentTimeMillis());
        if (!tempValDir.exists()) {
            tempValDir.mkdirs();
        }

        try {
            System.out.println("[EvoProductValidator] Extracting archive: " + archiveFile.getName() + " to " + tempValDir.getAbsolutePath());
            if (archiveFile.getName().endsWith(".zip")) {
                unzip(archiveFile, tempValDir);
            } else if (archiveFile.getName().endsWith(".tar.gz")) {
                untar(archiveFile, tempValDir);
            } else {
                System.err.println("[EvoProductValidator] Unsupported archive format: " + archiveFile.getName());
                return false;
            }

            // Find product root folder dynamically
            File[] files = tempValDir.listFiles(File::isDirectory);
            File rootDir = (files != null && files.length > 0) ? files[0] : tempValDir;
            System.out.println("[EvoProductValidator] Discovered product root folder: " + rootDir.getAbsolutePath());

            // Validate file layout
            if (!validateLayout(rootDir)) {
                System.err.println("[EvoProductValidator] Layout validation FAILED!");
                return false;
            }

            // Headless Launch and Startup Verification
            if (!verifyStartup(rootDir)) {
                System.err.println("[EvoProductValidator] Headless startup verification FAILED!");
                return false;
            }

            System.out.println("[EvoProductValidator] All product validation checks PASSED successfully!");
            return true;

        } catch (Exception e) {
            System.err.println("[EvoProductValidator] Validation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Clean up temporary validation directory
            File readyFile = new File(tempValDir, "self-dev-run/evo_ready.txt");
            if (readyFile.exists()) {
                readyFile.delete();
            }
            File readyFileSub = new File(tempValDir, "evolution/self-dev-run/evo_ready.txt");
            if (readyFileSub.exists()) {
                readyFileSub.delete();
            }
            deleteRecursively(tempValDir);
        }
    }

    boolean validateLayout(File rootDir) {
        List<String> missingItems = new ArrayList<>();

        if (rootDir == null || !rootDir.exists()) {
            missingItems.add("Product Root Directory (" + (rootDir != null ? rootDir.getAbsolutePath() : "null") + ")");
            printValidationFailure(rootDir, missingItems, "Root directory does not exist.");
            return false;
        }

        // Executable verification
        if (PlatformInfo.isWindows()) {
            File exeFile = new File(rootDir, "evo.exe");
            if (!exeFile.exists()) {
                missingItems.add("evo.exe");
            }
        } else {
            File nativeLauncher = new File(rootDir, "evo");
            File shLauncher = new File(rootDir, "evo.sh");
            if (!nativeLauncher.exists() && !shLauncher.exists()) {
                missingItems.add("evo (or evo.sh)");
            }
        }

        // evo.ini verification
        File evoIni = new File(rootDir, "evo.ini");
        if (!evoIni.exists()) {
            missingItems.add("evo.ini");
        }

        // plugins/ directory & EVO view bundle check
        File pluginsDir = new File(rootDir, "plugins");
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            missingItems.add("plugins/ directory");
        } else {
            File[] viewBundle = pluginsDir.listFiles((dir, name) ->
                name.startsWith("eu.kalafatic.evolution.view_") && name.endsWith(".jar")
            );
            if (viewBundle == null || viewBundle.length == 0) {
                missingItems.add("EVO application bundle (eu.kalafatic.evolution.view_*.jar)");
            }
        }

        // configuration/ directory & config.ini check
        File configDir = new File(rootDir, "configuration");
        if (!configDir.exists() || !configDir.isDirectory()) {
            missingItems.add("configuration/ directory");
        } else {
            File configIni = new File(configDir, "config.ini");
            if (!configIni.exists()) {
                missingItems.add("configuration/config.ini");
            } else {
                // Verify eclipse.application or eclipse.product
                try {
                    String configContent = Files.readString(configIni.toPath());
                    if (!configContent.contains("eclipse.application") && !configContent.contains("eclipse.product")) {
                        missingItems.add("eclipse.application / eclipse.product property in configuration/config.ini");
                    }
                } catch (IOException e) {
                    missingItems.add("Readable configuration/config.ini (" + e.getMessage() + ")");
                }
            }
        }

        if (!missingItems.isEmpty()) {
            printValidationFailure(rootDir, missingItems, "Missing critical product artifacts or configuration entries.");
            return false;
        }

        System.out.println("[LAYOUT] File layout verification successful.");
        return true;
    }

    private void printValidationFailure(File rootDir, List<String> missingItems, String reason) {
        System.err.println("==========================================================");
        System.err.println("EVO PRODUCT VALIDATION FAILED");
        System.err.println("Product root: " + (rootDir != null ? rootDir.getAbsolutePath() : "null"));
        System.err.println("Application: eu.kalafatic.evolution.view.application.Application");
        System.err.println("Reason: " + reason);
        System.err.println("Missing items:");
        for (String item : missingItems) {
            System.err.println("  - " + item);
        }
        System.err.println("==========================================================");
    }

    private boolean verifyStartup(File rootDir) {
        File readyFile = new File(rootDir, "self-dev-run/evo_ready.txt");
        if (readyFile.exists()) {
            readyFile.delete();
        }

        List<String> command = new ArrayList<>();
        if (PlatformInfo.isWindows()) {
            command.add(new File(rootDir, "evo.exe").getAbsolutePath());
        } else {
            File shLauncher = new File(rootDir, "evo.sh");
            File nativeLauncher = new File(rootDir, "evo");
            if (shLauncher.exists()) {
                shLauncher.setExecutable(true);
                command.add(shLauncher.getAbsolutePath());
            } else if (nativeLauncher.exists()) {
                nativeLauncher.setExecutable(true);
                command.add(nativeLauncher.getAbsolutePath());
            } else {
                command.add("./evo.sh");
            }
        }
        command.add("--server");
        command.add("--port");
        command.add("59099");

        System.out.println("[STARTUP] Launching headless product: " + command);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(rootDir);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = null;
        try {
            process = pb.start();

            // Wait for readiness marker file
            System.out.println("[STARTUP] Waiting for readiness signal (self-dev-run/evo_ready.txt)...");
            long timeoutMs = 30000;
            long start = System.currentTimeMillis();
            boolean ready = false;

            while (System.currentTimeMillis() - start < timeoutMs) {
                if (readyFile.exists()) {
                    ready = true;
                    break;
                }
                if (!process.isAlive()) {
                    System.err.println("[STARTUP] Product process exited prematurely with exit code: " + process.exitValue());
                    return false;
                }
                Thread.sleep(500);
            }

            if (!ready) {
                System.err.println("[STARTUP] Timeout reached waiting for product readiness signal!");
                return false;
            }

            System.out.println("[STARTUP] Product is READY! Reached operational state in " + (System.currentTimeMillis() - start) + "ms.");

            // Request Graceful Shutdown via unauthenticated /api/shutdown endpoint
            System.out.println("[STARTUP] Triggering clean shutdown...");
            triggerShutdown("http://127.0.0.1:59099/api/shutdown");

            // Wait for normal termination
            boolean exited = process.waitFor(5, TimeUnit.SECONDS);
            if (exited) {
                System.out.println("[STARTUP] Product shut down cleanly.");
            } else {
                System.err.println("[STARTUP] Product did not shut down within timeout. Forcing termination...");
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }

            return true;

        } catch (Exception e) {
            System.err.println("[STARTUP] Verification error: " + e.getMessage());
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            return false;
        }
    }

    private void triggerShutdown(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            System.out.println("[STARTUP] Shutdown endpoint response code: " + code);
        } catch (Exception e) {
            System.out.println("[STARTUP] Shutdown request sent. Exception (expected on immediate exit): " + e.getMessage());
        }
    }

    private void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                File filePath = new File(destDir, entry.getName());
                if (!entry.isDirectory()) {
                    if (filePath.getParentFile() != null && !filePath.getParentFile().exists()) {
                        filePath.getParentFile().mkdirs();
                    }
                    try (FileOutputStream bos = new FileOutputStream(filePath)) {
                        byte[] bytesIn = new byte[4096];
                        int read;
                        while ((read = zipIn.read(bytesIn)) != -1) {
                            bos.write(bytesIn, 0, read);
                        }
                    }
                } else {
                    filePath.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private void untar(File tarFile, File destDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tarFile.getAbsolutePath(), "-C", destDir.getAbsolutePath());
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            throw new IOException("tar extraction failed with exit code: " + code);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
