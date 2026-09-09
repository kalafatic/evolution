package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

import java.util.Set;

/**
 * Extensible quality scoring stage evaluating text signals (diversity, repetition, language confidence).
 */
public class TrainingSampleQualityScorer {

    private double minQualityScore = 0.5;
    private Set<String> allowedLanguages = Set.of("en", "cz", "de", "fr", "es");
    private boolean languageFilterEnabled = false;

    public TrainingSampleQualityScorer() {}

    public TrainingSampleQualityScorer(double minQualityScore) {
        this.minQualityScore = minQualityScore;
    }

    public double calculateQualityScore(NormalizedSample sample) {
        if (sample == null) return 0.0;

        String text = sample.toFullText();
        if (text == null || text.trim().isEmpty()) return 0.0;

        double score = 1.0;

        // Signal 1: Character repetition penalty
        int len = text.length();
        int uniqueChars = (int) text.chars().distinct().count();
        double charDiversity = (double) uniqueChars / Math.min(len, 100);
        if (charDiversity < 0.15) {
            score -= 0.4;
        }

        // Signal 2: Word repetition penalty
        String[] words = text.split("\\s+");
        if (words.length > 10) {
            long uniqueWords = java.util.Arrays.stream(words).distinct().count();
            double wordRatio = (double) uniqueWords / words.length;
            if (wordRatio < 0.3) {
                score -= 0.3;
            }
        }

        // Signal 3: Structural validity (extreme uppercase or malformed text penalty)
        long uppercaseCount = text.chars().filter(Character::isUpperCase).count();
        if (len > 20 && (double) uppercaseCount / len > 0.8) {
            score -= 0.3;
        }

        score = Math.max(0.0, Math.min(1.0, score));
        sample.setQualityScore(score);

        return score;
    }

    public boolean isAcceptable(NormalizedSample sample) {
        if (sample == null) return false;
        double score = calculateQualityScore(sample);
        if (score < minQualityScore) return false;

        if (languageFilterEnabled) {
            String lang = sample.getLanguage();
            if (lang != null && !allowedLanguages.isEmpty() && !allowedLanguages.contains(lang.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public double getMinQualityScore() { return minQualityScore; }
    public void setMinQualityScore(double minQualityScore) { this.minQualityScore = minQualityScore; }

    public boolean isLanguageFilterEnabled() { return languageFilterEnabled; }
    public void setLanguageFilterEnabled(boolean languageFilterEnabled) { this.languageFilterEnabled = languageFilterEnabled; }

    public Set<String> getAllowedLanguages() { return allowedLanguages; }
    public void setAllowedLanguages(Set<String> allowedLanguages) { this.allowedLanguages = allowedLanguages; }
}
