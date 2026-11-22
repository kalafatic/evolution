package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal source adapter for existing .evodata native training dataset archives.
 *
 * <p>Two container variants are supported:</p>
 * <ol>
 *   <li>The canonical native archive: a ZIP container written by
 *       {@link EvoDatasetArtifact#save} (entries {@code data.jsonl}, {@code val_data.jsonl},
 *       {@code metadata.json}, {@code report.txt}).</li>
 *   <li>The portable normalized fixture: a plain UTF-8 JSON Lines file (one
 *       {@code TrainingRecord} per line) that uses the {@code .evodata} extension.
 *       Documented record fields: {@code content}, {@code metadata}, {@code source},
 *       {@code sourceId}, {@code documentId}, {@code language}, {@code type},
 *       {@code timestamp}, {@code quality}. Sample content, IDs and provenance are
 *       preserved 1:1; only the serialization container differs.</li>
 * </ol>
 */
public class EVODataAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        return path.endsWith(".evodata") || "EVO_DATA".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "EVODataAdapter", false, 0, "evodata", false, "Item or path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "EVODataAdapter", exists, size, "evodata", exists, exists ? "Valid .evodata file" : "File does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [EVODataAdapter] Warning: File not found " + item.getPath());
            return samples;
        }

        if (isZipContainer(file)) {
            context.log("[forge.dataset] Loading native .evodata ZIP archive: " + file.getName());
            EvoDatasetArtifact artifact = EvoDatasetArtifact.load(file);
            if (artifact != null && artifact.getSamples() != null) {
                samples.addAll(artifact.getSamples());
                context.log("[forge.dataset] Reused " + samples.size() + " native samples from " + file.getName());
            }
            return samples;
        }

        context.log("[forge.dataset] Loading portable JSONL .evodata fixture: " + file.getName());
        List<NormalizedSample> fixtureSamples = readPortableJsonlFixture(file, context);
        samples.addAll(fixtureSamples);
        context.log("[forge.dataset] Converted " + fixtureSamples.size() + " portable fixture records from " + file.getName());
        return samples;
    }

    private boolean isZipContainer(File file) {
        byte[] head = new byte[2];
        int read = 0;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            while (read < 2) {
                int n = in.read(head, read, 2 - read);
                if (n < 0) break;
                read += n;
            }
        } catch (Exception e) {
            return false;
        }
        return read >= 2 && head[0] == 'P' && head[1] == 'K';
    }

    private List<NormalizedSample> readPortableJsonlFixture(File file, DatasetPreparationContext context) {
        List<NormalizedSample> samples = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;
                NormalizedSample sample = parseFixtureRecord(line);
                if (sample != null) {
                    samples.add(sample);
                }
            }
        } catch (Exception e) {
            context.log("[forge.dataset] [EVODataAdapter] Error reading portable fixture " + file.getName() + ": " + e.getMessage());
        }
        return samples;
    }

    /**
     * Parses one portable fixture record preserving all content and provenance.
     * The {@code content} field uses the canonical instruction/response layout
     * ("### Instruction" / "### Response"); those markers are mapped onto
     * {@link NormalizedSample} instruction/response fields so the documented
     * response-only loss masking applies. All other fields (sourceId, documentId,
     * timestamp, quality, language and the metadata object) are preserved.
     */
    private NormalizedSample parseFixtureRecord(String line) {
        JSONObject obj = new JSONObject(line);

        String content = obj.optString("content", null);
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        NormalizedSample sample = new NormalizedSample();
        sample.setType(TrainingSampleType.INSTRUCTION);

        String[] parts = splitInstructionResponse(content);
        sample.setInstruction(parts[0]);
        sample.setResponse(parts[1]);

        JSONObject metadata = obj.optJSONObject("metadata");
        String sourceId = obj.optString("sourceId", null);
        String source = obj.optString("source", "forge-portable-fixture");
        String documentId = obj.optString("documentId", null);
        String language = obj.optString("language", "en");
        String recordType = obj.optString("type", "instruction_response");
        String timestamp = obj.optString("timestamp", null);
        double quality = obj.optDouble("quality", 1.0);

        if (sourceId != null && !sourceId.isEmpty()) {
            sample.setId(sourceId);
        }
        sample.setSource(source);
        sample.setLanguage(language);
        sample.setQualityScore(quality);

        if (metadata != null) {
            String recordId = metadata.optString("recordId", null);
            String category = metadata.optString("category", null);
            String split = metadata.optString("split", null);
            JSONArray tags = metadata.optJSONArray("tags");
            if (category != null && !category.isEmpty()) {
                sample.setCategory(category);
            }
            if (recordId != null && !recordId.isEmpty()) {
                sample.getMetadata().put("recordId", recordId);
            }
            if (split != null && !split.isEmpty()) {
                sample.getMetadata().put("split", split);
            }
            if (tags != null) {
                List<String> tagList = new ArrayList<>();
                for (int i = 0; i < tags.length(); i++) {
                    tagList.add(tags.optString(i));
                }
                sample.getMetadata().put("tags", tagList);
            }
        }
        if (sourceId != null && !sourceId.isEmpty()) {
            sample.getMetadata().put("sourceId", sourceId);
        }
        if (documentId != null && !documentId.isEmpty()) {
            sample.getMetadata().put("documentId", documentId);
        }
        if (timestamp != null && !timestamp.isEmpty()) {
            sample.getMetadata().put("timestamp", timestamp);
        }
        sample.getMetadata().put("recordType", recordType);

        sample.recalculateCountsAndHash();
        return sample;
    }

    /**
     * Splits a canonical "### Instruction\n...\n\n### Response\n..." content block
     * into [instruction, response]. Returns the whole content as instruction with an
     * empty response when the canonical markers are absent, so no information is lost.
     */
    public static String[] splitInstructionResponse(String content) {
        String text = content == null ? "" : content;
        String instrMarker = "### Instruction";
        String respMarker = "\n\n### Response";

        int respIdx = text.indexOf(respMarker);
        String instructionPart;
        String responsePart;
        if (respIdx >= 0) {
            instructionPart = text.substring(0, respIdx);
            responsePart = text.substring(respIdx + respMarker.length());
        } else {
            instructionPart = text;
            responsePart = "";
        }

        if (instructionPart.startsWith(instrMarker)) {
            instructionPart = instructionPart.substring(instrMarker.length());
        }
        instructionPart = instructionPart.replaceFirst("^[\\r\\n]+", "").trim();

        if (responsePart.startsWith(":\n")) {
            responsePart = responsePart.substring(2);
        }
        responsePart = responsePart.replaceFirst("^[\\r\\n]+", "").trim();

        return new String[]{instructionPart, responsePart};
    }
}
