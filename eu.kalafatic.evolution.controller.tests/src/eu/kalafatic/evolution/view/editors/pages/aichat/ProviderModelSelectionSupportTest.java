package eu.kalafatic.evolution.view.editors.pages.aichat;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;

/**
 * Pure UI-selection logic tests for the AI Commander provider/model combos
 * (Test E — visibility/support, Test F — provider switching): provider labels,
 * origin-display formatting, selection preservation and duplicate elimination.
 * No SWT types are involved, so these run headless.
 */
public class ProviderModelSelectionSupportTest {

    private static LocalModelDescriptor ollama(String name) {
        return new LocalModelDescriptor(name, name, LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true);
    }

    private static LocalModelDescriptor evoNative(String name) {
        return new LocalModelDescriptor("evo-native:" + name, name,
                LocalModelDescriptor.ORIGIN_EVO_NATIVE, "evo native", true);
    }

    private static LocalModelDescriptor llamaCpp(String name) {
        return new LocalModelDescriptor(name, name, LocalModelDescriptor.ORIGIN_LLAMA_CPP,
                "llama-cpp", true);
    }

    @Test
    public void testSupportedProviderItems() {
        assertEquals(Arrays.asList("Ollama", "llama.cpp", "EVO Native ENR"),
                Arrays.asList(ProviderModelSelectionSupport.PROVIDER_ITEMS));
    }

    @Test
    public void testProviderLabelNormalizationIncludingLegacyValues() {
        assertEquals("Ollama", ProviderModelSelectionSupport.normalizeProviderLabel("Ollama"));
        assertEquals("Ollama", ProviderModelSelectionSupport.normalizeProviderLabel("ollama"));
        assertEquals("llama.cpp", ProviderModelSelectionSupport.normalizeProviderLabel("llama.cpp"));
        assertEquals("llama.cpp", ProviderModelSelectionSupport.normalizeProviderLabel("LLAMA-CPP"));
        assertEquals("llama.cpp", ProviderModelSelectionSupport.normalizeProviderLabel("llama_cpp"));
        assertEquals("EVO Native ENR", ProviderModelSelectionSupport.normalizeProviderLabel("EVO Native ENR"));
        assertEquals("EVO Native ENR", ProviderModelSelectionSupport.normalizeProviderLabel("ENR"));
        assertEquals("EVO Native ENR", ProviderModelSelectionSupport.normalizeProviderLabel("ENR-EVO Native"));
        assertEquals("EVO Native ENR", ProviderModelSelectionSupport.normalizeProviderLabel("evo native"));
        assertEquals("EVO Native ENR", ProviderModelSelectionSupport.normalizeProviderLabel("EVO Native"));
        assertEquals("", ProviderModelSelectionSupport.normalizeProviderLabel(null));
        assertEquals("", ProviderModelSelectionSupport.normalizeProviderLabel(""));
        // Unknown values are passed through unchanged, never silently remapped.
        assertEquals("SomethingElse", ProviderModelSelectionSupport.normalizeProviderLabel("SomethingElse"));
    }

    @Test
    public void testDisplayLabelsExposeOriginWithoutLosingIdentifier() {
        assertEquals("[OLLAMA] llama3.2:3b", ProviderModelSelectionSupport.displayLabel(ollama("llama3.2:3b")));
        assertEquals("[EVO NATIVE] my-model.evo",
                ProviderModelSelectionSupport.displayLabel(evoNative("my-model.evo")));
        assertEquals("[LLAMA-CPP] evo", ProviderModelSelectionSupport.displayLabel(llamaCpp("evo")));
    }

    @Test
    public void testSelectionResolutionPreservesModelWhenStillAvailable() {
        List<LocalModelDescriptor> models = new ArrayList<>();
        models.add(ollama("llama3.2:3b"));
        models.add(evoNative("my-model.evo"));

        // Match by exact identifier.
        assertEquals(1, ProviderModelSelectionSupport.resolveSelectionIndex(models, "evo-native:my-model.evo"));
        // Match by plain name (cross-provider switch, e.g. ENR -> Ollama).
        assertEquals(0, ProviderModelSelectionSupport.resolveSelectionIndex(models, "llama3.2:3b"));
        // Match by display label.
        assertEquals(1, ProviderModelSelectionSupport.resolveSelectionIndex(models, "[EVO NATIVE] my-model.evo"));
        // Nothing matches -> no selection carried over.
        assertEquals(-1, ProviderModelSelectionSupport.resolveSelectionIndex(models, "gemma2:2b"));
        assertEquals(-1, ProviderModelSelectionSupport.resolveSelectionIndex(models, ""));
        assertEquals(-1, ProviderModelSelectionSupport.resolveSelectionIndex(models, null));
        assertEquals(-1, ProviderModelSelectionSupport.resolveSelectionIndex(List.of(), "llama3.2:3b"));
    }

    @Test
    public void testDedupeByIdRemovesDuplicatesPreservingOrder() {
        List<LocalModelDescriptor> models = new ArrayList<>();
        models.add(ollama("a"));
        models.add(ollama("a"));
        models.add(evoNative("b.evo"));
        models.add(ollama("a"));
        models.add(ollama("c"));

        List<LocalModelDescriptor> unique = ProviderModelSelectionSupport.dedupeById(models);

        assertEquals(3, unique.size());
        assertEquals("a", unique.get(0).id());
        assertEquals("evo-native:b.evo", unique.get(1).id());
        assertEquals("c", unique.get(2).id());
        assertEquals(List.of(), ProviderModelSelectionSupport.dedupeById(null));
        assertEquals(List.of(), ProviderModelSelectionSupport.dedupeById(List.of()));
    }

    @Test
    public void testDedupeByIdPreservesDistinctIdsSharingADisplayName() {
        // Different providers can register models with the same human-readable name;
        // they are distinct identities and must never be merged by name.
        List<LocalModelDescriptor> models = new ArrayList<>();
        models.add(new LocalModelDescriptor("ollama:my-model", "my-model",
                LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true));
        models.add(new LocalModelDescriptor("evo-native:my-model", "my-model",
                LocalModelDescriptor.ORIGIN_EVO_NATIVE, "evo native", true));
        models.add(new LocalModelDescriptor("llama-cpp:my-model", "my-model",
                LocalModelDescriptor.ORIGIN_LLAMA_CPP, "llama-cpp", true));

        List<LocalModelDescriptor> unique = ProviderModelSelectionSupport.dedupeById(models);

        assertEquals(3, unique.size());
        assertTrue(unique.stream().anyMatch(m -> "ollama:my-model".equals(m.id())));
        assertTrue(unique.stream().anyMatch(m -> "evo-native:my-model".equals(m.id())));
        assertTrue(unique.stream().anyMatch(m -> "llama-cpp:my-model".equals(m.id())));
    }

    @Test
    public void testDedupeByIdPreservesEntireLargeModelList() {
        int ollamaCount = 44;
        List<LocalModelDescriptor> models = new ArrayList<>();
        for (int i = 0; i < ollamaCount; i++) {
            String id = "ollama:model-" + i;
            models.add(new LocalModelDescriptor(id, "model-" + i,
                    LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true));
        }
        // Mixed formats and origins must all survive alongside the Ollama set.
        models.add(new LocalModelDescriptor("evo-native:native.evo", "native.evo",
                LocalModelDescriptor.ORIGIN_EVO_NATIVE, "evo native", false));
        models.add(new LocalModelDescriptor("llama-cpp:native.gguf", "native.gguf",
                LocalModelDescriptor.ORIGIN_LLAMA_CPP, "llama-cpp", false));

        List<LocalModelDescriptor> unique = ProviderModelSelectionSupport.dedupeById(models);

        assertEquals(ollamaCount + 2, unique.size());
    }

    @Test
    public void testDedupeByIdNeverFiltersUnavailableModels() {
        // Visibility must not depend on load/readiness state: an unloaded model is
        // still visible in the combo and only fails at the inference layer.
        List<LocalModelDescriptor> models = new ArrayList<>();
        models.add(new LocalModelDescriptor("ollama:loaded", "loaded",
                LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true));
        models.add(new LocalModelDescriptor("ollama:unloaded", "unloaded",
                LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", false));

        List<LocalModelDescriptor> unique = ProviderModelSelectionSupport.dedupeById(models);

        assertEquals(2, unique.size());
        assertTrue(unique.stream().anyMatch(m -> "ollama:unloaded".equals(m.id()) && !m.available()));
    }

    @Test
    public void testProviderSwitchingDoesNotCarryIncompatibleSelections() {
        // ENR-only native model must not resolve against an Ollama-only list.
        List<LocalModelDescriptor> ollamaModels = List.of(ollama("llama3.2:3b"), ollama("gemma2:2b"));
        assertEquals(-1, ProviderModelSelectionSupport.resolveSelectionIndex(ollamaModels, "evo-native:x.evo"));

        // An Ollama model selected under ENR carries over to the Ollama provider by
        // name — also when the stored id still carries the ENR provider prefix.
        assertEquals(0, ProviderModelSelectionSupport.resolveSelectionIndex(ollamaModels, "ollama:llama3.2:3b"));
        assertEquals(0, ProviderModelSelectionSupport.resolveSelectionIndex(ollamaModels, "llama3.2:3b"));

        List<LocalModelDescriptor> enrModels = List.of(
                new LocalModelDescriptor("ollama:llama3.2:3b", "llama3.2:3b",
                        LocalModelDescriptor.ORIGIN_OLLAMA, "ollama", true),
                evoNative("x.evo"));
        int carried = ProviderModelSelectionSupport.resolveSelectionIndex(enrModels, "llama3.2:3b");
        assertEquals(0, carried);
        assertEquals("ollama:llama3.2:3b", enrModels.get(carried).id());
    }

    @Test
    public void testPlainNameStripsProviderPrefixes() {
        assertEquals("llama3.2:3b", LocalModelDescriptor.plainName("ollama:llama3.2:3b"));
        assertEquals("x.evo", LocalModelDescriptor.plainName("evo-native:x.evo"));
        assertEquals("evo", LocalModelDescriptor.plainName("llama-cpp:evo"));
        assertEquals("plain", LocalModelDescriptor.plainName("plain"));
        assertEquals("", LocalModelDescriptor.plainName(null));
    }
}
