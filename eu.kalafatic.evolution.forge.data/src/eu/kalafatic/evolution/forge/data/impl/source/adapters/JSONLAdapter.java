package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedMessage;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Universal streaming adapter for JSONL / JSONL.GZ dataset files.
 */
public class JSONLAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        return path.endsWith(".jsonl") || path.endsWith(".jsonl.gz") || "JSONL".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "JSONLAdapter", false, 0, "jsonl", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "JSONLAdapter", exists, size, "jsonl", exists, exists ? "JSONL Dataset File" : "File does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [JSONLAdapter] File not found: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing source JSONL: " + file.getName());

        int linesCount = 0;
        int malformedCount = 0;

        InputStream fis = new FileInputStream(file);
        if (file.getName().endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (context.isCancelled()) break;
                linesCount++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                try {
                    JSONObject json = new JSONObject(trimmed);
                    NormalizedSample sample = parseJsonObject(json, file.getName());
                    if (sample != null) {
                        samples.add(sample);
                    } else {
                        malformedCount++;
                    }
                } catch (Exception ex) {
                    // Reject malformed JSON lines
                    malformedCount++;
                }
            }
        }

        context.log("[forge.dataset] JSONL Read lines: " + linesCount + ", Valid samples: " + samples.size() + ", Malformed rejected: " + malformedCount);
        return samples;
    }

    public static NormalizedSample parseJsonObject(JSONObject json, String sourceName) {
        if (json == null) return null;

        String typeStr = json.optString("type", "");
        TrainingSampleType type = TrainingSampleType.fromString(typeStr);

        // 1. Conversation / Chat
        if (type == TrainingSampleType.CONVERSATION || json.has("conversationMessages") || json.has("messages")) {
            JSONArray msgArray = json.optJSONArray("conversationMessages");
            if (msgArray == null) {
                msgArray = json.optJSONArray("messages");
            }
            if (msgArray != null && msgArray.length() > 0) {
                List<NormalizedMessage> normMsgs = new ArrayList<>();
                for (int i = 0; i < msgArray.length(); i++) {
                    JSONObject mObj = msgArray.optJSONObject(i);
                    if (mObj != null) {
                        String role = mObj.optString("role", mObj.optString("from", "user"));
                        String text = mObj.optString("text", mObj.optString("content", mObj.optString("value", "")));
                        String msgId = mObj.has("messageId") && !mObj.isNull("messageId") ? mObj.optString("messageId") : null;
                        String parentId = mObj.has("parentMessageId") && !mObj.isNull("parentMessageId") ? mObj.optString("parentMessageId") : null;
                        if (!text.trim().isEmpty()) {
                            normMsgs.add(new NormalizedMessage(role, text, msgId, parentId));
                        }
                    }
                }
                if (!normMsgs.isEmpty()) {
                    String convoId = json.optString("conversationId", json.optString("id", null));
                    return NormalizedSample.createConversationSample(convoId, normMsgs, sourceName);
                }
            }
        }

        // 2. Instruction / Response
        String instruction = json.optString("instruction", json.optString("prompt", json.optString("input", null)));
        String response = json.optString("response", json.optString("output", json.optString("completion", null)));
        if (instruction != null && response != null && !instruction.trim().isEmpty() && !response.trim().isEmpty()) {
            return NormalizedSample.createInstructionSample(instruction, response, sourceName);
        }

        // 3. Plain Text
        String text = json.optString("text", json.optString("content", json.optString("document", json.optString("body", null))));
        if (text != null && !text.trim().isEmpty()) {
            return NormalizedSample.createTextSample(text, sourceName);
        }

        return null;
    }
}
