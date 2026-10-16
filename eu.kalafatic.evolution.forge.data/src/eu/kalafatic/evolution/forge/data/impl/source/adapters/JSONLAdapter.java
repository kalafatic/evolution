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
        int emptyLinesCount = 0;
        int validJsonCount = 0;
        int invalidJsonCount = 0;
        int rejectedConversationCount = 0;
        int unsupportedStructureCount = 0;

        InputStream fis = new FileInputStream(file);
        if (file.getName().endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (context != null && context.isCancelled()) break;
                linesCount++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    emptyLinesCount++;
                    continue;
                }

                JSONObject json;
                try {
                    json = new JSONObject(trimmed);
                    validJsonCount++;
                } catch (Exception ex) {
                    invalidJsonCount++;
                    continue;
                }

                NormalizedSample sample = parseJsonObject(json, file.getName(), linesCount);
                if (sample != null) {
                    samples.add(sample);
                } else {
                    if (json.has("text") && json.optString("text", "").contains("<|im_start|>")) {
                        rejectedConversationCount++;
                    } else {
                        unsupportedStructureCount++;
                    }
                }
            }
        }

        if (context != null) {
            context.log(String.format(
                "[forge.dataset] JSONL Read lines: %d (empty: %d), Valid JSON: %d, Invalid JSON: %d, Invalid conversations: %d, Unsupported structures: %d, Accepted samples: %d",
                linesCount, emptyLinesCount, validJsonCount, invalidJsonCount, rejectedConversationCount, unsupportedStructureCount, samples.size()
            ));
        }
        return samples;
    }

    public static NormalizedSample parseJsonObject(JSONObject json, String sourceName) {
        return parseJsonObject(json, sourceName, -1);
    }

    public static NormalizedSample parseJsonObject(JSONObject json, String sourceName, long lineIndex) {
        if (json == null) return null;

        String recordId = json.optString("conversationId", json.optString("id", null));
        if (recordId == null || recordId.trim().isEmpty()) {
            recordId = lineIndex >= 0 ? (sourceName + "#" + lineIndex) : sourceName;
        }

        if (json.has("text") && !json.isNull("text")) {
            String txtVal = json.optString("text", "");
            if (txtVal.contains("<|im_start|>")) {
                return parseImStartConversation(txtVal, recordId, sourceName);
            }
        }

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

    public static NormalizedSample parseImStartConversation(String text, String convoId, String sourceName) {
        if (text == null || !text.contains("<|im_start|>")) {
            return null;
        }
        List<NormalizedMessage> normMsgs = new ArrayList<>();
        int pos = 0;
        while (pos < text.length()) {
            int startIdx = text.indexOf("<|im_start|>", pos);
            if (startIdx == -1) {
                // If there is non-whitespace leftover after last <|im_end|>, reject as malformed conversation
                if (!text.substring(pos).trim().isEmpty()) {
                    return null;
                }
                break;
            }
            // Disallow unparsed non-whitespace text before/between <|im_start|> markers
            if (!text.substring(pos, startIdx).trim().isEmpty()) {
                return null;
            }

            int roleStart = startIdx + "<|im_start|>".length();
            int lineEnd = text.indexOf("\n", roleStart);
            if (lineEnd == -1) {
                return null;
            }

            String headerLine = text.substring(roleStart, lineEnd);
            if (headerLine.endsWith("\r")) {
                headerLine = headerLine.substring(0, headerLine.length() - 1);
            }
            String rawRole = headerLine.trim();
            String normalizedRole = NormalizedMessage.normalizeRole(rawRole);
            if (normalizedRole == null) {
                return null;
            }

            int endIdx = text.indexOf("<|im_end|>", lineEnd);
            if (endIdx == -1) {
                return null;
            }

            String msgText = text.substring(lineEnd + 1, endIdx);

            // Strip leading \r\n or \n and trailing \r\n or \n added by marker formatting without stripping internal whitespace/indentation
            if (msgText.startsWith("\r\n")) {
                msgText = msgText.substring(2);
            } else if (msgText.startsWith("\n")) {
                msgText = msgText.substring(1);
            }

            if (msgText.endsWith("\r\n")) {
                msgText = msgText.substring(0, msgText.length() - 2);
            } else if (msgText.endsWith("\n")) {
                msgText = msgText.substring(0, msgText.length() - 1);
            } else if (msgText.endsWith("\r")) {
                msgText = msgText.substring(0, msgText.length() - 1);
            }

            normMsgs.add(new NormalizedMessage(normalizedRole, msgText));
            pos = endIdx + "<|im_end|>".length();
        }

        if (normMsgs.isEmpty()) {
            return null;
        }

        boolean hasUser = false;
        boolean hasAssistant = false;
        for (NormalizedMessage msg : normMsgs) {
            if ("user".equals(msg.getRole())) {
                hasUser = true;
            } else if ("assistant".equals(msg.getRole())) {
                hasAssistant = true;
            }
        }

        if (!hasUser || !hasAssistant) {
            return null;
        }

        return NormalizedSample.createConversationSample(convoId, normMsgs, sourceName);
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
