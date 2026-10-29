package eu.kalafatic.evolution.controller.orchestration.adapter;

import java.util.List;

/**
 * Universal process adapter interface that normalizes different underlying engine processes
 * (e.g. Darwin Iterations, Forge Epochs, Workflow Steps) for UI pages like IterationPage,
 * ForgePage, and MultiPageEditor.
 */
public interface IProcessAdapter {

    /**
     * Identifies the primary process category (e.g. "DARWIN_ITERATION", "FORGE_TRAINING", "WORKFLOW_STEP").
     */
    String getProcessType();

    /**
     * Display name of the active process (e.g., "Darwin Evolution Loop", "Forge Model Training", "Self-Dev Workflow").
     */
    String getProcessName();

    /**
     * Session identifier associated with this process adapter.
     */
    String getSessionId();

    /**
     * Overall process status string (e.g., "READY", "RUNNING", "PAUSED", "COMPLETED", "FAILED").
     */
    String getProcessStatus();

    /**
     * Current active phase or step name.
     */
    String getCurrentPhase();

    /**
     * Progress percentage or fraction (0.0 to 1.0).
     */
    double getProgressFraction();

    /**
     * Label for unit selector in UI (e.g. "Select Iteration:", "Select Epoch:", "Select Step:").
     */
    String getUnitSelectorLabel();

    /**
     * Returns the list of adapted process units (Iterations, Epochs, or Steps) for selection.
     */
    List<ProcessUnitRecord> getUnits();

    /**
     * Returns flow step labels representing execution phases for the given unit (e.g., ["PLAN", "CODE", "TEST", "SCORE", "SELECT", "MERGE"]
     * or ["PREPARE", "BATCH", "FORWARD", "LOSS", "BACKWARD", "CHECKPOINT"]).
     */
    List<String> getFlowStepLabels();

    /**
     * Formatted string summary of the process state or metrics.
     */
    String getSummary();
}
