package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.llm.LlmResponse;
import eu.kalafatic.evolution.controller.orchestration.llm.ReasoningProtocol;
import eu.kalafatic.evolution.controller.orchestration.llm.ReasoningProtocolRegistry;
import eu.kalafatic.evolution.controller.orchestration.llm.ResponseKind;
import eu.kalafatic.evolution.controller.orchestration.llm.SeparatedFieldReasoningProtocol;
import eu.kalafatic.evolution.controller.orchestration.llm.StandardReasoningProtocol;
import eu.kalafatic.evolution.controller.orchestration.llm.StreamingReasoningParser;
import eu.kalafatic.evolution.controller.orchestration.llm.TagBasedReasoningProtocol;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

public class ReasoningProtocolTest {

    @Test
    public void testNormalModelFinalOnly() {
        ReasoningProtocol protocol = new StandardReasoningProtocol();
        LlmResponse response = protocol.parse("Hello world");
        assertEquals(ResponseKind.FINAL_ONLY, response.getKind());
        assertEquals("Hello world", response.getContent());
        assertEquals("", response.getReasoning());
        assertTrue(response.isComplete());
    }

    @Test
    public void testExplicitReasoningThinkTags() {
        ReasoningProtocol protocol = new TagBasedReasoningProtocol();
        String raw = "<think>\nStep 1: analyze\nStep 2: plan\n</think>\npublic class Main {}";
        LlmResponse response = protocol.parse(raw);
        assertEquals(ResponseKind.REASONING_AND_FINAL, response.getKind());
        assertEquals("Step 1: analyze\nStep 2: plan", response.getReasoning().trim());
        assertEquals("public class Main {}", response.getContent().trim());
        assertTrue(response.isComplete());
    }

    @Test
    public void testReasoningOnlyEos() {
        ReasoningProtocol protocol = new TagBasedReasoningProtocol();
        String raw = "<think>\nAnalyzing problem structure...";
        LlmResponse response = protocol.parse(raw);
        assertEquals(ResponseKind.REASONING_ONLY, response.getKind());
        assertEquals("Analyzing problem structure...", response.getReasoning().trim());
        assertEquals("", response.getContent().trim());
        assertFalse(response.isComplete());
    }

    @Test
    public void testMissingOpeningTag() {
        ReasoningProtocol protocol = new TagBasedReasoningProtocol();
        String raw = "Analyzing code layout...\n</think>\nHere is the answer";
        LlmResponse response = protocol.parse(raw);
        assertEquals(ResponseKind.REASONING_AND_FINAL, response.getKind());
        assertEquals("Analyzing code layout...", response.getReasoning().trim());
        assertEquals("Here is the answer", response.getContent().trim());
    }

    @Test
    public void testEmptyResponse() {
        ReasoningProtocol protocol = new TagBasedReasoningProtocol();
        LlmResponse response = protocol.parse("");
        assertEquals(ResponseKind.EMPTY, response.getKind());
        assertEquals("", response.getContent());
        assertEquals("", response.getReasoning());
    }

    @Test
    public void testSplitStreamingTags() {
        ReasoningProtocol protocol = new TagBasedReasoningProtocol();
        StreamingReasoningParser parser = protocol.createStreamingParser();

        parser.appendChunk("<thi");
        parser.appendChunk("nk>");
        parser.appendChunk("analyzing requirements");
        parser.appendChunk("</thi");
        parser.appendChunk("nk>");
        parser.appendChunk("final output");

        LlmResponse response = parser.finish();

        assertEquals(ResponseKind.REASONING_AND_FINAL, response.getKind());
        assertEquals("analyzing requirements", response.getReasoning().trim());
        assertEquals("final output", response.getContent().trim());
    }

    @Test
    public void testAlternativeTagsThinkingAndReasoning() {
        ReasoningProtocol protocol = new TagBasedReasoningProtocol();

        LlmResponse r1 = protocol.parse("<thinking>internal thought</thinking>final response");
        assertEquals(ResponseKind.REASONING_AND_FINAL, r1.getKind());
        assertEquals("internal thought", r1.getReasoning().trim());
        assertEquals("final response", r1.getContent().trim());

        LlmResponse r2 = protocol.parse("<reasoning>complex logic</reasoning>final output");
        assertEquals(ResponseKind.REASONING_AND_FINAL, r2.getKind());
        assertEquals("complex logic", r2.getReasoning().trim());
        assertEquals("final output", r2.getContent().trim());
    }

    @Test
    public void testSeparatedFieldResponse() {
        ReasoningProtocol protocol = new SeparatedFieldReasoningProtocol();
        LlmResponse response = protocol.parse("analysis of problem", "answer");
        assertEquals(ResponseKind.REASONING_AND_FINAL, response.getKind());
        assertEquals("analysis of problem", response.getReasoning());
        assertEquals("answer", response.getContent());
    }

    @Test
    public void testFinalAnswerContainingOrdinaryAngleBrackets() {
        ReasoningProtocol protocol = new TagBasedReasoningProtocol();
        String raw = "<think>analyze list</think>List<String> list = new ArrayList<>(); if (a < b) { return true; }";
        LlmResponse response = protocol.parse(raw);
        assertEquals(ResponseKind.REASONING_AND_FINAL, response.getKind());
        assertEquals("analyze list", response.getReasoning().trim());
        assertEquals("List<String> list = new ArrayList<>(); if (a < b) { return true; }", response.getContent().trim());
    }

    @Test
    public void testDarwinBranchReasoningIsolation() {
        BranchVariant branch = new BranchVariant();
        LlmResponse response = LlmResponse.reasoningAndFinal("internal branch reasoning", "public class App {}");

        branch.setLlmResponse(response);

        assertNotNull(branch.getLlmResponse());
        assertEquals("internal branch reasoning", branch.getReasoning());
        assertEquals("internal branch reasoning", branch.getLlmResponse().getReasoning());
        assertEquals("public class App {}", branch.getLlmResponse().getContent());
    }

    @Test
    public void testReasoningProtocolRegistryResolution() {
        ReasoningProtocol r1 = ReasoningProtocolRegistry.resolve("hf.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF");
        assertNotNull(r1);
        assertTrue(r1 instanceof TagBasedReasoningProtocol);

        ReasoningProtocol r2 = ReasoningProtocolRegistry.resolve("qwen2.5-coder:7b");
        assertNotNull(r2);
    }
}
