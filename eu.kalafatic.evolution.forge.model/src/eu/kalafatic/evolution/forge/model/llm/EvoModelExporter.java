package eu.kalafatic.evolution.forge.model.llm;

import java.nio.file.Path;

/**
 * Interface representing a model exporter format/target.
 */
public interface EvoModelExporter {
    /**
     * Exports the native model artifact to the specified output directory.
     *
     * @param model the self-contained native model artifact
     * @param outputDirectory the target folder where the exported files should be written
     * @throws Exception if any export or serialization error occurs
     */
    void export(EvoModelArtifact model, Path outputDirectory) throws Exception;
}
