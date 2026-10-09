package eu.kalafatic.evolution.controller.manager.removal;

import java.nio.file.Path;

public record ModelRemovalStepResult(
        String stepName,
        Path targetPath,
        boolean success,
        String message,
        Exception exception
) {}
