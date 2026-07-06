package eu.kalafatic.evolution.servers.mcp.demo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DemoDocumentService {
    private final String rootFolder;
    private final DemoDocumentScanner scanner;

    public DemoDocumentService(String rootFolder) {
        this.rootFolder = rootFolder;
        this.scanner = new DemoDocumentScanner(rootFolder);
    }

    public List<DemoDocumentScanner.DocumentInfo> listDocuments() {
        return scanner.scan();
    }

    public DocumentContent getDocument(String path) throws IOException {
        File root = new File(rootFolder).getCanonicalFile();
        File file = new File(root, path).getCanonicalFile();
        if (!file.getPath().startsWith(root.getPath())) {
            throw new SecurityException("Access denied: path is outside documentation folder");
        }
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String title = extractTitle(content, file.getName());
        return new DocumentContent(path, title, content);
    }

    private String extractTitle(String content, String defaultTitle) {
        if (content.startsWith("# ")) {
            int end = content.indexOf('\n');
            if (end != -1) {
                return content.substring(2, end).trim();
            }
            return content.substring(2).trim();
        }
        return defaultTitle;
    }

    public List<SearchResult> search(String query) {
        String lowerQuery = query.toLowerCase();
        return listDocuments().stream()
            .filter(doc -> {
                try {
                    DocumentContent content = getDocument(doc.getPath());
                    return content != null && (content.getTitle().toLowerCase().contains(lowerQuery) || 
                                              content.getContent().toLowerCase().contains(lowerQuery));
                } catch (IOException e) {
                    return false;
                }
            })
            .map(doc -> {
                try {
                    DocumentContent content = getDocument(doc.getPath());
                    return new SearchResult(doc.getPath(), content.getTitle());
                } catch (IOException e) {
                    return new SearchResult(doc.getPath(), doc.getName());
                }
            })
            .collect(Collectors.toList());
    }

    public static class DocumentContent {
        private final String path;
        private final String title;
        private final String content;

        public DocumentContent(String path, String title, String content) {
            this.path = path;
            this.title = title;
            this.content = content;
        }

        public String getPath() { return path; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
    }

    public static class SearchResult {
        private final String path;
        private final String title;

        public SearchResult(String path, String title) {
            this.path = path;
            this.title = title;
        }

        public String getPath() { return path; }
        public String getTitle() { return title; }
    }
}
