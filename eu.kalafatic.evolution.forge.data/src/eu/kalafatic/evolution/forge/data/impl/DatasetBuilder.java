package eu.kalafatic.evolution.forge.data.impl;

import java.util.ArrayList;
import java.util.List;

public class DatasetBuilder {
    public static class Sample {
        public List<Integer> input;
        public Integer target;

        public Sample(List<Integer> input, Integer target) {
            this.input = input;
            this.target = target;
        }
    }

    public List<Sample> buildSlidingWindow(List<Integer> tokens, int windowSize, int stride) {
        return buildSlidingWindow(tokens, windowSize, stride, Integer.MAX_VALUE);
    }

    public List<Sample> buildSlidingWindow(List<Integer> tokens, int windowSize, int stride, int maxSamples) {
        List<Sample> samples = new ArrayList<>();
        if (tokens == null || tokens.size() <= windowSize) {
            return samples;
        }
        for (int i = 0; i < tokens.size() - windowSize; i += stride) {
            if (samples.size() >= maxSamples) {
                break;
            }
            List<Integer> input = new ArrayList<>(tokens.subList(i, i + windowSize));
            Integer target = tokens.get(i + windowSize);
            samples.add(new Sample(input, target));
        }
        return samples;
    }
}
