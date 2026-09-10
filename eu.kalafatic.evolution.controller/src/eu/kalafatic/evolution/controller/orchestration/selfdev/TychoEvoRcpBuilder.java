package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
                    ", productFile=" + (productFile != null ? productFile.getName() : "null") +
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
        if (pom.exists()) {
            return sourceDir;
        }
        File parent = sourceDir.getParentFile();
        if (parent != null && new File(parent, "pom.xml").exists()) {
            return parent;
        }
        return null;
    }

    public ProductDefinition discoverProductDefinition(File reactorRoot) {
        if (reactorRoot == null || !reactorRoot.exists()) {
            return new ProductDefinition("evolution", "evo", "evolution", "eu.kalafatic.evolution.repository", null);
        }

        File repoModuleDir = new File(reactorRoot, "eu.kalafatic.evolution.repository");
        File productFile = null;

        if (repoModuleDir.exists() && repoModuleDir.isDirectory()) {
            File[] productFiles = repoModuleDir.listFiles((dir, name) -> name.endsWith(".product"));
            if (productFiles != null && productFiles.length > 0) {
                for (File p : productFiles) {
                    if (p.getName().equalsIgnoreCase("evolution.product")) {
                        productFile = p;
                        break;
                    }
                }
                if (productFile == null) {
                    productFile = productFiles[0];
                }
            }
        }

        if (productFile == null) {
            productFile = findProductFileRecursively(reactorRoot);
        }

        String productId = "evolution";
        String launcherName = "evo";
        String rootFolder = "evolution";
        String repoModuleName = repoModuleDir.exists() ? "eu.kalafatic.evolution.repository" : "eu.kalafatic.evolution.repository";

        if (productFile != null && productFile.exists()) {
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
                System.err.println("[TychoEvoRcpBuilder] Error reading product file " + productFile + ": " + e.getMessage());
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

        return new ProductDefinition(productId, launcherName, rootFolder, repoModuleName, productFile);
    }

    private File findProductFileRecursively(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".product")) {
                return f;
            } else if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().equals("target")) {
                File found = findProductFileRecursively(f);
                if (found != null) return found;
            }
        }
        return null;
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

    public String resolvePlatformProfile(String targetOs) {
        String os = targetOs != null ? targetOs.toLowerCase() : System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "-Pwindows";
        } else if (os.contains("linux")) {
            return "-Plinux";
        } else {
            return "-Pall-platforms";
        }
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

        ProductDefinition prodDef = discoverProductDefinition(reactorRoot);
        System.out.println("[TychoEvoRcpBuilder] Discovered reactor root: " + reactorRoot.getAbsolutePath() + ", Product: " + prodDef);

        File logFile = getLogFile(context, "evo_build.log");
        List<String> goals = Arrays.asList("clean", "verify");
        List<String> args = new ArrayList<>();

        String platformProfile = resolvePlatformProfile(null);
        args.add(platformProfile);

        if (isSkipTests()) {
            args.add("-DskipTests");
        }

        TaskResult buildResult = mavenExecutor.executeBuild(reactorRoot, goals, args, logFile, 45);
        if (!buildResult.isSuccess()) {
            return buildResult;
        }

        long duration = System.currentTimeMillis() - startTime;
        return new TaskResult.Builder("build_evo_rcp")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP Tycho reactor build completed successfully.")
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

        ProductDefinition prodDef = discoverProductDefinition(reactorRoot);

        BuildArtifact existingArtifact = getArtifact(context);
        if (existingArtifact != null && existingArtifact.getPath().exists()) {
            TaskResult valRes = validateProductDeployment(existingArtifact.getPath(), prodDef, existingArtifact.getPlatform());
            if (valRes.isSuccess()) {
                return new TaskResult.Builder("export_evo_rcp")
                        .status(TaskStatus.SUCCESS)
                        .message("Validated existing EVO RCP product artifact: " + existingArtifact.getPath().getAbsolutePath())
                        .artifact(existingArtifact)
                        .duration(System.currentTimeMillis() - startTime)
                        .build();
            }
        }

        File logFile = getLogFile(context, "evo_build.log");
        List<String> goals = Arrays.asList("clean", "verify");
        List<String> args = new ArrayList<>();
        args.add(resolvePlatformProfile(null));
        if (isSkipTests()) {
            args.add("-DskipTests");
        }

        TaskResult exportExecResult = mavenExecutor.executeBuild(reactorRoot, goals, args, logFile, 45);
        if (!exportExecResult.isSuccess()) {
            return TaskResult.failure("export_evo_rcp", "Tycho product export execution failed: " + exportExecResult.getMessage(), exportExecResult.getError());
        }

        File exportedLocation = locateExportedProduct(reactorRoot, prodDef, null, "x86_64", context);
        if (exportedLocation == null || !exportedLocation.exists()) {
            return TaskResult.failure("export_evo_rcp", "Could not locate exact exported EVO RCP product artifact for " + prodDef.getProductId() + " post-export build.", null);
        }

        TaskResult valRes = validateProductDeployment(exportedLocation, prodDef, System.getProperty("os.name"));
        if (!valRes.isSuccess()) {
            return TaskResult.failure("export_evo_rcp", "Exported EVO RCP product deployment validation failed: " + valRes.getMessage(), null);
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("productId", prodDef.getProductId());
        metadata.put("launcherName", prodDef.getLauncherName());
        metadata.put("rootFolder", prodDef.getRootFolder());
        metadata.put("repositoryModule", prodDef.getRepositoryModule());

        BuildArtifact artifact = new BuildArtifact(ArtifactType.EVO_RCP, exportedLocation, context.getSourceRevision(), System.getProperty("os.name"), metadata);
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

        ProductDefinition prodDef = discoverProductDefinition(reactorRoot);
        File exportedLocation = locateExportedProduct(reactorRoot, prodDef, null, "x86_64", context);
        if (exportedLocation == null || !exportedLocation.exists()) {
            return null;
        }

        TaskResult valRes = validateProductDeployment(exportedLocation, prodDef, System.getProperty("os.name"));
        if (!valRes.isSuccess()) {
            return null;
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("productId", prodDef.getProductId());
        metadata.put("launcherName", prodDef.getLauncherName());
        metadata.put("rootFolder", prodDef.getRootFolder());
        metadata.put("repositoryModule", prodDef.getRepositoryModule());

        return new BuildArtifact(ArtifactType.EVO_RCP, exportedLocation, context.getSourceRevision(), System.getProperty("os.name"), metadata);
    }

    public File locateExportedProduct(File reactorRoot, ProductDefinition prodDef, String os, String arch, SelfDevContext context) {
        if (reactorRoot == null || prodDef == null) return null;

        String targetOs = os != null ? os.toLowerCase() : System.getProperty("os.name").toLowerCase();
        boolean isWin = targetOs.contains("win");

        List<File> searchDirs = new ArrayList<>();
        searchDirs.add(new File(reactorRoot, prodDef.getRepositoryModule() + "/target/products"));
        searchDirs.add(new File(reactorRoot, "eu.kalafatic.evolution.repository/target/products"));
        searchDirs.add(new File(reactorRoot, "eu.kalafatic.evolution.view/target/products"));
        if (context != null && context.getExportDirectory() != null && context.getExportDirectory().exists()) {
            searchDirs.add(context.getExportDirectory());
        }

        for (File prodDir : searchDirs) {
            if (prodDir == null || !prodDir.exists() || !prodDir.isDirectory()) continue;

            File[] matchingArchives = prodDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                boolean isZipOrTar = lowerName.endsWith(".zip") || lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz");
                if (!isZipOrTar) return false;

                boolean matchesProduct = lowerName.contains(prodDef.getProductId().toLowerCase()) || lowerName.contains("evo");
                if (!matchesProduct) return false;

                if (isWin) {
                    return lowerName.contains("win") || lowerName.contains("win32");
                } else {
                    return lowerName.contains("linux") || lowerName.contains("gtk") || lowerName.contains("tar.gz");
                }
            });

            if (matchingArchives != null && matchingArchives.length > 0) {
                return matchingArchives[0];
            }

            File exactDir = isWin ?
                    new File(prodDir, prodDef.getProductId() + "/win32/win32/x86_64/" + prodDef.getRootFolder()) :
                    new File(prodDir, prodDef.getProductId() + "/linux/gtk/x86_64/" + prodDef.getRootFolder());

            if (exactDir.exists() && exactDir.isDirectory()) {
                return exactDir;
            }

            File rootFolderDir = new File(prodDir, prodDef.getRootFolder());
            if (rootFolderDir.exists() && rootFolderDir.isDirectory()) {
                return rootFolderDir;
            }
        }

        return null;
    }

    public TaskResult validateProductDeployment(File location, ProductDefinition prodDef, String targetOs) {
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
                    unzip(location, tempExtractDir);
                    rootDir = findProductRootDir(tempExtractDir, prodDef);
                } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
                    tempExtractDir = new File(location.getParentFile(), "temp_val_" + System.currentTimeMillis());
                    tempExtractDir.mkdirs();
                    untar(location, tempExtractDir);
                    rootDir = findProductRootDir(tempExtractDir, prodDef);
                } else {
                    return TaskResult.failure("validate_product", "Unsupported archive extension: " + location.getName(), null);
                }
            }

            return validateDirectoryLayout(rootDir, prodDef, targetOs);

        } catch (Exception e) {
            return TaskResult.failure("validate_product", "Product validation exception: " + e.getMessage(), e);
        } finally {
            if (tempExtractDir != null && tempExtractDir.exists()) {
                deleteRecursively(tempExtractDir);
            }
        }
    }

    private File findProductRootDir(File tempDir, ProductDefinition prodDef) {
        if (tempDir == null || !tempDir.exists()) return tempDir;

        String launcher = prodDef != null ? prodDef.getLauncherName() : "evo";
        File directLauncherWin = new File(tempDir, launcher + ".exe");
        File directLauncherLinux = new File(tempDir, launcher);
        if (directLauncherWin.exists() || directLauncherLinux.exists()) {
            return tempDir;
        }

        File[] subdirs = tempDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File sub : subdirs) {
                File subLauncherWin = new File(sub, launcher + ".exe");
                File subLauncherLinux = new File(sub, launcher);
                if (subLauncherWin.exists() || subLauncherLinux.exists()) {
                    return sub;
                }
            }
            if (subdirs.length == 1) {
                return subdirs[0];
            }
        }
        return tempDir;
    }

    private TaskResult validateDirectoryLayout(File rootDir, ProductDefinition prodDef, String targetOs) {
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return TaskResult.failure("validate_product", "Product root directory does not exist or is not a directory: " + (rootDir != null ? rootDir.getAbsolutePath() : "null"), null);
        }

        List<String> missingItems = new ArrayList<>();
        String os = targetOs != null ? targetOs.toLowerCase() : System.getProperty("os.name").toLowerCase();
        boolean isWin = os.contains("win");
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
            missingItems.add("Launcher config file (" + launcherName + ".ini or eclipse.ini)");
        }

        File pluginsDir = new File(rootDir, "plugins");
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            missingItems.add("plugins/ directory");
        } else {
            File[] pluginJars = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (pluginJars == null || pluginJars.length == 0) {
                missingItems.add("plugins/ directory contains no bundle JARs");
            } else {
                boolean hasViewBundle = false;
                for (File jar : pluginJars) {
                    if (jar.getName().startsWith("eu.kalafatic.evolution.") || jar.getName().startsWith("org.eclipse.")) {
                        hasViewBundle = true;
                        break;
                    }
                }
                if (!hasViewBundle) {
                    missingItems.add("plugins/ missing required application bundle (eu.kalafatic.evolution.*.jar)");
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
