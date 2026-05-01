package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SelfDevSupervisor {
    private static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    private final File baseDir;
    private final ResultReader reader = new ResultReader();
    private final ProcessRunner runner = new ProcessRunner();
    private final EvoValidator validator = new EvoValidator();
    private final IterationManager iterationManager;

    public SelfDevSupervisor(File baseDir) {
        this.baseDir = baseDir;
        this.iterationManager = new IterationManager(baseDir);
    }

    public void run() {
        File bootstrapFile = new File(baseDir, "self-dev-run/bootstrap.json");
        File stateFile = new File(baseDir, "self-dev.json");
        File workspaceDir = new File(baseDir, "workspace");
        File statusFile = new File(baseDir, "self-dev-run/status.json");

        try {
            while (true) {
                // Check bootstrap/state
                if (bootstrapFile.exists()) {
                    System.out.println("[SUPERVISOR] Bootstrap contract detected.");
                    Bootstrap bootstrap = reader.readBootstrap(bootstrapFile);
                    System.out.println("[SUPERVISOR] Source: " + bootstrap.getSourcePath());
                    System.out.println("[SUPERVISOR] Target: " + bootstrap.getTargetPath());
                    System.out.println("[SUPERVISOR] Action: " + bootstrap.getAction());
                }

                if (!stateFile.exists()) {
                    System.out.println("[SUPERVISOR] Waiting for state file: " + stateFile.getAbsolutePath());
                    Thread.sleep(2000);
                    continue;
                }

                State state = reader.readState(stateFile);
                if (!state.isActive()) {
                    System.out.println("[SUPERVISOR] Self-dev inactive. Exiting.");
                    updateStatus(statusFile, "STOPPED", null);
                    break;
                }

                updateStatus(statusFile, "STARTING", null);
                System.out.println("\n[SUPERVISOR] Starting Iteration: " + state.getIteration());
                File iterDir = iterationManager.prepareIteration(state.getIteration());

                Map<String, Result> variantResults = new HashMap<>();
                File[] variants = iterDir.listFiles(File::isDirectory);
                if (variants != null) {
                    for (File variant : variants) {
                        System.out.println("\n[VARIANT] Processing " + variant.getName());
                        updateStatus(statusFile, "BUILDING", variant.getName());

                        // Load and validate plan
                        try {
                            File planFile = new File(variant, "plan.json");
                            if (!planFile.exists()) {
                                // Try iteration directory
                                planFile = new File(iterDir, "plan.json");
                            }
                            if (planFile.exists()) {
                                EvoPlan plan = reader.readPlan(planFile);
                                validator.validate(variant, plan);
                            } else {
                                System.out.println("[WARNING] No plan.json found for variant " + variant.getName() + ". Skipping validation.");
                            }
                        } catch (IOException e) {
                            System.err.println("[ERROR] Failed to read or validate plan for " + variant.getName() + ": " + e.getMessage());
                            System.exit(1);
                        }

                        if (runner.runBuild(variant)) {
                            // Assume the JAR is produced in target/app.jar (adjust if needed)
                            // In this case, we'll look for a JAR in the variant directory or target
                            String jarName = findJar(variant);
                            if (jarName != null) {
                                updateStatus(statusFile, "RUNNING", variant.getName());
                                if (runner.runRCP(variant, jarName)) {
                                    try {
                                        Result result = reader.readResult(new File(variant, "result.json"));
                                        System.out.println("[RESULT] " + variant.getName() + " -> " + result);
                                        variantResults.put(variant.getName(), result);
                                    } catch (IOException e) {
                                        System.err.println("[ERROR] Failed to read result.json for " + variant.getName());
                                    }
                                } else {
                                    System.err.println("[RUN] Failed for " + variant.getName());
                                }
                            } else {
                                System.err.println("[BUILD] No JAR found for " + variant.getName());
                            }
                        } else {
                            System.err.println("[BUILD] Failed for " + variant.getName());
                        }
                    }
                }

                updateStatus(statusFile, "EVALUATING", null);
                String winner = evaluate(variantResults);
                if (winner != null) {
                    System.out.println("\n[WINNER] Selected " + winner);
                    iterationManager.promoteVariant(new File(iterDir, winner), workspaceDir);
                } else {
                    System.out.println("\n[REJECT] No variant met acceptance criteria.");
                }

                state.setIteration(state.getIteration() + 1);
                reader.writeState(stateFile, state);

                // For demonstration, break if not in a persistent loop or after handoff
                // In production this would wait for new state changes
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            System.err.println("[CRITICAL] Supervisor loop failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String evaluate(Map<String, Result> results) {
        String bestVariant = null;
        double maxScore = 0.7; // Threshold

        for (Map.Entry<String, Result> entry : results.entrySet()) {
            Result r = entry.getValue();
            if ("OK".equalsIgnoreCase(r.getStatus()) && r.getScore() > maxScore) {
                maxScore = r.getScore();
                bestVariant = entry.getKey();
            }
        }
        return bestVariant;
    }

    private void updateStatus(File statusFile, String phase, String instanceId) {
        try {
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("phase", phase);
            if (instanceId != null) status.put("instanceId", instanceId);
            mapper.writeValue(statusFile, status);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to update status: " + e.getMessage());
        }
    }

    private String findJar(File variantDir) {
        File targetDir = new File(variantDir, "target");
        if (targetDir.exists()) {
            File[] files = targetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.contains("sources"));
            if (files != null && files.length > 0) {
                return files[0].getAbsolutePath();
            }
        }
        return null;
    }
}
