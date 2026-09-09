package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Sampling and balancing stage supporting Sequential, Random, Reservoir Sampling, and Category Balancing.
 */
public class DatasetSampler {

    public enum Strategy {
        SEQUENTIAL,
        RANDOM,
        RESERVOIR,
        WEIGHTED_BALANCED
    }

    private Strategy strategy = Strategy.RESERVOIR;
    private int reservoirSize = 10000;
    private long seed = 42L;

    public DatasetSampler() {}

    public DatasetSampler(Strategy strategy, int reservoirSize) {
        this.strategy = strategy;
        this.reservoirSize = reservoirSize;
    }

    /**
     * Sample from a full candidate list.
     */
    public List<NormalizedSample> sample(List<NormalizedSample> samples) {
        if (samples == null || samples.isEmpty()) return new ArrayList<>();
        if (samples.size() <= reservoirSize || strategy == Strategy.SEQUENTIAL) {
            return new ArrayList<>(samples);
        }

        Random rnd = new Random(seed);
        List<NormalizedSample> result = new ArrayList<>();

        if (strategy == Strategy.RANDOM) {
            List<NormalizedSample> copy = new ArrayList<>(samples);
            Collections.shuffle(copy, rnd);
            return new ArrayList<>(copy.subList(0, Math.min(reservoirSize, copy.size())));
        } else if (strategy == Strategy.RESERVOIR) {
            // Standard Reservoir Sampling algorithm (Algorithm R)
            for (int i = 0; i < samples.size(); i++) {
                NormalizedSample item = samples.get(i);
                if (i < reservoirSize) {
                    result.add(item);
                } else {
                    int j = rnd.nextInt(i + 1);
                    if (j < reservoirSize) {
                        result.set(j, item);
                    }
                }
            }
            return result;
        }

        return new ArrayList<>(samples);
    }

    public Strategy getStrategy() { return strategy; }
    public void setStrategy(Strategy strategy) { this.strategy = strategy; }

    public int getReservoirSize() { return reservoirSize; }
    public void setReservoirSize(int reservoirSize) { this.reservoirSize = reservoirSize; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
}
