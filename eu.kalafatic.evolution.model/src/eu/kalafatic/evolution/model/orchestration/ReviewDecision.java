package eu.kalafatic.evolution.model.orchestration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.emf.common.util.Enumerator;

public enum ReviewDecision implements Enumerator {
    OPEN(0, "OPEN", "OPEN"),
    IN_REVIEW(1, "IN_REVIEW", "IN_REVIEW"),
    APPROVED(2, "APPROVED", "APPROVED"),
    REJECTED(3, "REJECTED", "REJECTED"),
    CHANGES_REQUESTED(4, "CHANGES_REQUESTED", "CHANGES_REQUESTED");

    public static final int OPEN_VALUE = 0;
    public static final int IN_REVIEW_VALUE = 1;
    public static final int APPROVED_VALUE = 2;
    public static final int REJECTED_VALUE = 3;
    public static final int CHANGES_REQUESTED_VALUE = 4;

    private static final ReviewDecision[] VALUES_ARRAY = new ReviewDecision[] {
        OPEN, IN_REVIEW, APPROVED, REJECTED, CHANGES_REQUESTED
    };

    public static final List<ReviewDecision> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

    public static ReviewDecision get(String literal) {
        for (ReviewDecision result : VALUES_ARRAY) {
            if (result.toString().equals(literal)) return result;
        }
        return null;
    }

    public static ReviewDecision getByName(String name) {
        for (ReviewDecision result : VALUES_ARRAY) {
            if (result.getName().equals(name)) return result;
        }
        return null;
    }

    public static ReviewDecision get(int value) {
        switch (value) {
            case OPEN_VALUE: return OPEN;
            case IN_REVIEW_VALUE: return IN_REVIEW;
            case APPROVED_VALUE: return APPROVED;
            case REJECTED_VALUE: return REJECTED;
            case CHANGES_REQUESTED_VALUE: return CHANGES_REQUESTED;
        }
        return null;
    }

    private final int value;
    private final String name;
    private final String literal;

    private ReviewDecision(int value, String name, String literal) {
        this.value = value;
        this.name = name;
        this.literal = literal;
    }

    @Override public int getValue() { return value; }
    @Override public String getName() { return name; }
    @Override public String getLiteral() { return literal; }
    @Override public String toString() { return literal; }
}
