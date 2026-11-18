package eu.kalafatic.evolution.controller.teip.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import eu.kalafatic.evolution.controller.teip.dto.TeipEvent;
import eu.kalafatic.evolution.controller.teip.dto.TeipEventsResponse;

/**
 * Thread-safe sequenced event store for TEIP 1.1 executions.
 * Supports cursor pagination, replay, and bounded retention. Events are persisted
 * through TeipPersistenceStore so sequences survive server restarts (TEIP 1.1 §4.5).
 *
 * Cursor semantics (TEIP 1.1 §4.5): nextSequence = last returned event's sequence + 1
 * (0 when no events exist). Clients use it verbatim as the next afterSequence.
 */
public class TeipEventStore {
    private static final TeipEventStore INSTANCE = new TeipEventStore();

    // Map: executionId -> List of TeipEvents
    private final Map<String, List<TeipEvent>> executionEvents = new ConcurrentHashMap<>();
    // Map: executionId -> sequence generator
    private final Map<String, AtomicLong> sequenceCounters = new ConcurrentHashMap<>();

    private volatile boolean hydrated = false;

    private TeipEventStore() {}

    public static TeipEventStore getInstance() {
        return INSTANCE;
    }

    /**
     * Hydrates persisted event streams after a restart so cursor replay keeps working
     * for executions whose events were written before the restart.
     */
    public synchronized void hydrateFromPersistence() {
        if (hydrated) return;
        hydrated = true;
        try {
            for (String[] row : TeipPersistenceStore.getInstance().loadAllExecutions()) {
                hydrateExecution(row[0]);
            }
        } catch (Throwable ignored) {}
    }

    private void hydrateExecution(String executionId) {
        try {
            List<TeipEvent> persisted = TeipPersistenceStore.getInstance().loadEvents(executionId);
            if (persisted.isEmpty()) return;
            List<TeipEvent> events = executionEvents.computeIfAbsent(executionId, k -> Collections.synchronizedList(new ArrayList<>()));
            AtomicLong seq = sequenceCounters.computeIfAbsent(executionId, k -> new AtomicLong(0));
            synchronized (events) {
                if (events.isEmpty()) {
                    events.addAll(persisted);
                    long max = 0;
                    for (TeipEvent evt : persisted) {
                        max = Math.max(max, evt.getSequence());
                    }
                    seq.set(max);
                }
            }
        } catch (Throwable ignored) {}
    }

    public synchronized TeipEvent appendEvent(String executionId, String type, String correlationId, Map<String, Object> data) {
        if (executionId == null || executionId.trim().isEmpty()) {
            throw new IllegalArgumentException("executionId cannot be null or empty");
        }
        List<TeipEvent> events = executionEvents.computeIfAbsent(executionId, k -> Collections.synchronizedList(new ArrayList<>()));
        AtomicLong seq = sequenceCounters.computeIfAbsent(executionId, k -> new AtomicLong(0));
        long sequenceNumber = seq.incrementAndGet();

        String eventId = "evt-" + executionId + "-" + sequenceNumber;
        TeipEvent event = new TeipEvent(eventId, executionId, sequenceNumber, type, correlationId, data);
        events.add(event);

        // Persist for restart survival (best-effort; in-memory remains authoritative)
        try {
            TeipPersistenceStore.getInstance().saveEvent(event);
        } catch (Throwable ignored) {}

        return event;
    }

    public TeipEventsResponse getEvents(String executionId, long afterSequence, int limit) {
        if (executionId == null) {
            return new TeipEventsResponse("", Collections.emptyList(), false, 0);
        }
        List<TeipEvent> events = executionEvents.get(executionId);
        if (events == null || events.isEmpty()) {
            return new TeipEventsResponse(executionId, Collections.emptyList(), false, 0);
        }

        int maxLimit = limit <= 0 ? 50 : Math.min(limit, 500);
        List<TeipEvent> matched = new ArrayList<>();
        boolean hasMore = false;
        long nextSeq = afterSequence;

        synchronized (events) {
            long lastMatchedSeq = afterSequence;
            boolean anyMatched = false;
            for (TeipEvent evt : events) {
                if (evt.getSequence() > afterSequence) {
                    if (matched.size() < maxLimit) {
                        matched.add(evt);
                        lastMatchedSeq = evt.getSequence();
                        anyMatched = true;
                    } else {
                        hasMore = true;
                        break;
                    }
                }
            }
            if (anyMatched) {
                // TEIP 1.1 §4.5: nextSequence = last returned event's sequence + 1
                nextSeq = lastMatchedSeq + 1;
            } else {
                nextSeq = afterSequence;
            }
        }

        return new TeipEventsResponse(executionId, matched, hasMore, nextSeq);
    }

    public List<TeipEvent> getAllEvents(String executionId) {
        List<TeipEvent> list = executionEvents.get(executionId);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public void clear(String executionId) {
        executionEvents.remove(executionId);
        sequenceCounters.remove(executionId);
    }

    public void reset() {
        executionEvents.clear();
        sequenceCounters.clear();
        hydrated = false;
    }
}
