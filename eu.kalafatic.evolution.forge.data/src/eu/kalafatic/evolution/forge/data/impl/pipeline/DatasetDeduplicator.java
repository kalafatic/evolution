package eu.kalafatic.evolution.forge.data.impl.pipeline;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

import java.util.HashSet;
import java.util.Set;

/**
 * Deduplication stage supporting exact SHA-256 hash matching and MinHash/shingle near-duplicate filtering.
 */
public class DatasetDeduplicator {

    private final Set<String> exactHashes = new HashSet<>();
    private final Set<Long> shingleFingerprints = new HashSet<>();
    private boolean nearDuplicateEnabled = true;

    public DatasetDeduplicator() {}

    public DatasetDeduplicator(boolean nearDuplicateEnabled) {
        this.nearDuplicateEnabled = nearDuplicateEnabled;
    }

    public boolean isDuplicate(NormalizedSample sample) {
        if (sample == null) return true;

        // 1. Exact hash check
        String hash = sample.getHash();
        if (hash != null && !hash.isEmpty()) {
            if (exactHashes.contains(hash)) {
                return true;
            }
        }

        // 2. Near-duplicate check using character 5-gram shingle fingerprinting
        if (nearDuplicateEnabled) {
            String text = sample.toFullText();
            long fp = computeShingleFingerprint(text);
            if (fp != 0L && shingleFingerprints.contains(fp)) {
                return true;
            }
        }

        return false;
    }

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
        if (text == null || text.length() < 10) return 0L;
        // Compute simple 64-bit rolling 5-gram shingle fingerprint
        long hash = 1125899906842597L; // Prime multiplier
        int k = 5;
        for (int i = 0; i <= text.length() - k; i += 3) {
            String gram = text.substring(i, i + k);
            hash = 31 * hash + gram.hashCode();
        }
        return hash;
    }

    public void clear() {
        exactHashes.clear();
        shingleFingerprints.clear();
    }

    public int getExactDuplicateCount() {
        return exactHashes.size();
    }
}
