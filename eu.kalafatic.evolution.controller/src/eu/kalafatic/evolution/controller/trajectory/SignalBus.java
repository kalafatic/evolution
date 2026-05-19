package eu.kalafatic.evolution.controller.trajectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Central system backplane for orchestration signals.
 * All proposal intelligence flows through this channel.
 *
 * <p><b>ARCHITECTURAL INVARIANT: PROPAGATION ONLY</b></p>
 * The SignalBus is a passive propagation and archival layer. It distributes
 * telemetry and signals but MUST NOT make decisions, decide outcomes,
 * or trigger variant activation. It serves as the data source for the DecisionResolver.
 */
@EvolutionComponent(
    domain = "trajectory",
    role = "signal-backplane",
    purpose = "Passive propagation and archival layer for orchestration signals",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.HIGH
)
public class SignalBus {
    private static final SignalBus INSTANCE = new SignalBus();

    private final Map<String, List<EvaluationSignal>> signalHistory = new ConcurrentHashMap<>();

    private SignalBus() {
        // Subscribe to standardized evaluation signals from the main event bus
        RuntimeEventBus.getInstance().subscribe(event -> {
            if (event.getType() == RuntimeEventType.EVALUATION_SIGNAL_CREATED && event.getPayload() instanceof EvaluationSignal) {
                publish((EvaluationSignal) event.getPayload());
            }
        });
    }

    public static SignalBus getInstance() {
        return INSTANCE;
    }

    /**
     * Publishes a signal to the bus and archives it in history.
     * Includes P1 Signal Quality: Scoped signals and Deduplication.
     */
    public void publish(EvaluationSignal signal) {
        String variantId = signal.getVariantId();
        List<EvaluationSignal> history = signalHistory.computeIfAbsent(variantId, k -> Collections.synchronizedList(new ArrayList<>()));

        // Signal Deduplication (P1)
        boolean duplicate = history.stream().anyMatch(s ->
            s.getEvaluatorId().equals(signal.getEvaluatorId()) &&
            s.getExplanation().equals(signal.getExplanation()) &&
            Math.abs(s.getScore() - signal.getScore()) < 0.01
        );

        if (!duplicate) {
            history.add(signal);
        }

        // Also ensure it's on the main event bus if it wasn't already
        // (to avoid infinite recursion we check source if needed,
        // but RuntimeEventBus handles subscribers independently)
    }

    /**
     * Retrieves all signals for a specific variant.
     */
    public List<EvaluationSignal> getSignalsForVariant(String variantId) {
        return new ArrayList<>(signalHistory.getOrDefault(variantId, Collections.emptyList()));
    }

    /**
     * Retrieves all signals collected in the current session.
     */
    public List<EvaluationSignal> getAllSignals() {
        return signalHistory.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Clears signal history for a new iteration if needed.
     */
    public void clearHistory() {
        signalHistory.clear();
    }
}
