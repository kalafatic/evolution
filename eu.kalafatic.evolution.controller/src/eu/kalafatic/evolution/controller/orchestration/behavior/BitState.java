package eu.kalafatic.evolution.controller.orchestration.behavior;

/**
 * Utility for encoding and decoding the orchestration state into a 64-bit bitfield.
 *
 * Layout:
 * bits 0–7     → MODE (LOCAL / REMOTE / HYBRID / PROXY / MEDIATED)
 * bits 8–15    → SUPERVISION (AUTO / MANUAL / HYBRID)
 * bits 16–23   → INTERACTION MODE (CONTINUOUS / STEP / GUIDED)
 * bits 24–31   → REASONING STYLE (ATOMIC / DARWIN / CONSERVATIVE / EXPLORATORY / ANALYTICAL)
 * bits 32–63   → EXTENSIONS / FUTURE FEATURES
 */
public class BitState {
    private static final int MODE_SHIFT = 0;
    private static final int SUPERVISION_SHIFT = 8;
    private static final int INTERACTION_SHIFT = 16;
    private static final int REASONING_SHIFT = 24;

    private static final long MASK = 0xFFL;

    public static long encode(int mode, int supervision, int interaction, int reasoning) {
        long state = 0;
        state |= ((long) (mode & 0xFF)) << MODE_SHIFT;
        state |= ((long) (supervision & 0xFF)) << SUPERVISION_SHIFT;
        state |= ((long) (interaction & 0xFF)) << INTERACTION_SHIFT;
        state |= ((long) (reasoning & 0xFF)) << REASONING_SHIFT;
        return state;
    }

    public static int getMode(long state) {
        return (int) ((state >> MODE_SHIFT) & MASK);
    }

    public static int getSupervision(long state) {
        return (int) ((state >> SUPERVISION_SHIFT) & MASK);
    }

    public static int getInteraction(long state) {
        return (int) ((state >> INTERACTION_SHIFT) & MASK);
    }

    public static int getReasoning(long state) {
        return (int) ((state >> REASONING_SHIFT) & MASK);
    }

    // --- MODE Constants ---
    public static final int MODE_LOCAL = 0;
    public static final int MODE_HYBRID = 1;
    public static final int MODE_REMOTE = 2;
    public static final int MODE_PROXY = 3;
    public static final int MODE_MEDIATED = 4;

    // --- SUPERVISION Constants ---
    public static final int SUPERVISION_AUTO = 0;
    public static final int SUPERVISION_MANUAL = 1;
    public static final int SUPERVISION_HYBRID = 2;

    // --- INTERACTION Constants ---
    public static final int INTERACTION_CONTINUOUS = 0;
    public static final int INTERACTION_STEP = 1;
    public static final int INTERACTION_GUIDED = 2;

    // --- REASONING Constants ---
    public static final int REASONING_ATOMIC = 0;
    public static final int REASONING_DARWIN = 1;
    public static final int REASONING_CONSERVATIVE = 2;
    public static final int REASONING_EXPLORATORY = 3;
    public static final int REASONING_ANALYTICAL = 4;
}
