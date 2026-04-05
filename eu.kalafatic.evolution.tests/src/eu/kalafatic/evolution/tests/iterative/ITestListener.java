package eu.kalafatic.evolution.tests.iterative;

/**
 * Listener interface for monitoring the progress of iterative development tests.
 */
public interface ITestListener {
    /**
     * Called when a step starts.
     * @param step the name of the step (e.g., "prompt", "plan", etc.)
     */
    void stepStarted(String step);

    /**
     * Called when a step completes successfully.
     * @param step the name of the step
     */
    void stepSuccess(String step);

    /**
     * Called when a step fails.
     * @param step the name of the step
     */
    void stepFailed(String step);

    /**
     * Called when a step is skipped.
     * @param step the name of the step
     */
    void stepSkipped(String step);

    /**
     * Called when a transition between nodes is active.
     * @param edgeId the ID of the edge being traversed
     */
    void transitionActive(String edgeId);

    /**
     * Called to reset all nodes and edges to default state.
     */
    void reset();
}
