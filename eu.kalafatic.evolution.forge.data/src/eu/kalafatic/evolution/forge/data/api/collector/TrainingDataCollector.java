package eu.kalafatic.evolution.forge.data.api.collector;

/**
 * Common abstract base class for all training-data collectors.
 */
public abstract class TrainingDataCollector {

    /**
     * Returns the unique collector type.
     */
    public abstract CollectorType getType();

    /**
     * Returns the human-readable display name of the collector.
     */
    public abstract String getName();

    /**
     * Executes data collection based on the provided collection context.
     *
     * @param context Context parameters including project directory, paths, queries, and options.
     * @return CollectionResult containing status, items, duration, and optional errors.
     */
    public abstract CollectionResult collect(CollectionContext context);
}
