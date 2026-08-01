package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.util.ArrayList;
import java.util.List;

public class TrainingDataAgent {

    public List<TrainingRecord> generate(List<KnowledgeFact> facts) {
        List<TrainingRecord> records = new ArrayList<>();
        for (KnowledgeFact fact : facts) {
            String concept = fact.getConcept();
            String definition = fact.getDefinition();
            String path = fact.getSourcePath();
            String hash = fact.getSourceHash();
            String evidence = fact.getEvidence();

            // 1. RAW_LANGUAGE_MODEL record
            String rawText = "In " + path + ", the concept of " + concept + " is defined as: " + definition + " (evidence: " + evidence + ")";
            records.add(new TrainingRecord("", rawText, "", path, hash, evidence, "RAW_LANGUAGE_MODEL", "AUTHORITATIVE_LOCAL"));

            // 2. INSTRUCTION/RESPONSE record (Question & Answer)
            String question = "What is the role or definition of " + concept + " according to the project specifications?";
            String answer = "According to the specifications in " + path + ", " + concept + " is defined as follows:\n\n" + definition;
            records.add(new TrainingRecord(question, answer, "", path, hash, evidence, "INSTRUCTION", "LLM_GENERATED"));

            // 3. ARCHITECTURE_KNOWLEDGE record
            String archQuestion = "Which component is responsible for " + concept + " and how is it structured in " + path + "?";
            String archAnswer = "In the system architecture outlined in " + path + ", " + concept + " is responsible for the following:\n\n" + definition + "\n\nEvidence: " + evidence;
            records.add(new TrainingRecord(archQuestion, archAnswer, "", path, hash, evidence, "ARCHITECTURE_KNOWLEDGE", "VALIDATED_DERIVED"));

            // 4. CODE_UNDERSTANDING record (especially if Java source or configuration)
            if (path.endsWith(".java") || path.endsWith(".xml") || path.endsWith(".json")) {
                String codeQuestion = "Explain the implementation or structure of " + concept + " in the codebase.";
                String codeAnswer = "The file " + path + " defines " + concept + " with the following context:\n\n" + definition + "\n\nSource reference: " + evidence;
                records.add(new TrainingRecord(codeQuestion, codeAnswer, "", path, hash, evidence, "CODE_UNDERSTANDING", "AUTHORITATIVE_LOCAL"));
            }
        }
        return records;
    }
}
