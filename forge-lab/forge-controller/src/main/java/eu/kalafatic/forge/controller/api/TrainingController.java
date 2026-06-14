package eu.kalafatic.forge.controller.api;

public interface TrainingController {

    void startTraining(
            String sessionId);

    void pauseTraining(
            String sessionId);

    void stopTraining(
            String sessionId);

    TrainingStatus getStatus(
            String sessionId);
}
