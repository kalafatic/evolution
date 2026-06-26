package eu.kalafatic.evolution.controller.orchestration;

import java.util.Objects;

/**
 * Holder for the current {@link SystemState}.
 * Enforces state immutability via {@link TransitionToken}.
 * Instance-based to prevent cross-test contamination.
 */
public class SystemStateHolder {
    private SystemState currentState = SystemState.INIT;

    public SystemStateHolder() {
    }

    public SystemState getState() {
        return currentState;
    }

    /**
     * Applies a state transition. Requires a valid {@link TransitionToken}.
     * Restricted to package-level authority.
     *
     * @param token The token proving authority to transition.
     * @param newState The target state.
     * @throws NullPointerException if token or newState is null.
     */
    public void applyTransition(TransitionToken token, SystemState newState) {
        Objects.requireNonNull(token, "TransitionToken is mandatory for state changes");
        Objects.requireNonNull(newState, "Target SystemState cannot be null");
        currentState = newState;
    }
}
