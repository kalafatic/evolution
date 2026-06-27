package eu.kalafatic.evolution.controller.trajectory;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.utils.semantic.EvolutionComponent;
import eu.kalafatic.utils.semantic.EvolutionaryImpact;
import eu.kalafatic.utils.semantic.Stability;

/**
 * Central system backplane for orchestration signals.
 * All proposal intelligence flows through this channel.
 */
@EvolutionComponent(
    domain = "trajectory",
    role = "signal-backplane",
    purpose = "Passive propagation and archival layer for orchestration signals",
    stability = Stability.STABLE,
    evolutionaryImpact = EvolutionaryImpact.HIGH
)
public class SignalBus {
    private final Map<String, List<EvaluationSignal>> signalHistory = new ConcurrentHashMap<>();
    private final RuntimeEventBus eventBus;

    public SignalBus(RuntimeEventBus eventBus) {
        this.eventBus = eventBus;
        // Subscribe to standardized evaluation signals from the provided event bus
        this.eventBus.subscribe(event -> {
            if (event.getType() == RuntimeEventType.EVALUATION_SIGNAL_CREATED && event.getPayload() instanceof EvaluationSignal) {
                publish((EvaluationSignal) event.getPayload());
            }
        });
    }

    /**
     * Publishes a signal to the bus and archives it in history.
     */
    public void publish(EvaluationSignal signal) {
        String variantId = signal.getVariantId();
        List<EvaluationSignal> history = signalHistory.computeIfAbsent(variantId, k -> Collections.synchronizedList(new ArrayList<>()));

        // Signal Deduplication
        boolean duplicate = history.stream().anyMatch(s ->
            s.getEvaluatorId().equals(signal.getEvaluatorId()) &&
            s.getExplanation().equals(signal.getExplanation()) &&
            Math.abs(s.getScore() - signal.getScore()) < 0.01
        );

        if (!duplicate) {
            history.add(signal);
        }
    }

    public List<EvaluationSignal> getSignalsForVariant(String variantId) {
        return new ArrayList<>(signalHistory.getOrDefault(variantId, Collections.emptyList()));
    }

    public List<EvaluationSignal> getAllSignals() {
        return signalHistory.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public void clearHistory() {
        signalHistory.clear();
    }
}
