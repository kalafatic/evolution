package eu.kalafatic.utils.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized AI Metadata type extending EvoMetadata for Functionality Intelligence.
 * Represents a single EVO capability with 6 evaluation dimensions (0-100 scale),
 * primary Java class mapping, and evaluation provenance evidence.
 */
public class FunctionalityMetadata extends EvoMetadata {

    private String functionalityId;
    private String functionalityName;
    private String primaryClass;
    private String fqcn;
    private String moduleName;
    private String sourcePath;
    private String implementationStatus = "IMPLEMENTED"; // IMPLEMENTED, PARTIAL, PROVISIONAL

    // Evaluation Dimensions (0-100 scale, -1 indicates UNKNOWN)
    private int importanceScore0To100 = -1;
    private int usageEstimate0To100 = -1;
    private int complexityScore0To100 = -1;
    private int centralityScore0To100 = -1;
    private int maturityScore0To100 = -1;
    private int riskScore0To100 = -1;

    // Provenance & Evidence
    private String evaluationRationale = "";
    private String evaluationMethod = "STATIC"; // STATIC, AI, RUNTIME
    private double confidenceLevel = 1.0;
    private String evaluationTimestamp = "";
    private String sourceRevision = "";
    private List<String> relatedFunctionalities = new ArrayList<>();

    public String getFunctionalityId() { return functionalityId; }
    public void setFunctionalityId(String functionalityId) { this.functionalityId = functionalityId; }

    public String getFunctionalityName() { return functionalityName; }
    public void setFunctionalityName(String functionalityName) { this.functionalityName = functionalityName; }

    public String getPrimaryClass() { return primaryClass; }
    public void setPrimaryClass(String primaryClass) { this.primaryClass = primaryClass; }

    public String getFqcn() { return fqcn; }
    public void setFqcn(String fqcn) { this.fqcn = fqcn; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getImplementationStatus() { return implementationStatus; }
    public void setImplementationStatus(String implementationStatus) { this.implementationStatus = implementationStatus; }

    public int getImportanceScore0To100() { return importanceScore0To100; }
    public void setImportanceScore0To100(int importanceScore0To100) { this.importanceScore0To100 = importanceScore0To100; }

    public int getUsageEstimate0To100() { return usageEstimate0To100; }
    public void setUsageEstimate0To100(int usageEstimate0To100) { this.usageEstimate0To100 = usageEstimate0To100; }

    public int getComplexityScore0To100() { return complexityScore0To100; }
    public void setComplexityScore0To100(int complexityScore0To100) { this.complexityScore0To100 = complexityScore0To100; }

    public int getCentralityScore0To100() { return centralityScore0To100; }
    public void setCentralityScore0To100(int centralityScore0To100) { this.centralityScore0To100 = centralityScore0To100; }

    public int getMaturityScore0To100() { return maturityScore0To100; }
    public void setMaturityScore0To100(int maturityScore0To100) { this.maturityScore0To100 = maturityScore0To100; }

    public int getRiskScore0To100() { return riskScore0To100; }
    public void setRiskScore0To100(int riskScore0To100) { this.riskScore0To100 = riskScore0To100; }

    public String getEvaluationRationale() { return evaluationRationale; }
    public void setEvaluationRationale(String evaluationRationale) { this.evaluationRationale = evaluationRationale; }

    public String getEvaluationMethod() { return evaluationMethod; }
    public void setEvaluationMethod(String evaluationMethod) { this.evaluationMethod = evaluationMethod; }

    public double getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }

    public String getEvaluationTimestamp() { return evaluationTimestamp; }
    public void setEvaluationTimestamp(String evaluationTimestamp) { this.evaluationTimestamp = evaluationTimestamp; }

    public String getSourceRevision() { return sourceRevision; }
    public void setSourceRevision(String sourceRevision) { this.sourceRevision = sourceRevision; }

    public List<String> getRelatedFunctionalities() { return relatedFunctionalities; }
    public void setRelatedFunctionalities(List<String> relatedFunctionalities) { this.relatedFunctionalities = relatedFunctionalities; }

    /**
     * Computes a weighted overall assessment score (0-100) based on importance, maturity, centrality, complexity, and risk.
     */
    public int getOverallScore() {
        int imp = importanceScore0To100 >= 0 ? importanceScore0To100 : 50;
        int mat = maturityScore0To100 >= 0 ? maturityScore0To100 : 50;
        int cen = centralityScore0To100 >= 0 ? centralityScore0To100 : 50;
        int cmp = complexityScore0To100 >= 0 ? complexityScore0To100 : 50;
        int rsk = riskScore0To100 >= 0 ? riskScore0To100 : 50;

        double overall = (imp * 0.35) + (mat * 0.25) + (cen * 0.20) + (cmp * 0.10) + ((100 - rsk) * 0.10);
        return (int) Math.round(overall);
    }
}
