package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import eu.kalafatic.evolution.controller.resource.ProductDefinition;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.controller.resource.TargetPlatform;

public class TychoEvoRcpBuilder extends AbstractProjectBuilder implements EvoRcpBuilder {

    private boolean skipTests = false;

    public TychoEvoRcpBuilder() {
        super("build_evo_rcp");
    }

    public boolean isSkipTests() {
        return skipTests || Boolean.getBoolean("evo.build.skipTests");
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    // Retain static nested classes for backwards compatibility
    public static class TargetPlatform extends eu.kalafatic.evolution.controller.resource.TargetPlatform {
        public TargetPlatform(String os, String ws, String arch, String packaging, String profile) {
            super(os, ws, arch, packaging, profile);
        }
    }

    public static class ProductDefinition extends eu.kalafatic.evolution.controller.resource.ProductDefinition {
        public ProductDefinition(String productId, String launcherName, String rootFolder, String repositoryModule, File productFile) {
            super(productId, launcherName, rootFolder, repositoryModule, productFile);
        }
    }

    public File discoverSourceDir(SelfDevContext context) {
        ResourceManager rm = context != null ? context.getResourceManager() : ResourceManager.getInstance();
        return rm.getEvoSource().toFile();
    }

    public File discoverReactorRoot(File sourceDir) {
        ResourceManager rm = ResourceManager.getInstance();
        return rm.getEvoReactor().toFile();
    }

    public ProductDefinition discoverTychoProduct(File reactorRoot) {
        ResourceManager rm = ResourceManager.getInstance();
        eu.kalafatic.evolution.controller.resource.ProductDefinition pd = rm.getProductDefinition();
        return new ProductDefinition(pd.getProductId(), pd.getLauncherName(), pd.getRootFolder(), pd.getRepositoryModule(), pd.getProductFile());
    }

    public TargetPlatform resolveTargetPlatform(SelfDevContext context) {
        ResourceManager rm = context != null ? context.getResourceManager() : ResourceManager.getInstance();
        eu.kalafatic.evolution.controller.resource.TargetPlatform tp = rm.getTargetPlatform();
        return new TargetPlatform(tp.getOs(), tp.getWs(), tp.getArch(), tp.getPackaging(), tp.getProfile());
    }

    @Override
    public TaskResult build(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("build_evo_rcp", "SelfDevContext is null", null);
        }

        ResourceManager rm = context.getResourceManager();
        File reactorRoot = rm.getEvoReactor().toFile();
        ProductDefinition prodDef = discoverTychoProduct(reactorRoot);
        TargetPlatform platform = resolveTargetPlatform(context);
        File logFile = getLogFile(context, "evo_build.log");

        List<String> goals = Arrays.asList("clean", "verify");
        List<String> args = new ArrayList<>();
        args.add(platform.getProfile());
        File wsBuildDir = context.getBuildDirectory();
        File moduleTarget = new File(wsBuildDir, "evo-rcp/target");
        args.add("-Dproject.build.directory=" + moduleTarget.getAbsolutePath());

        if (isSkipTests()) {
            args.add("-DskipTests");
        }

        System.out.println("[TychoEvoRcpBuilder] Building Tycho reactor at " + reactorRoot.getAbsolutePath() + " for platform " + platform);
        TaskResult buildResult = mavenExecutor.executeBuild(reactorRoot, goals, args, logFile, 45);
        if (!buildResult.isSuccess()) {
            return new TaskResult.Builder("build_evo_rcp")
                    .status(TaskStatus.FAILED)
                    .message("Tycho reactor build failed for " + prodDef.getProductId() + ": " + buildResult.getMessage())
                    .error(buildResult.getError())
                    .logFile(logFile)
                    .diagnostic("reactorRoot", reactorRoot.getAbsolutePath())
                    .diagnostic("productDefinition", prodDef.toString())
                    .diagnostic("targetPlatform", platform.toString())
                    .diagnostic("mavenCommand", buildResult.getCommand())
                    .build();
        }

        BuildArtifact artifact = getArtifact(context);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return new TaskResult.Builder("build_evo_rcp")
                    .status(TaskStatus.FAILED)
                    .message("Tycho reactor returned exit code 0 but expected product artifact was missing or unverified for " + prodDef.getProductId() + " (" + platform + ") under " + reactorRoot.getAbsolutePath())
                    .logFile(logFile)
                    .diagnostic("reactorRoot", reactorRoot.getAbsolutePath())
                    .diagnostic("productDefinition", prodDef.toString())
                    .diagnostic("targetPlatform", platform.toString())
                    .build();
        }

        context.recordArtifact(artifact);

        long duration = System.currentTimeMillis() - startTime;
        return new TaskResult.Builder("build_evo_rcp")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP Tycho reactor build completed successfully and artifact verified: " + artifact.getPath().getAbsolutePath())
                .artifact(artifact)
                .duration(duration)
                .logFile(logFile)
                .build();
    }

    @Override
    public TaskResult exportProduct(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("export_evo_rcp", "SelfDevContext is null", null);
        }

        ResourceManager rm = context.getResourceManager();
        File reactorRoot = rm.getEvoReactor().toFile();
        ProductDefinition prodDef = discoverTychoProduct(reactorRoot);
        TargetPlatform platform = resolveTargetPlatform(context);
        File logFile = getLogFile(context, "evo_build.log");

        // REQUIREMENT 11: BUILD MUST BUILD ONCE!
        // First check if a valid build artifact is already available in context or on disk.
        BuildArtifact existingArtifact = context.getArtifact(ArtifactType.EVO_RCP);
        if (existingArtifact == null) {
            existingArtifact = getArtifact(context);
        }

        if (existingArtifact != null && existingArtifact.getPath() != null && existingArtifact.getPath().exists()) {
            System.out.println("[TychoEvoRcpBuilder] Reusing existing verified build artifact for export: " + existingArtifact.getPath().getAbsolutePath());
            context.recordArtifact(existingArtifact);
            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder("export_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP product export reused existing build artifact: " + existingArtifact.getPath().getAbsolutePath())
                    .artifact(existingArtifact)
                    .duration(duration)
                    .logFile(logFile)
                    .build();
        }

        // If no artifact exists, trigger clean verify build once
        List<String> goals = Arrays.asList("clean", "verify");
        List<String> args = new ArrayList<>();
        args.add(platform.getProfile());
        File wsBuildDir = context.getBuildDirectory();
        File moduleTarget = new File(wsBuildDir, "evo-rcp/target");
        args.add("-Dproject.build.directory=" + moduleTarget.getAbsolutePath());

        if (isSkipTests()) {
            args.add("-DskipTests");
        }

        System.out.println("[TychoEvoRcpBuilder] Executing Tycho product export for " + prodDef.getProductId() + " (" + platform + ")...");
        TaskResult exportExecResult = mavenExecutor.executeBuild(reactorRoot, goals, args, logFile, 45);
        if (!exportExecResult.isSuccess()) {
            return new TaskResult.Builder("export_evo_rcp")
                    .status(TaskStatus.FAILED)
                    .message("Tycho product export build failed: " + exportExecResult.getMessage())
                    .error(exportExecResult.getError())
                    .logFile(logFile)
                    .diagnostic("reactorRoot", reactorRoot.getAbsolutePath())
                    .diagnostic("productDefinition", prodDef.toString())
                    .diagnostic("targetPlatform", platform.toString())
                    .diagnostic("mavenCommand", exportExecResult.getCommand())
                    .build();
        }

        File exportedLocation;
        try {
            exportedLocation = findExactExportedProduct(reactorRoot, prodDef, platform, context);
        } catch (Exception e) {
            return TaskResult.failure("export_evo_rcp", "Product discovery ambiguity error: " + e.getMessage(), e);
        }

        if (exportedLocation == null || !exportedLocation.exists()) {
            return TaskResult.failure("export_evo_rcp", "Could not locate exact exported product for " + prodDef.getProductId() + " (" + platform + ") under " + reactorRoot.getAbsolutePath(), null);
        }

        TaskResult valRes = validateProductDeployment(exportedLocation, prodDef, platform);
        if (!valRes.isSuccess()) {
            return TaskResult.failure("export_evo_rcp", "Exported EVO RCP product validation failed for " + exportedLocation.getAbsolutePath() + ": " + valRes.getMessage(), null);
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("productId", prodDef.getProductId());
        metadata.put("launcherName", prodDef.getLauncherName());
        metadata.put("rootFolder", prodDef.getRootFolder());
        metadata.put("repositoryModule", prodDef.getRepositoryModule());
        metadata.put("platform", platform.toString());

        BuildArtifact artifact = new BuildArtifact(ArtifactType.EVO_RCP, exportedLocation, context.getSourceRevision(), platform.getOs(), metadata);
        context.recordArtifact(artifact);

        long duration = System.currentTimeMillis() - startTime;
        return new TaskResult.Builder("export_evo_rcp")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP product built, exported, and validated: " + exportedLocation.getAbsolutePath())
                .artifact(artifact)
                .duration(duration)
                .logFile(logFile)
                .build();
    }

    @Override
    public BuildArtifact getArtifact(SelfDevContext context) {
        if (context == null) return null;

        ResourceManager rm = context.getResourceManager();
        File reactorRoot = rm.getEvoReactor().toFile();
        ProductDefinition prodDef = discoverTychoProduct(reactorRoot);
        TargetPlatform platform = resolveTargetPlatform(context);

        File exportedLocation;
        try {
            exportedLocation = findExactExportedProduct(reactorRoot, prodDef, platform, context);
        } catch (Exception e) {
            return null;
        }

        if (exportedLocation == null || !exportedLocation.exists()) {
            return null;
        }

        TaskResult valRes = validateProductDeployment(exportedLocation, prodDef, platform);
        if (!valRes.isSuccess()) {
            return null;
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("productId", prodDef.getProductId());
        metadata.put("launcherName", prodDef.getLauncherName());
        metadata.put("rootFolder", prodDef.getRootFolder());
        metadata.put("repositoryModule", prodDef.getRepositoryModule());
        metadata.put("platform", platform.toString());

        return new BuildArtifact(ArtifactType.EVO_RCP, exportedLocation, context.getSourceRevision(), platform.getOs(), metadata);
    }

    public File findExactExportedProduct(File reactorRoot, ProductDefinition prodDef, TargetPlatform platform, SelfDevContext context) throws IOException {
        if (reactorRoot == null || prodDef == null || platform == null) return null;

        List<File> candidates = new ArrayList<>();

        File wsBuildDir = context != null ? context.getBuildDirectory() : null;
        if (wsBuildDir != null && wsBuildDir.exists()) {
            File wsProductsDir = new File(wsBuildDir, "evo-rcp/target/products");
            if (wsProductsDir.exists() && wsProductsDir.isDirectory()) {
                File[] files = wsProductsDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (isExactMatchingArtifact(f, prodDef, platform) && !candidates.contains(f)) {
                            candidates.add(f);
                        }
                    }
                }
            }
        }

        File targetProductsDir = new File(reactorRoot, prodDef.getRepositoryModule() + "/target/products");
        if (targetProductsDir.exists() && targetProductsDir.isDirectory()) {
            File[] files = targetProductsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (isExactMatchingArtifact(f, prodDef, platform) && !candidates.contains(f)) {
                        candidates.add(f);
                    }
                }
            }
        }

        if (context != null && context.getExportDirectory() != null && context.getExportDirectory().exists()) {
            File[] expFiles = context.getExportDirectory().listFiles();
            if (expFiles != null) {
                for (File f : expFiles) {
                    if (isExactMatchingArtifact(f, prodDef, platform) && !candidates.contains(f)) {
                        candidates.add(f);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            // Check exact nested materialized directory
            File nestedDir = platform.isWindows() ?
                    new File(targetProductsDir, prodDef.getProductId() + "/win32/win32/x86_64/" + prodDef.getRootFolder()) :
                    new File(targetProductsDir, prodDef.getProductId() + "/linux/gtk/x86_64/" + prodDef.getRootFolder());

            if (nestedDir.exists() && nestedDir.isDirectory()) {
                candidates.add(nestedDir);
            } else {
                File rootFolderDir = new File(targetProductsDir, prodDef.getRootFolder());
                if (rootFolderDir.exists() && rootFolderDir.isDirectory()) {
                    candidates.add(rootFolderDir);
                }
            }
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        } else if (candidates.size() > 1) {
            throw new IOException("Multiple candidate exported products found matching " + prodDef.getProductId() + " (" + platform + "): " + candidates + ". Rejecting ambiguous selection.");
        }

        return null;
    }

    private boolean isExactMatchingArtifact(File file, ProductDefinition prodDef, TargetPlatform platform) {
        if (file == null || !file.exists()) return false;
        String name = file.getName().toLowerCase();

        if (file.isFile()) {
            boolean isZipOrTar = name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".tgz");
            if (!isZipOrTar) return false;

            boolean nameMatchesProduct = name.startsWith(prodDef.getProductId().toLowerCase()) || name.contains(prodDef.getProductId().toLowerCase()) || name.contains("evo");
            if (!nameMatchesProduct) return false;

            if (platform.isWindows()) {
                return name.contains("win32") || name.contains("win");
            } else {
                return name.contains("linux") || name.contains("gtk") || name.endsWith(".tar.gz");
            }
        } else if (file.isDirectory()) {
            boolean nameMatchesProduct = name.startsWith(prodDef.getProductId().toLowerCase()) || name.contains(prodDef.getProductId().toLowerCase());
            return nameMatchesProduct || name.equals(prodDef.getRootFolder().toLowerCase()) || name.equals(prodDef.getProductId().toLowerCase());
        }

        return false;
    }

    public TaskResult validateProductDeployment(File location, ProductDefinition prodDef, TargetPlatform platform) {
        if (location == null || !location.exists()) {
            return TaskResult.failure("validate_product", "Product location is null or non-existent.", null);
        }

        File rootDir = location;
        File tempExtractDir = null;

        try {
            if (location.isFile()) {
                String name = location.getName().toLowerCase();
                if (name.endsWith(".zip")) {
                    tempExtractDir = new File(location.getParentFile(), "temp_val_" + System.currentTimeMillis());
                    tempExtractDir.mkdirs();
                    unzipSafely(location, tempExtractDir);
                    rootDir = locateExtractedProductRoot(tempExtractDir, prodDef);
                } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
                    tempExtractDir = new File(location.getParentFile(), "temp_val_" + System.currentTimeMillis());
                    tempExtractDir.mkdirs();
                    untarSafely(location, tempExtractDir);
                    rootDir = locateExtractedProductRoot(tempExtractDir, prodDef);
                } else {
                    return TaskResult.failure("validate_product", "Unsupported archive format: " + location.getName(), null);
                }
            }

            return validateDirectoryLayout(rootDir, prodDef, platform);

        } catch (Exception e) {
            return TaskResult.failure("validate_product", "Product deployment validation exception: " + e.getMessage(), e);
        } finally {
            if (tempExtractDir != null && tempExtractDir.exists()) {
                deleteRecursively(tempExtractDir);
            }
        }
    }

    private File locateExtractedProductRoot(File tempDir, ProductDefinition prodDef) {
        if (tempDir == null || !tempDir.exists()) return tempDir;

        String launcher = prodDef != null ? prodDef.getLauncherName() : "evo";
        if (isProductRoot(tempDir, launcher)) {
            return tempDir;
        }

        File rootFolderSubdir = new File(tempDir, prodDef != null ? prodDef.getRootFolder() : "evolution");
        if (rootFolderSubdir.exists() && isProductRoot(rootFolderSubdir, launcher)) {
            return rootFolderSubdir;
        }

        File[] subdirs = tempDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File sub : subdirs) {
                if (isProductRoot(sub, launcher)) {
                    return sub;
                }
            }
        }

        return tempDir;
    }

    private boolean isProductRoot(File dir, String launcherName) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return false;
        boolean hasLauncher = new File(dir, launcherName + ".exe").exists() ||
                new File(dir, launcherName).exists() ||
                new File(dir, "eclipse.exe").exists() ||
                new File(dir, "eclipse").exists();
        boolean hasPlugins = new File(dir, "plugins").exists();
        boolean hasConfig = new File(dir, "configuration").exists();
        return hasLauncher && (hasPlugins || hasConfig);
    }

    private TaskResult validateDirectoryLayout(File rootDir, ProductDefinition prodDef, TargetPlatform platform) {
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return TaskResult.failure("validate_product", "Product root directory does not exist or is not a directory: " + (rootDir != null ? rootDir.getAbsolutePath() : "null"), null);
        }

        List<String> missingItems = new ArrayList<>();
        boolean isWin = platform != null ? platform.isWindows() : System.getProperty("os.name").toLowerCase().contains("win");
        String launcherName = prodDef != null ? prodDef.getLauncherName() : "evo";

        if (isWin) {
            File exeFile = new File(rootDir, launcherName + ".exe");
            File altExeFile = new File(rootDir, "eclipse.exe");
            if (!exeFile.exists() && !altExeFile.exists()) {
                missingItems.add("Launcher executable (" + launcherName + ".exe or eclipse.exe)");
            }
        } else {
            File nativeLauncher = new File(rootDir, launcherName);
            File shLauncher = new File(rootDir, launcherName + ".sh");
            File altLauncher = new File(rootDir, "eclipse");
            if (!nativeLauncher.exists() && !shLauncher.exists() && !altLauncher.exists()) {
                missingItems.add("Launcher executable (" + launcherName + " or " + launcherName + ".sh or eclipse)");
            } else {
                if (nativeLauncher.exists()) nativeLauncher.setExecutable(true);
                if (shLauncher.exists()) shLauncher.setExecutable(true);
            }
        }

        File iniFile = new File(rootDir, launcherName + ".ini");
        File altIniFile = new File(rootDir, "eclipse.ini");
        if (!iniFile.exists() && !altIniFile.exists()) {
            missingItems.add("Launcher configuration (" + launcherName + ".ini or eclipse.ini)");
        }

        File pluginsDir = new File(rootDir, "plugins");
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            missingItems.add("plugins/ directory");
        } else {
            File[] pluginJars = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (pluginJars == null || pluginJars.length == 0) {
                missingItems.add("plugins/ directory contains no bundle JARs");
            } else {
                boolean hasEvoBundle = false;
                for (File jar : pluginJars) {
                    if (jar.getName().startsWith("eu.kalafatic.evolution.") || jar.getName().startsWith("org.eclipse.")) {
                        hasEvoBundle = true;
                        break;
                    }
                }
                if (!hasEvoBundle) {
                    missingItems.add("plugins/ missing required EVO application bundle (eu.kalafatic.evolution.*.jar)");
                }
            }
        }

        File configDir = new File(rootDir, "configuration");
        if (!configDir.exists() || !configDir.isDirectory()) {
            missingItems.add("configuration/ directory");
        } else {
            File configIni = new File(configDir, "config.ini");
            if (!configIni.exists()) {
                missingItems.add("configuration/config.ini");
            } else {
                try {
                    String content = Files.readString(configIni.toPath());
                    if (!content.contains("eclipse.application") && !content.contains("eclipse.product")) {
                        missingItems.add("eclipse.application or eclipse.product entry in configuration/config.ini");
                    }
                } catch (IOException e) {
                    missingItems.add("Readable configuration/config.ini (" + e.getMessage() + ")");
                }
            }
        }

        if (!missingItems.isEmpty()) {
            return TaskResult.failure("validate_product", "Product deployment validation failed due to missing items: " + String.join(", ", missingItems), null);
        }

        return new TaskResult.Builder("validate_product")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP product deployment validation passed successfully at " + rootDir.getAbsolutePath())
                .workingDirectory(rootDir)
                .build();
    }

    private void unzipSafely(File zipFile, File destDir) throws IOException {
        String destCanonicalPath = destDir.getCanonicalPath();
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();
                if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                    throw new IOException("ZIP path traversal attempt detected in entry: " + entryName);
                }

                File filePath = new File(destDir, entryName);
                String entryCanonicalPath = filePath.getCanonicalPath();
                if (!entryCanonicalPath.startsWith(destCanonicalPath + File.separator) && !entryCanonicalPath.equals(destCanonicalPath)) {
                    throw new IOException("ZIP path traversal attempt detected outside target directory for entry: " + entryName);
                }

                if (!entry.isDirectory()) {
                    if (filePath.getParentFile() != null && !filePath.getParentFile().exists()) {
                        filePath.getParentFile().mkdirs();
                    }
                    try (FileOutputStream bos = new FileOutputStream(filePath)) {
                        byte[] bytesIn = new byte[8192];
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

    private void untarSafely(File tarFile, File destDir) throws IOException, InterruptedException {
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
