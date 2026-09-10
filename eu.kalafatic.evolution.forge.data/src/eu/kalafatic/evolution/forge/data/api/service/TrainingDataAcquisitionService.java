package eu.kalafatic.evolution.forge.data.api.service;

/**
 * Application-level service interface for acquiring and processing training datasets.
 * Coordinates sources, normalizers, quality filters, deduplication, size accounting,
 * and minimum usable content byte requirement enforcement.
 */
public interface TrainingDataAcquisitionService {

    /**
     * Executes training data acquisition across configured sources until the requested minimum usable content size is reached or sources are exhausted.
     *
     * @param request configuration specifying sources, target usable bytes, and parameters
     * @return result containing accepted samples, size metrics, status, and coverage details
     * @throws Exception if an unrecoverable failure occurs
     */
    TrainingDataAcquisitionResult acquireDataset(TrainingDataAcquisitionRequest request) throws Exception;
}
