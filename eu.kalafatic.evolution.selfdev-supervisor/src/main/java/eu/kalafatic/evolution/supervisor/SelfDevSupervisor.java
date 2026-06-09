package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.kalafatic.evolution.supervisor.model.*;

/**
 * Standalone Supervisor for autonomous self-development.
 * Responsible for iteration control, build/test execution, and Git management.
 */
public class SelfDevSupervisor {
    private final File baseDir;
    private final ProcessRunner runner = new ProcessRunner();
    private final GitManager git;
    private final ObjectMapper mapper = new ObjectMapper();
    private final File runDir;
    private final String goal;

    public SelfDevSupervisor(File baseDir, String goal) {
        this.baseDir = baseDir;
        this.goal = goal;
        this.git = new GitManager(baseDir);
        this.runDir = new File(baseDir, "self-dev-run");
        if (!runDir.exists()) runDir.mkdirs();
    }

    public void run() {
        System.out.println("[SUPERVISOR] Starting Standalone Evolution Loop...");

        // Start Darwin Engine Headless
        System.out.println("[SUPERVISOR] Spawning Darwin Evolution Engine...");
        runner.runApplication(baseDir, "eu.kalafatic.evolution.controller.headless.HeadlessEvolutionServer", baseDir.getAbsolutePath());

        try {
            for (int i = 1; i <= 10; i++) { // Max 10 iterations for safety
                System.out.println("\n--- ITERATION " + i + " ---");

                // 1. Capture State
                ContextSnapshot snapshot = captureState();

                // 2. Request Proposals from Darwin (via file protocol)
                DarwinProposalRequest request = new DarwinProposalRequest();
                request.setGoal(goal);
                request.setContext(snapshot);
                request.setIteration(i);

                writeRequest(request);

                // 3. Wait for Darwin Response
                DarwinProposalResponse response = waitForResponse();
                if (response == null || response.getProposals().isEmpty()) {
                    System.out.println("[SUPERVISOR] No proposals received. Stopping.");
                    break;
                }

                // 4. Select Plan (Pick best score)
                DarwinProposalResponse.Proposal best = response.getProposals().stream()
                        .max((p1, p2) -> Double.compare(p1.getScore(), p2.getScore()))
                        .orElse(response.getProposals().get(0));

                System.out.println("[SUPERVISOR] Selected Proposal: " + best.getId() + " (Score: " + best.getScore() + ")");

                // 5. Create Execution Plan & Branch
                String branchName = "evolution/iter-" + i + "-" + best.getId();
                git.createBranch(branchName);

                // 6. Apply Changes
                if (best.getDiff() != null && !best.getDiff().isEmpty()) {
                    if (!runner.applyPatch(baseDir, best.getDiff())) {
                        System.err.println("[SUPERVISOR] Failed to apply patch.");
                        git.rollback();
                        continue;
                    }
                }

                // 7. Commit
                git.commit("Evolutionary Step: " + best.getStrategy());

                // 8. Build & Test
                System.out.println("[SUPERVISOR] Building and testing...");
                if (runner.runBuild(baseDir) && runner.runTests(baseDir)) {
                    System.out.println("[SUPERVISOR] Iteration " + i + " SUCCESS.");
                } else {
                    System.err.println("[SUPERVISOR] Build/Test FAILED. Rolling back.");
                    git.rollback();
                    git.checkout("main"); // Assuming main is base
                }
            }
        } catch (Exception e) {
            System.err.println("[CRITICAL] Supervisor loop failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ContextSnapshot captureState() {
        ContextSnapshot snapshot = new ContextSnapshot();
        snapshot.setTimestamp(System.currentTimeMillis());
        snapshot.setMetadata(new HashMap<>());

        try {
            StringBuilder sb = new StringBuilder();
            Files.walk(baseDir.toPath(), 2)
                 .forEach(path -> sb.append(baseDir.toPath().relativize(path)).append("\n"));
            snapshot.setProjectStructure(sb.toString());
        } catch (IOException e) {
            snapshot.setProjectStructure("Error scanning project structure: " + e.getMessage());
        }

        return snapshot;
    }

    private void writeRequest(DarwinProposalRequest request) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(runDir, "darwin-request.json"), request);
    }

    private DarwinProposalResponse waitForResponse() throws InterruptedException, IOException {
        File responseFile = new File(runDir, "darwin-response.json");
        for (int i = 0; i < 30; i++) { // Wait max 60 seconds
            if (responseFile.exists()) {
                DarwinProposalResponse response = mapper.readValue(responseFile, DarwinProposalResponse.class);
                responseFile.delete();
                return response;
            }
            Thread.sleep(2000);
        }
        return null;
    }
}
