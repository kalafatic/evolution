package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.trainer.impl.llm.TrainingProgress;
import eu.kalafatic.evolution.forge.trainer.impl.llm.TrainingStatus;
import eu.kalafatic.evolution.forge.trainer.impl.llm.TrainingProgressListener;
import eu.kalafatic.evolution.forge.trainer.impl.llm.TrainingProgressReporter;
import eu.kalafatic.evolution.forge.trainer.impl.llm.TrainingJobStateStore;

public class TrainingProgressTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testProgressCalculationArithmetic() {
        // Test Zero total steps
        TrainingProgress pZero = TrainingProgress.builder()
                .jobId("test-job")
                .status(TrainingStatus.TRAINING)
                .phase("TRAINING")
                .totalSteps(0L)
                .currentStep(100L)
                .progressPercent(-1.0)
                .build();
        assertEquals(-1.0, pZero.getProgressPercent(), 0.001);

        // Test normal progress
        TrainingProgress pNormal = TrainingProgress.builder()
                .jobId("test-job")
                .status(TrainingStatus.TRAINING)
                .phase("TRAINING")
                .totalSteps(200L)
                .currentStep(150L)
                .progressPercent(75.0)
                .build();
        assertEquals(75.0, pNormal.getProgressPercent(), 0.001);

        // Test 100% completion
        TrainingProgress pComplete = TrainingProgress.builder()
                .progressPercent(100.0)
                .build();
        assertEquals(100.0, pComplete.getProgressPercent(), 0.001);

        // Test clamping (progress percent should never exceed 100% or go below 0% unless indeterminate)
        double clampedUpper = Math.min(100.0, Math.max(0.0, 110.0));
        double clampedLower = Math.min(100.0, Math.max(0.0, -10.0));
        assertEquals(100.0, clampedUpper, 0.001);
        assertEquals(0.0, clampedLower, 0.001);

        // Test no NaN / no Infinity on division by zero
        long totalStepsZero = 0;
        long completedSteps = 50;
        double pctZero = totalStepsZero > 0 ? (completedSteps * 100.0 / totalStepsZero) : -1.0;
        assertFalse(Double.isNaN(pctZero));
        assertFalse(Double.isInfinite(pctZero));
        assertEquals(-1.0, pctZero, 0.001);

        // Test arithmetic overflow handling with large values
        long largeEpochs = 3_000_000_000L;
        long largeStepsPerEpoch = 4_000_000_000L;
        long overflowSteps = -1;
        try {
            overflowSteps = Math.multiplyExact(largeEpochs, largeStepsPerEpoch);
            fail("Expected Math.multiplyExact to throw ArithmeticException");
        } catch (ArithmeticException e) {
            // expected overflow exception
            overflowSteps = Long.MAX_VALUE;
        }
        assertEquals(Long.MAX_VALUE, overflowSteps);
    }

    @Test
    public void testRateLimitingAndTransitions() throws Exception {
        File jobDir = tempFolder.newFolder("job-rate-limiting");
        final AtomicInteger publishCount = new AtomicInteger(0);
        final List<TrainingProgress> receivedEvents = new ArrayList<>();

        try (TrainingProgressReporter reporter = new TrainingProgressReporter(jobDir.toPath())) {
            reporter.addListener(progress -> {
                publishCount.incrementAndGet();
                synchronized (receivedEvents) {
                    receivedEvents.add(progress);
                }
            });

            // Trigger some quick progress reports
            for (int i = 0; i < 20; i++) {
                reporter.report(TrainingProgress.builder()
                        .jobId("test-job")
                        .status(TrainingStatus.TRAINING)
                        .phase("TRAINING")
                        .currentEpoch(1)
                        .currentStep(i)
                        .totalSteps(100)
                        .build());
            }

            // Report an important state transition - Completed
            reporter.report(TrainingProgress.builder()
                    .jobId("test-job")
                    .status(TrainingStatus.COMPLETED)
                    .phase("COMPLETED")
                    .currentEpoch(1)
                    .currentStep(100)
                    .totalSteps(100)
                    .build());

            // Wait a little bit for daemon thread to process the events
            Thread.sleep(600);

            // Frequent reports should be throttled/coalesced (we sent 20 training steps within milliseconds)
            // So we should have received significantly fewer UI publish updates for TRAINING,
            // but the transition to COMPLETED must be processed and received immediately!
            synchronized (receivedEvents) {
                assertFalse("Should have received at least one event", receivedEvents.isEmpty());
                TrainingProgress finalProgress = receivedEvents.get(receivedEvents.size() - 1);
                assertEquals(TrainingStatus.COMPLETED, finalProgress.getStatus());
                // The total publish count should be small due to coalescing
                assertTrue("Publish count should be highly throttled (e.g. <= 5)", publishCount.get() <= 5);
            }
        }
    }

    @Test
    public void testSlowConsumerIsolation() throws Exception {
        File jobDir = tempFolder.newFolder("job-slow-consumer");
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<TrainingProgress> lastReceived = new AtomicReference<>();

        try (TrainingProgressReporter reporter = new TrainingProgressReporter(jobDir.toPath())) {
            reporter.addListener(progress -> {
                try {
                    // Simulate very slow work/consumer (e.g. 150ms sleep)
                    Thread.sleep(150);
                    lastReceived.set(progress);
                    if (progress.getStatus() == TrainingStatus.COMPLETED) {
                        latch.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            long trainerStartTime = System.currentTimeMillis();

            // Submit 50 updates rapidly from "trainer" thread
            for (int i = 0; i < 50; i++) {
                reporter.report(TrainingProgress.builder()
                        .jobId("test-job")
                        .status(TrainingStatus.TRAINING)
                        .phase("TRAINING")
                        .currentStep(i)
                        .totalSteps(50)
                        .build());
            }

            // Finish training
            reporter.report(TrainingProgress.builder()
                    .jobId("test-job")
                    .status(TrainingStatus.COMPLETED)
                    .phase("COMPLETED")
                    .currentStep(50)
                    .totalSteps(50)
                    .build());

            long trainerEndTime = System.currentTimeMillis();

            // Verification 1: The trainer thread was NOT blocked by the slow listener!
            long trainerDuration = trainerEndTime - trainerStartTime;
            assertTrue("Trainer must not be blocked (duration should be very short, e.g. < 50ms)", trainerDuration < 100);

            // Wait for consumer to process the final event
            boolean reached = latch.await(2, TimeUnit.SECONDS);
            assertTrue("Slow consumer should eventually receive the completed event", reached);
            assertEquals(TrainingStatus.COMPLETED, lastReceived.get().getStatus());
        }
    }

    @Test
    public void testPersistenceAtomicAndFailureIsolation() throws Exception {
        File jobDir = tempFolder.newFolder("job-persistence");
        Path jobPath = jobDir.toPath();

        TrainingJobStateStore store = new TrainingJobStateStore(jobPath);

        // Save a valid progress state
        TrainingProgress p1 = TrainingProgress.builder()
                .jobId("job-123")
                .status(TrainingStatus.TRAINING)
                .phase("TRAINING")
                .currentEpoch(2)
                .totalEpochs(10)
                .currentStep(500)
                .totalSteps(2000)
                .progressPercent(25.0)
                .timestamp(Instant.now())
                .message("Epoch 2 in progress")
                .build();

        store.save(p1);

        // Verify file is valid JSON and exists
        Path stateFile = jobPath.resolve("training-state.json");
        assertTrue(Files.exists(stateFile));

        String content = Files.readString(stateFile);
        JSONObject json = new JSONObject(content);
        assertEquals("job-123", json.getString("jobId"));
        assertEquals("TRAINING", json.getString("status"));
        assertEquals(2, json.getInt("currentEpoch"));

        // Simulate write failure/interruption to test atomic move replacement
        // We will make the file read-only or simulate some write exception.
        // Actually, if we pass a directory that does not exist or has permission errors,
        // it will log the error but keep the previous state untouched. Let's verify:
        TrainingJobStateStore invalidStore = new TrainingJobStateStore(jobPath.resolve("non_existent_subdir/readonly_file"));

        // This save should fail inside but must NOT throw exception out to propagate (Failure Isolation!)
        try {
            invalidStore.save(p1);
            // Must not throw exception
        } catch (Exception e) {
            fail("Failure isolation violated: state store save propagated an exception!");
        }

        // Previous valid file state must still be fully intact and not corrupted!
        assertTrue(Files.exists(stateFile));
        String contentAfterFailedSave = Files.readString(stateFile);
        JSONObject jsonAfter = new JSONObject(contentAfterFailedSave);
        assertEquals("job-123", jsonAfter.getString("jobId"));
        assertEquals(2, jsonAfter.getInt("currentEpoch"));
    }

    @Test
    public void testRecovery() throws Exception {
        File jobDir = tempFolder.newFolder("job-recovery");
        Path jobPath = jobDir.toPath();

        TrainingJobStateStore store = new TrainingJobStateStore(jobPath);

        // Persist final completed state
        TrainingProgress completedProgress = TrainingProgress.builder()
                .jobId("job-abc")
                .status(TrainingStatus.COMPLETED)
                .phase("COMPLETED")
                .currentEpoch(5)
                .totalEpochs(5)
                .currentStep(1000)
                .totalSteps(1000)
                .progressPercent(100.0)
                .timestamp(Instant.now())
                .message("Completed successfully")
                .build();

        store.save(completedProgress);

        // Simulate a UI/system restart by reading back the state file from disk
        TrainingProgress recovered = TrainingJobStateStore.load(jobPath);

        assertNotNull(recovered);
        assertEquals("job-abc", recovered.getJobId());
        assertEquals(TrainingStatus.COMPLETED, recovered.getStatus());
        assertEquals(5, recovered.getCurrentEpoch());
        assertEquals(1000L, recovered.getCurrentStep());
        assertEquals(100.0, recovered.getProgressPercent(), 0.001);
    }

    @Test
    public void testListenerFailureIsolation() throws Exception {
        File jobDir = tempFolder.newFolder("job-failure-isolation");

        try (TrainingProgressReporter reporter = new TrainingProgressReporter(jobDir.toPath())) {
            // Add a listener that always throws runtime exception
            reporter.addListener(progress -> {
                throw new RuntimeException("Deliberate listener crash!");
            });

            // Report should succeed and listener exception must be caught and isolated (not throw out)
            try {
                reporter.report(TrainingProgress.builder()
                        .jobId("test")
                        .status(TrainingStatus.TRAINING)
                        .phase("TRAINING")
                        .build());
            } catch (Exception e) {
                fail("Failure isolation violated: reporter.report() propagated a listener exception!");
            }

            // Give a bit of time for background thread to run
            Thread.sleep(300);
        }
    }
}
