package eu.kalafatic.forge.controller.service;

import eu.kalafatic.forge.model.EvolutionSnapshot;
import eu.kalafatic.forge.model.ForgeModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LineageGraph {
    public List<EvolutionSnapshot> getAncestors(ForgeModel model, String snapshotId) {
        List<EvolutionSnapshot> ancestors = new ArrayList<>();
        String currentId = snapshotId;

        while (currentId != null) {
            String finalCurrentId = currentId;
            Optional<EvolutionSnapshot> snap = model.getEvolutionSnapshots().stream()
                .filter(s -> s.getId().equals(finalCurrentId))
                .findFirst();

            if (snap.isPresent()) {
                ancestors.add(snap.get());
                currentId = snap.get().getParentSnapshotId();
            } else {
                break;
            }
        }
        return ancestors;
    }
}
