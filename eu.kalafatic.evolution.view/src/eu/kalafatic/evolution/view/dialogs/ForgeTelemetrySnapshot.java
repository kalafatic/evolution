package eu.kalafatic.evolution.view.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.MetricSample;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.ForgeTrainingEstimationAgent;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.TrainingEtaEstimator;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.TrainingEtaEstimator.EtaEstimate;
import eu.kalafatic.evolution.forge.controller.service.impl.agents.TrainingEtaEstimator.Sample;

/**
 * Immutable, thread-safe view of the authoritative FORGE training state for a
 * single session, plus honest derived calculations (progress, ETA, budget
 * consumption, estimation quality).
 *
 * Data policy:
 * - Every metric originates from ForgeSessionManager.TrainingTelemetry (the
 *   single authoritative telemetry source) or from the session uiState
 *   configuration. Nothing is fabricated.
 * - Unavailable metrics are represented explicitly (N/A) and never invented.
 * - ETA prefers real measured throughput (live EMA or persisted measurement
 *   history) and only falls back to a clearly-labeled ANALYTICAL PRELIMINARY
 *   estimate when no measurement exists.
 */
public final class ForgeTelemetrySnapshot {

    public enum BudgetState {
        ON_TRACK, AT_RISK, EXCEEDS_BUDGET, COMPLETED, UNKNOWN
    }

    public enum ThroughputBasis {
        MEASURED_LIVE, MEASURED_HISTORY, ANALYTICAL_PRELIMINARY, UNAVAILABLE
    }

    public static final class HistoryPoint {
        public final long timestamp;
        public final int epoch;
        public final int step;
        public final double loss;
        public final double valLoss;
        public final double tokensPerSec;
        public final double stepsPerSec;
        public final double ramMb;
        public final double cpuPct;
        public final double learningRate;

        public HistoryPoint(long timestamp, int epoch, int step, double loss, double valLoss,
                            double tokensPerSec, double stepsPerSec, double ramMb, double cpuPct, double learningRate) {
            this.timestamp = timestamp;
            this.epoch = epoch;
            this.step = step;
            this.loss = loss;
            this.valLoss = valLoss;
            this.tokensPerSec = tokensPerSec;
            this.stepsPerSec = stepsPerSec;
            this.ramMb = ramMb;
            this.cpuPct = cpuPct;
            this.learningRate = learningRate;
        }
    }

    // --- Session identity ---
    public final String sessionId;
    public final String sessionName;

    // --- Authoritative status (telemetry) ---
    public final String status;
    public final String workflowStatus;

    // --- Timing ---
    public final long startTimeMs;
    public final long durationMs;
    public final long capturedAtMs;

    // --- Epoch / step / token counters ---
    public final int currentEpoch;
    public final int totalEpochs;
    public final int currentStep;
    public final int totalSteps;
    public final int currentSample;
    public final int totalSamples;
    public final long processedTokens;
    public final long totalTokens;

    // --- Losses ---
    public final double currentLoss;
    public final double valLoss;
    public final double bestValLoss;
    public final double initialLoss;
    public final double learningRate;

    // --- Throughput ---
    public final double tokensPerSec;
    public final double stepsPerSec;
    public final double trainerEstimatedRemainingSeconds;

    // --- Hardware ---
    public final double ramUsedMb;
    public final double ramTotalMb;
    public final double cpuPercent;
    public final String gpuStatus;

    // --- Diagnostic ---
    public final String lastEventMessage;
    public final List<String> logTail;

    // --- Timestamped history (copied; never shared with live telemetry) ---
    public final List<HistoryPoint> history;

    // --- Configured values (session uiState) ---
    public final String configuredModelSize;
    public final int configuredEpochs;
    public final double configuredDesiredLoss;
    public final double configuredTimeBudgetHours;
    public final boolean configuredEarlyStopping;

    // --- Historical (persisted config) loss series, only used when no
    // telemetry history exists yet; labeled as such in the UI ---
    public final double[] legacyLossHistory;

    // --- Derived (honest) values ---
    public final double overallProgress;      // 0..1, or -1 when unavailable
    public final double epochProgress;        // 0..1 within current epoch (step-based), or -1
    public final double elapsedSeconds;
    public final double effectiveTokensPerSec; // resolved throughput, or -1
    public final ThroughputBasis throughputBasis;
    public final double estimatedRemainingSeconds; // resolved ETA, or -1 (always -1 for terminal)
    public final double etaConfidence;          // 0..1
    public final String etaBasis;               // human-readable ETA basis
    public final double initialEstimateSeconds; // preliminary total estimate, or -1
    public final double estimatedTotalSeconds;  // terminal-aware total: elapsed for terminal, else elapsed+remaining
    public final double budgetConsumption;      // estTotal/budget, or -1
    public final BudgetState budgetState;
    public final boolean isTrainingActive;

    private ForgeTelemetrySnapshot(Builder b) {
        this.sessionId = b.sessionId;
        this.sessionName = b.sessionName;
        this.status = b.status;
        this.workflowStatus = b.workflowStatus;
        this.startTimeMs = b.startTimeMs;
        this.durationMs = b.durationMs;
        this.capturedAtMs = b.capturedAtMs;
        this.currentEpoch = b.currentEpoch;
        this.totalEpochs = b.totalEpochs;
        this.currentStep = b.currentStep;
        this.totalSteps = b.totalSteps;
        this.currentSample = b.currentSample;
        this.totalSamples = b.totalSamples;
        this.processedTokens = b.processedTokens;
        this.totalTokens = b.totalTokens;
        this.currentLoss = b.currentLoss;
        this.valLoss = b.valLoss;
        this.bestValLoss = b.bestValLoss;
        this.initialLoss = b.initialLoss;
        this.learningRate = b.learningRate;
        this.tokensPerSec = b.tokensPerSec;
        this.stepsPerSec = b.stepsPerSec;
        this.trainerEstimatedRemainingSeconds = b.trainerEstimatedRemainingSeconds;
        this.ramUsedMb = b.ramUsedMb;
        this.ramTotalMb = b.ramTotalMb;
        this.cpuPercent = b.cpuPercent;
        this.gpuStatus = b.gpuStatus;
        this.lastEventMessage = b.lastEventMessage;
        this.logTail = b.logTail;
        this.history = b.history;
        this.configuredModelSize = b.configuredModelSize;
        this.configuredEpochs = b.configuredEpochs;
        this.configuredDesiredLoss = b.configuredDesiredLoss;
        this.configuredTimeBudgetHours = b.configuredTimeBudgetHours;
        this.configuredEarlyStopping = b.configuredEarlyStopping;
        this.legacyLossHistory = b.legacyLossHistory;

        // --- Derived values ---
        this.elapsedSeconds = computeElapsedSeconds(b);

        double overall = -1.0;
        if (b.totalSteps > 0) {
            overall = Math.min(1.0, Math.max(0.0, (double) b.currentStep / (double) b.totalSteps));
        } else if (b.totalTokens > 0) {
            overall = Math.min(1.0, Math.max(0.0, (double) b.processedTokens / (double) b.totalTokens));
        } else if (isTerminal(b.status)) {
            overall = 1.0;
        }
        this.overallProgress = overall;

        double epProgress = -1.0;
        if (b.totalEpochs > 0 && b.totalSteps > 0) {
            int stepsPerEpoch = Math.max(1, b.totalSteps / Math.max(1, b.totalEpochs));
            int stepInEpoch = b.currentStep % stepsPerEpoch;
            epProgress = Math.min(1.0, Math.max(0.0, (double) stepInEpoch / stepsPerEpoch));
        } else if (b.totalEpochs > 0 && b.currentEpoch >= b.totalEpochs) {
            epProgress = 1.0;
        }
        this.epochProgress = epProgress;

        this.isTrainingActive = "RUNNING".equals(b.status) || "TRAINING".equals(b.status);

        // --- Throughput resolution ---
        double effTps;
        ThroughputBasis basis;
        if (isTrainingActive && b.tokensPerSec > 0.0) {
            effTps = b.tokensPerSec;
            basis = ThroughputBasis.MEASURED_LIVE;
        } else {
            double measured = ForgeTrainingEstimationAgent.getInstance().getMeasuredTokensPerSecond(b.configuredModelSize);
            if (measured > 0.0) {
                effTps = measured;
                basis = ThroughputBasis.MEASURED_HISTORY;
            } else {
                effTps = analyticalTokensPerSec(b.configuredModelSize);
                basis = effTps > 0.0 ? ThroughputBasis.ANALYTICAL_PRELIMINARY : ThroughputBasis.UNAVAILABLE;
            }
        }
        this.effectiveTokensPerSec = effTps;
        this.throughputBasis = basis;

        // --- ETA resolution: measured > history > analytical ---
        EtaEstimate measuredEta = computeMeasuredEta(b);
        double resolvedEta;
        double confidence;
        String etaBasis;
        if (measuredEta != null && measuredEta.isAvailable()) {
            resolvedEta = measuredEta.remainingSeconds;
            confidence = measuredEta.confidence;
            etaBasis = measuredEta.basis;
        } else if (basis == ThroughputBasis.MEASURED_HISTORY || basis == ThroughputBasis.MEASURED_LIVE) {
            long remainingTokens = Math.max(0, b.totalTokens - b.processedTokens);
            if (remainingTokens > 0 && effTps > 0.0) {
                resolvedEta = remainingTokens / effTps;
                etaBasis = "measured-throughput";
            } else {
                resolvedEta = -1.0;
                etaBasis = "no-remaining-work";
            }
            confidence = ForgeTrainingEstimationAgent.getInstance().getThroughputConfidence(b.configuredModelSize);
        } else {
            resolvedEta = -1.0;
            confidence = 0.0;
            etaBasis = "no-measurements";
        }
        this.estimatedRemainingSeconds = isTerminal(b.status) ? -1.0 : resolvedEta;
        this.etaConfidence = confidence;
        this.etaBasis = etaBasis;

        // --- Initial (preliminary) total estimate ---
        this.initialEstimateSeconds = computeInitialEstimateSeconds(b);

        // --- Terminal-aware total duration: a completed/cancelled/failed run has no
        // remaining time and its total is its actual elapsed duration, never a stale
        // projected estimate. ---
        this.estimatedTotalSeconds = estimateTotalSeconds(this.elapsedSeconds, this.estimatedRemainingSeconds,
                this.initialEstimateSeconds, b.status);

        // --- Budget consumption ---
        double consumption = -1.0;
        BudgetState budgetState = BudgetState.UNKNOWN;
        double budgetSec = b.configuredTimeBudgetHours > 0 ? b.configuredTimeBudgetHours * 3600.0 : -1.0;
        double estTotal = this.estimatedTotalSeconds;
        if (budgetSec > 0.0 && estTotal > 0.0) {
            consumption = estTotal / budgetSec;
            if (isTerminal(b.status)) {
                budgetState = BudgetState.COMPLETED;
            } else if (consumption <= 1.0) {
                budgetState = BudgetState.ON_TRACK;
            } else if (consumption <= 1.15) {
                budgetState = BudgetState.AT_RISK;
            } else {
                budgetState = BudgetState.EXCEEDS_BUDGET;
            }
        } else if (isTerminal(b.status)) {
            budgetState = BudgetState.COMPLETED;
        }
        this.budgetConsumption = consumption;
        this.budgetState = budgetState;
    }

    private static double estimateTotalSeconds(double elapsed, double remaining, double initialEstimate, String status) {
        if (isTerminal(status)) return elapsed;
        if (remaining >= 0.0) return elapsed + remaining;
        if (initialEstimate > 0.0) return Math.max(elapsed, initialEstimate);
        return -1.0;
    }

    private static double computeElapsedSeconds(Builder b) {
        if (b.startTimeMs > 0) {
            long end = b.durationMs > 0 ? b.startTimeMs + b.durationMs : b.capturedAtMs;
            return Math.max(0.0, (end - b.startTimeMs) / 1000.0);
        }
        return 0.0;
    }

    private static boolean isTerminal(String status) {
        if (status == null) return false;
        String s = status.toUpperCase();
        return "COMPLETED".equals(s) || "STOPPED".equals(s) || "FAILED".equals(s)
                || "ABORTED".equals(s) || "CANCELLED".equals(s) || "INTERRUPTED".equals(s);
    }

    private static EtaEstimate computeMeasuredEta(Builder b) {
        if (b.history == null || b.history.isEmpty()) return null;
        // Token counts per sample are not stored in history; derive monotonic
        // token positions from step progress for the token-based ETA path.
        List<Sample> samples = new ArrayList<>();
        long prevTokens = 0;
        for (HistoryPoint p : b.history) {
            long tokens;
            if (b.totalTokens > 0 && b.totalSteps > 0) {
                tokens = (long) ((double) p.step / b.totalSteps * b.totalTokens);
            } else {
                tokens = prevTokens;
            }
            tokens = Math.max(tokens, prevTokens);
            prevTokens = tokens;
            samples.add(new Sample(p.timestamp, tokens, p.step, b.totalTokens, b.totalSteps));
        }
        return TrainingEtaEstimator.compute(samples, b.processedTokens, b.currentStep, b.totalTokens, b.totalSteps, b.capturedAtMs);
    }

    private static double analyticalTokensPerSec(String modelSize) {
        return ForgeTrainingEstimationAgent.getInstance().getBaselineTokensPerSecond(resolveParamCount(modelSize));
    }

    /** Authoritative parameter count: ModelSizePreset (architecture-derived). */
    public static double resolveParamCount(String modelSize) {
        if (modelSize != null && !modelSize.trim().isEmpty()) {
            for (ModelSizePreset.Size size : ModelSizePreset.Size.values()) {
                if (size.name().equalsIgnoreCase(modelSize) || size.getDisplayName().equalsIgnoreCase(modelSize)) {
                    return size.getParameterCount() * 1_000_000.0;
                }
            }
        }
        return ForgeTrainingEstimationAgent.getInstance().getModelParameterCount(modelSize);
    }

    private static double computeInitialEstimateSeconds(Builder b) {
        if (b.totalDatasetBytes <= 0 || b.configuredEpochs <= 0) return -1.0;
        double params = resolveParamCount(b.configuredModelSize);
        return ForgeTrainingEstimationAgent.getInstance()
                .calculateEstimatedForgingSecondsWithParams(params, b.configuredEpochs, b.totalDatasetBytes);
    }

    /** Total checked dataset bytes used for the preliminary estimate. */
    public static final class Builder {
        public String sessionId = "";
        public String sessionName = "";
        public String status = "UNKNOWN";
        public String workflowStatus = "";
        public long startTimeMs = 0;
        public long durationMs = 0;
        public long capturedAtMs = System.currentTimeMillis();
        public int currentEpoch = 0;
        public int totalEpochs = 0;
        public int currentStep = 0;
        public int totalSteps = 0;
        public int currentSample = 0;
        public int totalSamples = 0;
        public long processedTokens = 0;
        public long totalTokens = 0;
        public double currentLoss = 0.0;
        public double valLoss = 0.0;
        public double bestValLoss = Double.MAX_VALUE;
        public double initialLoss = -1.0;
        public double learningRate = -1.0;
        public double tokensPerSec = 0.0;
        public double stepsPerSec = 0.0;
        public double trainerEstimatedRemainingSeconds = 0.0;
        public double ramUsedMb = 0.0;
        public double ramTotalMb = 0.0;
        public double cpuPercent = 0.0;
        public String gpuStatus = "N/A (CPU Mode)";
        public String lastEventMessage = "";
        public List<String> logTail = Collections.emptyList();
        public List<HistoryPoint> history = new ArrayList<>();
        public String configuredModelSize = "SMALL";
        public int configuredEpochs = 4;
        public double configuredDesiredLoss = 1.0;
        public double configuredTimeBudgetHours = -1.0;
        public boolean configuredEarlyStopping = false;
        public double[] legacyLossHistory = null;
        public long totalDatasetBytes = 0;

        public ForgeTelemetrySnapshot build() {
            if (bestValLoss == Double.MAX_VALUE) bestValLoss = 0.0;
            if (initialLoss < 0.0 && !history.isEmpty()) {
                initialLoss = history.get(0).loss;
            }
            return new ForgeTelemetrySnapshot(this);
        }
    }

    /**
     * Captures the authoritative state for the given session from
     * ForgeSessionManager. Never throws; unavailable state degrades to an
     * honest UNKNOWN snapshot.
     */
    public static ForgeTelemetrySnapshot capture(ForgeSessionManager mgr, String sessionId,
                                                 double[] legacyLossHistory, long totalDatasetBytes) {
        Builder b = new Builder();
        b.legacyLossHistory = legacyLossHistory;
        b.totalDatasetBytes = totalDatasetBytes;
        if (mgr == null) return b.build();

        try {
            String resolvedId = sessionId;
            if (resolvedId == null || resolvedId.trim().isEmpty()) {
                resolvedId = mgr.getActiveTrainingSessionId();
            }
            b.sessionId = resolvedId != null ? resolvedId : "";

            JSONObject uiState = (resolvedId != null && !resolvedId.isEmpty()) ? mgr.getUiState(resolvedId) : new JSONObject();
            b.configuredModelSize = uiState.optString("modelSize", "SMALL");
            b.configuredEpochs = uiState.optInt("epochs", 4);
            b.configuredDesiredLoss = uiState.optDouble("desiredLossThreshold", 1.0);
            b.configuredTimeBudgetHours = uiState.optDouble("timeBudgetHours", -1.0);
            b.configuredEarlyStopping = uiState.optBoolean("earlyStoppingEnabled", false);
            b.workflowStatus = uiState.optString("forge.workflow.status", "");

            if (resolvedId != null && !resolvedId.isEmpty()) {
                TrainingTelemetry t = mgr.getTelemetry(resolvedId);
                if (t != null) {
                    b.status = t.status != null ? t.status : "UNKNOWN";
                    b.startTimeMs = t.startTimeMs;
                    b.durationMs = t.durationMs;
                    b.currentEpoch = t.currentEpoch;
                    b.totalEpochs = t.totalEpochs;
                    b.currentStep = t.currentStep;
                    b.totalSteps = t.totalSteps;
                    b.currentSample = t.currentSample;
                    b.totalSamples = t.totalSamples;
                    b.processedTokens = t.processedTokens;
                    b.totalTokens = t.totalTokens;
                    b.currentLoss = t.currentLoss;
                    b.valLoss = t.valLoss;
                    b.bestValLoss = t.bestValLoss == Double.MAX_VALUE ? 0.0 : t.bestValLoss;
                    b.learningRate = t.learningRate;
                    b.tokensPerSec = t.tokensPerSec;
                    b.stepsPerSec = t.stepsPerSec;
                    b.trainerEstimatedRemainingSeconds = t.estimatedRemainingSeconds;
                    b.ramUsedMb = t.ramUsedMb;
                    b.ramTotalMb = t.ramTotalMb;
                    b.cpuPercent = t.cpuPercent;
                    b.gpuStatus = t.gpuStatus != null ? t.gpuStatus : "N/A (CPU Mode)";
                    b.lastEventMessage = t.lastEventMessage != null ? t.lastEventMessage : "";

                    List<HistoryPoint> hist = new ArrayList<>();
                    synchronized (t.history) {
                        for (MetricSample s : t.history) {
                            hist.add(new HistoryPoint(s.timestamp, s.epoch, s.step, s.loss, s.valLoss,
                                    s.tokensPerSec, s.stepsPerSec, s.ramMb, s.cpuPct, s.learningRate));
                        }
                    }
                    b.history = hist;

                    List<String> logs = new ArrayList<>();
                    synchronized (t.logStream) {
                        int start = Math.max(0, t.logStream.size() - 12);
                        for (int i = start; i < t.logStream.size(); i++) {
                            logs.add(t.logStream.get(i).level + " " + t.logStream.get(i).message);
                        }
                    }
                    b.logTail = logs;

                    eu.kalafatic.evolution.model.orchestration.ForgeSession s = mgr.findSessionById(resolvedId);
                    if (s == null) s = mgr.findSession(resolvedId);
                    if (s != null) b.sessionName = s.getName() != null ? s.getName() : "";
                }
            }
        } catch (Exception ignored) {
            // Telemetry unavailable: keep the honest UNKNOWN snapshot.
        }
        return b.build();
    }

    /** Captures using the dialog's configured values (config-only mode). */
    public static ForgeTelemetrySnapshot configOnly(String modelSize, int epochs, double desiredLoss,
                                                    double timeBudgetHours, boolean earlyStopping,
                                                    double[] legacyLossHistory, long totalDatasetBytes) {
        Builder b = new Builder();
        b.status = "IDLE";
        b.configuredModelSize = modelSize != null ? modelSize : "SMALL";
        b.configuredEpochs = Math.max(1, epochs);
        b.configuredDesiredLoss = desiredLoss > 0 ? desiredLoss : 1.0;
        b.configuredTimeBudgetHours = timeBudgetHours;
        b.configuredEarlyStopping = earlyStopping;
        b.legacyLossHistory = legacyLossHistory;
        b.totalDatasetBytes = totalDatasetBytes;
        return b.build();
    }

    /** Stable fingerprint used to avoid re-rendering unchanged state. */
    public String fingerprint() {
        int histSize = history.size();
        long lastTs = history.isEmpty() ? 0 : history.get(history.size() - 1).timestamp;
        return status + "|" + workflowStatus + "|" + currentEpoch + "|" + totalEpochs + "|" + currentStep + "|"
                + totalSteps + "|" + processedTokens + "|" + totalTokens + "|" + String.format("%.4f", currentLoss)
                + "|" + String.format("%.4f", valLoss) + "|" + String.format("%.2f", learningRate)
                + "|" + histSize + "|" + lastTs + "|" + String.format("%.1f", cpuPercent)
                + "|" + String.format("%.0f", ramUsedMb) + "|" + configuredModelSize + "|" + configuredEpochs
                + "|" + String.format("%.3f", configuredDesiredLoss) + "|" + String.format("%.1f", configuredTimeBudgetHours);
    }
}
