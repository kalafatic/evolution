package eu.kalafatic.evolution.forge.controller.service;

import eu.kalafatic.evolution.forge.controller.api.TrainingStatus;

public interface TrainingService {
    void startTraining(String sessionId);
    void pauseTraining(String sessionId);
    void stopTraining(String sessionId);
    TrainingStatus getStatus(String sessionId);
}
