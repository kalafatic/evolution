package eu.kalafatic.evolution.forge.data.api.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

import java.util.List;

/**
 * Common polymorphic source adapter abstraction converting format-specific dataset sources into EVO native NormalizedSample instances.
 */
public interface DatasetSourceAdapter {

    /**
     * Checks whether this adapter supports converting the given dataset item.
     */
    boolean supports(DatasetItem item);

    /**
     * Inspects the dataset item and returns format metadata and readiness info.
     */
    DatasetInspection inspect(DatasetItem item);

    /**
     * Converts the dataset item into native NormalizedSample instances.
     */
    List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception;
}
