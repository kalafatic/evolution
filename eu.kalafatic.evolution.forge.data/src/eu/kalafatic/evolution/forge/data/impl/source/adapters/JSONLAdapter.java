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
 * Schema-tolerant with fallback support for arbitrary JSON schemas.
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
        if (type == TrainingSampleType.CONVERSATION || json.has("conversationMessages") || json.has("messages") || json.has("conversations") || json.has("dialog")) {
            JSONArray msgArray = json.optJSONArray("conversationMessages");
            if (msgArray == null) msgArray = json.optJSONArray("messages");
            if (msgArray == null) msgArray = json.optJSONArray("conversations");
            if (msgArray == null) msgArray = json.optJSONArray("dialog");

            if (msgArray != null && msgArray.length() > 0) {
                List<NormalizedMessage> normMsgs = new ArrayList<>();
                for (int i = 0; i < msgArray.length(); i++) {
                    JSONObject mObj = msgArray.optJSONObject(i);
                    if (mObj != null) {
                        String role = mObj.optString("role", mObj.optString("from", mObj.optString("speaker", "user")));
                        String text = mObj.optString("text", mObj.optString("content", mObj.optString("value", mObj.optString("message", ""))));
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
        String instruction = optAnyString(json, "instruction", "prompt", "input", "question", "problem", "user_prompt", "query");
        String response = optAnyString(json, "response", "output", "completion", "answer", "solution", "assistant_response", "reply");
        if (instruction != null && response != null && !instruction.trim().isEmpty() && !response.trim().isEmpty()) {
            return NormalizedSample.createInstructionSample(instruction, response, sourceName);
        }

        // 3. Plain Text
        String text = optAnyString(json, "text", "content", "document", "body", "article", "code", "raw", "context", "chunk");
        if (text != null && !text.trim().isEmpty()) {
            return NormalizedSample.createTextSample(text, sourceName);
        }

        // 4. Fallback for unknown schema: collect non-empty string fields excluding metadata infrastructure keys
        List<String> textValues = new ArrayList<>();
        for (String key : json.keySet()) {
            String lowerKey = key.toLowerCase();
            if (isMetadataKey(lowerKey)) {
                continue;
            }
            Object val = json.opt(key);
            if (val instanceof String) {
                String str = ((String) val).trim();
                if (str.length() >= 10 && !str.startsWith("http://") && !str.startsWith("https://")) {
                    textValues.add(str);
                }
            }
        }
        if (!textValues.isEmpty()) {
            if (textValues.size() >= 2) {
                return NormalizedSample.createInstructionSample(textValues.get(0), String.join("\n", textValues.subList(1, textValues.size())), sourceName);
            } else {
                return NormalizedSample.createTextSample(textValues.get(0), sourceName);
            }
        }

        return null;
    }

    private static boolean isMetadataKey(String key) {
        return key.equals("url") || key.equals("etag") || key.equals("sha1") || key.equals("sha256") ||
               key.equals("md5") || key.equals("hash") || key.equals("checksum") || key.equals("download_url") ||
               key.equals("dataset_name") || key.equals("config") || key.equals("split") || key.equals("revision") ||
               key.equals("num_bytes") || key.equals("created_at") || key.equals("modified_at") || key.equals("status") ||
               key.equals("error") || key.equals("version") || key.equals("author") || key.equals("license") ||
               key.equals("repository") || key.equals("repo_id") || key.equals("splits") || key.equals("features");
    }

    private static String optAnyString(JSONObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.isNull(key)) {
                String val = json.optString(key, "").trim();
                if (!val.isEmpty()) {
                    return val;
                }
            }
        }
        return null;
    }
}
