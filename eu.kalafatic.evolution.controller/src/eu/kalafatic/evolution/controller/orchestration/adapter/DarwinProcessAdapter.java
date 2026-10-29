package eu.kalafatic.evolution.controller.orchestration.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker;
import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker.ProcessInfo;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Adapter for Darwin Evolution process loop (Iterations, Variants/Branches, Flow Steps).
 */
public class DarwinProcessAdapter implements IProcessAdapter {

    private final String sessionId;
    private final Orchestrator orchestrator;
    private final IterationMemoryService memoryService;

    public DarwinProcessAdapter(String sessionId, Orchestrator orchestrator, File projectRoot) {
        this.sessionId = sessionId != null ? sessionId : "Default";
        this.orchestrator = orchestrator;
        if (projectRoot != null && projectRoot.exists()) {
            this.memoryService = new IterationMemoryService(projectRoot);
        } else {
            this.memoryService = null;
        }
    }

    @Override
    public String getProcessType() {
        return "DARWIN_ITERATION";
    }

    @Override
    public String getProcessName() {
        return "Darwin Evolutionary Loop";
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
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            return orchestrator.getSelfDevSession().getStatus().getName();
        }
        return "READY";
    }

    @Override
    public String getCurrentPhase() {
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sessionId);
        if (proc != null) {
            return proc.getPhase();
        }
        return "EVOLVING";
    }

    @Override
    public double getProgressFraction() {
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sessionId);
        if (proc != null) {
            return proc.getProgressFraction();
        }
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            int max = orchestrator.getSelfDevSession().getMaxIterations();
            int current = orchestrator.getSelfDevSession().getIterations().size();
            return max > 0 ? (double) current / max : 0.0;
        }
        return 0.0;
    }

    @Override
    public String getUnitSelectorLabel() {
        return "Select Iteration:";
    }

    @Override
    public List<ProcessUnitRecord> getUnits() {
        if (memoryService != null) {
            memoryService.refresh();
        }

        List<IterationRecord> records = (memoryService != null) ? new ArrayList<>(memoryService.getRecords())
                : new ArrayList<>();

        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            List<eu.kalafatic.evolution.model.orchestration.Iteration> sessionIters = orchestrator.getSelfDevSession()
                    .getIterations();
            for (int i = 0; i < sessionIters.size(); i++) {
                eu.kalafatic.evolution.model.orchestration.Iteration iter = sessionIters.get(i);
                final int itNum = i + 1;
                boolean exists = records.stream().anyMatch(r -> r.getIteration() == itNum);
                if (!exists) {
                    IterationRecord r = new IterationRecord();
                    r.setIteration(itNum);
                    r.setGoal(orchestrator.getSelfDevSession().getInitialRequest());
                    r.setBranch(iter.getBranchName() != null ? iter.getBranchName() : "it-" + itNum);
                    r.setStatus(iter.getStatus() != null ? iter.getStatus().getName() : "RUNNING");
                    r.setResult("RUNNING");
                    r.setStrategy(iter.getPhase() != null ? iter.getPhase() : "EVOLVING");
                    records.add(r);
                }
            }
        }

        Map<Integer, List<IterationRecord>> iterationsMap = records.stream()
                .collect(Collectors.groupingBy(IterationRecord::getIteration, TreeMap::new, Collectors.toList()));

        List<ProcessUnitRecord> unitList = new ArrayList<>();
        for (Map.Entry<Integer, List<IterationRecord>> entry : iterationsMap.entrySet()) {
            int iterNum = entry.getKey();
            List<IterationRecord> variants = entry.getValue();

            ProcessUnitRecord unit = new ProcessUnitRecord(iterNum, "Iteration " + iterNum);
            unit.setUnitType("ITERATION");
            unit.setFlowSteps(getFlowStepLabels());

            if (!variants.isEmpty()) {
                IterationRecord first = variants.get(0);
                unit.setGoal(first.getGoal() != null ? first.getGoal() : "");
                boolean anySuccess = variants.stream().anyMatch(v -> "SUCCESS".equals(v.getResult()));
                unit.setResult(anySuccess ? "SUCCESS" : (variants.stream().allMatch(v -> "FAILED".equals(v.getResult())) ? "FAILED" : "RUNNING"));
                unit.setScore(variants.stream().mapToDouble(IterationRecord::getScore).max().orElse(0.0));

                String phase = first.getStrategy() != null ? first.getStrategy().toUpperCase() : "";
                unit.setActiveFlowStepIndex(determineFlowStepIndex(phase));

                List<ProcessUnitRecord.BranchRecord> branchRecords = new ArrayList<>();
                for (IterationRecord var : variants) {
                    ProcessUnitRecord.BranchRecord br = new ProcessUnitRecord.BranchRecord(
                            var.getBranch(), var.getStrategy(), var.getResult(), var.getScore());
                    br.setErrorMessage(var.getErrorMessage());
                    if (var.getChangedFiles() != null) {
                        br.setChangedFiles(var.getChangedFiles());
                    }
                    branchRecords.add(br);
                }
                unit.setBranches(branchRecords);
            }
            unitList.add(unit);
        }

        if (unitList.isEmpty()) {
            ProcessUnitRecord defaultUnit = new ProcessUnitRecord(1, "Iteration 1");
            defaultUnit.setUnitType("ITERATION");
            defaultUnit.setGoal(orchestrator != null && orchestrator.getSelfDevSession() != null ? orchestrator.getSelfDevSession().getInitialRequest() : "");
            defaultUnit.setStatus("READY");
            defaultUnit.setFlowSteps(getFlowStepLabels());
            unitList.add(defaultUnit);
        }

        return unitList;
    }

    private int determineFlowStepIndex(String phase) {
        if (phase.contains("PLAN")) return 0;
        if (phase.contains("CODE")) return 1;
        if (phase.contains("TEST") || phase.contains("VERIFY")) return 2;
        if (phase.contains("SCORE") || phase.contains("EVAL")) return 3;
        if (phase.contains("SELECT")) return 4;
        if (phase.contains("MERGE") || phase.contains("INTEGRAT")) return 5;
        return -1;
    }

    @Override
    public List<String> getFlowStepLabels() {
        return Arrays.asList("PLAN", "CODE", "TEST", "SCORE", "SELECT", "MERGE");
    }

    @Override
    public String getSummary() {
        return String.format("Darwin Iteration Loop - Status: %s, Progress: %.0f%%",
                getProcessStatus(), getProgressFraction() * 100);
    }
}
