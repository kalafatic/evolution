package eu.kalafatic.evolution.tests.iterative;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

public class DarwinEvolutionSimulation {

    private static final int K = 100;
    private static final int NUM_WORDS = 1_000_000;
    private static final List<String> INPUT_DATA = generateData(NUM_WORDS);

    private static List<String> generateData(int n) {
        List<String> data = new ArrayList<>(n);
        Random r = new Random(42);
        for (int i = 0; i < n; i++) {
            data.add("word" + r.nextInt(1000));
        }
        return data;
    }

    public interface TopKExtractor {
        List<String> extract(List<String> words, int k);
    }

    // V1: Naive Sort
    public static class V1_NaiveSort implements TopKExtractor {
        @Override
        public List<String> extract(List<String> words, int k) {
            Map<String, Integer> counts = new HashMap<>();
            for (String w : words) {
                counts.put(w, counts.getOrDefault(w, 0) + 1);
            }
            List<String> list = new ArrayList<>(counts.keySet());
            list.sort((a, b) -> {
                int cmp = counts.get(b).compareTo(counts.get(a));
                return cmp != 0 ? cmp : a.compareTo(b);
            });
            return list.subList(0, Math.min(k, list.size()));
        }
    }

    // V1: Priority Queue
    public static class V1_PriorityQueue implements TopKExtractor {
        @Override
        public List<String> extract(List<String> words, int k) {
            Map<String, Integer> counts = new HashMap<>();
            for (String w : words) {
                counts.put(w, counts.getOrDefault(w, 0) + 1);
            }
            PriorityQueue<String> pq = new PriorityQueue<>((a, b) -> {
                int cmp = counts.get(b).compareTo(counts.get(a));
                return cmp != 0 ? cmp : a.compareTo(b);
            });
            pq.addAll(counts.keySet());
            List<String> result = new ArrayList<>();
            for (int i = 0; i < k && !pq.isEmpty(); i++) {
                result.add(pq.poll());
            }
            return result;
        }
    }

    // V1: Stream API
    public static class V1_StreamAPI implements TopKExtractor {
        @Override
        public List<String> extract(List<String> words, int k) {
            return words.stream()
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = b.getValue().compareTo(a.getValue());
                    return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
                })
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
    }

    public static void main(String[] args) {
        DarwinEvolutionSimulation sim = new DarwinEvolutionSimulation();
        sim.runIteration1();
        sim.runIteration2();
        sim.runIteration3();
    }

    // V2: Min-Heap of size K (O(N log K))
    public static class V2_MinHeapSizeK implements TopKExtractor {
        @Override
        public List<String> extract(List<String> words, int k) {
            Map<String, Integer> counts = new HashMap<>();
            for (String w : words) {
                counts.put(w, counts.getOrDefault(w, 0) + 1);
            }
            PriorityQueue<String> pq = new PriorityQueue<>((a, b) -> {
                int cmp = counts.get(a).compareTo(counts.get(b));
                return cmp != 0 ? cmp : b.compareTo(a);
            });
            for (String w : counts.keySet()) {
                pq.offer(w);
                if (pq.size() > k) {
                    pq.poll();
                }
            }
            List<String> result = new ArrayList<>();
            while (!pq.isEmpty()) {
                result.add(pq.poll());
            }
            Collections.reverse(result);
            return result;
        }
    }

    // V2: Parallel Stream
    public static class V2_ParallelStream implements TopKExtractor {
        @Override
        public List<String> extract(List<String> words, int k) {
            return words.parallelStream()
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = b.getValue().compareTo(a.getValue());
                    return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
                })
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
    }

    @Test
    public void runIteration1() {
        System.out.println("--- Iteration 1 ---");
        evaluate("V1_NaiveSort", new V1_NaiveSort());
        evaluate("V1_PriorityQueue", new V1_PriorityQueue());
        evaluate("V1_StreamAPI", new V1_StreamAPI());
    }

    @Test
    public void runIteration2() {
        System.out.println("--- Iteration 2 ---");
        evaluate("V1_PriorityQueue", new V1_PriorityQueue()); // Baseline
        evaluate("V2_MinHeapSizeK", new V2_MinHeapSizeK());
        evaluate("V2_ParallelStream", new V2_ParallelStream());
    }

    // V3: Parallel Stream + Merge (O(N) for counting)
    public static class V3_ParallelStreamMerge implements TopKExtractor {
        @Override
        public List<String> extract(List<String> words, int k) {
            return words.parallelStream()
                .collect(Collectors.toConcurrentMap(w -> w, w -> 1L, Long::sum))
                .entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = b.getValue().compareTo(a.getValue());
                    return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
                })
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
    }

    @Test
    public void runIteration3() {
        System.out.println("--- Iteration 3 ---");
        evaluate("V2_ParallelStream", new V2_ParallelStream()); // Baseline
        evaluate("V3_ParallelStreamMerge", new V3_ParallelStreamMerge());
    }

    private void evaluate(String name, TopKExtractor extractor) {
        // Warmup
        for (int i = 0; i < 5; i++) {
            extractor.extract(INPUT_DATA, K);
        }
        long start = System.currentTimeMillis();
        List<String> result = extractor.extract(INPUT_DATA, K);
        long end = System.currentTimeMillis();
        long duration = end - start;

        // Correctness check (against known good result or first variant)
        // For simplicity, we compare all against NaiveSort
        List<String> expected = new V1_NaiveSort().extract(INPUT_DATA, K);
        boolean correct = result.equals(expected);

        double fitness = (correct ? 1000.0 : 0.0) / (duration + 1);

        System.out.printf("Variant: %s | Time: %d ms | Correct: %b | Fitness: %.4f%n",
            name, duration, correct, fitness);
    }
}
