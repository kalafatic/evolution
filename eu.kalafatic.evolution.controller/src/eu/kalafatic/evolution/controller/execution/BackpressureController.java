package eu.kalafatic.evolution.controller.execution;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors system saturation and produces backpressure signals.
 */
public class BackpressureController implements eu.kalafatic.evolution.controller.orchestration.capability.ICapability {
    private static final BackpressureController INSTANCE = new BackpressureController();
    private final AtomicInteger activeEvaluations = new AtomicInteger(0);
    private final AtomicLong signalCount = new AtomicLong(0);
    private long lastSignalCheck = System.currentTimeMillis();

    public BackpressureController() {}

    /**
     * @deprecated Use session-scoped BackpressureController via SessionContainer.
     */
    @Deprecated
    public static BackpressureController getInstance() {
        eu.kalafatic.evolution.controller.kernel.RuntimeInvariant.checkNoGlobalAccess("BackpressureController.getInstance");
        return INSTANCE;
    }

    @Override
    public String getCapabilityId() {
        return "capability.backpressure";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus getStatus() {
        return eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus.STARTED;
    }

    @Override
    public void initialize(eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext context) throws eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException {
    }

    @Override
    public void start() throws eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException {
    }

    @Override
    public void stop() throws eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException {
    }

    @Override
    public java.util.List<String> getSupportedContracts() {
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<String> getDependencies() {
        return java.util.Collections.emptyList();
    }

    @Override
    public eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth getHealth() {
        return new eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth(1.0, "Healthy", 0);
    }

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

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        double used = (double) memoryBean.getHeapMemoryUsage().getUsed();
        double max = (double) memoryBean.getHeapMemoryUsage().getMax();
        status.setMemoryPressure(used / max);

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        status.setCpuPressure(osBean.getSystemLoadAverage());

        return status;
    }

    public boolean shouldThrottleSignals(ExecutionBudget budget) {
        return signalCount.get() > budget.getMaxSignalThroughput() || currentStatus().isOverloaded(budget);
    }
}
