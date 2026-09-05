package eu.kalafatic.evolution.forge.model.llm;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interface representing a model exporter format/target.
 * Exporters consume canonical ModelSnapshot instances as their primary input boundary.
 */
public interface EvoModelExporter {

    /**
     * Exports the model snapshot to the specified target directory.
     *
     * @param snapshot the canonical model snapshot representing model state
     * @param outputDirectory the target folder where exported files are written
     * @throws Exception if export fails
     */
    default void exportSnapshot(ModelSnapshot snapshot, Path outputDirectory) throws Exception {
        if (snapshot == null) {
            throw new IllegalArgumentException("ModelSnapshot cannot be null");
        }
        EvoModelArtifact artifact = new EvoModelArtifact();
        EvoLlmModel model = new EvoLlmModel(snapshot.getArchitecture());
        if (snapshot.getVocabulary() != null) {
            model.getIdToToken().putAll(snapshot.getVocabulary());
            Map<String, Integer> tokenToId = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> entry : snapshot.getVocabulary().entrySet()) {
                tokenToId.put(entry.getValue(), entry.getKey());
            }
            artifact.initializeFromModel(snapshot.getMetadata() != null ? snapshot.getMetadata().getName() : "evo", model, tokenToId);
        } else {
            artifact.initializeFromModel(snapshot.getMetadata() != null ? snapshot.getMetadata().getName() : "evo", model, Collections.emptyMap());
        }
        export(artifact, outputDirectory);
    }

    /**
     * Exports the native model artifact to the specified output directory.
     *
     * @param model the self-contained native model artifact
     * @param outputDirectory the target folder where the exported files should be written
     * @throws Exception if any export or serialization error occurs
     */
    void export(EvoModelArtifact model, Path outputDirectory) throws Exception;
}
