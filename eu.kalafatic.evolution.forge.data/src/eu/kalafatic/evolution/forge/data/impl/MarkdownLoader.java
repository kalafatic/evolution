package eu.kalafatic.evolution.forge.data.impl;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MarkdownLoader {
    public String loadFromDirectory(Path directory) throws IOException {
        List<Path> markdownFiles = findMarkdownFiles(directory);
        StringBuilder corpus = new StringBuilder();
        for (Path file : markdownFiles) {
            corpus.append(Files.readString(file)).append("\n\n");
        }
        return corpus.toString();
    }

    private List<Path> findMarkdownFiles(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".md"))
                .filter(p -> !isIgnored(p))
                .collect(Collectors.toList());
        }
    }

    private boolean isIgnored(Path path) {
        String s = path.toString();
        return s.contains("/.git/") || s.contains("/target/") || s.contains("/node_modules/");
    }
}
