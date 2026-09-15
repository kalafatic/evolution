package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedMessage;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Source-specific adapter for OASST1 dataset files and structures.
 */
public class OASST1Adapter implements DatasetSourceAdapter {

    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant", "system", "tool");

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String pathLower = item.getPath().trim().toLowerCase();
        if (pathLower.contains("oasst") || pathLower.contains("openassistant")) {
            return true;
        }
        if ("OASST1".equalsIgnoreCase(item.getType())) {
            return true;
        }
        // Inspect content header if JSONL file
        if (pathLower.endsWith(".jsonl") || pathLower.endsWith(".jsonl.gz") || pathLower.endsWith(".json")) {
            return isOasstContent(new File(item.getPath()));
        }
        return false;
    }

    private boolean isOasstContent(File file) {
        if (!file.exists() || !file.isFile()) return false;
        try (InputStream fis = file.getName().endsWith(".gz") ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                JSONObject obj = new JSONObject(line.trim());
                if (obj.has("tree_id") || obj.has("message_tree_id") || obj.has("prompter") || obj.has("replies") ||
                    (obj.has("role") && "prompter".equalsIgnoreCase(obj.optString("role"))) ||
                    (obj.has("parent_id") && obj.has("message_id") && obj.has("text"))) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "OASST1Adapter", false, 0, "OASST1", false, "Item or path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists();
        long size = exists ? (file.isFile() ? file.length() : 0) : 0;
        return new DatasetInspection(item, "OASST1Adapter", exists, size, "OASST1", exists, exists ? "OASST1 Dataset Source" : "File/Folder does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists()) {
            context.log("[forge.dataset] [OASST1Adapter] Path does not exist: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing source OASST1: " + file.getName());
        context.log("[forge.dataset] Format: OASST1");

        List<JSONObject> rawFlatMessages = new ArrayList<>();
        List<JSONObject> rawTrees = new ArrayList<>();
        int linesRead = 0;
        int malformedLines = 0;

        List<File> filesToProcess = new ArrayList<>();
        if (file.isDirectory()) {
            File[] children = file.listFiles((dir, name) -> name.contains("oasst") || name.endsWith(".jsonl") || name.endsWith(".jsonl.gz") || name.endsWith(".json"));
            if (children != null) {
                for (File c : children) {
                    if (c.isFile()) filesToProcess.add(c);
                }
            }
        } else {
            filesToProcess.add(file);
        }

        for (File targetFile : filesToProcess) {
            InputStream fis = new FileInputStream(targetFile);
            if (targetFile.getName().endsWith(".gz")) {
                fis = new GZIPInputStream(fis);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (context.isCancelled()) return samples;
                    linesRead++;
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    try {
                        JSONObject obj = new JSONObject(trimmed);
                        if (obj.has("replies") || obj.has("prompt")) {
                            rawTrees.add(obj);
                        } else if (obj.has("role") || obj.has("text") || obj.has("message_id")) {
                            rawFlatMessages.add(obj);
                        }
                    } catch (Exception ex) {
                        malformedLines++;
                    }
                }
            }
        }

        int totalMessages = rawFlatMessages.size();
        int treesRead = rawTrees.size();
        int rejectedCount = malformedLines;

        if (!rawTrees.isEmpty()) {
            for (JSONObject tree : rawTrees) {
                if (context.isCancelled()) break;
                List<NormalizedSample> convs = convertTreeObject(tree, file.getName(), context);
                samples.addAll(convs);
            }
        } else if (!rawFlatMessages.isEmpty()) {
            List<NormalizedSample> convs = convertFlatMessages(rawFlatMessages, file.getName(), context);
            samples.addAll(convs);
        }

        int acceptedCount = 0;
        for (NormalizedSample s : samples) {
            if (s.getConversationMessages() != null) {
                acceptedCount += s.getConversationMessages().size();
            }
        }

        context.log("[forge.dataset] Trees read: " + treesRead);
        context.log("[forge.dataset] Messages read: " + (totalMessages + treesRead));
        context.log("[forge.dataset] Conversations generated: " + samples.size());
        context.log("[forge.dataset] Messages accepted: " + acceptedCount);
        context.log("[forge.dataset] Messages rejected: " + rejectedCount);

        return samples;
    }

    public static List<NormalizedSample> convertFlatMessages(List<JSONObject> rawMessages, String sourceName, DatasetPreparationContext context) {
        List<NormalizedSample> samples = new ArrayList<>();
        if (rawMessages == null || rawMessages.isEmpty()) return samples;

        Map<String, JSONObject> idToMsg = new HashMap<>();
        Map<String, List<JSONObject>> parentToChildren = new HashMap<>();
        Set<String> rootIds = new HashSet<>();

        for (JSONObject obj : rawMessages) {
            // Conservative filtering checks
            if (obj.optBoolean("deleted", false)) continue;
            String text = obj.optString("text", obj.optString("content", ""));
            if (text == null || text.trim().isEmpty()) continue;

            // Language check
            if (context != null && context.getLanguage() != null && obj.has("lang")) {
                String lang = obj.optString("lang", "");
                if (!lang.isEmpty() && !lang.equalsIgnoreCase(context.getLanguage())) {
                    continue;
                }
            }

            String msgId = obj.optString("message_id", obj.optString("id", null));
            if (msgId == null || msgId.trim().isEmpty()) continue;

            idToMsg.put(msgId, obj);

            String parentId = obj.optString("parent_id", null);
            if (parentId == null || parentId.trim().isEmpty() || "null".equalsIgnoreCase(parentId)) {
                rootIds.add(msgId);
            } else {
                parentToChildren.computeIfAbsent(parentId, k -> new ArrayList<>()).add(obj);
            }
        }

        for (String msgId : idToMsg.keySet()) {
            JSONObject obj = idToMsg.get(msgId);
            String parentId = obj.optString("parent_id", null);
            if (parentId != null && !parentId.trim().isEmpty() && !"null".equalsIgnoreCase(parentId)) {
                if (!idToMsg.containsKey(parentId)) {
                    rootIds.add(msgId); // Root if parent reference missing
                }
            }
        }

        for (String rootId : rootIds) {
            JSONObject root = idToMsg.get(rootId);
            if (root != null) {
                String treeId = root.optString("message_tree_id", root.optString("tree_id", rootId));
                traverseFlatTree(root, idToMsg, parentToChildren, new ArrayList<>(), samples, treeId, sourceName);
            }
        }

        return samples;
    }

    private static void traverseFlatTree(JSONObject current,
                                         Map<String, JSONObject> idToMsg,
                                         Map<String, List<JSONObject>> parentToChildren,
                                         List<NormalizedMessage> currentPath,
                                         List<NormalizedSample> samples,
                                         String treeId,
                                         String sourceName) {
        String rawRole = current.optString("role", current.optString("from", null));
        String text = current.optString("text", current.optString("content", ""));
        String msgId = current.optString("message_id", current.optString("id", null));
        String parentId = current.optString("parent_id", null);

        String normalizedRole = NormalizedMessage.normalizeRole(rawRole);
        if (normalizedRole == null || !ALLOWED_ROLES.contains(normalizedRole) || text == null || text.trim().isEmpty()) {
            return;
        }

        NormalizedMessage normMsg = new NormalizedMessage(normalizedRole, text.trim(), msgId, parentId);
        currentPath.add(normMsg);

        List<JSONObject> children = parentToChildren.get(msgId);
        if (children == null || children.isEmpty()) {
            if (isValidConversationPath(currentPath)) {
                String convoId = (msgId != null && !msgId.equals(treeId)) ? (treeId + ":" + msgId) : treeId;
                NormalizedSample sample = NormalizedSample.createConversationSample(convoId, new ArrayList<>(currentPath), sourceName);
                samples.add(sample);
            }
        } else {
            for (JSONObject child : children) {
                traverseFlatTree(child, idToMsg, parentToChildren, currentPath, samples, treeId, sourceName);
            }
        }

        currentPath.remove(currentPath.size() - 1);
    }

    public static List<NormalizedSample> convertTreeObject(JSONObject treeObj, String sourceName, DatasetPreparationContext context) {
        List<NormalizedSample> samples = new ArrayList<>();
        if (treeObj == null) return samples;

        JSONObject root = treeObj;
        if (treeObj.has("prompt") && treeObj.optJSONObject("prompt") != null) {
            root = treeObj.optJSONObject("prompt");
        }

        String treeId = treeObj.optString("message_tree_id", treeObj.optString("message_id", treeObj.optString("tree_id", "oasst-tree")));
        traverseNestedTree(root, new ArrayList<>(), samples, treeId, sourceName, context);
        return samples;
    }

    private static void traverseNestedTree(JSONObject current,
                                           List<NormalizedMessage> currentPath,
                                           List<NormalizedSample> samples,
                                           String treeId,
                                           String sourceName,
                                           DatasetPreparationContext context) {
        if (current == null) return;
        if (current.optBoolean("deleted", false)) return;

        String text = current.optString("text", current.optString("content", ""));
        if (text == null || text.trim().isEmpty()) return;

        if (context != null && context.getLanguage() != null && current.has("lang")) {
            String lang = current.optString("lang", "");
            if (!lang.isEmpty() && !lang.equalsIgnoreCase(context.getLanguage())) return;
        }

        String rawRole = current.optString("role", current.optString("from", null));
        String msgId = current.optString("message_id", current.optString("id", null));
        String parentId = current.optString("parent_id", null);

        String normalizedRole = NormalizedMessage.normalizeRole(rawRole);
        if (normalizedRole == null || !ALLOWED_ROLES.contains(normalizedRole)) {
            return;
        }

        NormalizedMessage normMsg = new NormalizedMessage(normalizedRole, text.trim(), msgId, parentId);
        currentPath.add(normMsg);

        JSONArray replies = current.optJSONArray("replies");
        if (replies == null || replies.length() == 0) {
            if (isValidConversationPath(currentPath)) {
                String convoId = (msgId != null && !msgId.equals(treeId)) ? (treeId + ":" + msgId) : treeId;
                NormalizedSample sample = NormalizedSample.createConversationSample(convoId, new ArrayList<>(currentPath), sourceName);
                samples.add(sample);
            }
        } else {
            for (int i = 0; i < replies.length(); i++) {
                JSONObject child = replies.optJSONObject(i);
                if (child != null) {
                    traverseNestedTree(child, currentPath, samples, treeId, sourceName, context);
                }
            }
        }

        currentPath.remove(currentPath.size() - 1);
    }

    private static boolean isValidConversationPath(List<NormalizedMessage> path) {
        if (path == null || path.isEmpty()) return false;
        boolean hasUser = false;
        boolean hasAssistant = false;
        for (NormalizedMessage msg : path) {
            if (msg == null || msg.getRole() == null || !ALLOWED_ROLES.contains(msg.getRole())) {
                return false;
            }
            if ("user".equals(msg.getRole())) {
                hasUser = true;
            } else if ("assistant".equals(msg.getRole())) {
                hasAssistant = true;
            }
        }
        return hasUser && hasAssistant;
    }
}
