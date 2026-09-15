package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.data.api.NormalizedMessage;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.TrainingSampleType;
import eu.kalafatic.evolution.forge.data.api.artifact.EvoDatasetArtifact;
import eu.kalafatic.evolution.forge.data.impl.source.OasstConverter;
import eu.kalafatic.evolution.forge.data.impl.source.OasstDatasetSource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying structured conversations, NormalizedSample CONVERSATION model extensions,
 * .evodata artifact backward compatibility, and OASST1 message tree conversions.
 */
public class StructuredConversationAndOasstTest {

    @Test
    public void testNormalizedMessageRoleNormalization() {
        assertEquals("user", NormalizedMessage.normalizeRole("prompter"));
        assertEquals("user", NormalizedMessage.normalizeRole("HUMAN"));
        assertEquals("assistant", NormalizedMessage.normalizeRole("assistant"));
        assertEquals("assistant", NormalizedMessage.normalizeRole("bot"));
        assertEquals("system", NormalizedMessage.normalizeRole("system"));
        assertEquals("tool", NormalizedMessage.normalizeRole("tool"));
        assertEquals("custom_role", NormalizedMessage.normalizeRole("custom_role"));
    }

    @Test
    public void testNormalizedSampleConversationSerializationAndDeserialization() {
        List<NormalizedMessage> msgs = new ArrayList<>();
        msgs.add(new NormalizedMessage("prompter", "Hello, can you help me?", "msg_1", null));
        msgs.add(new NormalizedMessage("assistant", "Yes, I am happy to assist you!", "msg_2", "msg_1"));

        NormalizedSample sample = NormalizedSample.createConversationSample("tree_101", msgs, "OASST1_TEST");

        assertEquals(TrainingSampleType.CONVERSATION, sample.getType());
        assertEquals("tree_101", sample.getConversationId());
        assertEquals(2, sample.getConversationMessages().size());
        assertTrue(sample.getTokenCount() > 0);
        assertTrue(sample.getCharCount() > 0);

        String jsonLine = sample.toJsonLine();
        assertTrue(jsonLine.contains("\"type\":\"CONVERSATION\""));
        assertTrue(jsonLine.contains("\"conversationId\":\"tree_101\""));
        assertTrue(jsonLine.contains("\"role\":\"user\""));
        assertTrue(jsonLine.contains("\"role\":\"assistant\""));
        assertTrue(jsonLine.contains("\"messageId\":\"msg_1\""));
        assertTrue(jsonLine.contains("\"parentMessageId\":\"msg_1\""));

        JSONObject json = new JSONObject(jsonLine);
        assertEquals("CONVERSATION", json.getString("type"));
        assertEquals("tree_101", json.getString("conversationId"));
        JSONArray msgArray = json.getJSONArray("messages");
        assertEquals(2, msgArray.length());
        assertEquals("user", msgArray.getJSONObject(0).getString("role"));
        assertEquals("Hello, can you help me?", msgArray.getJSONObject(0).getString("text"));
        assertEquals("msg_1", msgArray.getJSONObject(0).getString("messageId"));
        assertTrue(msgArray.getJSONObject(0).isNull("parentMessageId"));
    }

    @Test
    public void testEvoDatasetArtifactWithConversationAndLegacySamples() throws Exception {
        File tempFile = File.createTempFile("test_convo_artifact", ".evodata");
        tempFile.deleteOnExit();

        List<NormalizedSample> samples = new ArrayList<>();
        // 1. Legacy TEXT sample
        samples.add(NormalizedSample.createTextSample("Legacy wiki text sample content block.", "wikitext"));

        // 2. Legacy INSTRUCTION sample
        samples.add(NormalizedSample.createInstructionSample("Write a python function", "def foo(): pass", "alpaca"));

        // 3. New CONVERSATION sample
        List<NormalizedMessage> msgs = new ArrayList<>();
        msgs.add(new NormalizedMessage("user", "Explain quantum computing in simple terms.", "m1", null));
        msgs.add(new NormalizedMessage("assistant", "Quantum computing uses qubits instead of classical bits.", "m2", "m1"));
        samples.add(NormalizedSample.createConversationSample("q_comp_1", msgs, "OASST1"));

        EvoDatasetArtifact artifact = new EvoDatasetArtifact(tempFile);
        artifact.save(samples, null, null, 0.0);

        EvoDatasetArtifact loaded = EvoDatasetArtifact.load(tempFile);
        assertEquals(3, loaded.getTrainSamples().size());

        NormalizedSample loadedText = loaded.getTrainSamples().get(0);
        assertEquals(TrainingSampleType.TEXT, loadedText.getType());
        assertNull(loadedText.getConversationId());

        NormalizedSample loadedInst = loaded.getTrainSamples().get(1);
        assertEquals(TrainingSampleType.INSTRUCTION, loadedInst.getType());
        assertEquals("Write a python function", loadedInst.getInstruction());

        NormalizedSample loadedConvo = loaded.getTrainSamples().get(2);
        assertEquals(TrainingSampleType.CONVERSATION, loadedConvo.getType());
        assertEquals("q_comp_1", loadedConvo.getConversationId());
        assertEquals(2, loadedConvo.getConversationMessages().size());
        assertEquals("user", loadedConvo.getConversationMessages().get(0).getRole());
        assertEquals("m1", loadedConvo.getConversationMessages().get(0).getMessageId());
        assertEquals("m2", loadedConvo.getConversationMessages().get(1).getMessageId());
        assertEquals("m1", loadedConvo.getConversationMessages().get(1).getParentMessageId());
    }

    @Test
    public void testOasstConverterFlatMessagesBranchingTree() {
        List<JSONObject> rawFlat = new ArrayList<>();

        // Root user prompt
        JSONObject root = new JSONObject();
        root.put("message_id", "root1");
        root.put("parent_id", (Object) null);
        root.put("role", "prompter");
        root.put("text", "What is the capital of France?");
        rawFlat.add(root);

        // Branch A assistant reply
        JSONObject branchA = new JSONObject();
        branchA.put("message_id", "b_a");
        branchA.put("parent_id", "root1");
        branchA.put("role", "assistant");
        branchA.put("text", "The capital of France is Paris.");
        rawFlat.add(branchA);

        // Branch B assistant reply
        JSONObject branchB = new JSONObject();
        branchB.put("message_id", "b_b");
        branchB.put("parent_id", "root1");
        branchB.put("role", "assistant");
        branchB.put("text", "Paris is the capital city of France.");
        rawFlat.add(branchB);

        // User follow-up on Branch A
        JSONObject followUpA = new JSONObject();
        followUpA.put("message_id", "f_a");
        followUpA.put("parent_id", "b_a");
        followUpA.put("role", "prompter");
        followUpA.put("text", "What is its population?");
        rawFlat.add(followUpA);

        // Assistant follow-up on Branch A
        JSONObject followUpAnsA = new JSONObject();
        followUpAnsA.put("message_id", "f_ans_a");
        followUpAnsA.put("parent_id", "f_a");
        followUpAnsA.put("role", "assistant");
        followUpAnsA.put("text", "The population of Paris is around 2.1 million residents.");
        rawFlat.add(followUpAnsA);

        List<NormalizedSample> samples = OasstConverter.convertFlatMessages(rawFlat, "OASST1_FLAT_TEST");

        // Expect two independent conversation paths (root -> B, and root -> A -> f_a -> f_ans_a)
        assertEquals(2, samples.size());

        NormalizedSample sample1 = samples.get(0);
        assertEquals(TrainingSampleType.CONVERSATION, sample1.getType());
        assertEquals(4, sample1.getConversationMessages().size());
        assertEquals("user", sample1.getConversationMessages().get(0).getRole());
        assertEquals("assistant", sample1.getConversationMessages().get(1).getRole());
        assertEquals("user", sample1.getConversationMessages().get(2).getRole());
        assertEquals("assistant", sample1.getConversationMessages().get(3).getRole());

        NormalizedSample sample2 = samples.get(1);
        assertEquals(TrainingSampleType.CONVERSATION, sample2.getType());
        assertEquals(2, sample2.getConversationMessages().size());
        assertEquals("user", sample2.getConversationMessages().get(0).getRole());
        assertEquals("assistant", sample2.getConversationMessages().get(1).getRole());
    }

    @Test
    public void testOasstConverterNestedTreeAndUnknownRoleRejection() {
        JSONObject root = new JSONObject();
        root.put("message_id", "root_node");
        root.put("role", "prompter");
        root.put("text", "Hello system");

        JSONArray replies = new JSONArray();

        // Valid assistant reply
        JSONObject validAssistant = new JSONObject();
        validAssistant.put("message_id", "ans_node");
        validAssistant.put("role", "assistant");
        validAssistant.put("text", "Hello user, how can I help?");
        replies.put(validAssistant);

        // Invalid reply with unknown role
        JSONObject unknownRole = new JSONObject();
        unknownRole.put("message_id", "unknown_node");
        unknownRole.put("role", "alien_entity");
        unknownRole.put("text", "Bleep bloop");
        replies.put(unknownRole);

        root.put("replies", replies);

        List<NormalizedSample> samples = OasstConverter.convertTreeObject(root, "OASST1_TREE_TEST");

        // Only valid root->validAssistant path should be converted; unknown role branch is discarded
        assertEquals(1, samples.size());
        NormalizedSample convo = samples.get(0);
        assertEquals(2, convo.getConversationMessages().size());
        assertEquals("user", convo.getConversationMessages().get(0).getRole());
        assertEquals("assistant", convo.getConversationMessages().get(1).getRole());
    }

    @Test
    public void testOasstDatasetSourceReadingGzipJsonl() throws Exception {
        File tempGzFile = File.createTempFile("oasst_ready_trees", ".jsonl.gz");
        tempGzFile.deleteOnExit();

        JSONObject tree1 = new JSONObject();
        tree1.put("message_id", "tree1_root");
        tree1.put("role", "prompter");
        tree1.put("text", "Can you write a poem about evolution?");

        JSONArray replies1 = new JSONArray();
        JSONObject reply1 = new JSONObject();
        reply1.put("message_id", "tree1_ans");
        reply1.put("role", "assistant");
        reply1.put("text", "Code evolves, adapted to survive, algorithms thriving alive.");
        replies1.put(reply1);
        tree1.put("replies", replies1);

        try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(tempGzFile))) {
            gzos.write((tree1.toString() + "\n").getBytes(StandardCharsets.UTF_8));
        }

        try (OasstDatasetSource source = new OasstDatasetSource(tempGzFile)) {
            source.initialize();
            assertTrue(source.hasNext());
            NormalizedSample sample = source.next();
            assertNotNull(sample);
            assertEquals(TrainingSampleType.CONVERSATION, sample.getType());
            assertEquals(2, sample.getConversationMessages().size());
            assertEquals("Code evolves, adapted to survive, algorithms thriving alive.", sample.getConversationMessages().get(1).getText());
        }
    }
}
