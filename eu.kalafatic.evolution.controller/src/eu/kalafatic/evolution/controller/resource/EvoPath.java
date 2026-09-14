package eu.kalafatic.evolution.controller.resource;

/**
 * Enumeration of unambiguous semantic path concepts used across the EVO platform.
 */
public enum EvoPath {
    /** Canonical EVO source repository (${user.home}/git/evolution) */
    EVO_SOURCE_REPOSITORY,

    /** EVO self-development repository (${user.home}/git/evo) */
    EVO_SELFDEV_REPOSITORY,

    /** LLM / Forge self-development repository (${user.home}/git/llm) */
    LLM_SELFDEV_REPOSITORY,

    /** Active Eclipse workspace sandbox */
    ECLIPSE_WORKSPACE,

    /** Deployed/running application data root */
    EVO_RUNTIME_ROOT,

    /** Self-development run root (${WORKSPACE}/self-dev-run) */
    SELF_DEV_ROOT,

    /** Self-development source folder (${WORKSPACE}/self-dev-run/<run>/source) */
    SELF_DEV_SOURCE,

    /** Self-development build folder (${WORKSPACE}/self-dev-run/<run>/build) */
    SELF_DEV_BUILD,

    /** Self-development export folder (${WORKSPACE}/self-dev-run/<run>/export) */
    SELF_DEV_EXPORT,

    /** Self-development runtime folder (${WORKSPACE}/self-dev-run/<run>/runtime) */
    SELF_DEV_RUNTIME,

    /** Self-development logs folder (${WORKSPACE}/self-dev-run/<run>/logs) */
    SELF_DEV_LOGS,

    /** Supervisor source repository directory */
    SUPERVISOR_SOURCE,

    /** Supervisor runtime directory */
    SUPERVISOR_RUNTIME,

    /** EVO Genome module directory */
    GENOME,

    /** Forge input dataset directory (${RUNTIME_ROOT}/forge-input) */
    FORGE_INPUT,

    /** Forge output model directory (${RUNTIME_ROOT}/forge-output) */
    FORGE_OUTPUT,

    /** LLM models storage directory */
    MODELS,

    /** Datasets storage directory */
    DATASETS,

    // Backward compatibility aliases
    EVO_ROOT,
    WORKSPACE,
    PROJECT_ROOT,
    SOURCE_ROOT,
    RUNTIME_ROOT,
    BUILD_ROOT,
    EXPORT_ROOT
}
