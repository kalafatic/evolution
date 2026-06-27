package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

public class StateSnapshot {
    public enum BuildStatus { SUCCESS, FAIL }
    public enum ErrorType { compiler, runtime, test }

    public static class BuildInfo {
        public BuildStatus status;
        public int errorCount;
        public List<ErrorType> errorTypes = new ArrayList<>();
    }

    public static class TestInfo {
        public int total;
        public int passed;
        public int failed;
        public List<String> failingTests = new ArrayList<>();
    }

    public static class CoverageInfo {
        public Double percent;
    }

    public BuildInfo build = new BuildInfo();
    public TestInfo tests = new TestInfo();
    public CoverageInfo coverage = new CoverageInfo();
}
