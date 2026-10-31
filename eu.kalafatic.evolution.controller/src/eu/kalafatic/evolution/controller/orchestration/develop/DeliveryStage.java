package eu.kalafatic.evolution.controller.orchestration.develop;

/**
 * Independent delivery stages for autonomous development tasks.
 */
public enum DeliveryStage {
    CREATED,
    IMPLEMENTATION_COMPLETE,
    BUILD_VERIFIED,
    TESTS_VERIFIED,
    COMMIT_CREATED,
    BRANCH_PUSHED,
    PULL_REQUEST_CREATED
}
