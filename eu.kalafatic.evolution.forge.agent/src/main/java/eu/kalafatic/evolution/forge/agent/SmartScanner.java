package eu.kalafatic.evolution.forge.agent;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SmartScanner {
    private final Path rootPath;
    private final List<Pattern> ignorePatterns = new ArrayList<>();

    public SmartScanner(Path rootPath) {
        this.rootPath = rootPath;
        loadGitIgnore();
        // Add default ignores
        ignorePatterns.add(Pattern.compile(".*\\.git/.*"));
        ignorePatterns.add(Pattern.compile(".*/target/.*"));
        ignorePatterns.add(Pattern.compile(".*/bin/.*"));
        ignorePatterns.add(Pattern.compile(".*/\\.settings/.*"));
        ignorePatterns.add(Pattern.compile(".*\\.project"));
        ignorePatterns.add(Pattern.compile(".*\\.classpath"));
    }

    private void loadGitIgnore() {
        Path gitIgnore = rootPath.resolve(".gitignore");
        if (Files.exists(gitIgnore)) {
            try {
                List<String> lines = Files.readAllLines(gitIgnore);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    ignorePatterns.add(globToPattern(line));
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not read .gitignore: " + e.getMessage());
            }
        }
    }

    private Pattern globToPattern(String glob) {
        String regex = glob.replace(".", "\\.")
                           .replace("*", ".*")
                           .replace("?", ".");
        if (glob.endsWith("/")) {
            regex = regex + ".*";
        }
        return Pattern.compile(".*" + regex + ".*");
    }

    public List<Path> scan() throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (isIgnored(dir)) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!isIgnored(file) && isInteresting(file)) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private boolean isIgnored(Path path) {
        String relativePath = rootPath.relativize(path).toString().replace("\\", "/");
        for (Pattern pattern : ignorePatterns) {
            if (pattern.matcher(relativePath).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isInteresting(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".java") || name.endsWith(".md") || name.endsWith(".xml")
            || name.endsWith(".json") || name.endsWith(".properties") || name.endsWith(".sh");
    }
}
