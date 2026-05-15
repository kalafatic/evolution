package eu.kalafatic.evolution.controller.execution;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors system saturation and produces backpressure signals.
 */
public class BackpressureController {
    private static final BackpressureController INSTANCE = new BackpressureController();
    private final AtomicInteger activeEvaluations = new AtomicInteger(0);
    private final AtomicLong signalCount = new AtomicLong(0);
    private long lastSignalCheck = System.currentTimeMillis();

    private BackpressureController() {}

    public static BackpressureController getInstance() { return INSTANCE; }

    public void incrementEvaluations() { activeEvaluations.incrementAndGet(); }
    public void decrementEvaluations() { activeEvaluations.decrementAndGet(); }
    public void recordSignal() { signalCount.incrementAndGet(); }
    public void resetCounters() {
        signalCount.set(0);
        lastSignalCheck = System.currentTimeMillis();
    }

    public BackpressureStatus currentStatus() {
        BackpressureStatus status = new BackpressureStatus();
        status.setEvaluationQueueSize(activeEvaluations.get());

        long now = System.currentTimeMillis();
        long duration = now - lastSignalCheck;
        if (duration > 0) {
            status.setSignalEventRate((double) signalCount.get() / (duration / 1000.0));
        }
        // Reset counters for rate calculation if needed or keep rolling
        // For simplicity, we just provide current snapshot

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        double used = (double) memoryBean.getHeapMemoryUsage().getUsed();
        double max = (double) memoryBean.getHeapMemoryUsage().getMax();
        status.setMemoryPressure(used / max);

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        status.setCpuPressure(osBean.getSystemLoadAverage()); // Note: may be negative on some OS

        return status;
    }

    public boolean shouldThrottleSignals(ExecutionBudget budget) {
        return signalCount.get() > budget.getMaxSignalThroughput() || currentStatus().isOverloaded(budget);
    }
}
