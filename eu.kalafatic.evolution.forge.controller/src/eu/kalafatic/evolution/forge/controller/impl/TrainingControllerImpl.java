package eu.kalafatic.evolution.forge.controller.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

import eu.kalafatic.evolution.forge.controller.api.TrainingController;
import eu.kalafatic.evolution.forge.controller.api.TrainingStatus;
import eu.kalafatic.evolution.forge.controller.service.TrainingService;

public class TrainingControllerImpl implements TrainingController {
    private final TrainingService trainingService;

    public TrainingControllerImpl(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Override
    public void startTraining(String sessionId) {
        try {
            if (trainingService != null) trainingService.startTraining(sessionId);
            publishEvent(sessionId, "FORGE_TRAINING_STARTED", "Training started");
        } catch (Exception e) {
            publishEvent(sessionId, "FORGE_TRAINING_FAILED", e.getMessage());
            throw e;
        }
    }

    @Override
    public void pauseTraining(String sessionId) {
        if (trainingService != null) trainingService.pauseTraining(sessionId);
        publishEvent(sessionId, "VIEW_UPDATED", "Training paused");
    }

    @Override
    public void stopTraining(String sessionId) {
        if (trainingService != null) trainingService.stopTraining(sessionId);
        publishEvent(sessionId, "FORGE_TRAINING_STOPPED", "Training stopped");
    }

    @Override
    public void configureTraining(String sessionId, String configJson) {
        // Configuration logic would normally be in trainingService
        publishEvent(sessionId, "FORGE_TRAINING_CONFIGURED", configJson);
    }

    @Override
    public TrainingStatus getTrainingStatus(String sessionId) {
        if (trainingService == null) return TrainingStatus.IDLE;
        return trainingService.getTrainingStatus(sessionId);
    }

    @Override
    public Map<String, Object> getTrainingMetrics(String sessionId) {
        if (trainingService == null) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("loss", 0.5);
            metrics.put("acc", 0.8);
            return metrics;
        }
        return trainingService.getTrainingMetrics(sessionId);
    }

    @Override
    public List<String> getRecentEvents(String sessionId) {
        if (trainingService == null) {
            List<String> events = new ArrayList<>();
            events.add("14:22:01 Training Started");
            events.add("14:22:15 Epoch 1 Complete");
            events.add("14:22:18 Loss = 0.91");
            return events;
        }
        return trainingService.getRecentEvents(sessionId);
    }

    private void publishEvent(String sessionId, String typeName, Object payload) {
        try {
            Class<?> sessionManagerClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.SessionManager");
            Method getInstance = sessionManagerClass.getMethod("getInstance");
            Object sm = getInstance.invoke(null);

            Method getSession = sm.getClass().getMethod("getSession", String.class);
            Object session = getSession.invoke(sm, sessionId);

            if (session != null) {
                Method getEventBus = session.getClass().getMethod("getEventBus");
                Object bus = getEventBus.invoke(session);

                if (bus != null) {
                    Class<?> eventTypeClass = Class.forName("eu.kalafatic.evolution.controller.workflow.RuntimeEventType");
                    Object type = Enum.valueOf((Class<Enum>)eventTypeClass, typeName);

                    Class<?> eventClass = Class.forName("eu.kalafatic.evolution.controller.workflow.RuntimeEvent");
                    Object event = eventClass.getConstructor(eventTypeClass, String.class, String.class, Object.class)
                                             .newInstance(type, sessionId, "TrainingController", payload);

                    Method publish = bus.getClass().getMethod("publish", eventClass);
                    publish.invoke(bus, event);
                }
            }
        } catch (Exception e) {
            // Decoupled: fail silently if controller bundle is not present
        }
    }
}
