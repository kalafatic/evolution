package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.forge.controller.service.OllamaService;

public class KnowledgeExtractionAgent {
    private final OllamaService ollama;
    private final boolean useLlm;

    public KnowledgeExtractionAgent(OllamaService ollama, boolean useLlm) {
        this.ollama = ollama;
        this.useLlm = useLlm;
    }

    public List<KnowledgeFact> extract(List<KnowledgeUnit> units) {
        List<KnowledgeFact> facts = new ArrayList<>();
        for (KnowledgeUnit unit : units) {
            if (useLlm && ollama != null) {
                try {
                    List<KnowledgeFact> llmFacts = extractWithLlm(unit);
                    if (llmFacts != null && !llmFacts.isEmpty()) {
                        facts.addAll(llmFacts);
                        continue;
                    }
                } catch (Exception e) {
                    System.err.println("[KnowledgeExtractionAgent] LLM extraction failed for " + unit.getRelativePath() + ", falling back to deterministic extraction: " + e.getMessage());
                }
            }
            // Fallback deterministic extraction
            facts.addAll(extractDeterministic(unit));
        }
        return facts;
    }

    private List<KnowledgeFact> extractWithLlm(KnowledgeUnit unit) throws Exception {
        String prompt = "You are an AI data preparation sub-agent. Analyze the following content from file " + unit.getRelativePath() + " (type: " + unit.getFileType() + ").\n" +
                "Extract key concepts, definitions, component responsibilities, architecture rules, and invariants.\n" +
                "Return a JSON array of objects. Each object MUST contain these fields:\n" +
                "  - 'concept': the name of the concept, class, or responsibility\n" +
                "  - 'definition': a clear explanation or definition\n" +
                "  - 'sectionLocation': e.g., 'Section 1.2' or 'Class declaration' or line number\n" +
                "  - 'evidence': direct quote or reference from the content supporting this fact\n\n" +
                "Content:\n" + unit.getContent() + "\n\n" +
                "Return ONLY a valid JSON array wrapped in a markdown code block.";

        String response = ollama.generate(prompt);
        return parseLlmResponse(response, unit);
    }

    private List<KnowledgeFact> parseLlmResponse(String response, KnowledgeUnit unit) {
        List<KnowledgeFact> list = new ArrayList<>();
        try {
            int start = response.indexOf("[");
            int end = response.lastIndexOf("]");
            if (start != -1 && end != -1 && end > start) {
                String jsonStr = response.substring(start, end + 1);
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String concept = obj.optString("concept", "").trim();
                    String definition = obj.optString("definition", "").trim();
                    String sectionLocation = obj.optString("sectionLocation", "unknown").trim();
                    String evidence = obj.optString("evidence", "").trim();

                    if (!concept.isEmpty() && !definition.isEmpty()) {
                        KnowledgeFact fact = new KnowledgeFact(unit.getRelativePath(), unit.getHash(), sectionLocation, concept, definition, evidence);
                        list.add(fact);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[KnowledgeExtractionAgent] Error parsing LLM response: " + e.getMessage());
        }
        return list;
    }

    public List<KnowledgeFact> extractDeterministic(KnowledgeUnit unit) {
        List<KnowledgeFact> list = new ArrayList<>();
        String content = unit.getContent();
        String fileType = unit.getFileType();

        if ("MARKDOWN".equals(fileType)) {
            // Find headings and bold terms as concepts
            Pattern headingPattern = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
            Matcher matcher = headingPattern.matcher(content);
            while (matcher.find()) {
                String heading = matcher.group(2).trim();
                String location = matcher.group(1).length() + "-level Header";
                // Get next 300 characters as definition
                int start = matcher.end();
                int end = Math.min(content.length(), start + 300);
                String definitionSnippet = content.substring(start, end).trim();
                // strip next headings
                int nextHeading = definitionSnippet.indexOf("\n#");
                if (nextHeading != -1) {
                    definitionSnippet = definitionSnippet.substring(0, nextHeading).trim();
                }

                if (!heading.isEmpty() && !definitionSnippet.isEmpty()) {
                    list.add(new KnowledgeFact(unit.getRelativePath(), unit.getHash(), location, heading, definitionSnippet, "Header: " + heading));
                }
            }

            // Bold terms
            Pattern boldPattern = Pattern.compile("\\*\\*([^\\*]+)\\*\\*\\s*[:-]?\\s*([^\\n\\*\\.]+)", Pattern.MULTILINE);
            Matcher boldMatcher = boldPattern.matcher(content);
            while (boldMatcher.find()) {
                String concept = boldMatcher.group(1).trim();
                String definition = boldMatcher.group(2).trim();
                if (concept.length() > 2 && definition.length() > 10) {
                    list.add(new KnowledgeFact(unit.getRelativePath(), unit.getHash(), "In-text bold term", concept, definition, boldMatcher.group(0)));
                }
            }
        } else if ("JAVA".equals(fileType)) {
            // Class declaration
            Pattern classPattern = Pattern.compile("(?:public\\s+|private\\s+)?(?:class|interface|enum)\\s+(\\w+)", Pattern.MULTILINE);
            Matcher classMatcher = classPattern.matcher(content);
            if (classMatcher.find()) {
                String className = classMatcher.group(1);
                list.add(new KnowledgeFact(unit.getRelativePath(), unit.getHash(), "Class Declaration", className, "Java class/interface/enum structure defined in " + unit.getRelativePath(), "class " + className));
            }

            // Annotated components
            Pattern annotationPattern = Pattern.compile("@(\\w+)\\(([^)]+)\\)", Pattern.MULTILINE);
            Matcher annotationMatcher = annotationPattern.matcher(content);
            while (annotationMatcher.find()) {
                String annotName = annotationMatcher.group(1);
                String annotValue = annotationMatcher.group(2);
                list.add(new KnowledgeFact(unit.getRelativePath(), unit.getHash(), "Annotation", "@" + annotName, "Component annotated with @" + annotName + " with parameters: " + annotValue, annotationMatcher.group(0)));
            }
        } else if ("JSON".equals(fileType) || "XML".equals(fileType) || "POM".equals(fileType)) {
            // Basic project facts
            list.add(new KnowledgeFact(unit.getRelativePath(), unit.getHash(), "Configuration metadata", "Configuration: " + unit.getRelativePath(), "Configuration specification of file type " + fileType, "File name: " + unit.getRelativePath()));
        } else if ("PROPERTIES".equals(fileType)) {
            Pattern propPattern = Pattern.compile("^([^#\\s\\=]+)\\s*=\\s*([^\\n]+)", Pattern.MULTILINE);
            Matcher propMatcher = propPattern.matcher(content);
            while (propMatcher.find()) {
                String key = propMatcher.group(1).trim();
                String value = propMatcher.group(2).trim();
                list.add(new KnowledgeFact(unit.getRelativePath(), unit.getHash(), "Property Definition", key, "Property configuration option set to value: " + value, propMatcher.group(0)));
            }
        } else {
            // Generic fallback
            list.add(new KnowledgeFact(unit.getRelativePath(), unit.getHash(), "Generic Document", unit.getRelativePath(), "Structured metadata or project text parsed from raw files.", "Raw text snippet"));
        }
        return list;
    }
}
