package eu.kalafatic.forge.controller.service;

import eu.kalafatic.forge.controller.api.TrainingStatus;

public interface TrainingService {
    void startTraining(String sessionId);
    void pauseTraining(String sessionId);
    void stopTraining(String sessionId);
    TrainingStatus getStatus(String sessionId);
}
