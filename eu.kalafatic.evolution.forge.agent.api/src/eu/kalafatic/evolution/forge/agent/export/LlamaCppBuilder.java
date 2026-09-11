package eu.kalafatic.evolution.forge.agent.export;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * LlamaCppBuilder - Downloads pre-built llama.cpp binaries
 * No external dependencies - pure Java!
 */
public class LlamaCppBuilder {
    
    private static final String LLAMA_CPP_DIR = System.getProperty("user.home") + "/llama.cpp";
    private static final String GITHUB_API = "https://api.github.com/repos/ggerganov/llama.cpp/releases/latest";
    
    // These URLs use the actual GitHub release pattern
    private static final String RELEASE_BASE = "https://github.com/ggerganov/llama.cpp/releases/latest/download/";
    
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("nix") || 
                                              System.getProperty("os.name").toLowerCase().contains("nux");
    private static final boolean IS_ARM = System.getProperty("os.arch").toLowerCase().contains("aarch64") ||
                                           System.getProperty("os.arch").toLowerCase().contains("arm");
    
    private static String llamaCliPath = null;
    private static String llamaServerPath = null;
    private static boolean isBuilt = false;
    
    /**
     * Ensures llama.cpp binaries are available
     */
    public static synchronized void ensureLlamaCppAvailable() throws IOException, InterruptedException {
        if (isBuilt) {
            return;
        }
        
        System.out.println("[LlamaCpp] Checking llama.cpp installation...");
        
        String cliPath = getLlamaCliPath();
        if (Files.exists(Paths.get(cliPath))) {
            System.out.println("[LlamaCpp] llama.cpp found at: " + cliPath);
            llamaCliPath = cliPath;
            llamaServerPath = getLlamaServerPath();
            isBuilt = true;
            return;
        }
        
        // Try to find existing binaries in common locations
        if (findExistingBinaries()) {
            isBuilt = true;
            return;
        }
        
        System.out.println("[LlamaCpp] llama.cpp not found. Downloading...");
        downloadPrebuiltBinaries();
        
        // Verify
        if (Files.exists(Paths.get(getLlamaCliPath()))) {
            System.out.println("[LlamaCpp] Download successful!");
            llamaCliPath = getLlamaCliPath();
            llamaServerPath = getLlamaServerPath();
            isBuilt = true;
        } else {
            System.err.println("[LlamaCpp] Auto-download failed. Please download manually from:");
            System.err.println("[LlamaCpp] https://github.com/ggerganov/llama.cpp/releases");
            throw new IOException("Failed to download llama.cpp binaries.");
        }
    }
    
    /**
     * Tries to find existing llama.cpp binaries in common locations
     */
    private static boolean findExistingBinaries() {
        String osDir = IS_WINDOWS ? "win" : (IS_MAC ? "mac" : "linux");
        String cliName = IS_WINDOWS ? "llama-cli.exe" : "llama-cli";

        String osgiPath = resolveFromOsgiBundle(osDir, cliName);
        if (osgiPath != null && Files.exists(Paths.get(osgiPath))) {
            llamaCliPath = osgiPath;
            String serverPath = osgiPath.replace("cli", "server");
            if (Files.exists(Paths.get(serverPath))) {
                llamaServerPath = serverPath;
            }
            makeExecutableIfUnix(llamaCliPath);
            System.out.println("[LlamaCpp] Found existing OSGi installation at: " + llamaCliPath);
            return true;
        }

        String codebasePath = getCodebasePathViaReflection();
        java.util.List<String> searchPaths = new java.util.ArrayList<>();
        if (codebasePath != null) {
            searchPaths.add(codebasePath + "/eu.kalafatic.evolution.controller/lib/llama-cpp/" + osDir + "/" + cliName);
            searchPaths.add(codebasePath + "/eu.kalafatic.evolution.controller/lib/llama-cpp/linux/" + cliName);
            searchPaths.add(codebasePath + "/eu.kalafatic.evolution.controller/lib/llama-cpp/win/" + cliName);
            searchPaths.add(codebasePath + "/lib/llama-cpp/" + osDir + "/" + cliName);
        }
        String userDir = System.getProperty("user.dir");
        searchPaths.add(userDir + "/eu.kalafatic.evolution.controller/lib/llama-cpp/" + osDir + "/" + cliName);
        searchPaths.add(userDir + "/eu.kalafatic.evolution.controller/lib/llama-cpp/linux/" + cliName);
        searchPaths.add(userDir + "/eu.kalafatic.evolution.controller/lib/llama-cpp/win/" + cliName);
        searchPaths.add(userDir + "/../eu.kalafatic.evolution.controller/lib/llama-cpp/" + osDir + "/" + cliName);
        searchPaths.add(userDir + "/../eu.kalafatic.evolution.controller/lib/llama-cpp/linux/" + cliName);
        searchPaths.add(userDir + "/../eu.kalafatic.evolution.controller/lib/llama-cpp/win/" + cliName);
        searchPaths.add(userDir + "/lib/llama-cpp/" + osDir + "/" + cliName);
        searchPaths.add(userDir + "/eu.kalafatic.evolution.forge.agent.api/lib/llama-cpp/" + osDir + "/" + cliName);
        searchPaths.add(System.getProperty("user.home") + "/llama.cpp/build/bin/llama-cli" + (IS_WINDOWS ? ".exe" : ""));
        searchPaths.add(System.getProperty("user.home") + "/llama.cpp/llama-cli" + (IS_WINDOWS ? ".exe" : ""));
        searchPaths.add(System.getProperty("user.home") + "/.local/bin/llama-cli" + (IS_WINDOWS ? ".exe" : ""));
        searchPaths.add("/usr/local/bin/llama-cli" + (IS_WINDOWS ? ".exe" : ""));
        searchPaths.add("/usr/bin/llama-cli" + (IS_WINDOWS ? ".exe" : ""));

        for (String path : searchPaths) {
            try {
                Path p = Paths.get(path);
                if (Files.exists(p)) {
                    llamaCliPath = p.toAbsolutePath().normalize().toString();
                    String serverPath = llamaCliPath.replace("cli", "server");
                    if (Files.exists(Paths.get(serverPath))) {
                        llamaServerPath = serverPath;
                    }
                    makeExecutableIfUnix(llamaCliPath);
                    System.out.println("[LlamaCpp] Found existing installation at: " + llamaCliPath);
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static String getCodebasePathViaReflection() {
        try {
            Class<?> clazz = Class.forName("eu.kalafatic.evolution.controller.manager.ProjectModelManager");
            return (String) clazz.getMethod("getCodebasePath").invoke(null);
        } catch (Throwable t1) {
            try {
                Class<?> clazz = Class.forName("eu.kalafatic.evolution.view.provider.ProjectManager");
                return (String) clazz.getMethod("getCodebasePath").invoke(null);
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    private static String resolveFromOsgiBundle(String osDir, String cliName) {
        try {
            Class<?> frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil");

            java.util.List<Object> bundles = new java.util.ArrayList<>();
            Object b1 = frameworkUtilClass.getMethod("getBundle", Class.class).invoke(null, LlamaCppBuilder.class);
            if (b1 != null) bundles.add(b1);

            try {
                Class<?> llamaServiceClass = Class.forName("eu.kalafatic.evolution.controller.manager.LlamaService");
                Object b2 = frameworkUtilClass.getMethod("getBundle", Class.class).invoke(null, llamaServiceClass);
                if (b2 != null && !bundles.contains(b2)) bundles.add(b2);
            } catch (Throwable ignored) {}

            for (Object bundle : bundles) {
                String[] candidateSubPaths = {
                    "/lib/llama-cpp/" + osDir + "/" + cliName,
                    "/lib/llama-cpp/linux/" + cliName,
                    "/lib/llama-cpp/win/" + cliName,
                    "/lib/llama-cpp/" + cliName
                };

                for (String subPath : candidateSubPaths) {
                    Object entryUrl = bundle.getClass().getMethod("getEntry", String.class).invoke(bundle, subPath);
                    if (entryUrl != null) {
                        Class<?> fileLocatorClass = Class.forName("org.eclipse.core.runtime.FileLocator");
                        java.net.URL fileUrl = (java.net.URL) fileLocatorClass.getMethod("toFileURL", java.net.URL.class).invoke(null, entryUrl);
                        if (fileUrl != null) {
                            File extractedFile = new File(fileUrl.toURI());
                            if (extractedFile.exists()) {
                                return extractedFile.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // Ignore OSGi reflection failures
        }
        return null;
    }

    private static void makeExecutableIfUnix(String filePath) {
        if (!IS_WINDOWS && filePath != null) {
            try {
                File f = new File(filePath);
                if (f.exists()) {
                    f.setExecutable(true, false);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Gets the download URL using the correct GitHub release pattern
     */
    private static String getBinaryUrl() throws IOException, InterruptedException {
        // First try the GitHub API
        try {
            System.out.println("[LlamaCpp] Fetching latest release info from API...");
            
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Java-LlamaCpp-Downloader")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String url = parseAssetsForUrl(response.body());
                if (url != null) {
                    return url;
                }
            }
        } catch (Exception e) {
            System.err.println("[LlamaCpp] GitHub API failed: " + e.getMessage());
        }
        
        // Fallback: Use the latest release URL (GitHub redirects to the actual file)
        System.out.println("[LlamaCpp] Using redirect URL...");
        return getRedirectUrl();
    }
    
    /**
     * Uses GitHub's redirect mechanism to get the latest release
     */
    private static String getRedirectUrl() {
        if (IS_WINDOWS) {
            return RELEASE_BASE + "llama-b4489-bin-win-vulkan-x64.zip";
        } else if (IS_MAC) {
            if (IS_ARM) {
                return RELEASE_BASE + "llama-b4489-bin-macos-arm64.zip";
            } else {
                return RELEASE_BASE + "llama-b4489-bin-macos-x64.zip";
            }
        } else if (IS_LINUX) {
            return RELEASE_BASE + "llama-b4489-bin-ubuntu-x64.zip";
        }
        throw new UnsupportedOperationException("Unsupported platform: " + System.getProperty("os.name"));
    }
    
    /**
     * Parses GitHub API response to find the right asset URL (pure Java, no Gson)
     */
    private static String parseAssetsForUrl(String json) {
        String platformPattern = getPlatformPattern();
        String downloadUrl = null;
        String fallbackUrl = null;
        
        // Find the assets array
        int assetsStart = json.indexOf("\"assets\":[");
        if (assetsStart == -1) return null;
        
        // Parse each asset manually
        int pos = assetsStart;
        while (pos < json.length()) {
            int nameStart = json.indexOf("\"name\":\"", pos);
            if (nameStart == -1) break;
            nameStart += 8;
            int nameEnd = json.indexOf("\"", nameStart);
            if (nameEnd == -1) break;
            String name = json.substring(nameStart, nameEnd);
            
            int urlStart = json.indexOf("\"browser_download_url\":\"", pos);
            if (urlStart == -1) break;
            urlStart += 24;
            int urlEnd = json.indexOf("\"", urlStart);
            if (urlEnd == -1) break;
            String url = json.substring(urlStart, urlEnd);
            
            String nameLower = name.toLowerCase();
            
            // Check if this matches our platform
            if (nameLower.contains(platformPattern)) {
                if (downloadUrl == null || 
                    (!nameLower.contains("vulkan") && !nameLower.contains("cuda") && !nameLower.contains("rocm"))) {
                    downloadUrl = url;
                    System.out.println("[LlamaCpp] Found asset: " + name);
                }
            }
            
            // Keep a fallback
            if (fallbackUrl == null && 
                (nameLower.contains("win") || nameLower.contains("windows") ||
                 nameLower.contains("linux") || nameLower.contains("ubuntu") ||
                 nameLower.contains("macos") || nameLower.contains("mac"))) {
                fallbackUrl = url;
            }
            
            pos = urlEnd + 1;
        }
        
        if (downloadUrl != null) return downloadUrl;
        if (fallbackUrl != null) return fallbackUrl;
        
        return null;
    }
    
    /**
     * Gets the platform pattern for matching assets
     */
    private static String getPlatformPattern() {
        if (IS_WINDOWS) {
            return "win";
        } else if (IS_MAC) {
            return IS_ARM ? "macos-arm64" : "macos-x64";
        } else if (IS_LINUX) {
            return "ubuntu";
        }
        return "unknown";
    }
    
    /**
     * Downloads pre-built binaries for the current platform
     */
    private static void downloadPrebuiltBinaries() throws IOException, InterruptedException {
        String url = getBinaryUrl();
        System.out.println("[LlamaCpp] Downloading from: " + url);
        
        Path tempZip = Paths.get(System.getProperty("java.io.tmpdir"), "llama-cpp.zip");
        Path extractDir = Paths.get(System.getProperty("java.io.tmpdir"), "llama-cpp-extract");
        
        try {
            // Download the zip - handle redirects
            downloadFileWithRedirect(url, tempZip);
            
            // Extract
            extractZip(tempZip, extractDir);
            
            // Copy binaries to the target directory
            Path targetDir = Paths.get(LLAMA_CPP_DIR);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            // Find and copy binaries
            copyBinaries(extractDir, targetDir);
            
            // Make executable on Unix
            if (!IS_WINDOWS) {
                makeExecutable(targetDir);
            }
            
        } finally {
            // Clean up
            try {
                Files.deleteIfExists(tempZip);
                deleteDirectory(extractDir);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        
        System.out.println("[LlamaCpp] Binaries installed to: " + LLAMA_CPP_DIR);
    }
    
    /**
     * Downloads a file with redirect handling
     */
    private static void downloadFileWithRedirect(String url, Path destination) throws IOException, InterruptedException {
        System.out.println("[LlamaCpp] Downloading...");
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "Java-LlamaCpp-Downloader")
                .GET()
                .build();
        
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        
        if (response.statusCode() != 200) {
            // Try alternative URL without version number
            String altUrl = url.replace("llama-b4489-bin-", "llama-");
            if (!altUrl.equals(url)) {
                System.out.println("[LlamaCpp] Retrying with: " + altUrl);
                request = HttpRequest.newBuilder()
                        .uri(URI.create(altUrl))
                        .timeout(Duration.ofMinutes(5))
                        .header("User-Agent", "Java-LlamaCpp-Downloader")
                        .GET()
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            }
            
            if (response.statusCode() != 200) {
                throw new IOException("Download failed with status: " + response.statusCode() + " for URL: " + url);
            }
        }
        
        // Write to file with progress
        long totalBytes = 0;
        try (InputStream in = response.body();
             FileOutputStream fos = new FileOutputStream(destination.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                if (totalBytes % (1024 * 1024) == 0) {
                    System.out.print(".");
                }
            }
        }
        System.out.println("\n[LlamaCpp] Download complete: " + Files.size(destination) + " bytes");
    }
    
    /**
     * Extracts a zip file
     */
    private static void extractZip(Path zipPath, Path destDir) throws IOException {
        System.out.println("[LlamaCpp] Extracting...");
        
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = destDir.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (FileOutputStream fos = new FileOutputStream(entryPath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        
        System.out.println("[LlamaCpp] Extraction complete.");
    }
    
    /**
     * Copies binaries from extracted folder to target directory
     */
    private static void copyBinaries(Path extractDir, Path targetDir) throws IOException {
        String[] binaryNames = getBinaryNames();
        boolean found = false;
        
        // Search recursively in the extract directory
        try (var stream = Files.walk(extractDir)) {
            for (Path file : (Iterable<Path>) stream::iterator) {
                if (Files.isRegularFile(file)) {
                    String fileName = file.getFileName().toString();
                    for (String binaryName : binaryNames) {
                        if (fileName.equalsIgnoreCase(binaryName) || 
                            fileName.replace(".exe", "").equalsIgnoreCase(binaryName.replace(".exe", ""))) {
                            Path target = targetDir.resolve(binaryName);
                            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("[LlamaCpp] Copied: " + binaryName);
                            found = true;
                            
                            if (binaryName.contains("cli") || binaryName.contains("llama-cli")) {
                                llamaCliPath = target.toString();
                            }
                            if (binaryName.contains("server") || binaryName.contains("llama-server")) {
                                llamaServerPath = target.toString();
                            }
                        }
                    }
                }
            }
        }
        
        if (!found) {
            // Try to find any executable files
            System.out.println("[LlamaCpp] Looking for executables...");
            try (var stream = Files.walk(extractDir)) {
                for (Path file : (Iterable<Path>) stream::iterator) {
                    if (Files.isRegularFile(file)) {
                        String name = file.getFileName().toString().toLowerCase();
                        if ((name.contains("llama") || name.contains("llama_")) && 
                            (name.endsWith(".exe") || !name.contains("."))) {
                            String targetName = file.getFileName().toString();
                            Path target = targetDir.resolve(targetName);
                            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("[LlamaCpp] Found executable: " + targetName);
                            found = true;
                            
                            if (targetName.contains("cli") || targetName.contains("llama-cli")) {
                                llamaCliPath = target.toString();
                            }
                            if (targetName.contains("server") || targetName.contains("llama-server")) {
                                llamaServerPath = target.toString();
                            }
                        }
                    }
                }
            }
        }
        
        if (!found) {
            // Try one more: list all files in extractDir
            System.out.println("[LlamaCpp] Contents of extracted archive:");
            try (var stream = Files.walk(extractDir)) {
                for (Path file : (Iterable<Path>) stream::iterator) {
                    if (Files.isRegularFile(file)) {
                        System.out.println("  " + file.getFileName());
                    }
                }
            }
            throw new IOException("No binaries found in extracted archive.");
        }
    }
    
    /**
     * Makes binaries executable on Unix systems
     */
    private static void makeExecutable(Path targetDir) {
        try {
            String[] binaryNames = {"llama-cli", "llama-server"};
            for (String name : binaryNames) {
                Path file = targetDir.resolve(name);
                if (Files.exists(file)) {
                    file.toFile().setExecutable(true);
                }
            }
        } catch (Exception e) {
            // Ignore - not critical
        }
    }
    
    /**
     * Gets the binary names for the current platform
     */
    private static String[] getBinaryNames() {
        if (IS_WINDOWS) {
            return new String[]{"llama-cli.exe", "llama-server.exe", "llama-cli", "llama-server"};
        } else {
            return new String[]{"llama-cli", "llama-server"};
        }
    }
    
    /**
     * Gets the path to llama-cli
     */
    public static String getLlamaCliPath() {
        if (llamaCliPath != null) {
            return llamaCliPath;
        }
        if (IS_WINDOWS) {
            return LLAMA_CPP_DIR + "/llama-cli.exe";
        } else {
            return LLAMA_CPP_DIR + "/llama-cli";
        }
    }
    
    /**
     * Gets the path to llama-server
     */
    public static String getLlamaServerPath() {
        if (llamaServerPath != null) {
            return llamaServerPath;
        }
        if (IS_WINDOWS) {
            return LLAMA_CPP_DIR + "/llama-server.exe";
        } else {
            return LLAMA_CPP_DIR + "/llama-server";
        }
    }
    
    /**
     * Checks if llama.cpp is available
     */
    public static boolean isLlamaCppAvailable() {
        try {
            String cliPath = getLlamaCliPath();
            return Files.exists(Paths.get(cliPath));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Deletes a directory recursively
     */
    private static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walk(path)
            .sorted((a, b) -> -a.compareTo(b))
            .forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    // Ignore
                }
            });
    }
    
    /**
     * Runs a command and returns the output
     */
    public static String runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        p.waitFor();
        return output.toString();
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        try {
            System.out.println("[LlamaCpp] Platform: " + System.getProperty("os.name"));
            System.out.println("[LlamaCpp] Architecture: " + System.getProperty("os.arch"));
            
            ensureLlamaCppAvailable();
            
            String cliPath = getLlamaCliPath();
            System.out.println("[LlamaCpp] llama-cli path: " + cliPath);
            System.out.println("[LlamaCpp] llama-cli exists: " + Files.exists(Paths.get(cliPath)));
            
            if (Files.exists(Paths.get(cliPath))) {
                String version = runCommand(cliPath, "--version");
                System.out.println("[LlamaCpp] Version:\n" + version);
            }
            
        } catch (Exception e) {
            System.err.println("[LlamaCpp] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}