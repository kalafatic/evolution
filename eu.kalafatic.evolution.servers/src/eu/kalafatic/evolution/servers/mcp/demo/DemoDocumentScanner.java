package eu.kalafatic.evolution.servers.mcp.demo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DemoDocumentScanner {
    private final String rootFolder;

    public DemoDocumentScanner(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public List<DocumentInfo> scan() {
        List<DocumentInfo> documents = new ArrayList<>();
        File root = new File(rootFolder);
        if (root.exists() && root.isDirectory()) {
            scanRecursive(root, "", documents);
        }
        return documents;
    }

    private void scanRecursive(File folder, String relativePath, List<DocumentInfo> documents) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String path = relativePath.isEmpty() ? name : relativePath + "/" + name;
                if (file.isDirectory()) {
                    scanRecursive(file, path, documents);
                } else if (name.toLowerCase().endsWith(".md")) {
                    documents.add(new DocumentInfo(name, path));
                }
            }
        }
    }

    public static class DocumentInfo {
        private final String name;
        private final String path;

        public DocumentInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }

        public String getName() { return name; }
        public String getPath() { return path; }
    }
}
