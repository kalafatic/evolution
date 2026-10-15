package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.processor.DataFilter;

import java.util.Set;

/**
 * Extensible quality scoring stage evaluating text signals (diversity, repetition, language confidence).
 */
public class TrainingSampleQualityScorer implements DataFilter {

    private double minQualityScore = 0.5;
    private Set<String> allowedLanguages = Set.of("en", "cz", "de", "fr", "es");
    private boolean languageFilterEnabled = false;

    public TrainingSampleQualityScorer() {}

    public TrainingSampleQualityScorer(double minQualityScore) {
        this.minQualityScore = minQualityScore;
    }

    @Override
    public boolean accept(NormalizedSample sample) {
        return isAcceptable(sample);
    }

    @Override
    public double calculateQualityScore(NormalizedSample sample) {
        if (sample == null) return 0.0;

        String text = sample.toFullText();
        if (text == null || text.trim().isEmpty()) return 0.0;

        String strippedText = text.replaceAll("[^a-zA-Z0-9]", "").trim();
        if (strippedText.isEmpty()) {
            sample.setQualityScore(0.0);
            return 0.0; // Punctuation / noise only
        }

        double score = 1.0;

        // Signal 1: Corrupted character check (\uFFFD or unprintable noise ratio)
        long replacementChars = text.chars().filter(ch -> ch == '\uFFFD').count();
        if (text.length() > 0 && (double) replacementChars / text.length() > 0.05) {
            score -= 0.6;
        }

        // Signal 2: Invalid Instruction / Question-Answer pairs
        if (sample.getType() == eu.kalafatic.evolution.forge.data.api.TrainingSampleType.INSTRUCTION) {
            String inst = sample.getInstruction() != null ? sample.getInstruction().trim() : "";
            String resp = sample.getResponse() != null ? sample.getResponse().trim() : "";

            if (inst.isEmpty() || resp.isEmpty()) {
                score = 0.0; // Empty prompt or response
            } else {
                String normInst = inst.toLowerCase().replaceAll("[^a-z0-9]", "");
                String normResp = resp.toLowerCase().replaceAll("[^a-z0-9]", "");

                if (normInst.equals(normResp)) {
                    score -= 0.6; // Prompt equals response
                } else if (normInst.contains(normResp) && normResp.length() > 10 && normResp.length() == normInst.length()) {
                    score -= 0.5; // Response simply repeats instruction
                }

                if (resp.equalsIgnoreCase("n/a") || resp.equalsIgnoreCase("none") || resp.equalsIgnoreCase("no answer") || resp.equalsIgnoreCase("i don't know")) {
                    score -= 0.5; // Trivial non-answer
                }
            }
        }

        // Signal 3: Semantically repetitive conversation turns
        if ((sample.getType() == eu.kalafatic.evolution.forge.data.api.TrainingSampleType.CONVERSATION ||
             sample.getType() == eu.kalafatic.evolution.forge.data.api.TrainingSampleType.CHAT) &&
            sample.getConversationMessages() != null && sample.getConversationMessages().size() >= 2) {

            var msgs = sample.getConversationMessages();
            for (int i = 1; i < msgs.size(); i++) {
                String prevText = msgs.get(i - 1).getText() != null ? msgs.get(i - 1).getText().trim() : "";
                String currText = msgs.get(i).getText() != null ? msgs.get(i).getText().trim() : "";
                if (!prevText.isEmpty() && prevText.length() > 15) {
                    String normPrev = prevText.toLowerCase().replaceAll("[^a-z0-9]", "");
                    String normCurr = currText.toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (normPrev.equals(normCurr)) {
                        score -= 0.5; // Repeated turn in conversation
                    }
                }
            }
        }

        // Signal 4: Character repetition & diversity penalty
        int len = text.length();
        int uniqueChars = (int) text.chars().distinct().count();
        double charDiversity = (double) uniqueChars / Math.min(len, 100);
        if (charDiversity < 0.15) {
            score -= 0.4;
        }

        // Signal 5: Word repetition penalty
        String[] words = text.split("\\s+");
        if (words.length > 10) {
            long uniqueWords = java.util.Arrays.stream(words).distinct().count();
            double wordRatio = (double) uniqueWords / words.length;
            if (wordRatio < 0.3) {
                score -= 0.3;
            }
        }

        boolean isCode = sample.getType() == eu.kalafatic.evolution.forge.data.api.TrainingSampleType.CODE;

        // Signal 6: Structural validity (extreme uppercase penalty for non-code text)
        if (!isCode) {
            long uppercaseCount = text.chars().filter(Character::isUpperCase).count();
            if (len > 20 && (double) uppercaseCount / len > 0.8) {
                score -= 0.3;
            }
        }

        // Signal 7: Pathological sequence or word repetition
        if (words.length > 5) {
            int maxConsecutiveMatches = 0;
            int currentMatches = 1;
            for (int i = 1; i < words.length; i++) {
                if (words[i].equalsIgnoreCase(words[i - 1])) {
                    currentMatches++;
                    if (currentMatches > maxConsecutiveMatches) {
                        maxConsecutiveMatches = currentMatches;
                    }
                } else {
                    currentMatches = 1;
                }
            }
            if (maxConsecutiveMatches >= 6) {
                score -= 0.4; // Heavy penalty for pathological repeating word loops
            }
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
