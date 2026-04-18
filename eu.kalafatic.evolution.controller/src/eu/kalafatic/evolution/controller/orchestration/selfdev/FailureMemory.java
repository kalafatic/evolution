package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.HashMap;
import java.util.Map;

public class FailureMemory {
    private Map<String, Integer> fingerprints = new HashMap<>();

    public void addFingerprint(String fingerprint) {
        fingerprints.put(fingerprint, fingerprints.getOrDefault(fingerprint, 0) + 1);
    }

    public int getCount(String fingerprint) {
        return fingerprints.getOrDefault(fingerprint, 0);
    }

    public boolean isRepeating(String fingerprint) {
        return getCount(fingerprint) >= 2;
    }

    public Map<String, Integer> getFingerprints() {
        return fingerprints;
    }
}
