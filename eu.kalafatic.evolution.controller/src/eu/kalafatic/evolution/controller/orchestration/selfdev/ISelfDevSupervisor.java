package eu.kalafatic.evolution.controller.orchestration.selfdev;


import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * External Supervisor interface for Self-Dev mode.
 * Controls builds, tests, git operations, and deployment.
 * 
 * In Self-Dev mode, EVO acts as the "brain" and the Supervisor
 * acts as the "hands" - executing builds, managing branches, etc.
 * 
 * The Supervisor is responsible for:
 * - Creating and managing git branches
 * - Running builds and tests
 * - Evaluating fitness of changes
 * - Merging successful changes
 * - Rolling back failed changes
 */
public interface ISelfDevSupervisor {



	    
	    /**
	     * Creates a new branch for self-development.
	     * 
	     * @param name The name of the branch to create
	     * @param baseBranch The branch to branch from (e.g., "main", "master")
	     * @return BranchResult containing success status and branch details
	     */
	    BranchResult createBranch(String name, String baseBranch);
	    
	    /**
	     * Runs full build and tests on the current branch.
	     * 
	     * @param branchName The branch to build
	     * @param context The task context for logging and configuration
	     * @return BuildResult containing build status and details
	     */
	    BuildResult buildAndTest(String branchName, TaskContext context);
	    
	    /**
	     * Runs only tests (faster, used during iteration).
	     * 
	     * @param branchName The branch to test
	     * @param context The task context for logging and configuration
	     * @return TestResult containing test results
	     */
	    TestResult runTests(String branchName, TaskContext context);
	    
	    /**
	     * Evaluates the fitness of the current state based on build and test results.
	     * 
	     * @param buildResult The result of the build
	     * @param testResult The result of the tests
	     * @return FitnessResult containing the fitness score and threshold status
	     */
	    FitnessResult evaluateFitness(BuildResult buildResult, TestResult testResult);
	    
	    /**
	     * Merges the branch if fitness threshold is met.
	     * 
	     * @param branchName The branch to merge
	     * @param fitness The fitness result that triggered the merge
	     * @return MergeResult containing merge status and details
	     */
	    MergeResult mergeIfApproved(String branchName, FitnessResult fitness);
	    
	    /**
	     * Rolls back changes if fitness is unacceptable.
	     * 
	     * @param branchName The branch to rollback
	     * @param reason The reason for the rollback
	     * @return RollbackResult containing rollback status
	     */
	    RollbackResult rollback(String branchName, String reason);
	    
	    /**
	     * Gets the current main branch name.
	     * 
	     * @return The name of the main branch (e.g., "main", "master")
	     */
	    String getMainBranch();
	    
	    /**
	     * Gets the current status of the supervisor.
	     * 
	     * @return Current SupervisorStatus
	     */
	    SupervisorStatus getStatus();
	    
	    /**
	     * Gets the current branch name.
	     * 
	     * @return The current branch name
	     */
	    default String getCurrentBranch() {
	        return getMainBranch();
	    }
	    
	    /**
	     * Checks if the supervisor is currently busy.
	     * 
	     * @return true if the supervisor is busy, false otherwise
	     */
	    default boolean isBusy() {
	        return getStatus() != SupervisorStatus.IDLE && 
	               getStatus() != SupervisorStatus.SUCCESS;
	    }
	    
	    /**
	     * Checks if the supervisor is ready for new work.
	     * 
	     * @return true if the supervisor is ready, false otherwise
	     */
	    default boolean isReady() {
	        return getStatus() == SupervisorStatus.IDLE || 
	               getStatus() == SupervisorStatus.SUCCESS;
	    }
	}

	

	

	/**
	 * Result of a test operation.
	 */
	class TestResult {
	    public int total;
	    public int passed;
	    public int failed;
	    public int skipped;
	    public List<String> failures;
	    public double passRate; // 0.0 - 1.0
	    public long duration; // in milliseconds
	    public long timestamp;
	    
	    public TestResult() {
	        this.failures = new java.util.ArrayList<>();
	        this.timestamp = System.currentTimeMillis();
	    }
	    
	    public boolean allPassed() {
	        return failed == 0 && passed == total;
	    }
	    
	    public boolean hasFailures() {
	        return failed > 0;
	    }
	    
	    public double getPassRate() {
	        return passRate;
	    }
	    
	    @Override
	    public String toString() {
	        return "TestResult{" +
	                "total=" + total +
	                ", passed=" + passed +
	                ", failed=" + failed +
	                ", skipped=" + skipped +
	                ", passRate=" + String.format("%.2f%%", passRate * 100) +
	                ", duration=" + duration + "ms" +
	                '}';
	    }
	}

	

	

	

	/**
	 * Enum representing the current status of the supervisor.
	 */
	enum SupervisorStatus {
	    /** Supervisor is idle and ready for work */
	    IDLE,
	    
	    /** Supervisor is currently building the project */
	    BUILDING,
	    
	    /** Supervisor is currently running tests */
	    TESTING,
	    
	    /** Supervisor is currently merging a branch */
	    MERGING,
	    
	    /** Supervisor is currently rolling back changes */
	    ROLLING_BACK,
	    
	    /** Supervisor has failed an operation */
	    FAILED,
	    
	    /** Supervisor has successfully completed an operation */
	    SUCCESS
	}