package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.FileDescriptor;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetDescriptor;

/**
 * Selects high-value context while avoiding token floods.
 */
public class ContextCurator {

    public List<String> curate(TargetDescriptor target) {
        // Selection strategy:
        // 1. Entry points
        // 2. Main configuration files (pom.xml, package.json)
        // 3. High-density semantic markers (Interfaces, Components)

        List<String> curatedPaths = new ArrayList<>();

        curatedPaths.addAll(target.getFiles().stream()
            .filter(f -> f.getTags().contains("Entry Point"))
            .map(f -> f.getPath())
            .collect(Collectors.toList()));

        curatedPaths.addAll(target.getFiles().stream()
            .filter(f -> f.getPath().endsWith("pom.xml") || f.getPath().endsWith("package.json"))
            .map(f -> f.getPath())
            .collect(Collectors.toList()));

        curatedPaths.addAll(target.getFiles().stream()
            .filter(f -> f.getTags().contains("Interface") || f.getTags().contains("Spring Component"))
            .limit(10)
            .map(f -> f.getPath())
            .collect(Collectors.toList()));

        return curatedPaths.stream().distinct().collect(Collectors.toList());
    }
}
