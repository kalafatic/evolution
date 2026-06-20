package eu.kalafatic.evolution.selfdev.genome.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleMarkdownConverter {

    public static String toHtml(String markdown) {
        if (markdown == null) return "";

        // Strip YAML Metadata header if present
        if (markdown.startsWith("---")) {
            int secondDash = markdown.indexOf("---", 3);
            if (secondDash > 0) {
                markdown = markdown.substring(secondDash + 3).trim();
            }
        }

        // 1. Escape HTML special characters FIRST to avoid XSS and literal text issues
        String html = escapeHtml(markdown);

        // 2. Headings (must be at start of line)
        html = html.replaceAll("(?m)^### (.*)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^## (.*)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^# (.*)$", "<h1>$1</h1>");

        // 3. Bold and Italic
        html = html.replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", "<strong><em>$1</em></strong>");
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.*?)\\*", "<em>$1</em>");
        html = html.replaceAll("___(.*?)___", "<strong><em>$1</em></strong>");
        html = html.replaceAll("__(.*?)__", "<strong>$1</strong>");
        html = html.replaceAll("_(.*?)_", "<em>$1</em>");

        // 4. Code blocks (simple)
        // Note: Code blocks are already escaped by step 1, which is what we want for content inside code blocks.
        Pattern codeBlockPattern = Pattern.compile("```(\\w*)\\n?(.*?)\\n?```", Pattern.DOTALL);
        Matcher matcher = codeBlockPattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(html.substring(lastEnd, matcher.start()));
            sb.append("<pre><code>");
            sb.append(matcher.group(2));
            sb.append("</code></pre>");
            lastEnd = matcher.end();
        }
        sb.append(html.substring(lastEnd));
        html = sb.toString();

        // 5. Inline code
        html = html.replaceAll("`(.*?)`", "<code>$1</code>");

        // 6. Lists (unordered)
        html = html.replaceAll("(?m)^- (.*)$", "<li>$1</li>");
        // Wrap adjacent <li> in <ul>
        // This is a simple approximation.
        html = html.replaceAll("((?:<li>.*</li>\\n?)+)", "<ul>$1</ul>");

        // 7. Paragraphs (split by double newline)
        String[] segments = html.split("\\n\\s*\\n");
        StringBuilder finalHtml = new StringBuilder();
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("<h") || trimmed.startsWith("<ul") || trimmed.startsWith("<pre") || trimmed.startsWith("<li")) {
                finalHtml.append(trimmed).append("\n");
            } else {
                finalHtml.append("<p>").append(trimmed.replace("\n", "<br/>")).append("</p>\n");
            }
        }

        return finalHtml.toString();
    }

    private static String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
