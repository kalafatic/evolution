package eu.kalafatic.evolution.controller.manager;

import java.io.File;


import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

public class EnvironmentSuggestionService {

    public static class Suggestion {
        public String field;
        public String currentValue;
        public String suggestedValue;
        public String reason;
        public boolean isMissing;

        public Suggestion(String field, String current, String suggested, String reason, boolean missing) {
            this.field = field;
            this.currentValue = current;
            this.suggestedValue = suggested;
            this.reason = reason;
            this.isMissing = missing;
        }
    }

    public static List<Suggestion> getSuggestions(Orchestrator orchestrator, File projectRoot) {
        List<Suggestion> suggestions = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();

        // 1. Git Suggestions
        boolean hasGit = new File(projectRoot, ".git").exists();
        if (!hasGit) {
            suggestions.add(new Suggestion("Git", "Not initialized", "Initialize repository", "Git is required for version control and better diff tracking.", true));
        }

        // 2. Maven Suggestions
        String mavenCmd = os.contains("win") ? "mvn.cmd" : "mvn";
        boolean mavenFound = checkCommand(mavenCmd);
        if (!mavenFound) {
            suggestions.add(new Suggestion("Maven", "Not found", "Install Maven", "Maven is used for building and testing Java projects.", true));
        }

        // 3. Ollama Suggestions
        if (orchestrator.getOllama() == null || orchestrator.getOllama().getUrl() == null || orchestrator.getOllama().getUrl().isEmpty()) {
            OllamaConfigManager.OllamaDefaults defaults = OllamaConfigManager.getDefaults();
            suggestions.add(new Suggestion("Ollama URL", "Not set", defaults.apiUrl, "Local AI provider endpoint.", true));
        }

        if (orchestrator.getOllama() != null && (orchestrator.getOllama().getModel() == null || orchestrator.getOllama().getModel().isEmpty())) {
            suggestions.add(new Suggestion("Ollama Model", "Not set", "llama3.2:3b", "Compact and efficient model for local orchestration.", true));
        }

        // 4. Java Version
        String javaVer = System.getProperty("java.version");
        if (javaVer.startsWith("1.8") || javaVer.startsWith("8")) {
            suggestions.add(new Suggestion("Java Version", javaVer, "17 or 21", "The project works best with modern LTS Java versions.", false));
        }

        // 5. Database (Optional but helpful if model has it)
        if (orchestrator.getDatabase() != null && (orchestrator.getDatabase().getUrl() == null || orchestrator.getDatabase().getUrl().isEmpty())) {
             suggestions.add(new Suggestion("Database URL", "Empty", "jdbc:h2:mem:testdb", "Default H2 in-memory database for testing.", false));
        }

        return suggestions;
    }

    private static boolean checkCommand(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd + " -version");
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void applySetup(Orchestrator orchestrator, List<Suggestion> selectedSuggestions) {
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
        for (Suggestion s : selectedSuggestions) {
            if ("Ollama URL".equals(s.field)) {
                if (orchestrator.getOllama() == null) orchestrator.setOllama(factory.createOllama());
                orchestrator.getOllama().setUrl(s.suggestedValue);
            } else if ("Ollama Model".equals(s.field)) {
                if (orchestrator.getOllama() == null) orchestrator.setOllama(factory.createOllama());
                orchestrator.getOllama().setModel(s.suggestedValue);
            } else if ("Database URL".equals(s.field)) {
                if (orchestrator.getDatabase() == null) orchestrator.setDatabase(factory.createDatabase());
                orchestrator.getDatabase().setUrl(s.suggestedValue);
            }
        }
    }
}
