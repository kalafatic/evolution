package eu.kalafatic.evolution.controller.orchestration.dream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryQuery;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FunctionalityInventoryManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;

/**
 * Associative context retrieval engine for DREAM mode initialization.
 * Retrieves relevant facts, evidence, prior attempts, memories, and genome knowledge
 * associated with a selected experience candidate or manual topic seed.
 */
public class DreamContextRetriever {

    public static class RetrievedDreamContext {
        private final List<String> observedFacts = new ArrayList<>();
        private final List<String> supportingEvidence = new ArrayList<>();
        private final List<String> contradictoryEvidence = new ArrayList<>();
        private final List<String> openQuestions = new ArrayList<>();
        private final Set<String> evidenceReferences = new HashSet<>();

        public List<String> getObservedFacts() {
            return observedFacts;
        }

        public List<String> getSupportingEvidence() {
            return supportingEvidence;
        }

        public List<String> getContradictoryEvidence() {
            return contradictoryEvidence;
        }

        public List<String> getOpenQuestions() {
            return openQuestions;
        }

        public Set<String> getEvidenceReferences() {
            return evidenceReferences;
        }
    }

    public RetrievedDreamContext retrieveContext(String topicSeed, DreamExperienceCandidate candidate,
                                                 FailureMemory failureMemory, IterationMemoryService iterationMemoryService) {
        RetrievedDreamContext context = new RetrievedDreamContext();
        Set<String> seenContent = new HashSet<>();

        String rawSeed = topicSeed != null ? topicSeed.trim() : "";
        if (rawSeed.isEmpty() && candidate != null && candidate.getTopic() != null) {
            rawSeed = candidate.getTopic();
        }
        final String cleanSeed = rawSeed;

        addFact(context, seenContent, "Topic Focus Seed: " + cleanSeed);

        if (candidate != null) {
            addFact(context, seenContent, "Candidate Source: " + candidate.getSource() + " (" + candidate.getCategory() + ")");
            addFact(context, seenContent, "Candidate Rationale: " + candidate.getRationale());
            if (candidate.getEvidenceReferences() != null) {
                context.getEvidenceReferences().addAll(candidate.getEvidenceReferences());
            }
        }

        // 1. Retrieve related FailureMemory records
        if (failureMemory != null && failureMemory.getFingerprints() != null) {
            failureMemory.getFingerprints().forEach((fp, count) -> {
                if (matchesQuery(cleanSeed, fp)) {
                    addEvidence(context, seenContent, "FailureMemory Fingerprint (" + count + "x): " + fp);
                    context.getEvidenceReferences().add("FailureMemory:" + fp);
                }
            });
        }

        // 2. Retrieve related IterationMemoryService records
        if (iterationMemoryService != null && iterationMemoryService.getRecords() != null) {
            for (IterationRecord rec : iterationMemoryService.getRecords()) {
                if (rec == null) continue;
                String goal = rec.getGoal() != null ? rec.getGoal() : "";
                String err = rec.getErrorMessage() != null ? rec.getErrorMessage() : "";

                if (matchesQuery(cleanSeed, goal) || matchesQuery(cleanSeed, err) || matchesQuery(cleanSeed, rec.getBranch())) {
                    if ("SUCCESS".equalsIgnoreCase(rec.getResult())) {
                        addFact(context, seenContent, "Prior Successful Iteration [" + rec.getBranch() + "]: Goal='" + goal + "'");
                    } else {
                        addEvidence(context, seenContent, "Prior Failed Iteration [" + rec.getBranch() + "]: Goal='" + goal + "', Error=" + err);
                    }
                    context.getEvidenceReferences().add("Iteration:" + rec.getBranch());
                }
            }
        }

        // 3. Retrieve related persistent memories from MemoryService
        try {
            MemoryQuery query = new MemoryQuery();
            query.setSearchText(extractSearchKeywords(cleanSeed));
            query.setLimit(10);
            List<MemoryEntry> entries = MemoryService.getInstance().retrieveRelevant(query);

            for (MemoryEntry entry : entries) {
                if (entry != null && entry.getContent() != null) {
                    addFact(context, seenContent, "Persistent Memory (" + entry.getType() + "): " + entry.getContent());
                    context.getEvidenceReferences().add("Memory:" + entry.getId());
                }
            }
        } catch (Exception ignored) {
        }

        // 4. Retrieve related capability metadata from FunctionalityInventoryManager
        try {
            FunctionalityInventoryManager manager = FunctionalityInventoryManager.getInstance();
            if (manager != null) {
                List<FunctionalityInventoryManager.FunctionalityEntry> entries = manager.discoverCoreFunctionalities(null);
                for (FunctionalityInventoryManager.FunctionalityEntry entry : entries) {
                    if (entry != null && (matchesQuery(cleanSeed, entry.name) || matchesQuery(cleanSeed, entry.primaryClass))) {
                        addFact(context, seenContent, "Genome Capability [" + entry.id + " - " + entry.name + "]: Class="
                            + entry.primaryClass + ", Module=" + entry.module + ", Risk=" + entry.risk);
                        context.getEvidenceReferences().add("Functionality:" + entry.id);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 5. Populate default open questions
        context.getOpenQuestions().add("What are the root cause mechanisms underlying " + cleanSeed + "?");
        context.getOpenQuestions().add("What verifiable hypotheses explain the observed behavior and evidence?");

        return context;
    }

    private boolean matchesQuery(String query, String text) {
        if (query == null || text == null || query.isEmpty() || text.isEmpty()) return false;
        String qLower = query.toLowerCase();
        String tLower = text.toLowerCase();

        if (tLower.contains(qLower)) return true;

        String[] tokens = qLower.split("\\s+");
        for (String token : tokens) {
            if (token.length() > 3 && tLower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String extractSearchKeywords(String seed) {
        if (seed == null) return "";
        return seed.replaceAll("^(Recurring Failure:|Memory Observation:|Failed Iteration Goal:|Capability Focus:)\\s*", "").trim();
    }

    private void addFact(RetrievedDreamContext context, Set<String> seen, String fact) {
        if (fact != null && seen.add(fact)) {
            context.getObservedFacts().add(fact);
        }
    }

    private void addEvidence(RetrievedDreamContext context, Set<String> seen, String evidence) {
        if (evidence != null && seen.add(evidence)) {
            context.getSupportingEvidence().add(evidence);
        }
    }
}
