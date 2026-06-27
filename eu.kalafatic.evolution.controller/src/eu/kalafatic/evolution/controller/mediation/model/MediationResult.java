package eu.kalafatic.evolution.controller.mediation.model;

import java.util.List;

public class MediationResult {
    private final TargetSnapshot snapshot;
    private final TargetRealityModel model;
    private final List<Hotspot> hotspots;
    private final List<MediationCandidate> candidates;
    private final MediationCandidate winner;
    private final MediationDelta delta;
    private final long durationMs;
    
    public MediationResult(TargetSnapshot snapshot, TargetRealityModel model,
            List<Hotspot> hotspots, List<MediationCandidate> candidates,
            MediationCandidate winner, MediationDelta delta, long durationMs) {
        this.snapshot = snapshot;
        this.model = model;
        this.hotspots = hotspots;
        this.candidates = candidates;
        this.winner = winner;
        this.delta = delta;
        this.durationMs = durationMs;
    }
    
    public TargetSnapshot getSnapshot() { return snapshot; }
    public TargetRealityModel getModel() { return model; }
    public List<Hotspot> getHotspots() { return hotspots; }
    public List<MediationCandidate> getCandidates() { return candidates; }
    public MediationCandidate getWinner() { return winner; }
    public MediationDelta getDelta() { return delta; }
    public long getDurationMs() { return durationMs; }
    public boolean hasChanges() { return delta != null && delta.hasChanges(); }
}