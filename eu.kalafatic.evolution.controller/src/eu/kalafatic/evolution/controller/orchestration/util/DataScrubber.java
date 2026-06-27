package eu.kalafatic.evolution.controller.orchestration.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility to scrub sensitive data from strings before sending to external services.
 */
public class DataScrubber {

    private static final Pattern OPENAI_API_KEY_PATTERN = Pattern.compile("sk-[a-zA-Z0-9]{48}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
    private static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile("(?:[a-zA-Z]:\\\\|/)[a-zA-Z0-9/\\\\._-]+");

    /**
     * Scrubs sensitive data from the given text.
     *
     * @param text Text to scrub
     * @return Scrubbed text
     */
    public static String scrub(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String scrubbed = text;

        // Scrub OpenAI API Keys
        scrubbed = OPENAI_API_KEY_PATTERN.matcher(scrubbed).replaceAll("[SECRET_API_KEY]");

        // Scrub Email Addresses
        scrubbed = EMAIL_PATTERN.matcher(scrubbed).replaceAll("[EMAIL_REDACTED]");

        // Scrub absolute paths, but keep filenames if possible (simple heuristic)
        // This is tricky, so let's just mask the whole path if it looks absolute
        Matcher pathMatcher = ABSOLUTE_PATH_PATTERN.matcher(scrubbed);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (pathMatcher.find()) {
            sb.append(scrubbed, lastEnd, pathMatcher.start());
            String fullPath = pathMatcher.group();
            // Heuristic: If it looks like a file in a standard project, maybe keep just the filename?
            // For now, let's just mask it to be safe.
            sb.append("[PATH_REDACTED]");
            lastEnd = pathMatcher.end();
        }
        sb.append(scrubbed.substring(lastEnd));

        return sb.toString();
    }
}
