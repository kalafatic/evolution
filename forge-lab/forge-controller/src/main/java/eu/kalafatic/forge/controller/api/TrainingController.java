package eu.kalafatic.forge.controller.api;

import java.util.List;
import java.util.Map;

public interface TrainingController {
    void startTraining(String sessionId);
    void pauseTraining(String sessionId);
    void stopTraining(String sessionId);
    TrainingStatus getTrainingStatus(String sessionId);
    Map<String, Object> getTrainingMetrics(String sessionId);
    List<String> getRecentEvents(String sessionId);
}
