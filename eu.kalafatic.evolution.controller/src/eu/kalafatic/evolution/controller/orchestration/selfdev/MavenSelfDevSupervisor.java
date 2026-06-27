package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Maven-based Self-Dev Supervisor implementation.
 * Handles Maven builds, test execution, and git operations.
 */
public class MavenSelfDevSupervisor extends SelfDevSupervisor {
    
    private  TaskContext context;
    private  GitManager gitManager;
    private  String mainBranch = "main";
    private SupervisorStatus status = SupervisorStatus.IDLE;
    private String currentBranch = "main";
    
    public MavenSelfDevSupervisor(TaskContext context) {
    	super(context);
    
        
        // Use the correct GitManager constructor: GitManager(File root)
        // Get the GitManager from the context or create a new one
        File projectRoot = context.getProjectRoot();
        
        // Try to get existing GitManager from context, or create new one
        GitManager existing = context.getGitManager();
        if (existing != null) {
            this.gitManager = existing;
        } else {
            this.gitManager = new GitManager(projectRoot);
            context.setGitManager(this.gitManager);
        }
        
        if (this.gitManager != null && this.gitManager.isGitRepository()) {
            try {
                this.currentBranch = this.gitManager.getCurrentBranch();
                context.log("[MAVEN_SUPERVISOR] Git initialized. Current branch: " + currentBranch);
            } catch (Exception e) {
                context.log("[MAVEN_SUPERVISOR] Git initialization warning: " + e.getMessage());
                // Try to ensure initial commit for fresh repositories
                try {
                    this.gitManager.ensureInitialCommit();
                } catch (Exception ex) {
                    context.log("[MAVEN_SUPERVISOR] Could not initialize repository: " + ex.getMessage());
                }
            }
        } else {
            context.log("[MAVEN_SUPERVISOR] GitManager not available - using simulation mode");
        }
    }
    
    @Override
    public BranchResult createBranch(String name, String baseBranch) {
        context.log("[MAVEN_SUPERVISOR] Creating branch: " + name + " from " + baseBranch);
        BranchResult result = new BranchResult();
        try {
            if (gitManager != null && gitManager.isGitRepository()) {
                // First, ensure we're on the base branch
                gitManager.forceCheckout(baseBranch);
                
                // Check if branch already exists
                try {
                    // Try to create branch from base
                    gitManager.createBranchFrom(baseBranch, name);
                    result.success = true;
                    result.branchName = name;
                    result.commitId = gitManager.getHeadCommit();
                    result.message = "Branch created successfully";
                } catch (Exception e) {
                    // If branch creation fails (e.g., branch exists), try to checkout
                    try {
                        gitManager.forceCheckout(name);
                        result.success = true;
                        result.branchName = name;
                        result.commitId = gitManager.getHeadCommit();
                        result.message = "Branch checkout successful";
                    } catch (Exception e2) {
                        result.success = false;
                        result.message = "Failed to create or checkout branch: " + e2.getMessage();
                    }
                }
                
                if (result.success) {
                    this.currentBranch = name;
                    // Ensure we're on the new branch
                    try {
                        gitManager.forceCheckout(name);
                    } catch (Exception e) {
                        // Already on branch
                    }
                }
            } else {
                // Simulate for testing
                result.success = true;
                result.branchName = name;
                result.message = "Branch simulated (no git available)";
                this.currentBranch = name;
            }
        } catch (Exception e) {
            result.success = false;
            result.message = e.getMessage();
        }
        return result;
    }
    
    @Override
    public BuildResult buildAndTest(String branchName, TaskContext context) {
        this.status = SupervisorStatus.BUILDING;
        context.log("[MAVEN_SUPERVISOR] Building: " + branchName);
        
        BuildResult result = new BuildResult();
        try {
            // Ensure we're on the right branch
            if (gitManager != null && gitManager.isGitRepository()) {
                try {
                    gitManager.forceCheckout(branchName);
                } catch (Exception e) {
                    context.log("[MAVEN_SUPERVISOR] Could not checkout branch: " + e.getMessage());
                }
            }
            
            // Run Maven clean install
            ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "install", "-DskipTests");
            pb.directory(context.getProjectRoot());
            pb.redirectErrorStream(true);
            
            long startTime = System.currentTimeMillis();
            Process process = pb.start();
            int exitCode = process.waitFor();
            long endTime = System.currentTimeMillis();
            
            result.buildTime = (int)(endTime - startTime);
            result.exitCode = exitCode;
            result.success = exitCode == 0;
            
            if (!result.success) {
                result.errors = new ArrayList<>();
                result.errors.add("Maven build failed with exit code: " + exitCode);
            }
            this.status = result.success ? SupervisorStatus.SUCCESS : SupervisorStatus.FAILED;
            
        } catch (Exception e) {
            result.success = false;
            result.errors = List.of(e.getMessage());
            this.status = SupervisorStatus.FAILED;
        }
        
        return result;
    }
    
    @Override
    public TestResult runTests(String branchName, TaskContext context) {
        this.status = SupervisorStatus.TESTING;
        context.log("[MAVEN_SUPERVISOR] Running tests: " + branchName);
        
        TestResult result = new TestResult();
        try {
            // Ensure we're on the right branch
            if (gitManager != null && gitManager.isGitRepository()) {
                try {
                    gitManager.forceCheckout(branchName);
                } catch (Exception e) {
                    context.log("[MAVEN_SUPERVISOR] Could not checkout branch: " + e.getMessage());
                }
            }
            
            ProcessBuilder pb = new ProcessBuilder("mvn", "test");
            pb.directory(context.getProjectRoot());
            pb.redirectErrorStream(true);
            
            long startTime = System.currentTimeMillis();
            Process process = pb.start();
            int exitCode = process.waitFor();
            long endTime = System.currentTimeMillis();
            
            result.duration = endTime - startTime;
            result.total = 0; // Would need to parse test output for actual numbers
            result.passed = exitCode == 0 ? 1 : 0;
            result.failed = exitCode == 0 ? 0 : 1;
            result.passRate = result.total > 0 ? (double) result.passed / result.total : (exitCode == 0 ? 1.0 : 0.0);
            
        } catch (Exception e) {
            result.passRate = 0.0;
            result.failures = List.of(e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public FitnessResult evaluateFitness(BuildResult buildResult, TestResult testResult) {
        context.log("[MAVEN_SUPERVISOR] Evaluating fitness...");
        
        FitnessResult result = new FitnessResult();
        
        // Build weight: 40%
        double buildWeight = buildResult.success ? 0.4 : 0.0;
        
        // Test weight: 60%
        double testWeight = testResult.passRate * 0.6;
        
        result.score = buildWeight + testWeight;
        result.buildWeight = buildWeight;
        result.testWeight = testWeight;
        result.coverageWeight = 0.0;
        result.complexityWeight = 0.0;
        result.securityWeight = 0.0;
        
        // Set individual weights
        result.setWeight("build", buildWeight);
        result.setWeight("test", testWeight);
        result.setWeight("coverage", 0.0);
        
        // Threshold: 0.7 (70%)
        result.thresholdMet = result.score >= 0.7;
        result.justification = String.format(
            "Build: %.2f (success: %s), Tests: %.2f (pass rate: %.2f%%)", 
            buildWeight, buildResult.success, testWeight, testResult.passRate * 100
        );
        
        context.log("[MAVEN_SUPERVISOR] Fitness: " + String.format("%.3f", result.score) + 
                   " (threshold: " + result.thresholdMet + ")");
        
        return result;
    }
    
    @Override
    public MergeResult mergeIfApproved(String branchName, FitnessResult fitness) {
        this.status = SupervisorStatus.MERGING;
        context.log("[MAVEN_SUPERVISOR] Merging: " + branchName + " (score: " + fitness.score + ")");
        
        MergeResult result = new MergeResult();
        try {
            if (gitManager != null && gitManager.isGitRepository()) {
                // Ensure we're on the main branch
                gitManager.forceCheckout(mainBranch);
                
                // Merge the feature branch
                try {
                    gitManager.merge(branchName);
                    String commitId = gitManager.getHeadCommit();
                    result.merged = true;
                    result.commitId = commitId;
                    result.message = "Merged successfully";
                    this.currentBranch = mainBranch;
                    
                    // Commit the merge
                    gitManager.commit("Merge branch '" + branchName + "' from Self-Dev evolution", context);
                    
                } catch (Exception e) {
                    // Merge conflict or other issue
                    result.merged = false;
                    result.message = "Merge failed: " + e.getMessage();
                    
                    // Try to abort merge
                    try {
                        gitManager.rollback(context);
                    } catch (Exception ex) {
                        // Ignore rollback errors
                    }
                }
            } else {
                result.merged = true;
                result.commitId = "simulated-commit-" + System.currentTimeMillis();
                result.message = "Merge simulated (no git available)";
            }
            this.status = result.merged ? SupervisorStatus.SUCCESS : SupervisorStatus.FAILED;
        } catch (Exception e) {
            result.merged = false;
            result.message = e.getMessage();
            this.status = SupervisorStatus.FAILED;
        }
        return result;
    }
    
    @Override
    public RollbackResult rollback(String branchName, String reason) {
        this.status = SupervisorStatus.ROLLING_BACK;
        context.log("[MAVEN_SUPERVISOR] Rolling back: " + branchName + " - " + reason);
        
        RollbackResult result = new RollbackResult();
        try {
            if (gitManager != null && gitManager.isGitRepository()) {
                // Checkout main branch
                gitManager.forceCheckout(mainBranch);
                
                // Delete the feature branch if it exists
                try {
                    // Try to delete locally
                    gitManager.getGitTool().execute("branch -D " + branchName, 
                        context.getProjectRoot(), context);
                } catch (Exception e) {
                    context.log("[MAVEN_SUPERVISOR] Could not delete branch: " + e.getMessage());
                }
                
                // Hard reset to clean state
                try {
                    gitManager.rollback(context);
                } catch (Exception e) {
                    context.log("[MAVEN_SUPERVISOR] Could not rollback: " + e.getMessage());
                }
                
                result.success = true;
                result.message = "Rollback successful";
                this.currentBranch = mainBranch;
            } else {
                result.success = true;
                result.message = "Rollback simulated (no git available)";
            }
            this.status = SupervisorStatus.IDLE;
        } catch (Exception e) {
            result.success = false;
            result.message = e.getMessage();
            this.status = SupervisorStatus.FAILED;
        }
        return result;
    }
    
    @Override
    public String getMainBranch() { 
        return mainBranch; 
    }
    
    @Override
    public SupervisorStatus getStatus() { 
        return status; 
    }
    
    @Override
    public String getCurrentBranch() {
        return currentBranch;
    }
    
    /**
     * Gets the GitManager for advanced operations.
     */
    public GitManager getGitManager() {
        return gitManager;
    }
}