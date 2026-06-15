package eu.kalafatic.evolution.forge.controller.impl;

import eu.kalafatic.evolution.forge.controller.api.TrainingController;
import eu.kalafatic.evolution.forge.controller.api.TrainingStatus;
import eu.kalafatic.evolution.forge.controller.service.TrainingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainingControllerImpl implements TrainingController {
    private final TrainingService trainingService;

    public TrainingControllerImpl(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Override
    public void startTraining(String sessionId) {
        if (trainingService != null) trainingService.startTraining(sessionId);
    }

    @Override
    public void pauseTraining(String sessionId) {
        if (trainingService != null) trainingService.pauseTraining(sessionId);
    }

    @Override
    public void stopTraining(String sessionId) {
        if (trainingService != null) trainingService.stopTraining(sessionId);
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
}
