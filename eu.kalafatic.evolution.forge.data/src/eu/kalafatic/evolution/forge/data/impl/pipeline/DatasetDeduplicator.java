package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.processor.DataDeduplicator;

import java.util.HashSet;
import java.util.Set;

/**
 * Deduplication stage supporting exact SHA-256 hash matching and MinHash/shingle near-duplicate filtering.
 */
public class DatasetDeduplicator implements DataDeduplicator {

    private final Set<String> exactHashes = new HashSet<>();
    private final Set<Long> shingleFingerprints = new HashSet<>();
    private boolean nearDuplicateEnabled = true;

    public DatasetDeduplicator() {}

    public DatasetDeduplicator(boolean nearDuplicateEnabled) {
        this.nearDuplicateEnabled = nearDuplicateEnabled;
    }

    @Override
    public boolean isDuplicate(NormalizedSample sample) {
        if (sample == null) return true;

        // 1. Exact SHA-256 hash check
        String hash = sample.getHash();
        if (hash != null && !hash.isEmpty()) {
            if (exactHashes.contains(hash)) {
                return true;
            }
        }

        // 2. Near-duplicate check using MinHash 5-gram shingle fingerprinting for long documents (>= 150 chars)
        if (nearDuplicateEnabled) {
            String text = sample.toFullText();
            long fp = computeShingleFingerprint(text);
            if (fp != 0L && shingleFingerprints.contains(fp)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void register(NormalizedSample sample) {
        if (sample == null) return;
        if (sample.getHash() != null) {
            exactHashes.add(sample.getHash());
        }
        if (nearDuplicateEnabled) {
            long fp = computeShingleFingerprint(sample.toFullText());
            if (fp != 0L) {
                shingleFingerprints.add(fp);
            }
        }
    }

    private static final long[] SEEDS = {
        0x9e3779b97f4a7c15L,
        0xbf58476d1ce4e5b9L,
        0x94d049bb133111ebL,
        0x2545f4914f6cdd1dL
    };

    private long computeShingleFingerprint(String text) {
        if (text == null || text.length() < 150) return 0L;

        String[] words = text.toLowerCase().split("\\s+");
        if (words.length < 15) return 0L;

        int k = 5;
        long[] minHashes = new long[SEEDS.length];
        for (int s = 0; s < SEEDS.length; s++) {
            minHashes[s] = Long.MAX_VALUE;
        }

        for (int i = 0; i <= words.length - k; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < k; j++) {
                if (j > 0) sb.append(' ');
                sb.append(words[i + j]);
            }
            String gram = sb.toString();
            long baseHash = mixHash(gram.hashCode());

            for (int s = 0; s < SEEDS.length; s++) {
                long h = mixHash((int) (baseHash ^ SEEDS[s]));
                if (h < minHashes[s]) {
                    minHashes[s] = h;
                }
            }
        }

        long combined = 17L;
        for (int s = 0; s < SEEDS.length; s++) {
            if (minHashes[s] == Long.MAX_VALUE) return 0L;
            combined = combined * 31L + minHashes[s];
        }

        return combined != 0L ? combined : 1L;
    }

    private static long mixHash(int code) {
        long h = code & 0xFFFFFFFFL;
        h = (h ^ (h >>> 16)) * 0x45d9f3bL;
        h = (h ^ (h >>> 16)) * 0x45d9f3bL;
        h = (h ^ (h >>> 16));
        return h;
    }

    @Override
    public void clear() {
        exactHashes.clear();
        shingleFingerprints.clear();
    }

    public int getExactDuplicateCount() {
        return exactHashes.size();
    }
}
