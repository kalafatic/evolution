package eu.kalafatic.evolution.forge.trainer.impl.llm;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TrainingProgressReporter implements AutoCloseable {

    private final AtomicReference<TrainingProgress> latestProgress = new AtomicReference<>();
    private final List<TrainingProgressListener> listeners = new CopyOnWriteArrayList<>();
    private final TrainingJobStateStore stateStore;
    private final Thread workerThread;
    private final Object signal = new Object();
    private volatile boolean running = true;

    private long lastUiPublishTime = 0;
    private long lastPersistenceTime = 0;
    private long lastMetricsTime = 0;
    private TrainingProgress lastProcessedProgress = null;

    public TrainingProgressReporter(Path jobFolder) {
        this.stateStore = jobFolder != null ? new TrainingJobStateStore(jobFolder) : null;
        this.workerThread = new Thread(this::runLoop, "training-progress-reporter");
        this.workerThread.setDaemon(true);
        this.workerThread.setPriority(Thread.MIN_PRIORITY);
        this.workerThread.start();
    }

    public void addListener(TrainingProgressListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(TrainingProgressListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void report(TrainingProgress progress) {
        if (progress == null) return;
        latestProgress.set(progress);
        synchronized (signal) {
            signal.notifyAll();
        }
    }

    public TrainingProgress getLatestProgress() {
        return latestProgress.get();
    }

    private void runLoop() {
        while (running) {
            TrainingProgress current;
            synchronized (signal) {
                current = latestProgress.get();
                if (current == null || current == lastProcessedProgress) {
                    try {
                        signal.wait(200); // Wake up periodically or on notify
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
            }

            if (current == null) continue;

            long now = System.currentTimeMillis();
            boolean isTransition = lastProcessedProgress == null
                    || current.getStatus() != lastProcessedProgress.getStatus()
                    || !current.getPhase().equals(lastProcessedProgress.getPhase())
                    || current.getCurrentEpoch() != lastProcessedProgress.getCurrentEpoch();

            boolean shouldPublishUi = isTransition || (now - lastUiPublishTime >= 1000);
            boolean shouldPersist = isTransition || (now - lastPersistenceTime >= 20000);
            boolean shouldMetrics = isTransition || (now - lastMetricsTime >= 30000);

            // Special statuses that must be persisted and published immediately
            TrainingStatus status = current.getStatus();
            if (status == TrainingStatus.COMPLETED || status == TrainingStatus.FAILED ||
                status == TrainingStatus.CANCELLED || status == TrainingStatus.PAUSED ||
                status == TrainingStatus.RESUMING || status == TrainingStatus.SAVING_CHECKPOINT) {
                shouldPublishUi = true;
                shouldPersist = true;
            }

            if (shouldPublishUi) {
                for (TrainingProgressListener listener : listeners) {
                    try {
                        listener.onProgress(current);
                    } catch (Exception e) {
                        System.err.println("[TrainingProgressReporter] Listener error: " + e.getMessage());
                    }
                }
                lastUiPublishTime = now;
            }

            if (shouldPersist && stateStore != null) {
                try {
                    stateStore.save(current);
                } catch (Exception e) {
                    System.err.println("[TrainingProgressReporter] StateStore save error: " + e.getMessage());
                }
                lastPersistenceTime = now;
            }

            if (shouldMetrics && stateStore != null && status == TrainingStatus.TRAINING) {
                try {
                    stateStore.appendMetrics(current);
                } catch (Exception e) {
                    System.err.println("[TrainingProgressReporter] StateStore metrics error: " + e.getMessage());
                }
                lastMetricsTime = now;
            }

            lastProcessedProgress = current;
        }

        // On shutdown, ensure the final progress is saved/flushed
        TrainingProgress finalProgress = latestProgress.get();
        if (finalProgress != null && stateStore != null) {
            try {
                stateStore.save(finalProgress);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void close() {
        running = false;
        synchronized (signal) {
            signal.notifyAll();
        }
        try {
            workerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
