package eu.kalafatic.evolution.controller.mediation.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a structured, professional technical specification produced by
 * Darwin reasoning on raw user input.
 */
public class MediatedSpecification {

    private String originalRequest;
    private String primaryObjective;
    private List<String> subObjectives = new ArrayList<>();
    private List<String> scopeDefinitions = new ArrayList<>();
    private List<String> resolvedAmbiguities = new ArrayList<>();
    private List<String> implicitRequirements = new ArrayList<>();
    private List<String> technicalConstraints = new ArrayList<>();
    private List<String> affectedAreas = new ArrayList<>();
    private List<String> technicalRisks = new ArrayList<>();
    private List<String> expectedBehavior = new ArrayList<>();
    private List<String> acceptanceCriteria = new ArrayList<>();

    public MediatedSpecification() {
    }

    public MediatedSpecification(String originalRequest) {
        this.originalRequest = originalRequest;
        this.primaryObjective = originalRequest;
    }

    public String getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(String originalRequest) {
        this.originalRequest = originalRequest;
    }

    public String getPrimaryObjective() {
        return primaryObjective;
    }

    public void setPrimaryObjective(String primaryObjective) {
        this.primaryObjective = primaryObjective;
    }

    public List<String> getSubObjectives() {
        return subObjectives;
    }

    public void setSubObjectives(List<String> subObjectives) {
        this.subObjectives = subObjectives;
    }

    public List<String> getScopeDefinitions() {
        return scopeDefinitions;
    }

    public void setScopeDefinitions(List<String> scopeDefinitions) {
        this.scopeDefinitions = scopeDefinitions;
    }

    public List<String> getResolvedAmbiguities() {
        return resolvedAmbiguities;
    }

    public void setResolvedAmbiguities(List<String> resolvedAmbiguities) {
        this.resolvedAmbiguities = resolvedAmbiguities;
    }

    public List<String> getImplicitRequirements() {
        return implicitRequirements;
    }

    public void setImplicitRequirements(List<String> implicitRequirements) {
        this.implicitRequirements = implicitRequirements;
    }

    public List<String> getTechnicalConstraints() {
        return technicalConstraints;
    }

    public void setTechnicalConstraints(List<String> technicalConstraints) {
        this.technicalConstraints = technicalConstraints;
    }

    public List<String> getAffectedAreas() {
        return affectedAreas;
    }

    public void setAffectedAreas(List<String> affectedAreas) {
        this.affectedAreas = affectedAreas;
    }

    public List<String> getTechnicalRisks() {
        return technicalRisks;
    }

    public void setTechnicalRisks(List<String> technicalRisks) {
        this.technicalRisks = technicalRisks;
    }

    public List<String> getExpectedBehavior() {
        return expectedBehavior;
    }

    public void setExpectedBehavior(List<String> expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }

    public List<String> getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(List<String> acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    /**
     * Formats specification as a comprehensive markdown document.
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Professional Engineering Specification\n\n");

        sb.append("## 1. Original User Request\n");
        sb.append("> ").append(originalRequest != null ? originalRequest : "N/A").append("\n\n");

        sb.append("## 2. Primary Objective\n");
        sb.append(primaryObjective != null ? primaryObjective : "N/A").append("\n\n");

        if (!subObjectives.isEmpty()) {
            sb.append("### Sub-Objectives\n");
            for (String sub : subObjectives) {
                sb.append("- ").append(sub).append("\n");
            }
            sb.append("\n");
        }

        if (!scopeDefinitions.isEmpty()) {
            sb.append("## 3. Scope & Boundaries\n");
            for (String scope : scopeDefinitions) {
                sb.append("- ").append(scope).append("\n");
            }
            sb.append("\n");
        }

        if (!resolvedAmbiguities.isEmpty()) {
            sb.append("## 4. Resolved Ambiguities\n");
            for (String amb : resolvedAmbiguities) {
                sb.append("- ").append(amb).append("\n");
            }
            sb.append("\n");
        }

        if (!implicitRequirements.isEmpty()) {
            sb.append("## 5. Implicit Requirements\n");
            for (String req : implicitRequirements) {
                sb.append("- ").append(req).append("\n");
            }
            sb.append("\n");
        }

        if (!technicalConstraints.isEmpty()) {
            sb.append("## 6. Technical Constraints & Compatibility\n");
            for (String c : technicalConstraints) {
                sb.append("- ").append(c).append("\n");
            }
            sb.append("\n");
        }

        if (!affectedAreas.isEmpty()) {
            sb.append("## 7. Affected Architecture & Subsystems\n");
            for (String a : affectedAreas) {
                sb.append("- ").append(a).append("\n");
            }
            sb.append("\n");
        }

        if (!technicalRisks.isEmpty()) {
            sb.append("## 8. Technical Risks & Side Effects\n");
            for (String r : technicalRisks) {
                sb.append("- ").append(r).append("\n");
            }
            sb.append("\n");
        }

        if (!expectedBehavior.isEmpty()) {
            sb.append("## 9. Expected Changes & Behavior\n");
            for (String b : expectedBehavior) {
                sb.append("- ").append(b).append("\n");
            }
            sb.append("\n");
        }

        if (!acceptanceCriteria.isEmpty()) {
            sb.append("## 10. Acceptance & Verification Criteria\n");
            for (String ac : acceptanceCriteria) {
                sb.append("- [ ] ").append(ac).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
