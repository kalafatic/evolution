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

    public static class TargetPlatform {
        private final String os;
        private final String ws;
        private final String arch;
        private final String packaging;
        private final String profile;

        public TargetPlatform(String os, String ws, String arch, String packaging, String profile) {
            this.os = os != null ? os : "win32";
            this.ws = ws != null ? ws : (this.os.equals("win32") ? "win32" : "gtk");
            this.arch = arch != null ? arch : "x86_64";
            this.packaging = packaging != null ? packaging : "zip";
            this.profile = profile != null ? profile : (this.os.equals("win32") ? "-Pwindows" : "-Plinux");
        }

        public String getOs() { return os; }
        public String getWs() { return ws; }
        public String getArch() { return arch; }
        public String getPackaging() { return packaging; }
        public String getProfile() { return profile; }

        public boolean isWindows() { return "win32".equalsIgnoreCase(os) || os.toLowerCase().contains("win"); }
        public boolean isLinux() { return "linux".equalsIgnoreCase(os) || os.toLowerCase().contains("linux"); }

        @Override
        public String toString() {
            return os + "." + ws + "." + arch + " (" + packaging + ")";
        }
    }

    public static class ProductDefinition {
        private final String productId;
        private final String launcherName;
        private final String rootFolder;
        private final String repositoryModule;
        private final File productFile;

        public ProductDefinition(String productId, String launcherName, String rootFolder, String repositoryModule, File productFile) {
            this.productId = productId != null && !productId.trim().isEmpty() ? productId.trim() : "evolution";
            this.launcherName = launcherName != null && !launcherName.trim().isEmpty() ? launcherName.trim() : "evo";
            this.rootFolder = rootFolder != null && !rootFolder.trim().isEmpty() ? rootFolder.trim() : "evolution";
            this.repositoryModule = repositoryModule != null && !repositoryModule.trim().isEmpty() ? repositoryModule.trim() : "eu.kalafatic.evolution.repository";
            this.productFile = productFile;
        }

        public String getProductId() { return productId; }
        public String getLauncherName() { return launcherName; }
        public String getRootFolder() { return rootFolder; }
        public String getRepositoryModule() { return repositoryModule; }
        public File getProductFile() { return productFile; }

        @Override
        public String toString() {
            return "ProductDefinition{" +
                    "productId='" + productId + '\'' +
                    ", launcherName='" + launcherName + '\'' +
                    ", rootFolder='" + rootFolder + '\'' +
                    ", repositoryModule='" + repositoryModule + '\'' +
                    '}';
        }
    }

    public File discoverSourceDir(SelfDevContext context) {
        if (context == null) return new File(".");
        File srcDir = context.getSourceDirectory();
        if (srcDir != null && srcDir.exists() && new File(srcDir, "pom.xml").exists()) {
            return srcDir;
        }
        File projRoot = context.getProjectRoot();
        if (projRoot != null && projRoot.exists()) {
            return projRoot;
        }
        return srcDir != null ? srcDir : new File(".");
    }

    public File discoverReactorRoot(File sourceDir) {
        if (sourceDir == null) return null;
        File pom = new File(sourceDir, "pom.xml");
        if (pom.exists() && isEvoAggregatorPom(pom)) {
            return sourceDir;
        }
        File parent = sourceDir.getParentFile();
        if (parent != null && new File(parent, "pom.xml").exists() && isEvoAggregatorPom(new File(parent, "pom.xml"))) {
            return parent;
        }
        return pom.exists() ? sourceDir : null;
    }

    private boolean isEvoAggregatorPom(File pomFile) {
        try {
            String content = Files.readString(pomFile.toPath());
            return content.contains("eu.kalafatic.evolution.aggregator") || content.contains("eu.kalafatic.evolution");
        } catch (Exception e) {
            return false;
        }
    }

    public ProductDefinition discoverTychoProduct(File reactorRoot) {
        if (reactorRoot == null || !reactorRoot.exists()) {
            return new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        }

        File repoModuleDir = new File(reactorRoot, "eu.kalafatic.evolution.repository");
        File productFile = new File(repoModuleDir, "evolution.product");
        if (!productFile.exists()) {
            File[] productFiles = repoModuleDir.listFiles((dir, name) -> name.endsWith(".product"));
            if (productFiles != null && productFiles.length > 0) {
                productFile = productFiles[0];
            }
        }

        String productId = "evolution";
        String launcherName = "evo";
        String rootFolder = "evolution";
        String repoModuleName = "eu.kalafatic.evolution.repository";

        if (productFile.exists()) {
            try {
                String content = Files.readString(productFile.toPath());
                String parsedUid = extractAttribute(content, "uid");
                String parsedId = extractAttribute(content, "id");
                if (parsedUid != null && !parsedUid.isEmpty()) {
                    productId = parsedUid;
                } else if (parsedId != null && !parsedId.isEmpty()) {
                    productId = parsedId;
                }

                String parsedLauncher = extractLauncherName(content);
                if (parsedLauncher != null && !parsedLauncher.isEmpty()) {
                    launcherName = parsedLauncher;
                }
            } catch (Exception e) {
                System.err.println("[TychoEvoRcpBuilder] Error reading product definition " + productFile + ": " + e.getMessage());
            }
        }

        File repoPom = new File(repoModuleDir, "pom.xml");
        if (repoPom.exists()) {
            try {
                String pomContent = Files.readString(repoPom.toPath());
                String parsedRootFolder = extractTagValue(pomContent, "rootFolder");
                if (parsedRootFolder != null && !parsedRootFolder.isEmpty()) {
                    rootFolder = parsedRootFolder;
                }
            } catch (Exception e) {
                System.err.println("[TychoEvoRcpBuilder] Error reading repo pom " + repoPom + ": " + e.getMessage());
            }
        }

        return new ProductDefinition(productId, launcherName, rootFolder, repoModuleName, productFile.exists() ? productFile : null);
    }

    public TargetPlatform resolveTargetPlatform(SelfDevContext context) {
        if (context != null) {
            for (TaskResult tr : context.getTaskResults().values()) {
                if (tr != null && tr.getDiagnostics() != null) {
                    Object targetOsObj = tr.getDiagnostics().get("targetOS");
                    if (targetOsObj != null) {
                        String osStr = targetOsObj.toString().toLowerCase();
                        if (osStr.contains("win")) {
                            return new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");
                        } else if (osStr.contains("linux")) {
                            return new TargetPlatform("linux", "gtk", "x86_64", "tar.gz", "-Plinux");
                        }
                    }
                }
            }
        }

        String sysOs = System.getProperty("evo.target.os", System.getProperty("os.name")).toLowerCase();
        if (sysOs.contains("win")) {
            return new TargetPlatform("win32", "win32", "x86_64", "zip", "-Pwindows");
        } else {
            return new TargetPlatform("linux", "gtk", "x86_64", "tar.gz", "-Plinux");
        }
    }

    private String extractAttribute(String xmlContent, String attrName) {
        String key = attrName + "=\"";
        int idx = xmlContent.indexOf(key);
        if (idx != -1) {
            int start = idx + key.length();
            int end = xmlContent.indexOf("\"", start);
            if (end != -1) {
                return xmlContent.substring(start, end);
            }
        }
        return null;
    }

    private String extractLauncherName(String xmlContent) {
        int launcherIdx = xmlContent.indexOf("<launcher");
        if (launcherIdx != -1) {
            int nameIdx = xmlContent.indexOf("name=\"", launcherIdx);
            if (nameIdx != -1) {
                int start = nameIdx + "name=\"".length();
                int end = xmlContent.indexOf("\"", start);
                if (end != -1) {
                    return xmlContent.substring(start, end);
                }
            }
        }
        return null;
    }

    private String extractTagValue(String xmlContent, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = xmlContent.indexOf(openTag);
        if (start != -1) {
            int valStart = start + openTag.length();
            int end = xmlContent.indexOf(closeTag, valStart);
            if (end != -1) {
                return xmlContent.substring(valStart, end).trim();
            }
        }
        return null;
    }

    @Override
    public TaskResult build(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("build_evo_rcp", "SelfDevContext is null", null);
        }

        File srcDir = discoverSourceDir(context);
        File reactorRoot = discoverReactorRoot(srcDir);
        if (reactorRoot == null) {
            return TaskResult.failure("build_evo_rcp", "Tycho reactor root containing pom.xml not found at " + srcDir.getAbsolutePath(), null);
        }

        ProductDefinition prodDef = discoverTychoProduct(reactorRoot);
        TargetPlatform platform = resolveTargetPlatform(context);
        File logFile = getLogFile(context, "evo_build.log");

        List<String> goals = Arrays.asList("clean", "verify");
        List<String> args = new ArrayList<>();
        args.add(platform.getProfile());

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

        long duration = System.currentTimeMillis() - startTime;
        return new TaskResult.Builder("build_evo_rcp")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP Tycho reactor build completed successfully for " + prodDef.getProductId() + " (" + platform + ").")
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

        File srcDir = discoverSourceDir(context);
        File reactorRoot = discoverReactorRoot(srcDir);
        if (reactorRoot == null) {
            return TaskResult.failure("export_evo_rcp", "Tycho reactor root containing pom.xml not found at " + srcDir.getAbsolutePath(), null);
        }

        ProductDefinition prodDef = discoverTychoProduct(reactorRoot);
        TargetPlatform platform = resolveTargetPlatform(context);
        File logFile = getLogFile(context, "evo_build.log");

        // Note on EVO Tycho Product Export:
        // In eu.kalafatic.evolution.repository/pom.xml, tycho-p2-director-plugin goals 'materialize-products'
        // and 'archive-products' are bound to the 'package'/'verify' lifecycle phase.
        // Executing clean verify or clean package triggers the materialize and archive goals.

        List<String> goals = Arrays.asList("clean", "verify");
        List<String> args = new ArrayList<>();
        args.add(platform.getProfile());
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

        File srcDir = discoverSourceDir(context);
        File reactorRoot = discoverReactorRoot(srcDir);
        if (reactorRoot == null) return null;

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

        File targetProductsDir = new File(reactorRoot, prodDef.getRepositoryModule() + "/target/products");
        List<File> candidates = new ArrayList<>();

        if (targetProductsDir.exists() && targetProductsDir.isDirectory()) {
            File[] files = targetProductsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (isExactMatchingArtifact(f, prodDef, platform)) {
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

        boolean nameMatchesProduct = name.startsWith(prodDef.getProductId().toLowerCase());
        if (!nameMatchesProduct) return false;

        if (file.isFile()) {
            boolean isZipOrTar = name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".tgz");
            if (!isZipOrTar) return false;

            if (platform.isWindows()) {
                return name.contains("win32") || name.contains("win");
            } else {
                return name.contains("linux") || name.contains("gtk") || name.endsWith(".tar.gz");
            }
        } else if (file.isDirectory()) {
            return name.equals(prodDef.getRootFolder().toLowerCase()) || name.equals(prodDef.getProductId().toLowerCase());
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
