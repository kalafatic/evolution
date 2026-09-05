package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.math.core.SimpleTensor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default immutable implementation of {@link ModelSnapshot}.
 * Guarantees that parameter tensor data is safely copied and frozen so subsequent model updates
 * during training or mutation do not modify the snapshot.
 */
public class DefaultModelSnapshot implements ModelSnapshot {

    private static final long serialVersionUID = 1L;

    private final String format;
    private final int formatVersion;
    private final EvoLlmArchitecture architecture;
    private final ModelParameters parameters;
    private final Map<Integer, String> vocabulary;
    private final TrainingState trainingState;
    private final ModelMetadata metadata;

    public DefaultModelSnapshot(String format,
                                int formatVersion,
                                EvoLlmArchitecture architecture,
                                ModelParameters rawParameters,
                                Map<Integer, String> vocabulary,
                                TrainingState trainingState,
                                ModelMetadata metadata) {
        this.format = format != null ? format : "EVO_NATIVE";
        this.formatVersion = formatVersion > 0 ? formatVersion : 1;
        this.architecture = Objects.requireNonNull(architecture, "architecture cannot be null");

        // Deep copy all parameter tensors to ensure snapshot immutability
        DefaultModelParameters frozenParams = new DefaultModelParameters();
        if (rawParameters != null) {
            for (String name : rawParameters.names()) {
                Tensor t = rawParameters.get(name);
                if (t != null) {
                    SimpleTensor copy = new SimpleTensor(t.getShape().clone());
                    System.arraycopy(t.getData(), 0, copy.getData(), 0, t.getData().length);
                    frozenParams.register(name, copy);
                }
            }
        }
        this.parameters = frozenParams;

        this.vocabulary = vocabulary != null ? Collections.unmodifiableMap(new LinkedHashMap<>(vocabulary)) : Collections.emptyMap();

        if (trainingState != null) {
            this.trainingState = new TrainingState(
                    trainingState.getEpoch(),
                    trainingState.getStep(),
                    trainingState.getLastLoss(),
                    trainingState.getLearningRate(),
                    trainingState.getTotalTrainedTokens()
            );
            this.trainingState.setTimestamp(trainingState.getTimestamp());
        } else {
            this.trainingState = new TrainingState();
        }

        if (metadata != null) {
            ModelMetadata metaCopy = new ModelMetadata(metadata.getName(), metadata.getVersion(), metadata.getArchitectureName());
            metaCopy.setCreatedAt(metadata.getCreatedAt());
            for (Map.Entry<String, String> entry : metadata.getAttributes().entrySet()) {
                metaCopy.setAttribute(entry.getKey(), entry.getValue());
            }
            this.metadata = metaCopy;
        } else {
            this.metadata = new ModelMetadata("evo_model", "1.0.0", "evo_llm");
        }
    }

    @Override public String getFormat() { return format; }
    @Override public int getFormatVersion() { return formatVersion; }
    @Override public EvoLlmArchitecture getArchitecture() { return architecture; }
    @Override public ModelParameters getParameters() { return parameters; }
    @Override public Map<Integer, String> getVocabulary() { return vocabulary; }
    @Override public TrainingState getTrainingState() { return trainingState; }
    @Override public ModelMetadata getMetadata() { return metadata; }
}
