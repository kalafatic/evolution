package eu.kalafatic.evolution.forge.model.target;

public class ForgeTargetValidator {

    public static void validateForForging(ForgeTarget target) throws IllegalArgumentException {
        if (target == null) {
            throw new IllegalArgumentException("Forge target cannot be null");
        }

        if (!target.isValid()) {
            throw new IllegalArgumentException("Cannot forge from target. Reason: " + target.getStatusMessage());
        }

        if (target.getType() == ForgeTargetType.EVO_MODEL || target.getType() == ForgeTargetType.EVO_WORKSPACE) {
            if (target.getArtifact() == null) {
                throw new IllegalArgumentException("Cannot forge from EVO target: Model artifact is null or missing");
            }

            if (target.getVocabSize() <= 0) {
                throw new IllegalArgumentException("Cannot forge from EVO target: Vocabulary size is invalid (<= 0)");
            }

            if (target.getParameterCount() <= 0) {
                throw new IllegalArgumentException("Cannot forge from EVO target: Model parameter count is 0");
            }
        }
    }
}
