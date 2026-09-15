package eu.kalafatic.evolution.forge.data.impl.source;

import eu.kalafatic.evolution.forge.data.api.NormalizedMessage;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converter for converting OASST1 message trees and flat message datasets into EVO native NormalizedSample (CONVERSATION) instances.
 */
public class OasstConverter {

    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant", "system", "tool");

    public static List<NormalizedSample> convertFlatMessages(List<JSONObject> rawMessages, String sourceName) {
        List<NormalizedSample> samples = new ArrayList<>();
        if (rawMessages == null || rawMessages.isEmpty()) {
            return samples;
        }

        Map<String, JSONObject> idToMsg = new HashMap<>();
        Map<String, List<JSONObject>> parentToChildren = new HashMap<>();
        Set<String> rootIds = new HashSet<>();

        for (JSONObject obj : rawMessages) {
            String msgId = obj.optString("message_id", obj.optString("id", null));
            if (msgId == null || msgId.trim().isEmpty()) {
                continue;
            }
            idToMsg.put(msgId, obj);

            String parentId = obj.optString("parent_id", null);
            if (parentId == null || parentId.trim().isEmpty() || "null".equalsIgnoreCase(parentId)) {
                rootIds.add(msgId);
            } else {
                parentToChildren.computeIfAbsent(parentId, k -> new ArrayList<>()).add(obj);
            }
        }

        // Identify roots if parent_id was not null but parent message is missing from set
        for (String msgId : idToMsg.keySet()) {
            JSONObject obj = idToMsg.get(msgId);
            String parentId = obj.optString("parent_id", null);
            if (parentId != null && !parentId.trim().isEmpty() && !"null".equalsIgnoreCase(parentId)) {
                if (!idToMsg.containsKey(parentId)) {
                    rootIds.add(msgId);
                }
            }
        }

        for (String rootId : rootIds) {
            JSONObject root = idToMsg.get(rootId);
            if (root != null) {
                String treeId = root.optString("tree_id", root.optString("conversation_id", rootId));
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
        String text = current.optString("text", current.optString("content", current.optString("value", "")));
        String msgId = current.optString("message_id", current.optString("id", null));
        String parentId = current.optString("parent_id", null);

        String normalizedRole = NormalizedMessage.normalizeRole(rawRole);
        if (normalizedRole == null || !ALLOWED_ROLES.contains(normalizedRole) || text == null || text.trim().isEmpty()) {
            // Discard invalid path containing unknown/unsupported role or empty text
            return;
        }

        NormalizedMessage normMsg = new NormalizedMessage(normalizedRole, text.trim(), msgId, parentId);
        currentPath.add(normMsg);

        List<JSONObject> children = parentToChildren.get(msgId);
        if (children == null || children.isEmpty()) {
            // Leaf node: create conversation sample
            if (isValidConversationPath(currentPath)) {
                NormalizedSample sample = NormalizedSample.createConversationSample(treeId, new ArrayList<>(currentPath), sourceName);
                samples.add(sample);
            }
        } else {
            for (JSONObject child : children) {
                traverseFlatTree(child, idToMsg, parentToChildren, currentPath, samples, treeId, sourceName);
            }
        }

        currentPath.remove(currentPath.size() - 1);
    }

    public static List<NormalizedSample> convertTreeObject(JSONObject treeObj, String sourceName) {
        List<NormalizedSample> samples = new ArrayList<>();
        if (treeObj == null) {
            return samples;
        }

        JSONObject root = treeObj;
        if (treeObj.has("prompt") && treeObj.optJSONObject("prompt") != null) {
            root = treeObj.optJSONObject("prompt");
        }

        String treeId = treeObj.optString("message_id", treeObj.optString("tree_id", treeObj.optString("conversation_id", "oasst-tree")));
        traverseNestedTree(root, new ArrayList<>(), samples, treeId, sourceName);
        return samples;
    }

    private static void traverseNestedTree(JSONObject current,
                                           List<NormalizedMessage> currentPath,
                                           List<NormalizedSample> samples,
                                           String treeId,
                                           String sourceName) {
        if (current == null) return;

        String rawRole = current.optString("role", current.optString("from", null));
        String text = current.optString("text", current.optString("content", current.optString("value", "")));
        String msgId = current.optString("message_id", current.optString("id", null));
        String parentId = current.optString("parent_id", null);

        String normalizedRole = NormalizedMessage.normalizeRole(rawRole);
        if (normalizedRole == null || !ALLOWED_ROLES.contains(normalizedRole) || text == null || text.trim().isEmpty()) {
            // Discard path with unknown role
            return;
        }

        NormalizedMessage normMsg = new NormalizedMessage(normalizedRole, text.trim(), msgId, parentId);
        currentPath.add(normMsg);

        JSONArray replies = current.optJSONArray("replies");
        if (replies == null || replies.length() == 0) {
            if (isValidConversationPath(currentPath)) {
                NormalizedSample sample = NormalizedSample.createConversationSample(treeId, new ArrayList<>(currentPath), sourceName);
                samples.add(sample);
            }
        } else {
            for (int i = 0; i < replies.length(); i++) {
                JSONObject child = replies.optJSONObject(i);
                if (child != null) {
                    traverseNestedTree(child, currentPath, samples, treeId, sourceName);
                }
            }
        }

        currentPath.remove(currentPath.size() - 1);
    }

    private static boolean isValidConversationPath(List<NormalizedMessage> path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
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
