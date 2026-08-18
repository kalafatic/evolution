package eu.kalafatic.evolution.forge.data.impl.collector;

import java.util.List;

/**
 * Adapter that converts prepared dataset records into plain text corpus consumed by llmdarwinengine.
 */
public class PreparedDatasetAdapter {

    /**
     * Converts a list of PreparedRecords from the data preparation pipeline into a corpus string.
     */
    public static String toCorpus(List<DataPreparationPipeline.PreparedRecord> records) {
        if (records == null || records.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DataPreparationPipeline.PreparedRecord record : records) {
            if (record.getInstruction() != null && !record.getInstruction().isEmpty()) {
                sb.append(record.getInstruction()).append("\n");
            }
            if (record.getResponse() != null && !record.getResponse().isEmpty()) {
                sb.append(record.getResponse()).append("\n\n");
            }
        }
        return sb.toString();
    }
}
