package eu.kalafatic.evolution.selfdev.genome.selfupgrade;

import java.util.List;

public class ValidationHints {

    private boolean requiresFullRebuild;

    private boolean requiresTestSuite;

    private boolean requiresRestart;

    private List<String> criticalModulesToCheck;

    public boolean isRequiresFullRebuild() {
        return requiresFullRebuild;
    }

    public void setRequiresFullRebuild(boolean requiresFullRebuild) {
        this.requiresFullRebuild = requiresFullRebuild;
    }

    public boolean isRequiresTestSuite() {
        return requiresTestSuite;
    }

    public void setRequiresTestSuite(boolean requiresTestSuite) {
        this.requiresTestSuite = requiresTestSuite;
    }

    public boolean isRequiresRestart() {
        return requiresRestart;
    }

    public void setRequiresRestart(boolean requiresRestart) {
        this.requiresRestart = requiresRestart;
    }

    public List<String> getCriticalModulesToCheck() {
        return criticalModulesToCheck;
    }

    public void setCriticalModulesToCheck(List<String> criticalModulesToCheck) {
        this.criticalModulesToCheck = criticalModulesToCheck;
    }
}
