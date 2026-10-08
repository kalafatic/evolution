package eu.kalafatic.evolution.controller.memory;

import java.util.List;

/**
 * Transforms retrieved MemoryEntry objects into compact, prompt-friendly context blocks
 * for injection into LLM prompts and neural assistance pipelines.
 */
public class MemoryContextProvider {

    public String buildMemoryContext(String queryText, MemoryScope scope, String associatedId, int maxEntries) {
        MemoryQuery query = new MemoryQuery()
                .setSearchText(queryText)
                .setScope(scope)
                .setAssociatedId(associatedId)
                .setMinImportance(MemoryImportance.LOW)
                .setLimit(maxEntries > 0 ? maxEntries : 10);

        List<MemoryEntry> entries = MemoryService.getInstance().retrieveRelevant(query);
        if (entries == null || entries.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### USER & PROJECT MEMORY\n");
        sb.append("Consider the following persistent preferences, constraints, and facts:\n");

        for (MemoryEntry entry : entries) {
            String content = entry.getContent();
            if (content == null || content.trim().isEmpty()) continue;

            sb.append("- [").append(entry.getType() != null ? entry.getType().name() : "FACT").append("]");
            if (entry.getImportance() != null) {
                sb.append(" [").append(entry.getImportance().name()).append("]");
            }
            sb.append(" ").append(content.trim()).append("\n");
        }

        return sb.toString().trim();
    }

    public String buildMemoryContext(String queryText) {
        return buildMemoryContext(queryText, null, null, 8);
    }
}
