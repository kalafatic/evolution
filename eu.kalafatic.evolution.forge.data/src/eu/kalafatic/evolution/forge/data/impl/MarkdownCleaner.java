package eu.kalafatic.evolution.forge.data.impl;

public class MarkdownCleaner {
    public String clean(String corpus) {
        if (corpus == null) return "";

        // Remove HTML comments
        String cleaned = corpus.replaceAll("(?s)<!--.*?-->", "");

        // Remove repeated empty lines
        cleaned = cleaned.replaceAll("(?m)^\\s*$\\n{2,}", "\n\n");

        // Remove duplicate spaces (but preserve leading indentation for lists/code)
        cleaned = cleaned.replaceAll("([^\\S\\n])\\s+", "$1");

        return cleaned.trim();
    }
}
