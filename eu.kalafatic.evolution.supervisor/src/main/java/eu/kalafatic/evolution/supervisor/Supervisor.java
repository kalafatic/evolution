package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Supervisor {
    private final File baseDir;
    private final ResultReader reader = new ResultReader();
    private final ProcessRunner runner = new ProcessRunner();
    private final IterationManager iterationManager;

    public Supervisor(File baseDir) {
        this.baseDir = baseDir;
        this.iterationManager = new IterationManager(baseDir);
    }

    public void run() {
        File stateFile = new File(baseDir, "self-dev.json");
        File workspaceDir = new File(baseDir, "workspace");

        try {
            while (true) {
                State state = reader.readState(stateFile);
                if (!state.isActive()) {
                    System.out.println("[SUPERVISOR] Self-dev inactive. Exiting.");
                    break;
                }

                System.out.println("\n[SUPERVISOR] Starting Iteration: " + state.getIteration());
                File iterDir = iterationManager.prepareIteration(state.getIteration());

                Map<String, Result> variantResults = new HashMap<>();
                File[] variants = iterDir.listFiles(File::isDirectory);
                if (variants != null) {
                    for (File variant : variants) {
                        System.out.println("\n[VARIANT] Processing " + variant.getName());
                        if (runner.runBuild(variant)) {
                            // Assume the JAR is produced in target/app.jar (adjust if needed)
                            // In this case, we'll look for a JAR in the variant directory or target
                            String jarName = findJar(variant);
                            if (jarName != null) {
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

                String winner = evaluate(variantResults);
                if (winner != null) {
                    System.out.println("\n[WINNER] Selected " + winner);
                    iterationManager.promoteVariant(new File(iterDir, winner), workspaceDir);
                } else {
                    System.out.println("\n[REJECT] No variant met acceptance criteria.");
                }

                state.setIteration(state.getIteration() + 1);
                reader.writeState(stateFile, state);
            }
        } catch (IOException e) {
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
