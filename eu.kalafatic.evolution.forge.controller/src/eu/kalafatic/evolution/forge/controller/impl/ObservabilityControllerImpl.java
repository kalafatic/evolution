package eu.kalafatic.evolution.forge.controller.impl;

import eu.kalafatic.evolution.forge.controller.api.ObservabilityController;
import eu.kalafatic.evolution.forge.controller.api.ObservabilityEventType;
import eu.kalafatic.evolution.forge.controller.service.EvolutionService;
import eu.kalafatic.evolution.forge.observability.api.EventTracker;
import eu.kalafatic.evolution.forge.observability.api.ForgeEvent;

public class ObservabilityControllerImpl implements ObservabilityController {
    private final EventTracker eventTracker;
    private final EvolutionService evolutionService;

    public ObservabilityControllerImpl(EventTracker eventTracker, EvolutionService evolutionService) {
        this.eventTracker = eventTracker;
        this.evolutionService = evolutionService;
    }

    @Override
    public void trackEvent(String sessionId, String eventType, String data) {
        if (eventTracker != null) {
            // Mapping domain event to observability event
            ForgeEvent fEvent = ForgeEvent.TRAINING_STEP;
            if (eventType.equals(ObservabilityEventType.TRAINING_METRIC.name())) {
                fEvent = ForgeEvent.LOSS_UPDATED;
            }

            eventTracker.track(fEvent, sessionId, data);

            try {
                ObservabilityEventType type = ObservabilityEventType.valueOf(eventType);
                switch (type) {
                    case TRAINING_METRIC:
                        if (data != null && data.contains("plateau")) {
                            triggerEvolution(sessionId, "active-model");
                        }
                        break;
                    case RUNTIME_FEEDBACK:
                        // Queue for future batch evaluation
                        break;
                    case USER_CORRECTION:
                        // Direct dataset update trigger
                        break;
                    case SYSTEM_ALERT:
                        // High priority stabilization check
                        break;
                }
            } catch (IllegalArgumentException e) {
                // Ignore unknown event types
            }
        }
    }

    @Override
    public void triggerEvolution(String sessionId, String modelId) {
        if (evolutionService != null) {
            evolutionService.evolve(sessionId, modelId);
        }
    }
}
