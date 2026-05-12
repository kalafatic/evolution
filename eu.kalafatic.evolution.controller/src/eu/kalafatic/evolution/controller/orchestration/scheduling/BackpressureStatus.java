package eu.kalafatic.evolution.controller.orchestration.scheduling;

/**
 * Holds system saturation metrics for backpressure control.
 */
public class BackpressureStatus {
    private int evaluationQueueSize = 0;
    private double signalEventRate = 0.0;
    private int workspaceArtifactGrowth = 0;
    private double memoryPressure = 0.0;
    private double cpuPressure = 0.0;

    public int getEvaluationQueueSize() { return evaluationQueueSize; }
    public void setEvaluationQueueSize(int evaluationQueueSize) { this.evaluationQueueSize = evaluationQueueSize; }

    public double getSignalEventRate() { return signalEventRate; }
    public void setSignalEventRate(double signalEventRate) { this.signalEventRate = signalEventRate; }

    public int getWorkspaceArtifactGrowth() { return workspaceArtifactGrowth; }
    public void setWorkspaceArtifactGrowth(int workspaceArtifactGrowth) { this.workspaceArtifactGrowth = workspaceArtifactGrowth; }

    public double getMemoryPressure() { return memoryPressure; }
    public void setMemoryPressure(double memoryPressure) { this.memoryPressure = memoryPressure; }

    public double getCpuPressure() { return cpuPressure; }
    public void setCpuPressure(double cpuPressure) { this.cpuPressure = cpuPressure; }

    public boolean isOverloaded(ExecutionBudget budget) {
        return memoryPressure > budget.getMemoryPressureThreshold() ||
               cpuPressure > budget.getCpuPressureThreshold();
    }
}
