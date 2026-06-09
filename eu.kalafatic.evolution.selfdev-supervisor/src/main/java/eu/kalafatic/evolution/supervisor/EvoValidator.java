package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EvoValidator {

    private static final String EVO_REGEX = "//\\s*@evo:(\\d+):(\\w+)\\s+reason=([\\w-]+)";
    private static final Pattern EVO_PATTERN = Pattern.compile(EVO_REGEX);

    public void validate(File variantDir, EvoPlan plan) throws IOException {
        System.out.println("[VALIDATOR] Validating variant: " + variantDir.getName());

        List<File> javaFiles = Files.walk(variantDir.toPath())
                .filter(p -> p.toString().endsWith(".java"))
                .map(p -> p.toFile())
                .collect(Collectors.toList());

        for (File file : javaFiles) {
            String content = new String(Files.readAllBytes(file.toPath()));
            boolean isPlanned = isPlannedFile(file, plan.getFiles());

            validateFile(file, content, plan, isPlanned);
        }
    }

    private boolean isPlannedFile(File file, List<String> plannedFiles) {
        if (plannedFiles == null) return false;
        String fileName = file.getName();
        return plannedFiles.contains(fileName);
    }

    private void validateFile(File file, String content, EvoPlan plan, boolean isPlanned) {
        Matcher matcher = EVO_PATTERN.matcher(content);
        boolean foundAnyCorrect = false;

        while (matcher.find()) {
            int iter = Integer.parseInt(matcher.group(1));
            String variant = matcher.group(2);

            // Rule 3: No mismatched iteration/variant allowed (for current target)
            if (iter == plan.getIteration() && variant.equals(plan.getVariant())) {
                foundAnyCorrect = true;
            } else if (iter > plan.getIteration()) {
                // Safety: can't have future iterations
                fail("Future @evo marker found in " + file.getName() + ": " + iter + ":" + variant);
            }
        }

        // Rule 1: Each file in plan.json MUST contain at least one: @evo:<iteration>:<variant>
        if (isPlanned && !foundAnyCorrect) {
            fail("EVO VALIDATION FAILED: missing marker in planned file " + file.getName() + " for " + plan.getIteration() + ":" + plan.getVariant());
        }

        // Rule 2: No file outside plan.json may contain: @evo:<iteration>:<variant>
        if (!isPlanned && foundAnyCorrect) {
            fail("EVO VALIDATION FAILED: unexpected marker in unplanned file " + file.getName() + " for " + plan.getIteration() + ":" + plan.getVariant());
        }
    }

    private void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
