package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.workflow.EvolutionEvent.CausalityType;
import eu.kalafatic.evolution.controller.workflow.EvolutionEvent.EELType;
import eu.kalafatic.evolution.selfdev.genome.core.MetricArtifact;
import eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub;
import eu.kalafatic.evolution.selfdev.genome.repository.GenomeRepository;

/**
 * EvolutionaryObservabilityManager
 *
 * Pure transformation and enrichment layer.
 * Acts as a passive, read-only observer of existing system behavior.
 * Does NOT decide routing logic, interpret evolution outcomes, or steer the kernel.
 */
public class EvolutionaryObservabilityManager implements RuntimeEventListener {

    private final String sessionId;
    private final List<EvolutionEvent> eventLog = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<String>> causalChains = new ConcurrentHashMap<>();

    public EvolutionaryObservabilityManager(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onEvent(RuntimeEvent event) {
        if (!sessionId.equals(event.getSessionId())) return;

        EvolutionEvent eel = convertToEEL(event);
        if (eel != null) {
            enrichEEL(eel, event);
            eventLog.add(eel);
            performCausalLinking(eel, event);
        }
    }

    private EvolutionEvent convertToEEL(RuntimeEvent event) {
        EELType eelType;
        switch (event.getType().getCategory()) {
            case KERNEL:
            case FLOW:
                eelType = EELType.EVOLUTION;
                break;
            case FITNESS:
                eelType = EELType.FITNESS;
                break;
            case COGNITIVE:
                eelType = EELType.COGNITIVE;
                break;
            case EXECUTION:
                eelType = EELType.RUNTIME;
                break;
            case SYSTEM:
                eelType = EELType.SYSTEM;
                break;
            default:
                return null;
        }

        String taskId = (String) event.getMetadata().get("taskId");
        if (taskId == null && event.getType() == RuntimeEventType.TASK_STARTED) {
            taskId = event.getPayload() != null ? event.getPayload().toString() : null;
        }

        return new EvolutionEvent(
            eelType,
            event.getSource(),
            event.getSessionId(),
            (String) event.getMetadata().get("correlationId"),
            event.getIterationId(),
            taskId,
            (String) event.getMetadata().get("commitHash")
        );
    }

    private void enrichEEL(EvolutionEvent eel, RuntimeEvent event) {
        eel.setStateTransition((String) event.getMetadata().get("toState"));

        if (event.getType() == RuntimeEventType.TASK_FAILED || event.getType() == RuntimeEventType.COMMAND_FAILED) {
            eel.setSeverity("CRITICAL");
            eel.setCause(event.getPayload() != null ? event.getPayload().toString() : "Unknown failure");
        }

        if (event.getType() == RuntimeEventType.EVOLUTION_PROGRESS) {
            eel.setEvolutionSignal(event.getPayload() != null ? event.getPayload().toString() : null);
        }

        // Use existing metadata for impact if available
        Object impact = event.getMetadata().get("impact");
        if (impact != null) {
            eel.setImpact(impact.toString());
        }
    }

    private void performCausalLinking(EvolutionEvent eel, RuntimeEvent event) {
        routeToExistingSubsystems(eel, event);

        String correlationId = eel.getCorrelationId();
        String iterationId = eel.getIterationId();
        String taskId = eel.getTaskId();

        if (correlationId != null || taskId != null) {
            eel.setCausalityType(CausalityType.DIRECT);
            eel.setConfidenceScore(1.0);
            String key = correlationId != null ? correlationId : taskId;
            causalChains.computeIfAbsent(key, k -> new ArrayList<>()).add(eel.getTimestamp() + ":" + event.getType() + " [DIRECT]");
        } else if (iterationId != null) {
            eel.setCausalityType(CausalityType.STRUCTURAL);
            eel.setConfidenceScore(0.8);
            causalChains.computeIfAbsent(iterationId, k -> new ArrayList<>()).add(eel.getTimestamp() + ":" + event.getType() + " [STRUCTURAL]");
        } else {
            eel.setCausalityType(CausalityType.INFERRED);
            eel.setConfidenceScore(0.3);
        }
    }

    public List<EvolutionEvent> getEventLog() {
        return new ArrayList<>(eventLog);
    }

    public Map<String, List<String>> getCausalChains() {
        return new ConcurrentHashMap<>(causalChains);
    }

    public List<EvolutionEvent> getTimeline() {
        return eventLog.stream()
            .sorted((e1, e2) -> Long.compare(e1.getTimestamp(), e2.getTimestamp()))
            .collect(Collectors.toList());
    }

    private void routeToExistingSubsystems(EvolutionEvent eel, RuntimeEvent event) {
        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        if (session == null) return;

        // Route to EvolutionRegistry (Existing Signal aggregation)
        if (eel.getEventType() == EELType.FITNESS) {
            session.getEvolutionRegistry().processEvent(event, "default-trajectory");
        }

        // Aggregate signals for Genome
        if (event.getType() == RuntimeEventType.STABILITY_SIGNAL) {
             Object payload = event.getPayload();
             if (payload instanceof Double) {
                 updateGenomeSignals("stability", (Double) payload);
             }
        }

        if (event.getType() == RuntimeEventType.MUTATION_SUCCESS) {
             updateGenomeSignals("mutation_success", 1.0);
        }

        // Failure cluster detection for Supervisor
        if (event.getType() == RuntimeEventType.TASK_FAILED) {
            detectFailureClusters(eel);
        }
    }

    private void updateGenomeSignals(String name, double value) {
        // Genome Registry integration
        // Only derived signals, no raw logs.
        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        if (session != null) {
            GenomeRepository repo = null;
            // Best-effort discovery of genome repository via hub or registry
            try {
                SelfDevGenomeHub hub = (SelfDevGenomeHub)
                    session.getCapabilityRegistry().getContractImplementation("genome.hub", Object.class);
                if (hub != null) repo = hub.getRepository();
            } catch (Exception e) {}

            if (repo != null) {
                MetricArtifact metric =
                    new MetricArtifact(
                        "metric-" + System.currentTimeMillis(), name, value);
                metric.setSourceProject(sessionId);
                repo.save(metric);
            }
        }
    }

    private void detectFailureClusters(EvolutionEvent eel) {
        long clusterWindow = 60000; // 1 minute
        long now = System.currentTimeMillis();

        List<EvolutionEvent> recentFailures = eventLog.stream()
            .filter(e -> e.getEventType() == EELType.RUNTIME && "CRITICAL".equals(e.getSeverity()))
            .filter(e -> (now - e.getTimestamp()) < clusterWindow)
            .collect(Collectors.toList());

        if (recentFailures.size() >= 3) {
            // Publish cluster event
            SessionContainer session = SessionManager.getInstance().getSession(sessionId);
            if (session != null) {
                session.getEventBus().publish(new RuntimeEvent(RuntimeEventType.FAILURE_CLUSTER_DETECTED, sessionId, "ObservabilityManager", "Cluster of " + recentFailures.size() + " failures detected."));
            }
        }
    }

    public Map<String, Object> getSummaries() {
        Map<String, Object> summaries = new ConcurrentHashMap<>();
        summaries.put("totalEvents", eventLog.size());
        summaries.put("failureCount", eventLog.stream().filter(e -> "CRITICAL".equals(e.getSeverity())).count());
        return summaries;
    }

    public eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService getMemoryService() {
        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        if (session != null) {
            // Observability manager is passive, so we use a null project root check
            // or retrieve it from the session context if possible.
            // In most cases, the SessionContext already has the memory service initialized.
            return session.getMemoryService(null);
        }
        return null;
    }
}
