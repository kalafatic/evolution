package eu.kalafatic.evolution.controller.orchestration.dream;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Minimal coordinator for WAKE / DREAM lifecycle operating states, elastic branch capacity,
 * and startup crash-recovery persistence.
 */
public class WakeDreamCoordinator {

    private static final Logger LOGGER = Logger.getLogger(WakeDreamCoordinator.class.getName());
    private static final WakeDreamCoordinator INSTANCE = new WakeDreamCoordinator();

    private final AtomicReference<WakeDreamState> currentState = new AtomicReference<>(WakeDreamState.WAKE_IDLE);
    private final AtomicInteger maxAllowedBranches = new AtomicInteger(1);
    private final AtomicInteger activeBranches = new AtomicInteger(0);

    private volatile boolean dreamEnabled = false;
    private volatile String blockedReason = "DREAM mode disabled by default";
    private volatile long activeForegroundRequests = 0;

    private WakeDreamCoordinator() {
        String sysProp = System.getProperty("evo.dream.enabled", "false");
        this.dreamEnabled = Boolean.parseBoolean(sysProp);
        if (dreamEnabled) {
            this.blockedReason = "No active DREAM cycle";
        }
        recoverPersistedStateOnStartup();
    }

    public static WakeDreamCoordinator getInstance() {
        return INSTANCE;
    }

    private void recoverPersistedStateOnStartup() {
        DreamCheckpoint cp = DreamCheckpointStore.loadCheckpoint();
        if (cp != null && !"COMPLETE".equalsIgnoreCase(cp.getStage())) {
            LOGGER.info("[WAKE_DREAM] Startup recovery: Found interrupted DREAM checkpoint [" + cp.getCycleId() + "] at stage " + cp.getStage());
            this.blockedReason = "Interrupted checkpoint available: " + cp.getCycleId() + " (" + cp.getStage() + ")";
        }
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
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public int getMaxAllowedBranches() {
        return maxAllowedBranches.get();
    }

    public void setMaxAllowedBranches(int limit) {
        this.maxAllowedBranches.set(Math.max(1, limit));
    }

    public int getActiveBranches() {
        return activeBranches.get();
    }

    public boolean canSpawnBranch() {
        return activeBranches.get() < maxAllowedBranches.get() && evaluateDreamEligibility();
    }

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
    }

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
    }

    public synchronized boolean evaluateDreamEligibility() {
        return evaluateDreamEligibility(false);
    }

    public synchronized boolean evaluateDreamEligibility(boolean isManual) {
        if (!dreamEnabled && !isManual) {
            blockedReason = "DREAM disabled by configuration";
            return false;
        }
        if (!isManual && (activeForegroundRequests > 0 || currentState.get() == WakeDreamState.WAKE_ACTIVE)) {
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

    public synchronized boolean requestDreamStart(String cycleId) {
        return requestDreamStart(cycleId, false);
    }

    public synchronized boolean requestDreamStart(String cycleId, boolean isManual) {
        if (!evaluateDreamEligibility(isManual)) {
            LOGGER.info("[WAKE_DREAM] DREAM start rejected for " + cycleId + " (manual=" + isManual + "): " + blockedReason);
            return false;
        }

        currentState.set(WakeDreamState.DREAM_ACTIVE);
        activeBranches.set(1);
        blockedReason = "DREAM cycle active (" + cycleId + ")";
        LOGGER.info("[WAKE_DREAM] DREAM_START: " + cycleId + " (manual=" + isManual + ")");
        return true;
    }

    public synchronized void requestDreamPause(String reason) {
        WakeDreamState state = currentState.get();
        if (state == WakeDreamState.DREAM_ACTIVE) {
            currentState.set(WakeDreamState.DREAM_PAUSING);
            blockedReason = "Pause requested: " + reason;
            LOGGER.info("[WAKE_DREAM] DREAM_PAUSE_REQUESTED: " + reason);
        }
    }

    public synchronized void confirmDreamPaused(String cycleId) {
        currentState.set(WakeDreamState.DREAM_PAUSED);
        activeBranches.set(0);
        blockedReason = "DREAM paused safely (" + cycleId + ")";
        LOGGER.info("[WAKE_DREAM] DREAM_PAUSED: " + cycleId);
    }

    public synchronized void notifyDreamComplete(String cycleId, boolean success) {
        currentState.set(success ? WakeDreamState.WAKE_IDLE : WakeDreamState.DREAM_FAILED);
        activeBranches.set(0);
        blockedReason = success ? "Cycle completed successfully" : "Cycle failed";
        LOGGER.info("[WAKE_DREAM] DREAM_COMPLETE: " + cycleId + " (Success=" + success + ")");
        if (success) {
            DreamCheckpointStore.clearCheckpoint();
        }
    }

    public synchronized void resetState() {
        currentState.set(WakeDreamState.WAKE_IDLE);
        activeForegroundRequests = 0;
        activeBranches.set(0);
        blockedReason = dreamEnabled ? "Idle" : "DREAM mode disabled by configuration";
    }
}
