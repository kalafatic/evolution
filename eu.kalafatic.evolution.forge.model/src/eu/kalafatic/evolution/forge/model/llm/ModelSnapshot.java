package eu.kalafatic.evolution.forge.model.llm;

import java.io.Serializable;
import java.util.Map;

/**
 * Immutable canonical model snapshot interface representing the export boundary.
 */
public interface ModelSnapshot extends Serializable {

    String getFormat();

    int getFormatVersion();

    EvoLlmArchitecture getArchitecture();

    ModelParameters getParameters();

    Map<Integer, String> getVocabulary();

    TrainingState getTrainingState();

    ModelMetadata getMetadata();
}
