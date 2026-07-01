package eu.kalafatic.evolution.controller.discovery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import eu.kalafatic.evolution.controller.log.Log;

/**
 * Resolver for discovering the primary source repository from the Eclipse workspace.
 */
public class WorkspaceSourceResolver {

    private static final Set<String> EXCLUDE_DIRS = Set.of(
        "target", "bin", "build", "out", ".settings", ".metadata", "node_modules", ".git"
    );

    private static final Pattern RUNTIME_WORKSPACE_PATTERN = Pattern.compile("runtime-eu\\.kalafatic\\..*");

    public SourceDiscoveryResult discover(SourceDiscoveryRequest request) {
        SourceDiscoveryResult result = new SourceDiscoveryResult();
        Log.log("[DISCOVERY] Starting Workspace Source Discovery...");

        // Phase 1 - Resolve Eclipse Projects
        resolveProjects(result);

        // Phase 2 - Find Git Roots
        findGitRoots(result);

        // Phase 3-5 - Build File Inventory & Filtering & Exclusions
        buildInventory(result, request);

        // Phase 6 - Repository Compression / Statistics
        buildStatistics(result);

        calculateConfidence(result);

        return result;
    }

    private void resolveProjects(SourceDiscoveryResult result) {
        try {
            File workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
            result.setWorkspaceRoot(workspaceRoot);
            Log.log("[DISCOVERY] Workspace Root: " + workspaceRoot.getAbsolutePath());

            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            for (IProject project : projects) {
                if (project.isOpen()) {
                    IPath location = project.getLocation();
                    if (location != null) {
                        File projectDir = location.toFile();
                        if (isExcludedWorkspace(projectDir)) {
                            Log.log("[DISCOVERY] Skipping runtime workspace project: " + projectDir.getName());
                            continue;
                        }
                        result.getProjectRoots().add(projectDir);
                    }
                }
            }
            Log.log("[DISCOVERY] Resolved " + result.getProjectRoots().size() + " projects.");
        } catch (Exception e) {
            Log.log("[DISCOVERY] Error resolving projects: " + e.getMessage());
        }
    }

    private boolean isExcludedWorkspace(File dir) {
        if (dir == null) return false;
        String name = dir.getName();
        if (RUNTIME_WORKSPACE_PATTERN.matcher(name).matches()) return true;

        // Check parents for runtime workspaces too
        File parent = dir.getParentFile();
        while (parent != null) {
            if (RUNTIME_WORKSPACE_PATTERN.matcher(parent.getName()).matches()) return true;
            parent = parent.getParentFile();
        }
        return false;
    }

    private void findGitRoots(SourceDiscoveryResult result) {
        for (File projectDir : result.getProjectRoots()) {
            findGitRoot(projectDir).ifPresent(root -> result.getGitRepositories().add(root));
        }
        Log.log("[DISCOVERY] Found " + result.getGitRepositories().size() + " Git repositories.");

        // Find dominant repository
        if (!result.getGitRepositories().isEmpty()) {
            Map<File, Integer> repoCounts = new HashMap<>();
            for (File projectDir : result.getProjectRoots()) {
                findGitRoot(projectDir).ifPresent(root -> repoCounts.merge(root, 1, Integer::sum));
            }

            File dominant = null;
            int max = -1;
            for (Map.Entry<File, Integer> entry : repoCounts.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    dominant = entry.getKey();
                }
            }
            result.setPrimaryRepository(dominant);
            Log.log("[DISCOVERY] Primary Repository: " + (dominant != null ? dominant.getAbsolutePath() : "None"));
        }
    }

    private Optional<File> findGitRoot(File dir) {
        File current = dir;
        File gitRoot = null;
        File mavenRoot = null;

        while (current != null) {
            File gitDir = new File(current, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                gitRoot = current;
                // Once we find a Git root, we can stop walking up for Git,
                // but we might want to see if there's a Maven parent even higher (unlikely but possible)
                // or if the Git root itself is the Maven root.
            }

            File pomFile = new File(current, "pom.xml");
            if (pomFile.exists()) {
                mavenRoot = current;
            }

            current = current.getParentFile();
        }

        // Prefer Git root, but if Maven root is found and it's a parent of the project, it's a strong candidate
        if (gitRoot != null) return Optional.of(gitRoot);
        if (mavenRoot != null) return Optional.of(mavenRoot);

        return Optional.empty();
    }

    private void buildInventory(SourceDiscoveryResult result, SourceDiscoveryRequest request) {
        File primary = result.getPrimaryRepository();
        if (primary == null) return;

        Log.log("[DISCOVERY] Building file inventory for: " + primary.getAbsolutePath());

        try (Stream<Path> stream = Files.walk(primary.toPath())) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> !isExcluded(path, primary.toPath()))
                  .filter(path -> matchesRequest(path, request))
                  .limit(request != null ? request.getMaxFiles() : 50000)
                  .forEach(path -> {
                      File f = path.toFile();
                      String relPath = primary.toPath().relativize(path).toString().replace('\\', '/');
                      String ext = getExtension(f.getName());

                      // Find which project this file belongs to
                      File projectRoot = null;
                      for (File pRoot : result.getProjectRoots()) {
                          if (f.getAbsolutePath().startsWith(pRoot.getAbsolutePath())) {
                              projectRoot = pRoot;
                              break;
                          }
                      }

                      result.getFiles().add(new SourceFileDescriptor(f, primary, projectRoot, relPath, ext));
                  });
        } catch (IOException e) {
            Log.log("[DISCOVERY] Error walking repository: " + e.getMessage());
        }
        Log.log("[DISCOVERY] Inventory build complete. Files found: " + result.getFiles().size());
    }

    private boolean isExcluded(Path path, Path root) {
        for (Path part : root.relativize(path)) {
            String name = part.toString();
            if (EXCLUDE_DIRS.contains(name)) return true;
            if (RUNTIME_WORKSPACE_PATTERN.matcher(name).matches()) return true;
        }
        return false;
    }

    private boolean matchesRequest(Path path, SourceDiscoveryRequest request) {
        if (request == null) return true;

        String fileName = path.getFileName().toString();
        if (request.getIncludeExtensions() != null) {
            String ext = getExtension(fileName);
            if (!request.getIncludeExtensions().matcher(ext).matches()) return false;
        }

        if (request.getPathFilter() != null) {
            String fullPath = path.toString().replace('\\', '/');
            if (!request.getPathFilter().matcher(fullPath).find()) return false;
        }

        return true;
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private void buildStatistics(SourceDiscoveryResult result) {
        Map<String, Integer> stats = result.getStatistics();
        for (SourceFileDescriptor fd : result.getFiles()) {
            stats.merge(fd.getExtension(), 1, Integer::sum);
        }

        Log.log("[DISCOVERY] Repository Statistics:");
        stats.entrySet().stream()
             .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
             .limit(10)
             .forEach(e -> Log.log("  " + e.getKey() + ": " + e.getValue()));
    }

    private void calculateConfidence(SourceDiscoveryResult result) {
        if (result.getPrimaryRepository() == null) {
            result.setConfidence(0.0);
            return;
        }

        double conf = 0.5; // Base confidence if primary repo found

        if (result.getGitRepositories().size() == 1) conf += 0.2;
        if (result.getProjectRoots().size() > 0) conf += 0.1;

        // If it looks like EVO repository
        File primary = result.getPrimaryRepository();
        if (new File(primary, "eu.kalafatic.evolution.controller").exists()) conf += 0.15;
        if (new File(primary, "pom.xml").exists()) conf += 0.05;

        result.setConfidence(Math.min(1.0, conf));
        Log.log("[DISCOVERY] Repository Confidence: " + (int)(result.getConfidence() * 100) + "%");
    }
}
