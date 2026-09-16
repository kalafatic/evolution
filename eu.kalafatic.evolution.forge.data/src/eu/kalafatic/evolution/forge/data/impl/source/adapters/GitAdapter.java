package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;
import eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Universal source adapter for Git repositories and local git working trees.
 */
public class GitAdapter implements DatasetSourceAdapter {

    private final SourceAdapterRegistry registry;

    public GitAdapter() {
        this(null);
    }

    public GitAdapter(SourceAdapterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String type = item.getType() != null ? item.getType().trim().toUpperCase() : "";
        String path = item.getPath().trim();

        if ("GIT".equals(type) || "GIT_REPOSITORY".equals(type)) {
            return true;
        }

        if (path.endsWith(".git") || path.startsWith("git@") || (path.startsWith("http") && path.contains("git"))) {
            return true;
        }

        File f = new File(path);
        if (f.exists() && f.isDirectory() && new File(f, ".git").exists()) {
            return true;
        }

        return false;
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "GitAdapter", false, 0, "git", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isDirectory();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "GitAdapter", exists, size, "git", exists, exists ? "Git Repository Source" : "Directory does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File dir = new File(item.getPath());
        if (!dir.exists() || !dir.isDirectory()) {
            context.log("[forge.dataset] [GitAdapter] Directory not found: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing Git repository source: " + dir.getName());

        SourceAdapterRegistry activeRegistry = registry != null ? registry : SourceAdapterRegistry.createDefaultRegistry();

        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains("/.git/") && !p.toString().contains("\\.git\\")
                              && !p.toString().contains("/target/") && !p.toString().contains("\\target\\"))
                    .toList();

            for (Path file : files) {
                if (context.isCancelled()) break;
                DatasetItem childItem = new DatasetItem(true, file.toAbsolutePath().toString(), "FILE");
                DatasetSourceAdapter childAdapter = activeRegistry.findAdapter(childItem);

                if (childAdapter != null && !(childAdapter instanceof GitAdapter)) {
                    List<NormalizedSample> childSamples = childAdapter.convert(childItem, context);
                    if (childSamples != null) {
                        samples.addAll(childSamples);
                    }
                } else if (file.toString().endsWith(".txt") || file.toString().endsWith(".md") || file.toString().endsWith(".java")) {
                    String content = Files.readString(file, StandardCharsets.UTF_8).trim();
                    if (!content.isEmpty()) {
                        samples.add(NormalizedSample.createTextSample(content, file.getFileName().toString()));
                    }
                }
            }
        }

        context.log("[forge.dataset] Git Repository Parsed " + samples.size() + " samples from " + dir.getName());
        return samples;
    }
}
