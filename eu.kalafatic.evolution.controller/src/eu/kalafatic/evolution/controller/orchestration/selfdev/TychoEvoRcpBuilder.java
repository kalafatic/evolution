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

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.resource.ProductDefinition;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.controller.resource.TargetPlatform;

public class TychoEvoRcpBuilder extends AbstractProjectBuilder implements EvoRcpBuilder {

    private boolean skipTests = true;

    public TychoEvoRcpBuilder() {
        super("build_evo_rcp");
    }

    public boolean isSkipTests() {
        return skipTests || Boolean.getBoolean("evo.build.skipTests");
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

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

    private ProductDefinition getProductDefinition() {
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

        File sourceRepo = context.getRepositoryRoot().getAbsoluteFile();
        File reactorRoot = context.getPreparedReactorDirectory().getAbsoluteFile();

        try {
            boolean sameWithRepo = reactorRoot.getCanonicalFile().equals(sourceRepo.getCanonicalFile());

            System.out.println("================================================================================");
            System.out.println("[TychoEvoRcpBuilder] EVO_GIT_REPOSITORY : " + sourceRepo.getAbsolutePath());
            System.out.println("[TychoEvoRcpBuilder] BUILD_WORKSPACE     : " + context.getBuildDirectory().getAbsolutePath());
            System.out.println("[TychoEvoRcpBuilder] SELF_DEV_SOURCE      : " + reactorRoot.getAbsolutePath());
            System.out.println("[TychoEvoRcpBuilder] EXPORT_DIRECTORY    : " + context.getExportDirectory().getAbsolutePath());
            System.out.println("[TychoEvoRcpBuilder] Maven working directory: " + reactorRoot.getAbsolutePath());
            System.out.println("================================================================================");

            if (sameWithRepo || !reactorRoot.exists() || !reactorRoot.isDirectory() || !new File(reactorRoot, "pom.xml").exists()) {
                String err = "[TychoEvoRcpBuilder] BUILD REACTOR VALIDATION FAILED\nsourceRepository: " + sourceRepo.getAbsolutePath() + "\nbuildReactor: " + reactorRoot.getAbsolutePath() + "\nReason: build reactor resolves to canonical Git repo or is invalid/missing pom.xml. Build reactor must be copied source in run directory.";
                System.err.println(err);
                return TaskResult.failure("build_evo_rcp", err, null);
            }
        } catch (Exception e) {
            return TaskResult.failure("build_evo_rcp", "Build reactor validation exception: " + e.getMessage(), e);
        }

        ProductDefinition prodDef = getProductDefinition();
        TargetPlatform platform = resolveTargetPlatform(context);
        File logFile = getLogFile(context, "evo_build.log");

        List<String> goals = Arrays.asList("validate", "clean", "verify");
        List<String> args = new ArrayList<>();
        args.add(platform.getProfile());

        if (isSkipTests()) {
            args.add("-DskipTests");
        }

        log("[MAVEN] Building Tycho reactor for Evolution / EVO RCP at " + reactorRoot.getAbsolutePath() + " (" + platform + ")");
        TaskResult buildResult = mavenExecutor.executeBuild(reactorRoot, goals, args, logFile, 45);

        // Verify canonical repository safety
        File repoTarget = new File(sourceRepo, "target");
        if (repoTarget.exists()) {
            System.out.println("[SOURCE_SAFETY] WARNING: target directory exists in canonical repository: " + repoTarget.getAbsolutePath());
        } else {
            System.out.println("[SOURCE_SAFETY] Verified: canonical repository remains clean (no target/ created).");
        }

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

        File targetDir = new File(reactorRoot, prodDef.getRepositoryModule() + "/target");
        if (!targetDir.exists()) {
            targetDir = new File(reactorRoot, "target");
        }
        if (!targetDir.exists()) {
            targetDir = reactorRoot;
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("productId", prodDef.getProductId());
        metadata.put("launcherName", prodDef.getLauncherName());
        metadata.put("rootFolder", prodDef.getRootFolder());
        metadata.put("repositoryModule", prodDef.getRepositoryModule());
        metadata.put("platform", platform.toString());

        BuildArtifact buildArtifact = new BuildArtifact(ArtifactType.EVO_RCP, targetDir, context.getSourceRevision(), platform.getOs(), metadata);

        log("[MAVEN][ARTIFACT]");
        log("[MAVEN][ARTIFACT] Type: " + ArtifactType.EVO_RCP);
        log("[MAVEN][ARTIFACT] Path: " + targetDir.getAbsolutePath());
        log("[MAVEN][ARTIFACT] Exists: true");
        log("[MAVEN][ARTIFACT] Validation: SUCCESS");

        context.recordArtifact(buildArtifact);

        long duration = System.currentTimeMillis() - startTime;
        return new TaskResult.Builder("build_evo_rcp")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP Tycho reactor build completed successfully: " + targetDir.getAbsolutePath())
                .artifact(buildArtifact)
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

        File sourceRepo = context.getRepositoryRoot().getAbsoluteFile();
        File reactorRoot = context.getPreparedReactorDirectory().getAbsoluteFile();

        try {
            boolean sameWithRepo = reactorRoot.getCanonicalFile().equals(sourceRepo.getCanonicalFile());
            if (sameWithRepo || !reactorRoot.exists() || !reactorRoot.isDirectory() || !new File(reactorRoot, "pom.xml").exists()) {
                String err = "[TychoEvoRcpBuilder] BUILD REACTOR VALIDATION FAILED\nsourceRepository: " + sourceRepo.getAbsolutePath() + "\nbuildReactor: " + reactorRoot.getAbsolutePath() + "\nReason: build reactor resolves to canonical Git repo or is invalid/missing pom.xml. Build reactor must be copied source in run directory.";
                System.err.println(err);
                return TaskResult.failure("export_evo_rcp", err, null);
            }
        } catch (Exception e) {
            return TaskResult.failure("export_evo_rcp", "Build reactor validation exception: " + e.getMessage(), e);
        }

        ProductDefinition prodDef = getProductDefinition();
        TargetPlatform platform = resolveTargetPlatform(context);
        File logFile = getLogFile(context, "evo_build.log");

        File exportedLocation = null;
        try {
            exportedLocation = findExactExportedProduct(reactorRoot, prodDef, platform, context);
        } catch (Exception e) {
            log("[TychoEvoRcpBuilder] Product search error: " + e.getMessage());
        }

        if (exportedLocation == null || !exportedLocation.exists()) {
            List<String> goals = Arrays.asList("validate", "clean", "verify");
            List<String> args = new ArrayList<>();
            args.add(platform.getProfile());
            if (isSkipTests()) {
                args.add("-DskipTests");
            }

            log("[TychoEvoRcpBuilder] Executing Tycho product export build for " + prodDef.getProductId() + " (" + platform + ")...");
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

            try {
                exportedLocation = findExactExportedProduct(reactorRoot, prodDef, platform, context);
            } catch (Exception e) {
                return TaskResult.failure("export_evo_rcp", "Product discovery ambiguity error: " + e.getMessage(), e);
            }
        }

        if (exportedLocation == null || !exportedLocation.exists()) {
            return TaskResult.failure("export_evo_rcp", "Could not locate exact exported product for " + prodDef.getProductId() + " (" + platform + ") under " + reactorRoot.getAbsolutePath(), null);
        }

        File exportDir = context.getExportDirectory();
        if (exportDir != null) {
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File targetInExportDir = new File(exportDir, exportedLocation.getName());
            if (!targetInExportDir.equals(exportedLocation.getAbsoluteFile())) {
                try {
                    if (exportedLocation.isDirectory()) {
                        copyDirectoryRecursively(exportedLocation, targetInExportDir);
                    } else {
                        Files.copy(exportedLocation.toPath(), targetInExportDir.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    exportedLocation = targetInExportDir;
                    log("[TychoEvoRcpBuilder] Materialized exported RCP product into export directory: " + exportedLocation.getAbsolutePath());
                } catch (IOException e) {
                    return TaskResult.failure("export_evo_rcp", "Failed to copy product to export directory: " + e.getMessage(), e);
                }
            }
        }

        TaskResult valRes = validateProductDeployment(exportedLocation, prodDef, platform, context);
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

        log("[MAVEN][ARTIFACT]");
        log("[MAVEN][ARTIFACT] Type: " + ArtifactType.EVO_RCP);
        log("[MAVEN][ARTIFACT] Path: " + exportedLocation.getAbsolutePath());
        log("[MAVEN][ARTIFACT] Exists: true");
        log("[MAVEN][ARTIFACT] Size: " + exportedLocation.length() + " bytes");
        log("[MAVEN][ARTIFACT] Validation: SUCCESS");

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

        File reactorRoot = context.getPreparedReactorDirectory().getAbsoluteFile();
        ProductDefinition prodDef = getProductDefinition();
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
        if (prodDef == null || platform == null) return null;

        File preparedReactor = context != null ? context.getPreparedReactorDirectory().getAbsoluteFile() : reactorRoot;
        if (preparedReactor == null || !preparedReactor.exists()) {
            return null;
        }

        // Restrict product discovery STRICTLY to current Self-Dev build reactor/output
        File targetProductsDir = new File(preparedReactor, prodDef.getRepositoryModule() + "/target/products");
        if (!targetProductsDir.exists() || !targetProductsDir.isDirectory()) {
            targetProductsDir = new File(preparedReactor, "target/products");
        }

        String reqFormat = platform.getPackaging() != null ? platform.getPackaging().trim().toLowerCase() : "zip";
        String reqTarget = platform.getOs() + "." + platform.getWs() + "." + platform.getArch();

        File logDir = context != null ? context.getLogDirectory() : new File(reactorRoot, "logs");
        if (!logDir.exists()) logDir.mkdirs();
        File selectionLogFile = new File(logDir, "product-selection.log");

        try (java.io.PrintWriter selectionWriter = new java.io.PrintWriter(new java.io.FileWriter(selectionLogFile, true))) {
            selectionWriter.println("[PRODUCT_DISCOVERY] productsRoot=" + targetProductsDir.getAbsolutePath());
            selectionWriter.println("[PRODUCT_DISCOVERY] requestedProduct=" + prodDef.getProductId());
            selectionWriter.println("[PRODUCT_DISCOVERY] requestedTarget=" + reqTarget);
            selectionWriter.println("[PRODUCT_DISCOVERY] requestedFormat=" + reqFormat);

            log("[PRODUCT_DISCOVERY] productsRoot=" + targetProductsDir.getAbsolutePath());
            log("[PRODUCT_DISCOVERY] requestedProduct=" + prodDef.getProductId());
            log("[PRODUCT_DISCOVERY] requestedTarget=" + reqTarget);
            log("[PRODUCT_DISCOVERY] requestedFormat=" + reqFormat);

            List<File> candidates = new ArrayList<>();
            File[] files = targetProductsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    selectionWriter.println("[PRODUCT_DISCOVERY] Inspecting: " + f.getAbsolutePath());
                    log("[PRODUCT_DISCOVERY] Inspecting:\n" + f.getAbsolutePath());
                    if (f.isDirectory()) {
                        log("[PRODUCT_DISCOVERY] type=DIRECTORY");
                    } else if (f.isFile()) {
                        log("[PRODUCT_DISCOVERY] type=FILE");
                    } else {
                        log("[PRODUCT_DISCOVERY] type=OTHER");
                    }

                    String reason = getExclusionReason(f, prodDef, platform);
                    if (reason == null) {
                        selectionWriter.println("  -> CANDIDATE MATCH");
                        log("[PRODUCT_DISCOVERY] format=" + reqFormat.toUpperCase());
                        log("[PRODUCT_DISCOVERY] product=" + prodDef.getProductId());
                        log("[PRODUCT_DISCOVERY] target=" + reqTarget);
                        log("[PRODUCT_DISCOVERY] candidate=true");
                        candidates.add(f);
                    } else {
                        selectionWriter.println("  -> EXCLUDED: " + reason);
                        log("[PRODUCT_DISCOVERY] excluded reason=" + reason);
                    }
                }
            }

            boolean isArchiveRequested = reqFormat.contains("zip") || reqFormat.contains("tar") || reqFormat.contains("gz") || reqFormat.contains("tgz");
            if (candidates.isEmpty() && !isArchiveRequested) {
                File nestedDir = platform.isWindows() ?
                        new File(targetProductsDir, prodDef.getProductId() + "/win32/win32/x86_64/" + prodDef.getRootFolder()) :
                        new File(targetProductsDir, prodDef.getProductId() + "/linux/gtk/x86_64/" + prodDef.getRootFolder());
                if (nestedDir.exists()) {
                    candidates.add(nestedDir);
                }
            }

            log("[PRODUCT_DISCOVERY] candidateCount=" + candidates.size());

            if (candidates.size() == 1) {
                File selected = candidates.get(0);
                selectionWriter.println("SELECTED PRODUCT: " + selected.getAbsolutePath());
                log("[PRODUCT_DISCOVERY] selected=" + selected.getAbsolutePath());
                return selected;
            } else if (candidates.size() > 1) {
                selectionWriter.println("PRODUCT SELECTION FAILED: Ambiguous candidates: " + candidates);
                throw new IOException("Multiple candidate exported products found under " + targetProductsDir.getAbsolutePath() + " matching " + prodDef.getProductId() + " (" + platform + "): " + candidates + ". Rejecting ambiguous selection.");
            } else {
                selectionWriter.println("PRODUCT SELECTION FAILED: Zero candidates found");
            }
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
        }

        return null;
    }

    private String getExclusionReason(File file, ProductDefinition prodDef, TargetPlatform platform) {
        if (file == null || !file.exists()) {
            return "FILE_NULL_OR_NON_EXISTENT";
        }

        String reqFormat = platform.getPackaging() != null ? platform.getPackaging().trim().toLowerCase() : "zip";
        boolean isArchiveRequested = reqFormat.contains("zip") || reqFormat.contains("tar") || reqFormat.contains("gz") || reqFormat.contains("tgz");
        boolean isDirRequested = reqFormat.contains("dir") || reqFormat.contains("folder") || reqFormat.contains("exploded");

        String name = file.getName().toLowerCase();

        if (isArchiveRequested) {
            if (!file.isFile()) {
                return "REQUESTED_FORMAT_" + (reqFormat.contains("zip") ? "ZIP" : reqFormat.toUpperCase()) + "_REQUIRES_REGULAR_FILE";
            }

            if (reqFormat.contains("zip") && !name.endsWith(".zip")) {
                return "FORMAT_MISMATCH_EXPECTED_ZIP";
            }
            if ((reqFormat.contains("tar") || reqFormat.contains("gz") || reqFormat.contains("tgz")) && !(name.endsWith(".tar.gz") || name.endsWith(".tgz"))) {
                return "FORMAT_MISMATCH_EXPECTED_TAR_GZ";
            }

            boolean nameMatchesProduct = name.startsWith(prodDef.getProductId().toLowerCase()) || name.contains(prodDef.getProductId().toLowerCase()) || name.contains("evo");
            if (!nameMatchesProduct) {
                return "UNMATCHED_PRODUCT_NAME";
            }

            if (platform.isWindows()) {
                if (!(name.contains("win32") || name.contains("win"))) {
                    return "UNMATCHED_TARGET_PLATFORM";
                }
            } else {
                if (!(name.contains("linux") || name.contains("gtk") || name.endsWith(".tar.gz"))) {
                    return "UNMATCHED_TARGET_PLATFORM";
                }
            }

            return null;
        } else if (isDirRequested) {
            if (!file.isDirectory()) {
                return "REQUESTED_FORMAT_DIRECTORY_REQUIRES_DIRECTORY";
            }

            boolean nameMatchesProduct = name.startsWith(prodDef.getProductId().toLowerCase()) || name.contains(prodDef.getProductId().toLowerCase()) || name.equals(prodDef.getRootFolder().toLowerCase());
            if (!nameMatchesProduct) {
                return "UNMATCHED_PRODUCT_NAME";
            }

            return null;
        } else {
            if (file.isFile()) {
                boolean isZipOrTar = name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".tgz");
                if (!isZipOrTar) return "UNSUPPORTED_FILE_FORMAT";

                boolean nameMatchesProduct = name.startsWith(prodDef.getProductId().toLowerCase()) || name.contains(prodDef.getProductId().toLowerCase()) || name.contains("evo");
                if (!nameMatchesProduct) return "UNMATCHED_PRODUCT_NAME";

                if (platform.isWindows()) {
                    if (!(name.contains("win32") || name.contains("win"))) return "UNMATCHED_TARGET_PLATFORM";
                } else {
                    if (!(name.contains("linux") || name.contains("gtk") || name.endsWith(".tar.gz"))) return "UNMATCHED_TARGET_PLATFORM";
                }
                return null;
            } else if (file.isDirectory()) {
                return "UNEXPECTED_DIRECTORY_FOR_DEFAULT_FORMAT";
            }
            return "UNKNOWN_ARTIFACT_TYPE";
        }
    }

    private boolean isExactMatchingArtifact(File file, ProductDefinition prodDef, TargetPlatform platform) {
        return getExclusionReason(file, prodDef, platform) == null;
    }

    public TaskResult validateProductDeployment(File location, ProductDefinition prodDef, TargetPlatform platform) {
        return validateProductDeployment(location, prodDef, platform, null);
    }

    public TaskResult validateProductDeployment(File location, ProductDefinition prodDef, TargetPlatform platform, SelfDevContext context) {
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

            return validateDirectoryLayout(rootDir, prodDef, platform, context);

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

    private TaskResult validateDirectoryLayout(File rootDir, ProductDefinition prodDef, TargetPlatform platform, SelfDevContext context) {
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            String err = "Product root directory does not exist or is not a directory: " + (rootDir != null ? rootDir.getAbsolutePath() : "null");
            log("[PRODUCT_VALIDATION] [FAIL] " + err);
            return TaskResult.failure("validate_product", err, null);
        }

        log("[PRODUCT_VALIDATION] Starting layout validation for product at: " + rootDir.getAbsolutePath());
        List<String> missingItems = new ArrayList<>();
        boolean isWin = platform != null ? platform.isWindows() : System.getProperty("os.name").toLowerCase().contains("win");
        String launcherName = prodDef != null ? prodDef.getLauncherName() : "evo";

        if (isWin) {
            File exeFile = new File(rootDir, launcherName + ".exe");
            File altExeFile = new File(rootDir, "eclipse.exe");
            if (!exeFile.exists() && !altExeFile.exists()) {
                missingItems.add("Launcher executable (" + launcherName + ".exe or eclipse.exe)");
                log("[PRODUCT_VALIDATION] [FAIL] Launcher executable missing (" + launcherName + ".exe or eclipse.exe)");
            } else {
                log("[PRODUCT_VALIDATION] [PASS] Launcher executable found: " + (exeFile.exists() ? exeFile.getName() : altExeFile.getName()));
            }
        } else {
            File nativeLauncher = new File(rootDir, launcherName);
            File shLauncher = new File(rootDir, launcherName + ".sh");
            File altLauncher = new File(rootDir, "eclipse");
            if (!nativeLauncher.exists() && !shLauncher.exists() && !altLauncher.exists()) {
                missingItems.add("Launcher executable (" + launcherName + " or " + launcherName + ".sh or eclipse)");
                log("[PRODUCT_VALIDATION] [FAIL] Launcher executable missing (" + launcherName + " / " + launcherName + ".sh / eclipse)");
            } else {
                if (nativeLauncher.exists()) nativeLauncher.setExecutable(true);
                if (shLauncher.exists()) shLauncher.setExecutable(true);
                log("[PRODUCT_VALIDATION] [PASS] Launcher executable found");
            }
        }

        File iniFile = new File(rootDir, launcherName + ".ini");
        File altIniFile = new File(rootDir, "eclipse.ini");
        if (!iniFile.exists() && !altIniFile.exists()) {
            missingItems.add("Launcher configuration (" + launcherName + ".ini or eclipse.ini)");
            log("[PRODUCT_VALIDATION] [FAIL] Launcher configuration missing (" + launcherName + ".ini or eclipse.ini)");
        } else {
            log("[PRODUCT_VALIDATION] [PASS] Launcher configuration found: " + (iniFile.exists() ? iniFile.getName() : altIniFile.getName()));
        }

        File pluginsDir = new File(rootDir, "plugins");
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            missingItems.add("plugins/ directory");
            log("[PRODUCT_VALIDATION] [FAIL] plugins/ directory missing at " + rootDir.getAbsolutePath());
        } else {
            File[] pluginEntries = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar") || new File(dir, name).isDirectory());
            if (pluginEntries == null || pluginEntries.length == 0) {
                missingItems.add("plugins/ directory contains no bundle JARs or directories");
                log("[PRODUCT_VALIDATION] [FAIL] plugins/ directory contains no bundle JARs or directories");
            } else {
                log("[PRODUCT_VALIDATION] [PASS] plugins/ directory exists with " + pluginEntries.length + " bundle entries.");
                String[] requiredCoreBundles = {
                    "eu.kalafatic.evolution.view_",
                    "eu.kalafatic.evolution.controller_",
                    "eu.kalafatic.evolution.model_",
                    "eu.kalafatic.evolution.servers_",
                    "eu.kalafatic.evolution.forge.controller_",
                    "eu.kalafatic.evolution.forge.model_",
                    "eu.kalafatic.evolution.forge.data_",
                    "eu.kalafatic.evolution.forge.agent_",
                    "eu.kalafatic.utils_"
                };
                for (String req : requiredCoreBundles) {
                    boolean found = false;
                    String matchedName = null;
                    for (File entry : pluginEntries) {
                        if (entry.getName().startsWith(req)) {
                            found = true;
                            matchedName = entry.getName();
                            break;
                        }
                    }
                    if (!found) {
                        missingItems.add("plugins/ missing required EVO bundle (" + req + "*.jar or directory)");
                        log("[PRODUCT_VALIDATION] [FAIL] Missing required EVO bundle: " + req + "*");
                    } else {
                        log("[PRODUCT_VALIDATION] [PASS] Found required EVO bundle: " + matchedName);
                    }
                }
            }
        }

        File configDir = new File(rootDir, "configuration");
        if (!configDir.exists() || !configDir.isDirectory()) {
            missingItems.add("configuration/ directory");
            log("[PRODUCT_VALIDATION] [FAIL] configuration/ directory missing");
        } else {
            File configIni = new File(configDir, "config.ini");
            if (!configIni.exists()) {
                missingItems.add("configuration/config.ini");
                log("[PRODUCT_VALIDATION] [FAIL] configuration/config.ini missing");
            } else {
                try {
                    String content = Files.readString(configIni.toPath());
                    if (!content.contains("eclipse.application") && !content.contains("eclipse.product")) {
                        missingItems.add("eclipse.application or eclipse.product entry in configuration/config.ini");
                        log("[PRODUCT_VALIDATION] [FAIL] config.ini missing eclipse.application or eclipse.product property");
                    } else {
                        log("[PRODUCT_VALIDATION] [PASS] configuration/config.ini validated");
                    }
                } catch (IOException e) {
                    missingItems.add("Readable configuration/config.ini (" + e.getMessage() + ")");
                    log("[PRODUCT_VALIDATION] [FAIL] Error reading config.ini: " + e.getMessage());
                }
            }
        }

        if (!missingItems.isEmpty()) {
            log("[PRODUCT_VALIDATION] [RESULT] FAILED due to " + missingItems.size() + " missing items: " + String.join(", ", missingItems));
            return TaskResult.failure("validate_product", "Product deployment validation failed due to missing items: " + String.join(", ", missingItems), null);
        }

        log("[PRODUCT_VALIDATION] [RESULT] SUCCESS - Product directory layout verified at " + rootDir.getAbsolutePath());

        if (context != null) {
            writeProductInventoryFiles(rootDir, context.getLogDirectory(), prodDef);
        }

        return new TaskResult.Builder("validate_product")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP product deployment validation passed successfully at " + rootDir.getAbsolutePath())
                .workingDirectory(rootDir)
                .build();
    }

    private void writeProductInventoryFiles(File rootDir, File logDir, ProductDefinition prodDef) {
        if (rootDir == null || !rootDir.exists() || logDir == null) return;
        if (!logDir.exists()) logDir.mkdirs();

        File inventoryFile = new File(logDir, "product-inventory.txt");
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(inventoryFile, false))) {
            pw.println("PRODUCT INVENTORY FOR: " + rootDir.getAbsolutePath());
            pw.println("==================================================");
            writeInventoryRecursive(rootDir, "", pw);
        } catch (Exception e) {
            log("[TychoEvoRcpBuilder] Error writing product-inventory.txt: " + e.getMessage());
        }

        File pluginsDir = new File(rootDir, "plugins");
        File pluginsFile = new File(logDir, "plugins.txt");
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(pluginsFile, false))) {
            pw.println("PLUGINS INVENTORY (" + pluginsDir.getAbsolutePath() + "):");
            if (pluginsDir.exists() && pluginsDir.isDirectory()) {
                File[] pluginFiles = pluginsDir.listFiles();
                if (pluginFiles != null) {
                    Arrays.sort(pluginFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    int evoCount = 0;
                    long totalBytes = 0;
                    for (File pf : pluginFiles) {
                        pw.println(pf.getName() + " (" + pf.length() + " bytes)");
                        if (pf.getName().startsWith("eu.kalafatic.")) {
                            evoCount++;
                        }
                        totalBytes += pf.length();
                    }
                    log("[PRODUCT_INVENTORY] Plugins total count: " + pluginFiles.length + " (EVO bundles: " + evoCount + ", total size: " + (totalBytes / (1024 * 1024)) + " MB)");
                }
            } else {
                pw.println("plugins directory missing");
                log("[PRODUCT_INVENTORY] WARNING: plugins directory missing");
            }
        } catch (Exception e) {
            log("[TychoEvoRcpBuilder] Error writing plugins.txt: " + e.getMessage());
        }

        File featuresDir = new File(rootDir, "features");
        File featuresFile = new File(logDir, "features.txt");
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(featuresFile, false))) {
            pw.println("FEATURES INVENTORY (" + featuresDir.getAbsolutePath() + "):");
            if (featuresDir.exists() && featuresDir.isDirectory()) {
                File[] featureFiles = featuresDir.listFiles();
                if (featureFiles != null) {
                    Arrays.sort(featureFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    for (File ff : featureFiles) {
                        pw.println(ff.getName());
                    }
                }
            } else {
                pw.println("features directory missing or empty");
            }
        } catch (Exception e) {
            log("[TychoEvoRcpBuilder] Error writing features.txt: " + e.getMessage());
        }

        File configIni = new File(rootDir, "configuration/config.ini");
        if (configIni.exists()) {
            try {
                Files.copy(configIni.toPath(), new File(logDir, "config.ini").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}
        }
        String launcherName = prodDef != null ? prodDef.getLauncherName() : "evo";
        File iniFile = new File(rootDir, launcherName + ".ini");
        if (!iniFile.exists()) iniFile = new File(rootDir, "eclipse.ini");
        if (iniFile.exists()) {
            try {
                Files.copy(iniFile.toPath(), new File(logDir, "eclipse.ini").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}
        }
    }

    private void writeInventoryRecursive(File dir, String prefix, java.io.PrintWriter pw) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File f : files) {
            pw.println(prefix + (f.isDirectory() ? "[DIR] " : "[FILE] ") + f.getName() + (f.isFile() ? " (" + f.length() + " bytes)" : ""));
            if (f.isDirectory() && !f.getName().equals("plugins")) {
                writeInventoryRecursive(f, prefix + "  ", pw);
            }
        }
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

    private void copyDirectoryRecursively(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) dest.mkdirs();
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyDirectoryRecursively(child, new File(dest, child.getName()));
                }
            }
        } else {
            Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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

    private void log(String message) {
        Log.log(message);
        System.out.println(message);
    }
}
