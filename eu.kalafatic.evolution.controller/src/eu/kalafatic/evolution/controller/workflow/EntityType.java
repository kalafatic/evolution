package eu.kalafatic.evolution.controller.workflow;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

public enum EntityType {
    USER,
    LOCAL_LLM,
    REMOTE_LLM,
    MEDIATED_FLOW,
    SELF_DEV_TASK,
    ZIP_EXPORT,
    PROMPT_FILE,
    SUPERVISOR,
    GIT_BRANCH,
    EVOLUTION_LOOP,
    PATCH,
    RCP_HOST,
    DEPLOYMENT_TARGET,
    ITERATION_RESULT,
    DARWIN_VARIANT,
    BEST_BRANCH,
    TASK_STATUS,
    HUMAN_STEP
}
