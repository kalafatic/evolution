package eu.kalafatic.evolution.controller.orchestration.dto;

import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import java.util.List;

public class RealityResult {
    private final boolean successful;
    private final double score;
    private final List<String> errors;
    private final List<String> warnings;
    private final RealityLevel level;

    public RealityResult(boolean successful, double score, List<String> errors, List<String> warnings, RealityLevel level) {
        this.successful = successful;
        this.score = score;
        this.errors = errors;
        this.warnings = warnings;
        this.level = level;
    }

    public boolean isSuccessful() { return successful; }
    public double getScore() { return score; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public RealityLevel getLevel() { return level; }
}
