package eu.kalafatic.evolution.controller.hardware;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared single-threaded worker for hardware diagnostics. Both the CUDA
 * diagnostic and the Forge GPU verification run here so they are serialized
 * (they share the process-wide CUDA context) and never block the SWT UI
 * thread. The thread is a daemon and is never shut down while the process
 * lives; tasks must therefore be bounded and non-blocking.
 */
final class DiagnosticWorker {

    private static final java.util.concurrent.ExecutorService EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "evo-hardware-diagnostics");
                t.setDaemon(true);
                return t;
            });

    private DiagnosticWorker() {
    }

    static void submit(Runnable task) {
        EXECUTOR.submit(task);
    }
}
