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
 * Thread-safe sequenced event store for TEIP 1.0 executions.
 * Supports cursor pagination, replay, and bounded retention.
 */
public class TeipEventStore {
    private static final TeipEventStore INSTANCE = new TeipEventStore();

    // Map: executionId -> List of TeipEvents
    private final Map<String, List<TeipEvent>> executionEvents = new ConcurrentHashMap<>();
    // Map: executionId -> sequence generator
    private final Map<String, AtomicLong> sequenceCounters = new ConcurrentHashMap<>();

    private TeipEventStore() {}

    public static TeipEventStore getInstance() {
        return INSTANCE;
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
            for (TeipEvent evt : events) {
                if (evt.getSequence() > afterSequence) {
                    if (matched.size() < maxLimit) {
                        matched.add(evt);
                        nextSeq = evt.getSequence();
                    } else {
                        hasMore = true;
                        break;
                    }
                }
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
    }
}
