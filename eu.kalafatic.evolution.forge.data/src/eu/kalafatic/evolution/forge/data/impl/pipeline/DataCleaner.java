package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

/**
 * Preprocessing and cleaning stage for NormalizedSample records.
 */
public class DataCleaner {

    private boolean normalizeWhitespace = true;
    private boolean stripHtml = true;
    private int minCharLength = 10;
    private int maxCharLength = 100000;

    public DataCleaner() {}

    public DataCleaner(boolean normalizeWhitespace, boolean stripHtml, int minCharLength, int maxCharLength) {
        this.normalizeWhitespace = normalizeWhitespace;
        this.stripHtml = stripHtml;
        this.minCharLength = minCharLength;
        this.maxCharLength = maxCharLength;
    }

    public NormalizedSample clean(NormalizedSample sample) {
        if (sample == null) return null;

        String rawText = sample.toFullText();
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }

        String text = rawText;

        // 1. Whitespace normalization
        if (normalizeWhitespace) {
            text = text.replace("\r\n", "\n").replace("\r", "\n");
            // Collapse excessive blank lines
            text = text.replaceAll("\n{3,}", "\n\n").trim();
        }

        // 2. HTML stripping if configured
        if (stripHtml && (text.contains("<html") || text.contains("<div") || text.contains("<p>"))) {
            text = text.replaceAll("<[^>]*>", " ");
            text = text.replaceAll("\\s+", " ").trim();
        }

        // 3. Length checks
        if (text.length() < minCharLength || text.length() > maxCharLength) {
            return null;
        }

        // Update sample text
        sample.setText(text);
        sample.recalculateCountsAndHash();

        return sample;
    }

    public boolean isNormalizeWhitespace() { return normalizeWhitespace; }
    public void setNormalizeWhitespace(boolean normalizeWhitespace) { this.normalizeWhitespace = normalizeWhitespace; }

    public boolean isStripHtml() { return stripHtml; }
    public void setStripHtml(boolean stripHtml) { this.stripHtml = stripHtml; }

    public int getMinCharLength() { return minCharLength; }
    public void setMinCharLength(int minCharLength) { this.minCharLength = minCharLength; }

    public int getMaxCharLength() { return maxCharLength; }
    public void setMaxCharLength(int maxCharLength) { this.maxCharLength = maxCharLength; }
}
