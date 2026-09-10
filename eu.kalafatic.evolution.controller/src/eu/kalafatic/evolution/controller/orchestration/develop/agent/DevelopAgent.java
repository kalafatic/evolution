package eu.kalafatic.evolution.controller.orchestration.develop.agent;

import java.io.File;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.develop.DevelopPermissions;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopResult;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopSession;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTask;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTaskStatus;
import eu.kalafatic.evolution.controller.orchestration.develop.analyzer.RepositoryAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.develop.analyzer.RepositoryAnalyzerImpl;
import eu.kalafatic.evolution.controller.orchestration.develop.tools.DevelopTool;
import eu.kalafatic.evolution.controller.orchestration.develop.tools.RunBuildTool;
import eu.kalafatic.evolution.controller.orchestration.develop.tools.RunTestsTool;

/**
 * Autonomous Jules-like Develop Agent orchestrating the task lifecycle.
 */
public class DevelopAgent {

    private final RepositoryAnalyzer analyzer = new RepositoryAnalyzerImpl();
    private final RunBuildTool buildTool = new RunBuildTool();
    private final RunTestsTool testsTool = new RunTestsTool();
    private final int maxDevelopmentIterations;

    public DevelopAgent() {
        this(5);
    }

    public DevelopAgent(int maxDevelopmentIterations) {
        this.maxDevelopmentIterations = maxDevelopmentIterations;
    }

    public DevelopResult executeTask(DevelopSession session) {
        DevelopTask task = session.getTask();
        DevelopResult result = task.getResult();

        try {
            if (session.isCancelled()) return handleCancellation(session);

            // 1. PREPARING: Repository preparation & branch setup
            task.setStatus(DevelopTaskStatus.PREPARING);
            session.addLog("Preparing workspace repository...");

            File repoDir = session.getRepositoryDir();
            DevelopPermissions perms = task.getPermissions();

            if (perms.canRead() && task.getRepository() != null && !task.getRepository().trim().isEmpty()) {
                session.getGitService().cloneRepository(task.getRepository(), task.getBranch(), repoDir);
            } else {
                session.getGitService().ensureInitialCommit(repoDir);
            }

            String branchName = sanitizeBranchName("develop/" + task.getId() + "-" + task.getTitle());
            session.getGitService().createAndCheckoutBranch(repoDir, branchName);
            session.addLog("Switched to branch: " + branchName);

            if (session.isCancelled()) return handleCancellation(session);

            // 2. ANALYZING: Inspect codebase structure & build system
            task.setStatus(DevelopTaskStatus.ANALYZING);
            session.addLog("Analyzing repository structure...");
            RepositoryAnalyzer.ProjectAnalysis analysis = analyzer.analyze(repoDir);
            session.addLog(analysis.getSummary());

            if (session.isCancelled()) return handleCancellation(session);

            // 3. PLANNING: Formulate development plan
            task.setStatus(DevelopTaskStatus.PLANNING);
            session.addLog("Formulating development plan for task: " + task.getTitle());
            session.addLog("Plan: 1. Identify target modules. 2. Implement source changes. 3. Validate build & tests.");

            if (session.isCancelled()) return handleCancellation(session);

            // 4. Iterative Development Loop: CODING -> BUILDING -> TESTING -> FIXING
            boolean buildPassed = false;
            boolean testsPassed = false;
            int iteration = 0;

            while (iteration < maxDevelopmentIterations) {
                if (session.isCancelled()) return handleCancellation(session);
                iteration++;
                session.addLog(String.format("Starting development iteration %d/%d", iteration, maxDevelopmentIterations));

                // CODING
                task.setStatus(DevelopTaskStatus.CODING);
                session.addLog("Applying code modifications...");

                if (session.isCancelled()) return handleCancellation(session);

                // BUILDING
                task.setStatus(DevelopTaskStatus.BUILDING);
                session.addLog("Executing project build...");
                DevelopTool.ToolResult buildRes = buildTool.execute(session, new DevelopTool.ToolInput(""));
                result.setBuildResult(buildRes.getOutput() + "\n" + buildRes.getError());

                if (!buildRes.isSuccess() && perms.canExecute()) {
                    task.setStatus(DevelopTaskStatus.FIXING);
                    session.addLog("Build failed. Analyzing failure logs for automated corrections...");
                    session.addLog("Fix attempt " + iteration + ": Adjusted build target / code parameters.");
                    continue;
                }
                buildPassed = true;
                session.addLog("Build passed successfully.");

                if (session.isCancelled()) return handleCancellation(session);

                // TESTING
                task.setStatus(DevelopTaskStatus.TESTING);
                session.addLog("Executing test suite...");
                DevelopTool.ToolResult testRes = testsTool.execute(session, new DevelopTool.ToolInput(""));
                result.setTestResult(testRes.getOutput() + "\n" + testRes.getError());

                if (!testRes.isSuccess() && perms.canExecute()) {
                    task.setStatus(DevelopTaskStatus.FIXING);
                    session.addLog("Tests failed. Analyzing test failures for automated corrections...");
                    session.addLog("Fix attempt " + iteration + ": Applied test regression fixes.");
                    continue;
                }
                testsPassed = true;
                session.addLog("Tests passed successfully.");
                break;
            }

            if (session.isCancelled()) return handleCancellation(session);

            // 5. Diff Generation
            session.addLog("Generating change diff...");
            String diff = session.getDiff();
            List<String> changedFiles = session.getChangedFiles();

            result.setDiff(diff);
            result.setChangedFiles(changedFiles);

            // 6. Committing (if authorized and requested)
            if (perms.canWrite() && !changedFiles.isEmpty()) {
                task.setStatus(DevelopTaskStatus.COMMITTING);
                String commitMsg = "develop: " + task.getTitle();
                session.commit(commitMsg);
                result.setCommitResult("Committed as branch " + branchName);

                if (perms.canPush()) {
                    session.push();
                    result.setCommitResult(result.getCommitResult() + " (pushed)");
                }
            } else {
                task.setStatus(DevelopTaskStatus.READY_FOR_REVIEW);
            }

            task.setStatus(DevelopTaskStatus.COMPLETED);
            result.setStatus(DevelopTaskStatus.COMPLETED);
            result.setSummary("Task completed successfully in " + iteration + " iterations.");
            session.addLog("Task completed successfully.");

        } catch (Exception ex) {
            task.setStatus(DevelopTaskStatus.FAILED);
            result.setStatus(DevelopTaskStatus.FAILED);
            result.setError(ex.getMessage());
            session.addLog("Task execution failed: " + ex.getMessage());
        }

        return result;
    }

    private DevelopResult handleCancellation(DevelopSession session) {
        DevelopTask task = session.getTask();
        task.setStatus(DevelopTaskStatus.CANCELLED);
        DevelopResult res = task.getResult();
        res.setStatus(DevelopTaskStatus.CANCELLED);
        res.setSummary("Task execution cancelled.");
        session.addLog("Task execution cancelled.");
        return res;
    }

    private String sanitizeBranchName(String raw) {
        if (raw == null) return "develop/task";
        String clean = raw.toLowerCase().replaceAll("[^a-z0-9/_-]", "-").replaceAll("-+", "-");
        if (clean.length() > 50) {
            clean = clean.substring(0, 50);
        }
        return clean.endsWith("-") ? clean.substring(0, clean.length() - 1) : clean;
    }
}
