package eu.kalafatic.evolution.controller.orchestration.dto;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.enums.SelectionMode;
import java.util.List;

public class SelectionResult {
    private final BranchVariant winner;
    private final List<BranchVariant> allVariants;
    private final boolean rejected;
    private final String rejectedNodeId;
    private final String rejectionReason;
    private final SelectionMode mode;

    public SelectionResult(BranchVariant winner, List<BranchVariant> allVariants, boolean rejected, String rejectedNodeId, String rejectionReason, SelectionMode mode) {
        this.winner = winner;
        this.allVariants = allVariants;
        this.rejected = rejected;
        this.rejectedNodeId = rejectedNodeId;
        this.rejectionReason = rejectionReason;
        this.mode = mode;
    }

    public BranchVariant getWinner() { return winner; }
    public List<BranchVariant> getAllVariants() { return allVariants; }
    public boolean isRejected() { return rejected; }
    public String getRejectedNodeId() { return rejectedNodeId; }
    public String getRejectionReason() { return rejectionReason; }
    public SelectionMode getMode() { return mode; }
}
