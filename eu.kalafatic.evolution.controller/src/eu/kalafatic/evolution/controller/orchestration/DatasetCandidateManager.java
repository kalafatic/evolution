package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.forge.data.api.discovery.DatasetCandidate;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton manager bridging discovered remote dataset candidates to EMF model persistence.
 * Uses ForgeSession.getModelState().getDatasetBindings() to persist and load candidate lists.
 */
public class DatasetCandidateManager {

    private static final DatasetCandidateManager INSTANCE = new DatasetCandidateManager();

    private DatasetCandidateManager() {}

    public static DatasetCandidateManager getInstance() {
        return INSTANCE;
    }

    /**
     * Loads saved dataset candidates for the given ForgeSession from EMF persistence.
     */
    public List<DatasetCandidate> getCandidates(ForgeSession session) {
        List<DatasetCandidate> candidates = new ArrayList<>();
        if (session == null || session.getModelState() == null) return candidates;

        String datasetBindings = session.getModelState().getDatasetBindings();
        if (datasetBindings == null || datasetBindings.trim().isEmpty() || datasetBindings.equals("[]")) {
            return candidates;
        }

        try {
            JSONArray arr = new JSONArray(datasetBindings);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                DatasetCandidate cand = DatasetCandidate.fromJsonObject(obj);
                if (cand != null) {
                    candidates.add(cand);
                }
            }
        } catch (Exception ex) {
            // Non-JSON or legacy dataset binding format fallback
        }

        candidates.sort((c1, c2) -> Integer.compare(c2.getCompatibilityScore(), c1.getCompatibilityScore()));
        return candidates;
    }

    /**
     * Loads saved dataset candidates by session ID.
     */
    public List<DatasetCandidate> getCandidates(String sessionId) {
        ForgeSession session = ForgeSessionManager.getInstance().findSession(sessionId);
        return getCandidates(session);
    }

    /**
     * Saves candidate list to EMF persistence for the given ForgeSession without duplicates.
     */
    public synchronized void saveCandidates(ForgeSession session, List<DatasetCandidate> candidates) {
        if (session == null || session.getModelState() == null) return;

        Map<String, DatasetCandidate> candidateMap = new LinkedHashMap<>();
        if (candidates != null) {
            for (DatasetCandidate candidate : candidates) {
                if (candidate != null && candidate.getId() != null) {
                    candidateMap.put(candidate.getId(), candidate);
                }
            }
        }

        JSONArray arr = new JSONArray();
        for (DatasetCandidate candidate : candidateMap.values()) {
            arr.put(candidate.toJsonObject());
        }

        session.getModelState().setDatasetBindings(arr.toString());
        session.setLastModified(System.currentTimeMillis());

        try {
            Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
            if (orch != null && orch.eResource() != null) {
                ProjectModelManager.getInstance().saveResource(orch.eResource());
            }
        } catch (Exception ex) {
            // EMF persistence save warning log
        }
    }

    /**
     * Merges newly discovered dataset candidates into the persisted candidate list without duplicates.
     */
    public synchronized void addCandidates(ForgeSession session, List<DatasetCandidate> newCandidates) {
        if (session == null || newCandidates == null || newCandidates.isEmpty()) return;

        List<DatasetCandidate> existing = getCandidates(session);
        Map<String, DatasetCandidate> map = new LinkedHashMap<>();

        for (DatasetCandidate c : existing) {
            if (c != null && c.getId() != null) {
                map.put(c.getId(), c);
            }
        }

        for (DatasetCandidate c : newCandidates) {
            if (c != null && c.getId() != null) {
                // Update or add fresh discovery metadata
                map.put(c.getId(), c);
            }
        }

        List<DatasetCandidate> merged = new ArrayList<>(map.values());
        merged.sort((c1, c2) -> Integer.compare(c2.getCompatibilityScore(), c1.getCompatibilityScore()));

        saveCandidates(session, merged);
    }

    /**
     * Removes a candidate by ID or repository name from EMF persistence.
     */
    public synchronized boolean removeCandidate(ForgeSession session, String candidateIdOrRepo) {
        if (session == null || candidateIdOrRepo == null || candidateIdOrRepo.trim().isEmpty()) return false;

        List<DatasetCandidate> existing = getCandidates(session);
        boolean removed = existing.removeIf(c -> c.getId().equalsIgnoreCase(candidateIdOrRepo) || c.getRepository().equalsIgnoreCase(candidateIdOrRepo));

        if (removed) {
            saveCandidates(session, existing);
        }
        return removed;
    }

    /**
     * Clears all candidates for the given ForgeSession in EMF persistence.
     */
    public synchronized void clearCandidates(ForgeSession session) {
        if (session == null || session.getModelState() == null) return;
        session.getModelState().setDatasetBindings("[]");
        session.setLastModified(System.currentTimeMillis());

        try {
            Orchestrator orch = OrchestratorServiceImpl.getInstance().getOrchestrator();
            if (orch != null && orch.eResource() != null) {
                ProjectModelManager.getInstance().saveResource(orch.eResource());
            }
        } catch (Exception ignored) {}
    }

    /**
     * Returns saved candidates sorted descending by compatibility score to serve as fallback candidates.
     */
    public List<DatasetCandidate> getFallbackCandidates(ForgeSession session) {
        List<DatasetCandidate> list = getCandidates(session);
        list.sort((c1, c2) -> Integer.compare(c2.getCompatibilityScore(), c1.getCompatibilityScore()));
        return list;
    }
}
