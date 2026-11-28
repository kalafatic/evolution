package eu.kalafatic.evolution.controller.manager.removal;

import java.nio.file.Path;

/**
 * Result of a single removal stage. A skipped stage is an explicit outcome,
 * not a failure and not a success for the stage's required effect.
 */
public record ModelRemovalStepResult(
        String stepName,
        Path targetPath,
        boolean success,
        String message,
        Exception exception,
        StepOutcome outcome
) {

    public enum StepOutcome {
        STARTED,
        SUCCESS,
        SKIPPED,
        FAILED
    }

    public ModelRemovalStepResult(String stepName, Path targetPath, boolean success,
                                  String message, Exception exception) {
        this(stepName, targetPath, success, message, exception,
                success ? StepOutcome.SUCCESS : StepOutcome.FAILED);
    }

    public static ModelRemovalStepResult success(String step, String message) {
        return new ModelRemovalStepResult(step, null, true, message, null, StepOutcome.SUCCESS);
    }

    public static ModelRemovalStepResult success(String step, Path path, String message) {
        return new ModelRemovalStepResult(step, path, true, message, null, StepOutcome.SUCCESS);
    }

    public static ModelRemovalStepResult skipped(String step, String message) {
        return new ModelRemovalStepResult(step, null, true, message, null, StepOutcome.SKIPPED);
    }

    public static ModelRemovalStepResult skipped(String step, Path path, String message) {
        return new ModelRemovalStepResult(step, path, true, message, null, StepOutcome.SKIPPED);
    }

    public static ModelRemovalStepResult failed(String step, String message, Exception exception) {
        return new ModelRemovalStepResult(step, null, false, message, exception, StepOutcome.FAILED);
    }

    public static ModelRemovalStepResult failed(String step, Path path, String message, Exception exception) {
        return new ModelRemovalStepResult(step, path, false, message, exception, StepOutcome.FAILED);
    }

    public StepOutcome outcome() {
        return outcome != null ? outcome : (success ? StepOutcome.SUCCESS : StepOutcome.FAILED);
    }
}
