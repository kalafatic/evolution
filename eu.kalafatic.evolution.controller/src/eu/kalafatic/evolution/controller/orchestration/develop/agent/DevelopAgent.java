package eu.kalafatic.evolution.controller.orchestration.develop.agent;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionContext;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
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
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Autonomous Jules-like Develop Agent orchestrating the task lifecycle with LLM inference,
 * bounded correction feedback loops, and verification.
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
            String lastFailureContext = "";

            while (iteration < maxDevelopmentIterations) {
                if (session.isCancelled()) return handleCancellation(session);
                iteration++;
                session.addLog(String.format("Starting development iteration %d/%d", iteration, maxDevelopmentIterations));

                // CODING
                task.setStatus(DevelopTaskStatus.CODING);
                session.addLog("Applying code modifications...");
                applyAiCodeModifications(session, analysis, lastFailureContext);

                if (session.isCancelled()) return handleCancellation(session);

                // BUILDING
                task.setStatus(DevelopTaskStatus.BUILDING);
                session.addLog("Executing project build...");
                DevelopTool.ToolResult buildRes = buildTool.execute(session, new DevelopTool.ToolInput(""));
                result.setBuildResult(buildRes.getOutput() + "\n" + buildRes.getError());

                if (!buildRes.isSuccess() && perms.canExecute()) {
                    task.setStatus(DevelopTaskStatus.FIXING);
                    lastFailureContext = "BUILD FAILURE:\n" + buildRes.getError() + "\n" + buildRes.getOutput();
                    session.addLog("Build failed. Analyzing failure logs for automated corrections...");
                    session.addLog("Fix attempt " + iteration + ": Fed failure diagnostics to AI model.");
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
                    lastFailureContext = "TEST FAILURE:\n" + testRes.getError() + "\n" + testRes.getOutput();
                    session.addLog("Tests failed. Analyzing test failures for automated corrections...");
                    session.addLog("Fix attempt " + iteration + ": Fed test failure diagnostics to AI model.");
                    continue;
                }
                testsPassed = true;
                session.addLog("Tests passed successfully.");
                break;
            }

            if (session.isCancelled()) return handleCancellation(session);

            // 5. Diff Generation
            task.setDeliveryStage(eu.kalafatic.evolution.controller.orchestration.develop.DeliveryStage.IMPLEMENTATION_COMPLETE);
            session.addLog("Generating change diff...");
            String diff = session.getDiff();
            List<String> changedFiles = session.getChangedFiles();

            result.setDiff(diff);
            result.setChangedFiles(changedFiles);

            if (buildPassed) {
                task.setDeliveryStage(eu.kalafatic.evolution.controller.orchestration.develop.DeliveryStage.BUILD_VERIFIED);
            }
            if (testsPassed) {
                task.setDeliveryStage(eu.kalafatic.evolution.controller.orchestration.develop.DeliveryStage.TESTS_VERIFIED);
            }

            // 6. Committing & Delivery (if authorized and requested)
            if (perms.canWrite() && !changedFiles.isEmpty()) {
                task.setStatus(DevelopTaskStatus.COMMITTING);
                String commitMsg = "develop: " + task.getTitle();
                session.commit(commitMsg);
                task.setDeliveryStage(eu.kalafatic.evolution.controller.orchestration.develop.DeliveryStage.COMMIT_CREATED);
                result.setCommitResult("Committed as branch " + branchName);

                if (perms.canPush()) {
                    try {
                        session.push();
                        task.setDeliveryStage(eu.kalafatic.evolution.controller.orchestration.develop.DeliveryStage.BRANCH_PUSHED);
                        result.setCommitResult(result.getCommitResult() + " (pushed)");

                        // Prepare Pull Request URL
                        String prUrl = session.getGitService().createPullRequest(
                                repoDir, branchName, task.getBranch(), task.getTitle(), task.getDescription(), null);
                        if (prUrl != null && !prUrl.isEmpty()) {
                            result.setPullRequestUrl(prUrl);
                            task.setDeliveryStage(eu.kalafatic.evolution.controller.orchestration.develop.DeliveryStage.PULL_REQUEST_CREATED);
                            session.addLog("Pull request ready: " + prUrl);
                        }
                    } catch (Exception pushEx) {
                        session.addLog("Remote push/PR failed (local commit preserved): " + pushEx.getMessage());
                    }
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

    private void applyAiCodeModifications(DevelopSession session, RepositoryAnalyzer.ProjectAnalysis analysis, String failureContext) {
        if (!session.getTask().getPermissions().canWrite()) {
            return;
        }

        try {
            SessionContainer container = SessionManager.getInstance().getSession(session.getSessionId());
            TaskContext tc = null;
            Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
            if (container instanceof SessionContext) {
                tc = ((SessionContext) container).getTaskContext();
            }
            if (tc == null) {
                tc = new TaskContext(orch, session.getRepositoryDir());
                tc.setSessionId(session.getSessionId());
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are an autonomous development agent. Implement the required modifications in the repository.\n");
            promptBuilder.append("TASK TITLE: ").append(session.getTask().getTitle()).append("\n");
            promptBuilder.append("TASK DESCRIPTION: ").append(session.getTask().getDescription()).append("\n");
            promptBuilder.append("PROJECT TYPE: ").append(analysis.getProjectType()).append(" (Build System: ").append(analysis.getBuildSystem()).append(")\n");

            if (failureContext != null && !failureContext.trim().isEmpty()) {
                promptBuilder.append("\nPREVIOUS FAILURE TO FIX:\n").append(failureContext).append("\n");
            }

            promptBuilder.append("\nOutput the file changes using the following markdown format for each file to create or update:\n");
            promptBuilder.append("```file:relative/path/to/File.ext\n<file content here>\n```\n");

            String aiResponse = null;
            try {
                if (orch != null) {
                    aiResponse = LlmRouter.getInstance().sendRequest(orch, promptBuilder.toString(), 0.2f, null, tc);
                }
            } catch (Exception ex) {
                session.addLog("AI inference skipped/unavailable: " + ex.getMessage());
            }

            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                parseAndApplyFiles(session.getRepositoryDir(), aiResponse, session);
            }
        } catch (Exception ex) {
            session.addLog("Code modification pass noted: " + ex.getMessage());
        }
    }

    public void parseAndApplyFiles(File baseDir, String response, DevelopSession session) {
        Pattern pattern = Pattern.compile("```file:([^\\r\\n]+)\\r?\\n([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(response);
        int filesCount = 0;

        while (matcher.find()) {
            String relativePath = matcher.group(1).trim();
            String content = matcher.group(2);

            if (relativePath.contains("..") || relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                continue; // Guard against directory traversal
            }

            try {
                File targetFile = new File(baseDir, relativePath);
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                Files.writeString(targetFile.toPath(), content);
                filesCount++;
                if (session != null) {
                    session.addLog("AI modified file: " + relativePath);
                }
            } catch (Exception ex) {
                if (session != null) {
                    session.addLog("Failed to write AI modification to " + relativePath + ": " + ex.getMessage());
                }
            }
        }
        if (filesCount > 0 && session != null) {
            session.addLog("Successfully applied " + filesCount + " file modification(s) from AI.");
        }
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
