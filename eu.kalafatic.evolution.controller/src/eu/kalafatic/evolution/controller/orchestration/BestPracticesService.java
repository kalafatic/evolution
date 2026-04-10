package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
import eu.kalafatic.utils.log.Log;

/**
 * Service to load and manage best practices from Markdown files.
 */
public class BestPracticesService {
    private final File baseDir;
    private final Map<String, String> practicesMap = new HashMap<>();

    public BestPracticesService(File projectRoot) {
        this.baseDir = new File(projectRoot, "orchestrator/best_practices");
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        reload();
    }

    public void reload() {
        practicesMap.clear();
        loadFromSubDir("architect");
        loadFromSubDir("planner");
        loadFromSubDir("agent");
        loadFromSubDir("tools");
    }

    private void loadFromSubDir(String category) {
        File subDir = new File(baseDir, category);
        if (subDir.exists() && subDir.isDirectory()) {
            File[] files = subDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".md"));
            if (files != null) {
                StringBuilder sb = new StringBuilder();
                for (File file : files) {
                    try {
                        String content = Files.readString(file.toPath());
                        sb.append("--- ").append(file.getName()).append(" ---\n");
                        sb.append(content).append("\n\n");
                    } catch (IOException e) {
                        Log.log("Failed to read best practice file: " + file.getAbsolutePath() + " - " + e.getMessage());
                    }
                }
                if (sb.length() > 0) {
                    practicesMap.put(category, sb.toString().trim());
                }
            }
        }
    }

    public String getPractices(String category) {
        return practicesMap.getOrDefault(category.toLowerCase(), "");
    }

    public Map<String, String> getAllPractices() {
        return new HashMap<>(practicesMap);
    }
}
