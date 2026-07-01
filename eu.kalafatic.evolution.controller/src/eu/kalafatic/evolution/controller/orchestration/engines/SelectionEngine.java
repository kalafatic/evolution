package eu.kalafatic.evolution.controller.orchestration.engines;

import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

public class SelectionEngine {

    public String selectWinnerAuto(List<BranchVariant> variants) {
        return variants.stream()
                .max((v1, v2) -> Double.compare(v1.getScore(), v2.getScore()))
                .map(v -> v.getId())
                .orElse(null);
    }

    public String handleManualSelection(TaskContext context, List<BranchVariant> variants, String goal, IterationManager manager) throws Exception {
        while (true) {
            manager.transition(SystemState.AWAITING_BRANCH_SELECTION, context);
            context.log("[COGNITION] Trajectory Competition: Pausing for semantic selection (Manual Mode).");

            StringBuilder sb = new StringBuilder("Darwin evolved " + variants.size() + " trajectories for your review:\n");
            for (BranchVariant v : variants) {
                String status = (v.getActivationState() == BranchVariant.ActivationState.KEPT) ? " [KEPT]" : "";
                sb.append(String.format("- [%s] %s (Predicted Score: %.2f)%s\n", v.getId(), v.getStrategy(), v.getScore(), status));

                // Display snippets of generated implementation
                if (!v.getActions().isEmpty()) {
                    for (BranchVariant.Action a : v.getActions()) {
                        if (a.getImplementation() != null && !a.getImplementation().isEmpty()) {
                            String code = a.getImplementation();
                            String snippet = code.length() > 200 ? code.substring(0, 200) + "..." : code;
                            sb.append("  > ").append(a.getTarget()).append(":\n")
                              .append("```java\n").append(snippet).append("\n```\n");
                        }
                    }
                }
            }
            sb.append("\nMANUAL MODE: ALL branches preserved. No auto-collapse.\n");
            sb.append("Select a trajectory to execute (e.g. 'Select v0'), Keep to save, or Reject to stop.");

            String input = context.requestInput(sb.toString()).get();
            String trimmed = (input != null) ? input.trim() : "";

            if (trimmed.isEmpty() || "Approved".equalsIgnoreCase(trimmed) || "Yes".equalsIgnoreCase(trimmed) || "Proceed".equalsIgnoreCase(trimmed) || "OK".equalsIgnoreCase(trimmed)) {
                context.log("[KERNEL] User approved best trajectory via fast-approval.");
                return selectWinnerAuto(variants);
            }

            if ("Force Solution".equalsIgnoreCase(trimmed)) {
                context.log("[KERNEL] Force Solution requested. Picking best variant and enabling final convergence.");
                context.getOrchestrationState().getMetadata().put("forceSolution", true);
                context.setAutoApprove(true);
                return selectWinnerAuto(variants);
            }

            if (trimmed.startsWith("Select ") || trimmed.startsWith("Approve variant ")) {
                String manualId = trimmed.startsWith("Select ") ? trimmed.substring(7).trim() : trimmed.substring(16).trim();
                boolean found = variants.stream().anyMatch(v -> v.getId().equals(manualId));
                if (found) {
                    context.log("[KERNEL] User selected trajectory: " + manualId);
                    return manualId;
                } else {
                    context.log("[KERNEL] Warning: Selected trajectory ID not found: " + manualId);
                }
            } else if (trimmed.startsWith("Keep variant ")) {
                String keepId = trimmed.substring(13).trim();
                variants.stream().filter(v -> v.getId().equals(keepId)).findFirst().ifPresent(v -> {
                    v.setActivationState(BranchVariant.ActivationState.KEPT);
                    context.log("[KERNEL] Trajectory " + keepId + " marked as KEPT for final evaluation.");
                });
            } else if (trimmed.startsWith("Reject variant ")) {
                String rejectedId = trimmed.substring(15).trim();
                variants.stream().filter(v -> v.getId().equals(rejectedId)).findFirst().ifPresent(v -> {
                    v.setActivationState(BranchVariant.ActivationState.REJECTED);
                    context.log("[KERNEL] Trajectory " + rejectedId + " rejected by user.");
                });
            } else if ("Rejected".equalsIgnoreCase(trimmed) || "Reject".equalsIgnoreCase(trimmed) || "No".equalsIgnoreCase(trimmed)) {
                return "FAILED";
            } else if (trimmed.startsWith("Propose:") || trimmed.startsWith("{")) {
                context.log("[KERNEL] User injected a new trajectory. Integrating as a first-class candidate.");

                Object gmObj = context.getOrchestrationState().getMetadata().get("goalModel");
                GoalModel goalModel = null;
                if (gmObj instanceof GoalModel) {
                    goalModel = (GoalModel) gmObj;
                } else if (gmObj instanceof Map) {
                    goalModel = new com.fasterxml.jackson.databind.ObjectMapper()
                        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .convertValue(gmObj, GoalModel.class);
                }

                BranchVariant userVariant = createUserVariant(trimmed, goalModel, context);
                variants.add(userVariant);
                context.log("[KERNEL] User trajectory " + userVariant.getId() + " added to the evolutionary pool.");
            } else {
                context.log("[KERNEL] User provided guidance: " + trimmed + ". Refining intent and regenerating trajectories.");
                String newGoal = goal + " (Guidance: " + trimmed + ")";
                context.getOrchestrationState().setRawInput(newGoal);
                context.getOrchestrationState().getMetadata().remove("goalModel");
                if (context.getOrchestrator().getSelfDevSession() != null) {
                     context.getOrchestrator().getSelfDevSession().setInitialRequest(newGoal);
                }
                return "REGENERATE";
            }
        }
    }

    private BranchVariant createUserVariant(String input, GoalModel goal, TaskContext context) {
        BranchVariant v = new BranchVariant();
        v.setId("v-user-" + System.currentTimeMillis());
        v.setBranchId(v.getId());
        v.setLineageId(context.getSessionId());
        v.setActivationState(BranchVariant.ActivationState.ARCHIVED);
        v.setStrategyType("USER_PROPOSAL");

        String strategyText = input.startsWith("Propose:") ? input.substring(8).trim() : input;

        if (strategyText.trim().startsWith("{")) {
            try {
                org.json.JSONObject obj = new org.json.JSONObject(strategyText);
                v.setStrategy(obj.optString("strategy", "User-defined strategy"));
                v.setSurvivalArgument(obj.optString("survival_argument", "User injection"));
                v.setTradeoffs(obj.optString("tradeoffs", "Explicit user directive"));
            } catch (Exception e) {
                v.setStrategy(strategyText);
            }
        } else {
            v.setStrategy(strategyText);
            v.setSurvivalArgument("Direct user proposal");
            v.setTradeoffs("User-defined trajectory");
        }

        v.setScore(0.95);
        v.setBranchName("exp/user/" + sanitizeForBranch(v.getStrategy()));

        eu.kalafatic.evolution.controller.trajectory.Trajectory t = new eu.kalafatic.evolution.controller.trajectory.Trajectory(v.getId(), v.getStrategy());
        t.setFitnessScore(v.getScore());
        v.setTrajectoryId(t.getTrajectoryId());

        if (context.getKernelContext().getMemoryService().getTrajectoryMemory() != null) {
            context.getKernelContext().getMemoryService().getTrajectoryMemory().recordTrajectory(t);
        }

        return v;
    }

    private String sanitizeForBranch(String s) {
        if (s == null || s.isEmpty()) return "unnamed";
        String sanitized = s.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("-")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.isEmpty()) return "unnamed";
        return sanitized.substring(0, Math.min(sanitized.length(), 30));
    }
}
