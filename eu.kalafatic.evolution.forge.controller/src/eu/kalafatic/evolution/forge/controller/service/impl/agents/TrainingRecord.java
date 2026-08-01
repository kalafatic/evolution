package eu.kalafatic.evolution.forge.controller.service.impl.agents;

public class TrainingRecord {
    private final String instruction;
    private final String response;
    private final String input;
    private final String sourcePath;
    private final String sourceHash;
    private final String evidence;
    private final String type; // e.g. RAW_LANGUAGE_MODEL, INSTRUCTION, etc.
    private final String sourceTrust; // AUTHORITATIVE_LOCAL, LLM_GENERATED, VALIDATED_DERIVED

    public TrainingRecord(String instruction, String response, String input, String sourcePath, String sourceHash, String evidence, String type, String sourceTrust) {
        this.instruction = instruction;
        this.response = response;
        this.input = input;
        this.sourcePath = sourcePath;
        this.sourceHash = sourceHash;
        this.evidence = evidence;
        this.type = type;
        this.sourceTrust = sourceTrust;
    }

    public String getInstruction() {
        return instruction;
    }

    public String getResponse() {
        return response;
    }

    public String getInput() {
        return input;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getType() {
        return type;
    }

    public String getSourceTrust() {
        return sourceTrust;
    }
}
