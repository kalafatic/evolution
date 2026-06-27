package eu.kalafatic.evolution.controller.execution;

/**
 * Defines resource constraints per iteration for Darwin execution.
 */
public class ExecutionBudget {
    private int maxVariantsAllowed = 5;
    private int maxParallelEvaluations = 3;
    private int maxSignalThroughput = 100;
    private int maxWorkspaceWrites = 50;
    private int maxDarwinDepth = 3;
    private long timeBudgetMs = 300000; // 5 minutes
    private double memoryPressureThreshold = 0.85;
    private double cpuPressureThreshold = 0.80;

    public int getMaxVariantsAllowed() { return maxVariantsAllowed; }
    public void setMaxVariantsAllowed(int maxVariantsAllowed) { this.maxVariantsAllowed = maxVariantsAllowed; }

    public int getMaxParallelEvaluations() { return maxParallelEvaluations; }
    public void setMaxParallelEvaluations(int maxParallelEvaluations) { this.maxParallelEvaluations = maxParallelEvaluations; }

    public int getMaxSignalThroughput() { return maxSignalThroughput; }
    public void setMaxSignalThroughput(int maxSignalThroughput) { this.maxSignalThroughput = maxSignalThroughput; }

    public int getMaxWorkspaceWrites() { return maxWorkspaceWrites; }
    public void setMaxWorkspaceWrites(int maxWorkspaceWrites) { this.maxWorkspaceWrites = maxWorkspaceWrites; }

    public int getMaxDarwinDepth() { return maxDarwinDepth; }
    public void setMaxDarwinDepth(int maxDarwinDepth) { this.maxDarwinDepth = maxDarwinDepth; }

    public long getTimeBudgetMs() { return timeBudgetMs; }
    public void setTimeBudgetMs(long timeBudgetMs) { this.timeBudgetMs = timeBudgetMs; }

    public double getMemoryPressureThreshold() { return memoryPressureThreshold; }
    public void setMemoryPressureThreshold(double memoryPressureThreshold) { this.memoryPressureThreshold = memoryPressureThreshold; }

    public double getCpuPressureThreshold() { return cpuPressureThreshold; }
    public void setCpuPressureThreshold(double cpuPressureThreshold) { this.cpuPressureThreshold = cpuPressureThreshold; }

    public static ExecutionBudget defaultProfile() {
        return new ExecutionBudget();
    }
}
