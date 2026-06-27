package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Records the outcome of branch verification (compilation, tests).
 */
public class VerificationRecord {
    private boolean compiled;
    private int testsPassed;
    private int testsTotal;
    private double testPassRate;
    private String verificationOutput;

    public boolean isCompiled() { return compiled; }
    public void setCompiled(boolean compiled) { this.compiled = compiled; }

    public int getTestsPassed() { return testsPassed; }
    public void setTestsPassed(int testsPassed) { this.testsPassed = testsPassed; }

    public int getTestsTotal() { return testsTotal; }
    public void setTestsTotal(int testsTotal) { this.testsTotal = testsTotal; }

    public double getTestPassRate() { return testPassRate; }
    public void setTestPassRate(double testPassRate) { this.testPassRate = testPassRate; }

    public String getVerificationOutput() { return verificationOutput; }
    public void setVerificationOutput(String verificationOutput) { this.verificationOutput = verificationOutput; }
}
