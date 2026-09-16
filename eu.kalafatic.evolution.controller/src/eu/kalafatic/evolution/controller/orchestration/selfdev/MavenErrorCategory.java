package eu.kalafatic.evolution.controller.orchestration.selfdev;

public enum MavenErrorCategory {
    COMPILATION(false),
    DEPENDENCY(false),
    TARGET_PLATFORM(false),
    TYCHO(false),
    MAVEN(false),
    NETWORK(true),
    FILESYSTEM(false),
    JAVA(false),
    POM(false),
    PACKAGING(false),
    TEST_FAILURE(false),
    TRANSIENT(true),
    UNKNOWN(false);

    private final boolean recoverable;

    MavenErrorCategory(boolean recoverable) {
        this.recoverable = recoverable;
    }

    public boolean isRecoverable() {
        return recoverable;
    }
}
