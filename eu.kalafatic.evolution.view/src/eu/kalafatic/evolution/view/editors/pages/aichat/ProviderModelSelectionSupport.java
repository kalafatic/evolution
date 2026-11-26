package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;

/**
 * Pure selection/support logic for the AI Commander provider/model combos.
 *
 * <p>No SWT types and no UI state: everything here is unit-testable and shared by the
 * {@code ChatMgmtGroup} widgets. Provider selection is the authoritative source for
 * model discovery; this class only maps labels, formats display entries and resolves
 * which previously selected model (if any) survives a provider switch.</p>
 */
public final class ProviderModelSelectionSupport {

    /** Display labels of the supported local providers. */
    public static final String PROVIDER_OLLAMA = "Ollama";
    public static final String PROVIDER_LLAMA_CPP = "llama.cpp";
    public static final String PROVIDER_ENR = "EVO Native ENR";

    public static final String[] PROVIDER_ITEMS = { PROVIDER_OLLAMA, PROVIDER_LLAMA_CPP, PROVIDER_ENR };

    private ProviderModelSelectionSupport() {
    }

    /**
     * Maps a persisted provider value onto one of the current provider combo items.
     * Handles the canonical labels, the lowercase engine ids produced by
     * {@code LlamaService#detectInferenceEngine} and legacy values persisted by
     * earlier AI Commander versions ({@code ENR}, {@code ENR-EVO Native},
     * {@code EVO Native}, {@code LLAMA-CPP}, {@code ollama}, …).
     *
     * @return The canonical provider label, or the input unchanged when unknown.
     */
    public static String normalizeProviderLabel(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (LlmRouter.isEnrEngine(value)) {
            return PROVIDER_ENR;
        }
        String lower = value.trim().toLowerCase();
        if (lower.contains("llama-cpp") || lower.contains("llama_cpp") || lower.contains("llama.cpp")
                || lower.equals("cpp")) {
            return PROVIDER_LLAMA_CPP;
        }
        if (lower.contains("evo native") || lower.contains("evo-native")
                || lower.contains("evo_native") || lower.equals("native")) {
            // Native EVO models are selected through the unified ENR provider.
            return PROVIDER_ENR;
        }
        if (lower.contains("ollama")) {
            return PROVIDER_OLLAMA;
        }
        return value;
    }

    /**
     * The canonical provider key ("ollama", "llama-cpp", "enr") for a provider label.
     */
    public static String providerKey(String label) {
        return LlmRouter.normalizeProviderKey(label);
    }

    /**
     * Builds the display entry for a discovered model, making its origin
     * distinguishable without losing the underlying identifier (kept in the
     * descriptor list parallel to the combo items).
     */
    public static String displayLabel(LocalModelDescriptor model) {
        if (model == null) {
            return "";
        }
        String origin;
        switch (model.origin() == null ? "" : model.origin().toLowerCase()) {
            case LocalModelDescriptor.ORIGIN_OLLAMA -> origin = "OLLAMA";
            case LocalModelDescriptor.ORIGIN_EVO_NATIVE -> origin = "EVO NATIVE";
            case LocalModelDescriptor.ORIGIN_LLAMA_CPP -> origin = "LLAMA-CPP";
            default -> origin = model.origin().toUpperCase();
        }
        String name = model.name() == null || model.name().isBlank() ? model.id() : model.name();
        return "[" + origin + "] " + name;
    }

    /**
     * Resolves which discovered model corresponds to a previously selected value.
     * Matching order: exact identifier, plain name, display label.
     *
     * @return The index into {@code models}, or -1 when nothing matches.
     */
    public static int resolveSelectionIndex(List<LocalModelDescriptor> models, String previousSelection) {
        if (models == null || models.isEmpty() || previousSelection == null || previousSelection.isBlank()) {
            return -1;
        }
        String previous = previousSelection.trim();
        String previousPlain = LocalModelDescriptor.plainName(previous);
        for (int i = 0; i < models.size(); i++) {
            LocalModelDescriptor m = models.get(i);
            if (previous.equals(m.id()) || previous.equals(m.name())
                    || previous.equals(displayLabel(m))
                    || previousPlain.equals(m.id()) || previousPlain.equals(m.name())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes duplicate model entries by identifier, preserving first-occurrence order.
     */
    public static List<LocalModelDescriptor> dedupeById(List<LocalModelDescriptor> models) {
        if (models == null || models.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, LocalModelDescriptor> unique = new LinkedHashMap<>();
        for (LocalModelDescriptor m : models) {
            if (m == null || m.id() == null || m.id().isBlank()) {
                continue;
            }
            unique.putIfAbsent(m.id(), m);
        }
        return new ArrayList<>(unique.values());
    }
}
