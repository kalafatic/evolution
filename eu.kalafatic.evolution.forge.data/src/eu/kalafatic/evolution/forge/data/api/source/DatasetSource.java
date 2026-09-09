package eu.kalafatic.evolution.forge.data.api.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import java.io.Closeable;
import java.util.Iterator;

/**
 * Interface representing a source of training samples (e.g., Local files, Hugging Face).
 * Supports bounded streaming over samples.
 */
public interface DatasetSource extends Closeable, Iterable<NormalizedSample> {

    String getSourceName();

    DatasetSourceConfig getConfig();

    void initialize() throws Exception;

    boolean hasNext();

    NormalizedSample next();

    @Override
    default Iterator<NormalizedSample> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return DatasetSource.this.hasNext();
            }

            @Override
            public NormalizedSample next() {
                return DatasetSource.this.next();
            }
        };
    }

    DatasetSourceStats getStats();

    @Override
    default void close() {
        // Default no-op
    }
}
