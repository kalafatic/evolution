package eu.kalafatic.evolution.controller.orchestration.llm;

public enum ResponseKind {
    FINAL_ONLY,
    REASONING_AND_FINAL,
    REASONING_ONLY,
    EMPTY,
    MALFORMED
}
