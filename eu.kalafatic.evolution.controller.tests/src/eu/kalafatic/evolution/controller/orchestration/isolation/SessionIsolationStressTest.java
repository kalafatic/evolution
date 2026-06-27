package eu.kalafatic.evolution.controller.orchestration.isolation;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.workflow.*;

/**
 * Stress test for session isolation.
 */
public class SessionIsolationStressTest {

    @Test
    public void testParallelSessionIsolation() throws Exception {
        int sessionCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(sessionCount);
        List<Future<Integer>> results = new ArrayList<>();

        final List<RuntimeEvent> allReceivedEvents = new CopyOnWriteArrayList<>();

        for (int i = 0; i < sessionCount; i++) {
            final String sessionId = "session-" + i;
            results.add(executor.submit(() -> {
                SessionContainer container = SessionManager.getInstance().getOrCreateSession(sessionId);

                // Track events for this session
                final List<RuntimeEvent> sessionEvents = new ArrayList<>();
                container.getEventBus().subscribe(event -> {
                    sessionEvents.add(event);
                    allReceivedEvents.add(event);
                });

                // Publish some events
                for (int j = 0; j < 100; j++) {
                    container.getEventBus().publish(new RuntimeEvent(
                        RuntimeEventType.TASK_STARTED, sessionId, "Test", "task-" + j));
                }

                // Wait for throttled delivery (RuntimeEventBus uses 100ms throttle)
                Thread.sleep(500);

                // Verify that ALL received events for this subscriber belong to this session
                synchronized (sessionEvents) {
                    for (RuntimeEvent event : sessionEvents) {
                        if (!sessionId.equals(event.getSessionId())) {
                            return -1; // SIGNAL BLEED!
                        }
                    }
                    return sessionEvents.size();
                }
            }));
        }

        for (Future<Integer> future : results) {
            int count = future.get();
            assertNotEquals("Signal bleed detected!", -1, count);
            assertEquals(100, count);
        }

        executor.shutdown();
    }
}
