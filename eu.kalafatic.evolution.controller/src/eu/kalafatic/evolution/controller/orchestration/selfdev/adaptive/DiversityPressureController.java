package eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Controller to enforce diversity and prevent convergence into similar solutions.
 */
public class DiversityPressureController {
    private double pressureLevel = 0.0;

    public void increasePressure() {
        pressureLevel = Math.min(pressureLevel + 0.2, 1.0);
    }

    public void reset() {
        pressureLevel = 0.0;
    }

    public double calculateSimilarity(BranchVariant v1, BranchVariant v2) {
        // Basic similarity based on strategy text
        if (v1.getStrategy() == null || v2.getStrategy() == null) return 0.0;

        String[] w1 = v1.getStrategy().toLowerCase().split("\\s+");
        String[] w2 = v2.getStrategy().toLowerCase().split("\\s+");

        long matches = 0;
        for (String s1 : w1) {
            for (String s2 : w2) {
                if (s1.equals(s2) && s1.length() > 3) matches++;
            }
        }

        return (double) matches / Math.max(w1.length, w2.length);
    }

    public double getPressureLevel() {
        return pressureLevel;
    }
}
