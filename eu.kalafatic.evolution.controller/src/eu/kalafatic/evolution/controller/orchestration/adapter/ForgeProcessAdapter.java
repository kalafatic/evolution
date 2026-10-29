package eu.kalafatic.evolution.controller.orchestration.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker;
import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker.ProcessInfo;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Adapter for Forge LLM Training process (Epochs, Training Steps, Loss Metrics, Validation).
 * Normalizes training epochs/steps into ProcessUnitRecord objects for UI tab views.
 */
public class ForgeProcessAdapter implements IProcessAdapter {

    private final String sessionId;
    private final Orchestrator orchestrator;

    public ForgeProcessAdapter(String sessionId, Orchestrator orchestrator) {
        this.sessionId = sessionId != null ? sessionId : "Default";
        this.orchestrator = orchestrator;
    }

    @Override
    public String getProcessType() {
        return "FORGE_TRAINING";
    }

    @Override
    public String getProcessName() {
        return "Forge LLM Model Training";
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getProcessStatus() {
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sessionId);
        if (proc != null) {
            return proc.getState();
        }
        ForgeSession s = ForgeSessionManager.getInstance().findSession(sessionId);
        if (s != null && s.getStatus() != null) {
            return s.getStatus().getName();
        }
        return "READY";
    }

    @Override
    public String getCurrentPhase() {
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sessionId);
        if (proc != null) {
            return proc.getPhase();
        }
        ForgeSession s = ForgeSessionManager.getInstance().findSession(sessionId);
        if (s != null) {
            return s.getStatus() == ForgeStatus.TRAINING ? "TRAINING_EPOCH" : "IDLE";
        }
        return "IDLE";
    }

    @Override
    public double getProgressFraction() {
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sessionId);
        if (proc != null) {
            return proc.getProgressFraction();
        }
        TrainingTelemetry telem = ForgeSessionManager.getInstance().getTelemetry(sessionId);
        if (telem != null && telem.totalSteps > 0) {
            return Math.min(1.0, (double) telem.currentStep / telem.totalSteps);
        }
        return 0.0;
    }

    @Override
    public String getUnitSelectorLabel() {
        return "Select Epoch:";
    }

    @Override
    public List<ProcessUnitRecord> getUnits() {
        List<ProcessUnitRecord> unitList = new ArrayList<>();
        TrainingTelemetry telem = ForgeSessionManager.getInstance().getTelemetry(sessionId);

        int totalEpochs = (telem != null && telem.totalEpochs > 0) ? telem.totalEpochs : 1;
        int currentEpoch = (telem != null && telem.currentEpoch > 0) ? telem.currentEpoch : 1;

        for (int ep = 1; ep <= totalEpochs; ep++) {
            ProcessUnitRecord unit = new ProcessUnitRecord(ep, "Epoch " + ep);
            unit.setUnitType("EPOCH");
            unit.setGoal("Train local EVO LLM model - Epoch " + ep + " / " + totalEpochs);
            unit.setFlowSteps(getFlowStepLabels());

            if (ep < currentEpoch) {
                unit.setStatus("SUCCESS");
                unit.setResult("SUCCESS");
                unit.setScore(1.0 - (telem != null ? Math.min(1.0, telem.lastLoss) : 0.0));
                unit.setActiveFlowStepIndex(5); // CHECKPOINT
            } else if (ep == currentEpoch) {
                String status = getProcessStatus();
                unit.setStatus(status);
                unit.setResult("TRAINING".equals(status) || "RUNNING".equals(status) ? "RUNNING" : ("SUCCESS".equals(status) ? "SUCCESS" : "READY"));
                unit.setActiveFlowStepIndex(3); // LOSS / FORWARD
            } else {
                unit.setStatus("PENDING");
                unit.setResult("PENDING");
                unit.setActiveFlowStepIndex(-1);
            }

            if (telem != null) {
                unit.setMetricsSummary(String.format("Loss: %.4f | Val Loss: %.4f | Tokens/sec: %.1f",
                        telem.lastLoss, telem.lastValLoss, telem.tokensPerSec));
            }

            // Create primary branch/variant record for training metrics
            ProcessUnitRecord.BranchRecord branch = new ProcessUnitRecord.BranchRecord(
                    "main-epoch-" + ep,
                    "LLM Optimization Pass",
                    unit.getResult(),
                    unit.getScore()
            );
            unit.setBranches(Arrays.asList(branch));

            unitList.add(unit);
        }

        return unitList;
    }

    @Override
    public List<String> getFlowStepLabels() {
        return Arrays.asList("DATA_PREP", "BATCH", "FORWARD", "LOSS", "BACKWARD", "CHECKPOINT");
    }

    @Override
    public String getSummary() {
        TrainingTelemetry telem = ForgeSessionManager.getInstance().getTelemetry(sessionId);
        if (telem != null) {
            return String.format("Forge LLM Training - Epoch %d/%d, Step %d/%d (Loss: %.4f, Progress: %.0f%%)",
                    telem.currentEpoch, telem.totalEpochs, telem.currentStep, telem.totalSteps,
                    telem.lastLoss, getProgressFraction() * 100);
        }
        return String.format("Forge LLM Training - Status: %s", getProcessStatus());
    }
}
