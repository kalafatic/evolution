package eu.kalafatic.evolution.controller.orchestration.dream;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Minimal coordinator for WAKE / DREAM lifecycle operating states and preemption policies.
 */
public class WakeDreamCoordinator {

    private static final Logger LOGGER = Logger.getLogger(WakeDreamCoordinator.class.getName());
    private static final WakeDreamCoordinator INSTANCE = new WakeDreamCoordinator();

    private final AtomicReference<WakeDreamState> currentState = new AtomicReference<>(WakeDreamState.WAKE_IDLE);

    private volatile boolean dreamEnabled = false;
    private volatile String blockedReason = "DREAM mode disabled by default";
    private volatile long activeForegroundRequests = 0;

    private WakeDreamCoordinator() {
        // DREAM is disabled by default unless explicitly enabled via system property
        String sysProp = System.getProperty("evo.dream.enabled", "false");
        this.dreamEnabled = Boolean.parseBoolean(sysProp);
        if (dreamEnabled) {
            this.blockedReason = "No active DREAM cycle";
        }
    }

    public static WakeDreamCoordinator getInstance() {
        return INSTANCE;
    }

    public WakeDreamState getCurrentState() {
        return currentState.get();
    }

    public boolean isDreamEnabled() {
        return dreamEnabled;
    }

    public void setDreamEnabled(boolean enabled) {
        this.dreamEnabled = enabled;
        if (!enabled) {
            this.blockedReason = "DREAM mode disabled by configuration";
            requestDreamPause("DREAM disabled by configuration");
        } else {
            this.blockedReason = "No active DREAM cycle";
        }
        publishStateChange("CONFIG_UPDATED");
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    /**
     * Called when a foreground user/task request begins.
     * Immediately transitions WAKE state to WAKE_ACTIVE and preempts any active DREAM work.
     */
    public synchronized void notifyWakeStart(String requestId) {
        activeForegroundRequests++;
        WakeDreamState previous = currentState.get();
        currentState.set(WakeDreamState.WAKE_ACTIVE);
        blockedReason = "Foreground activity active (" + requestId + ")";

        LOGGER.info("[WAKE_DREAM] WAKE_START: " + requestId + " (State: " + previous + " -> WAKE_ACTIVE)");

        if (previous == WakeDreamState.DREAM_ACTIVE || previous == WakeDreamState.DREAM_PENDING) {
            currentState.set(WakeDreamState.DREAM_PAUSING);
            LOGGER.info("[WAKE_DREAM] Preempting active DREAM cycle due to WAKE request: " + requestId);
        }

        publishStateChange("WAKE_START:" + requestId);
    }

    /**
     * Called when a foreground request finishes or fails.
     */
    public synchronized void notifyWakeEnd(String requestId) {
        if (activeForegroundRequests > 0) {
            activeForegroundRequests--;
        }

        if (activeForegroundRequests == 0) {
            if (currentState.get() == WakeDreamState.WAKE_ACTIVE || currentState.get() == WakeDreamState.DREAM_PAUSING) {
                currentState.set(WakeDreamState.WAKE_IDLE);
                blockedReason = "Idle - eligible for DREAM if enabled";
                LOGGER.info("[WAKE_DREAM] WAKE_END: " + requestId + " -> State: WAKE_IDLE");
            }
        }
        publishStateChange("WAKE_END:" + requestId);
    }

    /**
     * Evaluates whether DREAM work is eligible to start or resume.
     */
    public synchronized boolean evaluateDreamEligibility() {
        if (!dreamEnabled) {
            blockedReason = "DREAM disabled by configuration";
            return false;
        }
        if (activeForegroundRequests > 0 || currentState.get() == WakeDreamState.WAKE_ACTIVE) {
            blockedReason = "Active foreground request running";
            return false;
        }
        WakeDreamState state = currentState.get();
        if (state == WakeDreamState.DREAM_ACTIVE) {
            blockedReason = "DREAM cycle already active";
            return false;
        }
        if (state == WakeDreamState.DREAM_PAUSING) {
            blockedReason = "DREAM cycle pausing";
            return false;
        }

        blockedReason = "Eligible";
        return true;
    }

    /**
     * Attempts to transition to DREAM_ACTIVE if eligible.
     */
    public synchronized boolean requestDreamStart(String cycleId) {
        if (!evaluateDreamEligibility()) {
            LOGGER.info("[WAKE_DREAM] DREAM start rejected for " + cycleId + ": " + blockedReason);
            return false;
        }

        currentState.set(WakeDreamState.DREAM_ACTIVE);
        blockedReason = "DREAM cycle active (" + cycleId + ")";
        LOGGER.info("[WAKE_DREAM] DREAM_START: " + cycleId);
        publishStateChange("DREAM_START:" + cycleId);
        return true;
    }

    /**
     * Requests safe pause/suspension of the active DREAM operation.
     */
    public synchronized void requestDreamPause(String reason) {
        WakeDreamState state = currentState.get();
        if (state == WakeDreamState.DREAM_ACTIVE) {
            currentState.set(WakeDreamState.DREAM_PAUSING);
            blockedReason = "Pause requested: " + reason;
            LOGGER.info("[WAKE_DREAM] DREAM_PAUSE_REQUESTED: " + reason);
            publishStateChange("DREAM_PAUSE_REQUESTED");
        }
    }

    /**
     * Confirms that DREAM reached a safe pause point.
     */
    public synchronized void confirmDreamPaused(String cycleId) {
        currentState.set(WakeDreamState.DREAM_PAUSED);
        blockedReason = "DREAM paused safely (" + cycleId + ")";
        LOGGER.info("[WAKE_DREAM] DREAM_PAUSED: " + cycleId);
        publishStateChange("DREAM_PAUSED:" + cycleId);
    }

    /**
     * Marks completion of a DREAM cycle.
     */
    public synchronized void notifyDreamComplete(String cycleId, boolean success) {
        currentState.set(success ? WakeDreamState.WAKE_IDLE : WakeDreamState.DREAM_FAILED);
        blockedReason = success ? "Cycle completed successfully" : "Cycle failed";
        LOGGER.info("[WAKE_DREAM] DREAM_COMPLETE: " + cycleId + " (Success=" + success + ")");
        publishStateChange("DREAM_COMPLETE:" + cycleId + ":" + success);
    }

    public synchronized void resetState() {
        currentState.set(WakeDreamState.WAKE_IDLE);
        activeForegroundRequests = 0;
        blockedReason = dreamEnabled ? "Idle" : "DREAM mode disabled by configuration";
        publishStateChange("RESET");
    }

    private void publishStateChange(String detail) {
        LOGGER.info("[WAKE_DREAM] State: " + currentState.get() + " | Detail: " + detail);
    }
}
