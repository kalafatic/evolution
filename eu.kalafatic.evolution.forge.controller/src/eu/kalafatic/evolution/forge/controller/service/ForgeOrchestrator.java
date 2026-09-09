package eu.kalafatic.evolution.forge.controller.service;

import java.nio.file.Path;

import eu.kalafatic.evolution.forge.controller.api.ForgeJob;

public interface ForgeOrchestrator {

    /**
     * Executes the complete single authoritative Forge workflow for a ForgeJob.
     * Workflow:
     *   1. Source Selection & Analysis (ANALYZING)
     *   2. Dataset Composition & Preparation (PREPARING)
     *   3. Preflight Validation (READY_TO_TRAIN)
     *   4. Training Phase (TRAINING)
     *   5. Evaluation Phase (EVALUATING)
     *   6. Finalizing & Persistence (FINALIZING)
     *   7. Model Validation & Native Inference Smoke Test (VALIDATING)
     *   8. Completed Status (COMPLETED)
     */
    ForgeJob executeJob(ForgeJob job, Path projectPath) throws Exception;
}
