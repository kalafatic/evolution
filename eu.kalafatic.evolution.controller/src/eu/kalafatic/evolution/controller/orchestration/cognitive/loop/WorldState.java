package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-scoped representation of the system's understanding of world state,
 * known facts, artifacts, goal progress, information gain, strategy history, and capability availability.
 */
public class WorldState {

    public static class StrategyAttemptRecord {
        private final CognitiveStrategy strategy;
        private final CognitiveObservation observation;
        private final CognitiveFailureType failureType;
        private final int iteration;
        private final long timestamp;

        public StrategyAttemptRecord(CognitiveStrategy strategy, CognitiveObservation observation, CognitiveFailureType failureType, int iteration) {
            this.strategy = strategy;
            this.observation = observation;
            this.failureType = failureType != null ? failureType : CognitiveFailureType.UNKNOWN_FAILURE;
            this.iteration = iteration;
            this.timestamp = System.currentTimeMillis();
        }

        public CognitiveStrategy getStrategy() { return strategy; }
        public CognitiveObservation getObservation() { return observation; }
        public CognitiveFailureType getFailureType() { return failureType; }
        public int getIteration() { return iteration; }
        public long getTimestamp() { return timestamp; }
    }

    private final String sessionId;
    private final Map<String, Object> facts = new ConcurrentHashMap<>();
    private final Map<String, Object> artifacts = new ConcurrentHashMap<>();
    private final List<CognitiveObservation> observations = Collections.synchronizedList(new ArrayList<>());
    private final List<CognitiveDecision> decisionHistory = Collections.synchronizedList(new ArrayList<>());
    private final List<StrategyAttemptRecord> attemptHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> retryCounts = new ConcurrentHashMap<>();
    private final Map<String, Boolean> executedActionsGuard = new ConcurrentHashMap<>();

    private double currentProgress = 0.0;
    private double informationGain = 0.0;
    private CognitiveStrategy activeStrategy;

    public WorldState(String sessionId) {
        this.sessionId = sessionId;
        this.activeStrategy = CognitiveStrategy.ofDefault("INITIAL_DEFAULT_STRATEGY");
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setFact(String key, Object value) {
        if (key != null && value != null) {
            facts.put(key, value);
        }
    }

    public Object getFact(String key) {
        return facts.get(key);
    }

    public Map<String, Object> getFacts() {
        return Collections.unmodifiableMap(facts);
    }

    public void addArtifact(String name, Object artifact) {
        if (name != null && artifact != null) {
            artifacts.put(name, artifact);
        }
    }

    public Map<String, Object> getArtifacts() {
        return Collections.unmodifiableMap(artifacts);
    }

    public void addObservation(CognitiveObservation obs) {
        if (obs != null) {
            observations.add(obs);
            if (obs.getStdout() != null && !obs.getStdout().isEmpty()) {
                informationGain += 0.1;
            }
        }
    }

    public List<CognitiveObservation> getObservations() {
        return Collections.unmodifiableList(new ArrayList<>(observations));
    }

    public void addDecision(CognitiveDecision decision) {
        if (decision != null) {
            decisionHistory.add(decision);
        }
    }

    public List<CognitiveDecision> getDecisionHistory() {
        return Collections.unmodifiableList(new ArrayList<>(decisionHistory));
    }

    public double getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(double currentProgress) {
        this.currentProgress = Math.max(0.0, Math.min(1.0, currentProgress));
    }

    public double getInformationGain() {
        return informationGain;
    }

    public CognitiveStrategy getActiveStrategy() {
        return activeStrategy;
    }

    public void setActiveStrategy(CognitiveStrategy activeStrategy) {
        this.activeStrategy = activeStrategy != null ? activeStrategy : CognitiveStrategy.ofDefault("DEFAULT_STRATEGY");
    }

    public void recordStrategyAttempt(CognitiveStrategy strategy, CognitiveObservation obs, int iteration) {
        if (strategy == null || obs == null) return;
        CognitiveFailureType failureType = obs.getFailureType();
        String sig = strategy.getSignature();
        StrategyAttemptRecord record = new StrategyAttemptRecord(strategy, obs, failureType, iteration);
        attemptHistory.add(record);

        if (failureType.isTransient()) {
            retryCounts.merge(sig, 1, Integer::sum);
        }
    }

    public List<StrategyAttemptRecord> getAttemptHistory() {
        return Collections.unmodifiableList(new ArrayList<>(attemptHistory));
    }

    public boolean isStrategyExhausted(CognitiveStrategy strategy) {
        return isStrategyExhausted(strategy, 2);
    }

    public boolean isStrategyExhausted(String signature) {
        return isStrategyExhausted(signature, 2);
    }

    public boolean isStrategyExhausted(CognitiveStrategy strategy, int maxRetries) {
        if (strategy == null) return false;
        return isStrategyExhausted(strategy.getSignature(), maxRetries);
    }

    public boolean isStrategyExhausted(String signature, int maxRetries) {
        if (signature == null) return false;
        String normalizedSig = signature.trim().toLowerCase();
        for (StrategyAttemptRecord rec : attemptHistory) {
            if (rec.getStrategy() != null) {
                String recSig = rec.getStrategy().getSignature();
                if (recSig.equals(normalizedSig) || recSig.contains(normalizedSig) || normalizedSig.contains(recSig)) {
                    CognitiveFailureType ft = rec.getFailureType();
                    if (ft != null && ft.isFailure()) {
                        if (!ft.isTransient()) {
                            return true;
                        } else if (getRetryCount(recSig) >= maxRetries) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public int getRetryCount(CognitiveStrategy strategy) {
        if (strategy == null) return 0;
        return getRetryCount(strategy.getSignature());
    }

    public int getRetryCount(String signature) {
        if (signature == null) return 0;
        return retryCounts.getOrDefault(signature.trim().toLowerCase(), 0);
    }

    public boolean isActionAlreadyExecuted(String actionKey) {
        if (actionKey == null) return false;
        return executedActionsGuard.putIfAbsent(actionKey, Boolean.TRUE) != null;
    }

    public boolean detectStagnation(int windowSize) {
        if (observations.size() < windowSize) {
            return false;
        }
        int count = observations.size();
        CognitiveObservation last = observations.get(count - 1);
        for (int i = count - 2; i >= count - windowSize; i--) {
            CognitiveObservation prev = observations.get(i);
            if (last.getActionName().equalsIgnoreCase(prev.getActionName()) &&
                !last.isSuccess() && !prev.isSuccess() &&
                last.getStructuredError() != null && last.getStructuredError().equals(prev.getStructuredError())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "WorldState{" +
                "sessionId='" + sessionId + '\'' +
                ", progress=" + String.format("%.2f", currentProgress) +
                ", infoGain=" + String.format("%.2f", informationGain) +
                ", activeStrategy='" + activeStrategy.getIdentifier() + '\'' +
                ", observationsCount=" + observations.size() +
                ", attemptsCount=" + attemptHistory.size() +
                '}';
    }
}
