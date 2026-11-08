package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Test;

import eu.kalafatic.evolution.forge.model.inference.InferenceExecutionTrace;
import eu.kalafatic.evolution.forge.model.inference.InferenceExecutionTrace.EventType;
import eu.kalafatic.evolution.forge.model.inference.InferenceProvenance;

public class InferenceProvenanceTest {

    @Test
    public void testInferenceProvenanceLifecycleAndSerialization() {
        String infId = UUID.randomUUID().toString();
        String sessId = "session-test-01";
        InferenceExecutionTrace trace = new InferenceExecutionTrace(infId, sessId);
        assertNotNull(trace);
        assertEquals(infId, trace.getInferenceId());
        assertEquals(sessId, trace.getSessionId());

        InferenceProvenance prov = trace.getProvenance();
        assertNotNull(prov);
        assertEquals(infId, prov.getInferenceId());
        assertEquals(sessId, prov.getSessionId());

        trace.addEvent(EventType.REQUEST_RECEIVED, "Request received", "model=test");
        trace.addEvent(EventType.MODEL_RESOLVED, "Model resolved", "path=/tmp/test.evo");

        prov.setArtifactName("test.evo");
        prov.setArtifactPath("/tmp/test.evo");
        prov.setArtifactFormat("EVO");
        prov.setArtifactChecksum("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        prov.setVerificationStatus("VERIFIED");
        prov.setActualEngine("ReferenceEvoInferenceEngine");
        prov.setRuntimeProvider("NativeEvoInferenceEngine");
        prov.setBackend("NATIVE_EVO");
        prov.setNative(true);
        prov.setPromptTokens(10);
        prov.setCompletionTokens(20);
        prov.setTotalTokens(30);
        prov.setExecutionDurationMs(150);

        JSONObject json = prov.toJsonObject();
        assertNotNull(json);
        assertEquals("test.evo", json.getString("artifactName"));
        assertEquals("NATIVE_EVO", json.getString("backend"));
        assertTrue(json.getBoolean("isNative"));
        assertEquals("VERIFIED", json.getString("verificationStatus"));
        assertEquals(30, json.getInt("totalTokens"));

        InferenceProvenance restored = InferenceProvenance.fromJsonObject(json);
        assertNotNull(restored);
        assertEquals(prov.getInferenceId(), restored.getInferenceId());
        assertEquals(prov.getArtifactName(), restored.getArtifactName());
        assertEquals(prov.getBackend(), restored.getBackend());
        assertEquals(prov.isNative(), restored.isNative());
        assertEquals(prov.getVerificationStatus(), restored.getVerificationStatus());
        assertEquals(prov.getPromptTokens(), restored.getPromptTokens());
        assertEquals(prov.getCompletionTokens(), restored.getCompletionTokens());
        assertEquals(prov.getTotalTokens(), restored.getTotalTokens());
        assertEquals(prov.getExecutionDurationMs(), restored.getExecutionDurationMs());
    }

    @Test
    public void testFallbackProvenanceTracking() {
        String infId = UUID.randomUUID().toString();
        InferenceExecutionTrace trace = new InferenceExecutionTrace(infId, "fallback-session");
        InferenceProvenance prov = trace.getProvenance();

        // Native failed -> Fallback to Ollama
        prov.setFallbackActivated(true);
        prov.setFallbackReason("Native EVO inference failed: corrupt tensor weights");
        prov.setVerificationStatus("VERIFIED");
        prov.setBackend("OLLAMA");
        prov.setActualEngine("OllamaService");
        prov.setRuntimeProvider("Ollama");
        prov.setNative(false);

        JSONObject json = prov.toJsonObject();
        assertTrue(json.getBoolean("fallbackActivated"));
        assertEquals("OLLAMA", json.getString("backend"));
        assertFalse(json.getBoolean("isNative"));
        assertTrue(json.getString("fallbackReason").contains("corrupt tensor weights"));
    }

    @Test
    public void testGgufConversionLineage() {
        InferenceProvenance prov = new InferenceProvenance();
        prov.setArtifactName("evo.gguf");
        prov.setArtifactFormat("GGUF");
        prov.setSourceEvoArtifact("forged-model.evo");
        prov.setExportedGgufArtifact("evo.gguf");
        prov.setGgufChecksum("9f83c60517b4d02f83684e902b41042c747f3978e30e3e374320b7a6dd1a60b0");
        prov.setArtifactRelationship("EXPORTED_GGUF");
        prov.setBackend("LLAMA_CPP");
        prov.setActualEngine("LlamaService");
        prov.setNative(false);
        prov.setVerificationStatus("VERIFIED");

        JSONObject json = prov.toJsonObject();
        assertEquals("EXPORTED_GGUF", json.getString("artifactRelationship"));
        assertEquals("forged-model.evo", json.getString("sourceEvoArtifact"));
        assertEquals("evo.gguf", json.getString("exportedGgufArtifact"));
        assertEquals("LLAMA_CPP", json.getString("backend"));
        assertFalse(json.getBoolean("isNative"));
    }

    @Test
    public void testSessionIsolation() {
        InferenceExecutionTrace trace1 = new InferenceExecutionTrace("inf-1", "session-A");
        InferenceExecutionTrace trace2 = new InferenceExecutionTrace("inf-2", "session-B");

        trace1.getProvenance().setBackend("NATIVE_EVO");
        trace2.getProvenance().setBackend("OLLAMA");

        assertEquals("session-A", trace1.getSessionId());
        assertEquals("session-B", trace2.getSessionId());
        assertEquals("NATIVE_EVO", trace1.getProvenance().getBackend());
        assertEquals("OLLAMA", trace2.getProvenance().getBackend());
    }
}
