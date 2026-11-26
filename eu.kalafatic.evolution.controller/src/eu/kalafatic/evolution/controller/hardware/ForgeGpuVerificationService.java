package eu.kalafatic.evolution.controller.hardware;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.forge.data.api.TrainingSample;
import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.device.ForgeDeviceSelector;
import eu.kalafatic.evolution.forge.trainer.device.GpuTrainingAdapter;
import eu.kalafatic.evolution.forge.trainer.device.TrainingDevice;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Centralized Forge GPU training verification — the single authoritative
 * source for the question "is Forge actually training on the GPU?", used by
 * the Forge page and the AI Commander Forge panel.
 *
 * <p>The verification runs the canonical training path
 * ({@code ForgeDeviceSelector → GpuTrainingAdapter → GpuTrainingEngine → CUDA},
 * or the CPU {@code EvoLlmTrainer}) on a tiny deterministic in-memory model
 * and dataset. It never touches the user's model, dataset, checkpoints or
 * settings, and it refuses to run while a Forge training session is active.
 * CUDA usage is proven through the backend's kernel-launch / memory-transfer
 * instrumentation counters plus parameter-change verification — never through
 * device detection or configuration alone.</p>
 *
 * <p>Semantics mirror the production selector:</p>
 * <ul>
 *   <li>CPU policy → trains on CPU, PASS when parameters change.</li>
 *   <li>AUTO → CUDA when the backend actually initializes, otherwise CPU
 *       fallback with an explicit reason.</li>
 *   <li>CUDA → must execute CUDA; if the backend is unavailable the test
 *       reports FAILED with the fallback reason (never a silent CPU run).</li>
 * </ul>
 */
public final class ForgeGpuVerificationService {

    public static final int HISTORY_LIMIT = 5;

    // Tiny deterministic diagnostic model (sinusoidal encoding, matches the
    // canonical 6-argument EvoLlmModel constructor and the adapter's RoPE=false).
    private static final int VOCAB_SIZE = 64;
    private static final int D_MODEL = 32;
    private static final int NUM_HEADS = 4;
    private static final int NUM_BLOCKS = 1;
    private static final int DFF = 64;
    private static final int MAX_SEQ_LEN = 32;
    private static final int SAMPLE_COUNT = 8;
    private static final int SAMPLE_LEN = 16;

    private static final ForgeGpuVerificationService INSTANCE = new ForgeGpuVerificationService();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<ForgeGpuVerificationReport> current =
            new AtomicReference<>(null);
    private final Deque<ForgeGpuVerificationReport> history = new ArrayDeque<>();
    private final List<Consumer<ForgeGpuVerificationReport>> listeners =
            new CopyOnWriteArrayList<>();

    private ForgeGpuVerificationService() {
    }

    public static ForgeGpuVerificationService getInstance() {
        return INSTANCE;
    }

    public boolean isRunning() {
        return running.get();
    }

    public ForgeGpuVerificationReport getCurrent() {
        return current.get();
    }

    public synchronized List<ForgeGpuVerificationReport> getHistory() {
        return new ArrayList<>(history);
    }

    public void addListener(Consumer<ForgeGpuVerificationReport> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<ForgeGpuVerificationReport> listener) {
        listeners.remove(listener);
    }

    /**
     * The currently configured Forge device policy (AUTO/CPU/CUDA) resolved
     * from {@code evo.forge.device} by the authoritative
     * {@link ForgeDeviceSelector} — no duplicated policy logic in the UI.
     */
    public String getCurrentPolicyLabel() {
        return ForgeDeviceSelector.resolvePolicy().name();
    }

    /**
     * Starts the verification on the shared worker thread. If a test is
     * already running, the current report is returned and no duplicate test is
     * launched. Never blocks.
     */
    public ForgeGpuVerificationReport startAsync() {
        if (!running.compareAndSet(false, true)) {
            return current.get();
        }
        final ForgeGpuVerificationReport report = new ForgeGpuVerificationReport();
        current.set(report);
        DiagnosticWorker.submit(() -> {
            try {
                execute(report);
            } finally {
                running.set(false);
                notifyListeners(report);
                synchronized (this) {
                    history.addFirst(report);
                    while (history.size() > HISTORY_LIMIT) {
                        history.removeLast();
                    }
                }
            }
        });
        return report;
    }

    /**
     * Executes the verification synchronously on the calling thread.
     * Intended for tests and tooling; UI code must use {@link #startAsync()}.
     */
    public ForgeGpuVerificationReport run() {
        ForgeGpuVerificationReport report = new ForgeGpuVerificationReport();
        current.set(report);
        execute(report);
        notifyListeners(report);
        synchronized (this) {
            history.addFirst(report);
            while (history.size() > HISTORY_LIMIT) {
                history.removeLast();
            }
        }
        return report;
    }

    private void notifyListeners(ForgeGpuVerificationReport report) {
        for (Consumer<ForgeGpuVerificationReport> l : listeners) {
            try {
                l.accept(report);
            } catch (Throwable ignored) {
            }
        }
    }

    private void execute(ForgeGpuVerificationReport r) {
        r.startedAt = Instant.now();
        try {
            // 1. Capture the current device policy and selection.
            ForgeDeviceSelector.Policy policy = ForgeDeviceSelector.resolvePolicy();
            ForgeDeviceSelector.Selection sel = ForgeDeviceSelector.select(policy);
            r.policyLabel = policy.name();
            r.requestedDevice = sel.requested.label();
            r.selectedDevice = sel.selected.label();
            r.cudaAvailable = sel.cudaAvailable;
            r.selectionNote = sel.note == null ? "" : sel.note;

            // 2. Never interfere with an active Forge training session.
            ForgeSessionManager manager = ForgeSessionManager.getInstance();
            String activeSession = manager != null ? manager.getActiveTrainingSessionId() : null;
            if (activeSession != null && !activeSession.trim().isEmpty()) {
                r.blockedByActiveSession = true;
                r.blockedReason = "A Forge training session is active (" + activeSession
                        + "). The GPU verification cannot run concurrently and was not executed.";
                r.overallStatus = DiagnosticStatus.NOT_TESTED;
                return;
            }

            // 3. Isolated deterministic model + dataset (in-memory only).
            EvoLlmModel model = new EvoLlmModel(VOCAB_SIZE, D_MODEL, NUM_HEADS, NUM_BLOCKS,
                    DFF, MAX_SEQ_LEN);
            List<TrainingSample> samples = buildSyntheticSamples();
            r.modelSummary = "vocab=" + VOCAB_SIZE + " dModel=" + D_MODEL + " heads=" + NUM_HEADS
                    + " blocks=" + NUM_BLOCKS + " dff=" + DFF + " seqLen=" + MAX_SEQ_LEN
                    + " params=" + model.parameters().stream().mapToLong(Tensor::getSize).sum();

            float[] initialWeights = snapshotWeights(model);

            long t0 = System.currentTimeMillis();
            if (sel.selected == TrainingDevice.CUDA) {
                executeCudaPath(r, policy, model, samples, initialWeights);
            } else if (policy == ForgeDeviceSelector.Policy.CUDA) {
                // Explicit CUDA requested but the selector fell back to CPU:
                // report a clear failure instead of a silent CPU run.
                r.actualBackend = "not executed";
                r.actualDevice = "none";
                r.overallStatus = DiagnosticStatus.FAILED;
                r.failureDetail = "Explicit CUDA requested but unavailable — "
                        + (sel.note != null ? sel.note : "CPU fallback")
                        + ". CUDA training verification FAILED; no CPU fallback training was executed.";
            } else {
                executeCpuPath(r, model, samples, initialWeights);
            }
            r.durationMs = System.currentTimeMillis() - t0;
        } catch (Throwable t) {
            r.overallStatus = DiagnosticStatus.FAILED;
            r.failureDetail = describe(t);
        } finally {
            r.finishedAt = Instant.now();
        }
    }

    private void executeCudaPath(ForgeGpuVerificationReport r, ForgeDeviceSelector.Policy policy,
                                 EvoLlmModel model, List<TrainingSample> samples,
                                 float[] initialWeights) {
        long kernelsBefore = 0, memcpysBefore = 0, allocsBefore = 0, freesBefore = 0;
        long freeBefore = 0;
        Object device = null;
        boolean instrumentation = false;
        try {
            ClassLoader loader = ForgeDeviceSelector.class.getClassLoader();
            Class<?> cudaDevice = Class.forName("eu.kalafatic.evolution.gpu.cuda.CudaDevice",
                    true, loader);
            device = cudaDevice.getMethod("tryCreate").invoke(null);
            if (device != null) {
                kernelsBefore = (Long) cudaDevice.getMethod("getKernelLaunchCount").invoke(device);
                memcpysBefore = (Long) cudaDevice.getMethod("getMemcpyCount").invoke(device);
                allocsBefore = (Long) cudaDevice.getMethod("getMemAllocCount").invoke(device);
                freesBefore = (Long) cudaDevice.getMethod("getMemFreeCount").invoke(device);
                long[] mem = (long[]) cudaDevice.getMethod("memGetInfo").invoke(device);
                freeBefore = mem[0];
                instrumentation = true;
            }
        } catch (Throwable t) {
            r.failureDetail = "CUDA instrumentation unavailable: " + describe(t);
        }

        try {
            GpuTrainingAdapter adapter = new GpuTrainingAdapter(model);
            adapter.setMicroBatchSize(4);
            adapter.setAccumulationSteps(1);
            adapter.setBaseSeed(42L);
            adapter.train(samples, 1);

            r.actualBackend = adapter.getBackendName();
            r.actualDevice = adapter.getActualDevice().label();
            r.trainingStepsExecuted = (int) adapter.getOptimizerStepsExecuted();
            r.lossHistory = new ArrayList<>(adapter.getLossHistory());
            if (!r.lossHistory.isEmpty()) {
                r.initialLoss = r.lossHistory.get(0);
                r.finalLoss = r.lossHistory.get(r.lossHistory.size() - 1);
            }
        } catch (Throwable t) {
            r.actualBackend = "EVO-CUDA";
            r.actualDevice = "CUDA";
            r.overallStatus = DiagnosticStatus.FAILED;
            r.failureDetail = "EVO-CUDA training failed: " + describe(t);
            return;
        }

        // Post-execution evidence + parameter change verification.
        long kernelsAfter = 0, memcpysAfter = 0, allocsAfter = 0, freesAfter = 0;
        long freeAfter = 0;
        try {
            if (device != null) {
                Class<?> cudaDevice = device.getClass();
                kernelsAfter = (Long) cudaDevice.getMethod("getKernelLaunchCount").invoke(device);
                memcpysAfter = (Long) cudaDevice.getMethod("getMemcpyCount").invoke(device);
                allocsAfter = (Long) cudaDevice.getMethod("getMemAllocCount").invoke(device);
                freesAfter = (Long) cudaDevice.getMethod("getMemFreeCount").invoke(device);
                long[] mem = (long[]) cudaDevice.getMethod("memGetInfo").invoke(device);
                freeAfter = mem[0];
            }
        } catch (Throwable t) {
            r.failureDetail = (r.failureDetail.isEmpty() ? "" : r.failureDetail + "; ")
                    + "post-execution instrumentation failed: " + describe(t);
        }
        r.kernelLaunchDelta = kernelsAfter - kernelsBefore;
        r.memcpyDelta = memcpysAfter - memcpysBefore;
        r.memAllocDelta = allocsAfter - allocsBefore;
        r.memFreeDelta = freesAfter - freesBefore;
        r.freeMemBeforeBytes = freeBefore;
        r.freeMemAfterBytes = freeAfter;
        r.cleanupVerified = allocsAfter - allocsBefore <= freesAfter - freesBefore
                && freeBefore > 0 && (freeBefore - freeAfter) < 64L * 1024 * 1024;

        verifyParameterChange(r, model, initialWeights);

        if (!r.parametersChanged || r.lossHistory.isEmpty()) {
            r.overallStatus = DiagnosticStatus.FAILED;
            if (r.failureDetail.isEmpty()) {
                r.failureDetail = "Training completed without measurable parameter changes "
                        + "(changed=" + r.parameterChangeCount + ") or without loss history.";
            }
            return;
        }
        if (instrumentation && r.kernelLaunchDelta > 0) {
            r.overallStatus = DiagnosticStatus.PASS;
        } else {
            r.overallStatus = DiagnosticStatus.PARTIAL;
            r.failureDetail = "CUDA training executed (params changed, steps="
                    + r.trainingStepsExecuted + ") but kernel execution could not be "
                    + "independently counted (kernelLaunchDelta=" + r.kernelLaunchDelta + ")";
        }
    }

    private void executeCpuPath(ForgeGpuVerificationReport r, EvoLlmModel model,
                                List<TrainingSample> samples, float[] initialWeights) {
        EvoLlmTrainer trainer = new EvoLlmTrainer(model);
        trainer.setMicroBatchSize(4);
        trainer.setAccumulationSteps(1);
        trainer.setBaseSeed(42L);
        try {
            trainer.train(samples, 1);
            r.actualBackend = trainer.getBackendName();
            r.actualDevice = trainer.getActualDevice();
            r.trainingStepsExecuted = (int) trainer.getOptimizerStepsExecuted();
            r.lossHistory = new ArrayList<>(trainer.getLossHistory());
            if (!r.lossHistory.isEmpty()) {
                r.initialLoss = r.lossHistory.get(0);
                r.finalLoss = r.lossHistory.get(r.lossHistory.size() - 1);
            }
        } catch (Throwable t) {
            r.overallStatus = DiagnosticStatus.FAILED;
            r.failureDetail = "CPU training failed: " + describe(t);
            return;
        }
        verifyParameterChange(r, model, initialWeights);
        r.cleanupVerified = true; // CPU path allocates no persistent native resources
        if (r.parametersChanged && !r.lossHistory.isEmpty()) {
            r.overallStatus = DiagnosticStatus.PASS;
        } else {
            r.overallStatus = DiagnosticStatus.FAILED;
            r.failureDetail = "CPU training completed without measurable parameter changes "
                    + "(changed=" + r.parameterChangeCount + ") or without loss history.";
        }
    }

    private void verifyParameterChange(ForgeGpuVerificationReport r, EvoLlmModel model,
                                       float[] initialWeights) {
        float[] after = snapshotWeights(model);
        int changed = 0;
        double maxAbs = 0.0;
        int n = Math.min(initialWeights.length, after.length);
        for (int i = 0; i < n; i++) {
            double d = Math.abs(after[i] - initialWeights[i]);
            if (d > 1e-9) {
                changed++;
            }
            if (d > maxAbs) {
                maxAbs = d;
            }
        }
        r.parameterChangeCount = changed;
        r.parameterChangeMaxAbs = maxAbs;
        r.parametersChanged = changed > 0;
    }

    private static float[] snapshotWeights(EvoLlmModel model) {
        List<Tensor> params = model.parameters();
        int n = 0;
        for (Tensor p : params) {
            n += p.getData().length;
        }
        float[] flat = new float[n];
        int off = 0;
        for (Tensor p : params) {
            float[] d = p.getData();
            System.arraycopy(d, 0, flat, off, d.length);
            off += d.length;
        }
        return flat;
    }

    /** Deterministic synthetic samples: pattern of next-token prediction. */
    private static List<TrainingSample> buildSyntheticSamples() {
        List<TrainingSample> samples = new ArrayList<>();
        for (int s = 0; s < SAMPLE_COUNT; s++) {
            int[] tokens = new int[SAMPLE_LEN];
            for (int i = 0; i < SAMPLE_LEN; i++) {
                tokens[i] = (s * 7 + i * 5) % VOCAB_SIZE;
            }
            int[] labels = new int[SAMPLE_LEN];
            boolean[] lossMask = new boolean[SAMPLE_LEN];
            float[] attMask = new float[SAMPLE_LEN];
            for (int i = 0; i < SAMPLE_LEN; i++) {
                labels[i] = (i + 1 < SAMPLE_LEN) ? tokens[i + 1] : tokens[i];
                lossMask[i] = (i + 1 < SAMPLE_LEN);
                attMask[i] = 1.0f;
            }
            samples.add(new TrainingSample(tokens, labels, lossMask, attMask));
        }
        return samples;
    }

    private static String describe(Throwable t) {
        String msg = t.getMessage();
        String cls = t.getClass().getSimpleName();
        return msg == null || msg.isEmpty() ? cls : cls + ": " + msg;
    }
}
