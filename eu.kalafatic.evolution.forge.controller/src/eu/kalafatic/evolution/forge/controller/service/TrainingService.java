package eu.kalafatic.evolution.forge.controller.service;

import eu.kalafatic.evolution.forge.controller.api.TrainingStatus;
import java.util.List;
import java.util.Map;

public interface TrainingService {
    void startTraining(String sessionId);
    void pauseTraining(String sessionId);
    void stopTraining(String sessionId);
    TrainingStatus getTrainingStatus(String sessionId);
    Map<String, Object> getTrainingMetrics(String sessionId);
    List<String> getRecentEvents(String sessionId);
}
