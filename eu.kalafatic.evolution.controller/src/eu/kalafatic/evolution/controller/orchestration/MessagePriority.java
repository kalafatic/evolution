package eu.kalafatic.evolution.controller.orchestration;

/**
 * Message priorities for the Conversation Output Controller.
 */
public enum MessagePriority {
    /**
     * Internal Darwin traces, low-level orchestration events.
     */
    DEBUG(0),

    /**
     * Thinking, iteration updates, tool status.
     */
    PROGRESS(1),

    /**
     * Standard AI responses, general updates.
     */
    NORMAL(2),

    /**
     * Clarification request, approval request.
     */
    USER_ACTION_REQUIRED(3),

    /**
     * Final answer, branch selection.
     */
    FINAL(4);

    private final int level;

    MessagePriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static MessagePriority fromLevel(int level) {
        for (MessagePriority p : values()) {
            if (p.level == level) return p;
        }
        return NORMAL;
    }
}
