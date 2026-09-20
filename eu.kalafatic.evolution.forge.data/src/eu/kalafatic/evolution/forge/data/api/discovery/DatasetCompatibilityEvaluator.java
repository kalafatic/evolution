package eu.kalafatic.evolution.forge.data.api.discovery;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic compatibility evaluator comparing remote dataset candidate metadata against a requested dataset search profile.
 */
public class DatasetCompatibilityEvaluator {

    public static class CompatibilityResult {
        private final int score;
        private final List<String> reasons;
        private final List<String> warnings;
        private final String status;

        public CompatibilityResult(int score, List<String> reasons, List<String> warnings, String status) {
            this.score = score;
            this.reasons = reasons;
            this.warnings = warnings;
            this.status = status;
        }

        public int getScore() { return score; }
        public List<String> getReasons() { return reasons; }
        public List<String> getWarnings() { return warnings; }
        public String getStatus() { return status; }
    }

    public CompatibilityResult evaluate(DatasetCandidate candidate, DatasetSearchRequest request) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (candidate == null || request == null) {
            return new CompatibilityResult(0, reasons, List.of("Invalid candidate or request profile"), "INCOMPATIBLE");
        }

        // 1. Task / Domain Match (Max 30 points)
        String reqTask = request.getTask().toUpperCase();
        String candTask = candidate.getTask().toUpperCase();
        String candDomain = candidate.getDomain().toUpperCase();

        if (reqTask.equalsIgnoreCase(candTask) || candDomain.contains(reqTask) || reqTask.contains(candDomain)) {
            score += 30;
            reasons.add("+ Exact task/domain match (" + reqTask + ")");
        } else if (isRelatedTask(reqTask, candTask, candidate.getTags())) {
            score += 20;
            reasons.add("+ Related domain/task (" + candTask + ")");
        } else {
            warnings.add("- Task mismatch: requested " + reqTask + ", found " + candTask);
        }

        // 2. Language Match (Max 20 points)
        String reqLang = request.getLanguage().toLowerCase();
        String candLang = candidate.getLanguage().toLowerCase();

        if (reqLang.equalsIgnoreCase(candLang) || reqLang.startsWith(candLang) || candLang.startsWith(reqLang)) {
            score += 20;
            reasons.add("+ Language match (" + reqLang + ")");
        } else {
            warnings.add("- Language mismatch: requested " + reqLang + ", candidate is " + candLang);
        }

        // 3. Format / Schema Match (Max 20 points)
        String reqFormat = request.getFormat().toUpperCase();
        String candFormat = candidate.getFormat().toUpperCase();

        if (reqFormat.equalsIgnoreCase(candFormat)) {
            score += 20;
            reasons.add("+ Format structure match (" + reqFormat + ")");
        } else if ("INSTRUCTION".equalsIgnoreCase(reqFormat) && ("CHAT".equalsIgnoreCase(candFormat) || "CONVERSATION".equalsIgnoreCase(candFormat))) {
            score += 15;
            reasons.add("+ Convertible format (" + candFormat + " -> " + reqFormat + ")");
        } else if ("GENERAL_TEXT".equalsIgnoreCase(reqFormat) || "TEXT".equalsIgnoreCase(reqFormat)) {
            score += 15;
            reasons.add("+ Compatible text content");
        } else {
            warnings.add("- Format difference: requested " + reqFormat + ", found " + candFormat);
        }

        // 4. Required Fields / Schema Match (Max 15 points)
        List<String> reqFields = request.getRequiredFields();
        List<String> candSchema = candidate.getSchema();

        if (!reqFields.isEmpty()) {
            int matchCount = 0;
            for (String field : reqFields) {
                if (containsIgnoreCase(candSchema, field)) {
                    matchCount++;
                }
            }
            if (matchCount == reqFields.size()) {
                score += 15;
                reasons.add("+ All required schema fields detected " + reqFields);
            } else if (matchCount > 0) {
                int pts = (15 * matchCount) / reqFields.size();
                score += pts;
                reasons.add("+ Partial schema fields detected (" + matchCount + "/" + reqFields.size() + ")");
                warnings.add("- Missing some schema fields");
            } else {
                warnings.add("- Required schema fields not verified");
            }
        } else {
            score += 15;
            reasons.add("+ Standard schema requirements satisfied");
        }

        // 5. Preferred Split Available (Max 10 points)
        String prefSplit = request.getPreferredSplit().toLowerCase();
        List<String> splits = candidate.getSplits();

        if (splits.isEmpty() || containsIgnoreCase(splits, prefSplit) || containsIgnoreCase(splits, "train")) {
            score += 10;
            reasons.add("+ Target split available (" + prefSplit + ")");
        } else {
            warnings.add("- Preferred split '" + prefSplit + "' not explicitly listed");
        }

        // 6. Size Threshold (Max 5 points)
        long targetBytes = request.getTargetSizeBytes();
        long candBytes = candidate.getSizeBytes();

        if (candBytes <= 0) {
            score += 3;
            reasons.add("+ Size metadata dynamically streamable");
        } else if (candBytes >= targetBytes) {
            score += 5;
            reasons.add("+ Sufficient size available (" + (candBytes / (1024 * 1024)) + " MB >= " + (targetBytes / (1024 * 1024)) + " MB target)");
        } else if (candBytes >= request.getMinimumSizeBytes() && candBytes > 0) {
            score += 3;
            reasons.add("+ Meets minimum size threshold");
            warnings.add("- Candidate size below full target size");
        } else {
            warnings.add("- Candidate dataset is smaller than requested target");
        }

        score = Math.min(100, Math.max(0, score));

        String status = "READY";
        if (candidate.getRepository().equalsIgnoreCase(request.getDatasetName())) {
            status = "EXACT_MATCH";
        } else if (candBytes > 0 && candBytes < (10 * 1024 * 1024L)) { // < 10MB
            status = "SMALL";
        } else if (!warnings.isEmpty() && score < 60) {
            status = "WARNING";
        }

        candidate.setCompatibilityScore(score);
        candidate.setCompatibilityReasons(reasons);
        candidate.setCompatibilityWarnings(warnings);
        candidate.setStatus(status);

        return new CompatibilityResult(score, reasons, warnings, status);
    }

    private boolean isRelatedTask(String reqTask, String candTask, List<String> tags) {
        if (reqTask.contains("CODE") && (candTask.contains("PYTHON") || candTask.contains("SOFTWARE") || containsIgnoreCase(tags, "code"))) return true;
        if (reqTask.contains("INSTRUCTION") && (candTask.contains("SFT") || candTask.contains("CHAT") || containsIgnoreCase(tags, "instruction"))) return true;
        if (reqTask.contains("REASONING") && (candTask.contains("MATH") || candTask.contains("LOGIC") || containsIgnoreCase(tags, "reasoning"))) return true;
        return false;
    }

    private boolean containsIgnoreCase(List<String> list, String target) {
        if (list == null || target == null) return false;
        for (String item : list) {
            if (item != null && item.equalsIgnoreCase(target)) return true;
        }
        return false;
    }
}
