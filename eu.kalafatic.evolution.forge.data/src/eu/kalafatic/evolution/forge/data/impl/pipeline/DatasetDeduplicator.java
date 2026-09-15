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

    private long computeShingleFingerprint(String text) {
        if (text == null || text.length() < 150) return 0L;
        // Compute MinHash fingerprint over character 5-grams
        int k = 5;
        long minHash = Long.MAX_VALUE;
        long maxHash = Long.MIN_VALUE;
        for (int i = 0; i <= text.length() - k; i += 2) {
            String gram = text.substring(i, i + k);
            long h = mixHash(gram.hashCode());
            if (h < minHash) minHash = h;
            if (h > maxHash) maxHash = h;
        }
        return minHash != Long.MAX_VALUE ? (minHash ^ maxHash) : 0L;
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
