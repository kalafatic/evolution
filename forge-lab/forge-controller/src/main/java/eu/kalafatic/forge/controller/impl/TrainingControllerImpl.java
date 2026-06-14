package eu.kalafatic.forge.controller.impl;

import eu.kalafatic.forge.controller.api.TrainingController;
import eu.kalafatic.forge.controller.api.TrainingStatus;
import eu.kalafatic.forge.controller.service.TrainingService;

public class TrainingControllerImpl implements TrainingController {
    private final TrainingService trainingService;

    public TrainingControllerImpl(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Override
    public void startTraining(String sessionId) {
        trainingService.startTraining(sessionId);
    }

    @Override
    public void pauseTraining(String sessionId) {
        trainingService.pauseTraining(sessionId);
    }

    @Override
    public void stopTraining(String sessionId) {
        trainingService.stopTraining(sessionId);
    }

    @Override
    public TrainingStatus getStatus(String sessionId) {
        return trainingService.getStatus(sessionId);
    }
}
