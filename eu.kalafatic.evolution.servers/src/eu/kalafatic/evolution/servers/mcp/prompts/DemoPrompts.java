package eu.kalafatic.evolution.servers.mcp.prompts;

import com.fasterxml.jackson.databind.JsonNode;
import eu.kalafatic.evolution.servers.mcp.model.Prompt;
import java.util.*;

public class DemoPrompts {
    public static void registerAll(PromptRegistry registry) {
        registry.register(new Prompt("architecture-review", "Reviews the system architecture", Collections.emptyList()),
            args -> "Please review the following architectural components...");

        registry.register(new Prompt("bug-analysis", "Analyzes a given bug report", Collections.singletonList(new Prompt.PromptArgument("report", "Bug report content", true))),
            args -> "Analyze the bug reported here: " + (args != null && args.has("report") ? args.get("report").asText() : "N/A"));

        registry.register(new Prompt("project-summary", "Generates a summary of the project", Collections.emptyList()),
            args -> "Provide a high-level summary of the evolution platform...");

        registry.register(new Prompt("metadata-discovery", "Discovers metadata from the environment", Collections.emptyList()),
            args -> "Search for technical metadata in the current project context...");
    }
}
