package eu.kalafatic.evolution.forge.data.impl.collector;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common training data preparation pipeline.
 *
 * Sequence:
 * COLLECTION -> NORMALIZATION -> ANALYSIS -> CHUNKING -> CONTEXT/RELATIONSHIPS -> TRAINING EXAMPLE GENERATION -> QUALITY FILTER -> DEDUPLICATION -> JSONL DATASET
 */
public class DataPreparationPipeline {

    public static class Chunk {
        private String source;
        private String content;
        private String title;
        private String hash;

        public Chunk(String source, String title, String content, String hash) {
            this.source = source;
            this.title = title;
            this.content = content;
            this.hash = hash;
        }

        public String getSource() { return source; }
        public String getContent() { return content; }
        public String getTitle() { return title; }
        public String getHash() { return hash; }
    }

    public static class PreparedRecord {
        private String instruction;
        private String response;
        private String source;
        private String hash;

        public PreparedRecord(String instruction, String response, String source, String hash) {
            this.instruction = instruction;
            this.response = response;
            this.source = source;
            this.hash = hash;
        }

        public String getInstruction() { return instruction; }
        public String getResponse() { return response; }
        public String getSource() { return source; }
        public String getHash() { return hash; }

        public String toJsonLLine() {
            String escInst = escapeJson(instruction);
            String escResp = escapeJson(response);
            return "{\"prompt\":\"" + escInst + "\",\"response\":\"" + escResp + "\",\"source\":\"" + escapeJson(source) + "\"}";
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t");
        }
    }

    private final TrainingDataCollectorManager collectorManager;

    public DataPreparationPipeline() {
        this(new TrainingDataCollectorManager());
    }

    public DataPreparationPipeline(TrainingDataCollectorManager collectorManager) {
        this.collectorManager = collectorManager;
    }

    public List<PreparedRecord> runPipeline(CollectionContext context, Set<CollectorType> selectedSources) {
        // 1. COLLECTION
        CollectionResult collectionResult = collectorManager.collect(context, selectedSources);
        List<TrainingDataItem> rawItems = collectionResult.getItems();

        // 2. NORMALIZATION
        List<TrainingDataItem> normalizedItems = normalize(rawItems);

        // 3. ANALYSIS & CHUNKING
        List<Chunk> chunks = chunk(normalizedItems);

        // 4. TRAINING EXAMPLE GENERATION
        List<PreparedRecord> rawRecords = generateTrainingExamples(chunks);

        // 5. QUALITY FILTER
        List<PreparedRecord> filteredRecords = qualityFilter(rawRecords);

        // 6. DEDUPLICATION
        List<PreparedRecord> deduplicatedRecords = deduplicate(filteredRecords);

        return deduplicatedRecords;
    }

    private List<TrainingDataItem> normalize(List<TrainingDataItem> items) {
        List<TrainingDataItem> normalized = new ArrayList<>();
        if (items == null) return normalized;

        for (TrainingDataItem item : items) {
            if (item == null || "COLLECTION_SUMMARY".equals(item.getTitle())) {
                continue; // Skip metadata summary item
            }
            if (item.getContent() == null || item.getContent().trim().isEmpty()) {
                continue;
            }
            // Normalize line endings and trim whitespace
            String cleanContent = item.getContent().replace("\r\n", "\n").replace("\r", "\n").trim();
            item.setContent(cleanContent);
            normalized.add(item);
        }
        return normalized;
    }

    private List<Chunk> chunk(List<TrainingDataItem> items) {
        List<Chunk> chunks = new ArrayList<>();
        int maxChunkLength = 1500;

        for (TrainingDataItem item : items) {
            String content = item.getContent();
            String source = item.getSource();
            String title = item.getTitle();

            if (content.length() <= maxChunkLength) {
                chunks.add(new Chunk(source, title, content, computeHash(content)));
            } else {
                // Split large content into overlapping chunks
                int offset = 0;
                int overlap = 200;
                while (offset < content.length()) {
                    int end = Math.min(content.length(), offset + maxChunkLength);
                    String sub = content.substring(offset, end);
                    chunks.add(new Chunk(source, title, sub, computeHash(sub)));
                    if (end == content.length()) break;
                    offset += (maxChunkLength - overlap);
                }
            }
        }
        return chunks;
    }

    private List<PreparedRecord> generateTrainingExamples(List<Chunk> chunks) {
        List<PreparedRecord> records = new ArrayList<>();
        for (Chunk chunk : chunks) {
            String instruction = "Explain code and architecture details for source artifact: " + chunk.getTitle();
            String response = chunk.getContent();
            String hash = computeHash(instruction + "\n" + response);
            records.add(new PreparedRecord(instruction, response, chunk.getSource(), hash));
        }
        return records;
    }

    private List<PreparedRecord> qualityFilter(List<PreparedRecord> records) {
        List<PreparedRecord> filtered = new ArrayList<>();
        for (PreparedRecord rec : records) {
            if (rec.getResponse() != null && rec.getResponse().length() >= 10) {
                filtered.add(rec);
            }
        }
        return filtered;
    }

    private List<PreparedRecord> deduplicate(List<PreparedRecord> records) {
        List<PreparedRecord> result = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();

        for (PreparedRecord record : records) {
            if (!seenHashes.contains(record.getHash())) {
                seenHashes.add(record.getHash());
                result.add(record);
            }
        }
        return result;
    }

    public String exportToJsonL(List<PreparedRecord> records) {
        StringBuilder sb = new StringBuilder();
        for (PreparedRecord r : records) {
            sb.append(r.toJsonLLine()).append("\n");
        }
        return sb.toString();
    }

    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    public TrainingDataCollectorManager getCollectorManager() {
        return collectorManager;
    }
}
