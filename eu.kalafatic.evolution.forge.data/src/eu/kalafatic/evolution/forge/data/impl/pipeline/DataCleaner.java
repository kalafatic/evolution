package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.processor.DataNormalizer;

/**
 * Preprocessing and cleaning stage for NormalizedSample records.
 */
public class DataCleaner implements DataNormalizer {

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

    @Override
    public NormalizedSample normalize(NormalizedSample sample) {
        return clean(sample);
    }

    public NormalizedSample clean(NormalizedSample sample) {
        if (sample == null) return null;

        String rawText = sample.toFullText();
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }

        String text = rawText;

        // 1. Strip non-printable ASCII control characters and Unicode replacement chars
        text = text.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\uFFFD]", "");

        boolean isCode = sample.getType() == eu.kalafatic.evolution.forge.data.api.TrainingSampleType.CODE;

        // 2. Whitespace normalization
        if (normalizeWhitespace) {
            text = text.replace("\r\n", "\n").replace("\r", "\n");
            // Collapse excessive blank lines
            text = text.replaceAll("\n{3,}", "\n\n").trim();
        }

        // 3. HTML stripping if configured (suppressed for CODE samples to preserve Java generics <T>, XML, JSX)
        if (stripHtml && !isCode && (text.contains("<html") || text.contains("<div") || text.contains("<p>") || text.contains("<body") || text.contains("<!DOCTYPE"))) {
            text = text.replaceAll("<[^>]*>", " ");
            text = text.replaceAll("\\s+", " ").trim();
        }

        // 4. Length checks (allow short meaningful code statements like "int x = 1;")
        int effMinLength = isCode ? Math.min(3, minCharLength) : minCharLength;
        if (text.length() < effMinLength || text.length() > maxCharLength) {
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
