package eu.kalafatic.evolution.controller.orchestration.llm;

/**
 * A model discovered through a local LLM provider's own discovery mechanism.
 *
 * <p>This is the wire representation shared by provider discovery and the UI model
 * selection: {@link #id()} is the identifier that must be passed to inference (for
 * ENR it carries the provider prefix, e.g. {@code ollama:llama3.2:3b} or
 * {@code evo-native:my-model.evo}), {@link #name()} is the plain model name, and
 * {@link #origin()} records which underlying engine registered the model so the UI
 * can display the model origin without losing the real identifier.</p>
 */
public record LocalModelDescriptor(String id, String name, String origin, String engine,
        boolean available) {

    public static final String ORIGIN_OLLAMA = "ollama";
    public static final String ORIGIN_EVO_NATIVE = "evo-native";
    public static final String ORIGIN_LLAMA_CPP = "llama-cpp";

    public static final String PREFIX_OLLAMA = "ollama:";
    public static final String PREFIX_EVO_NATIVE = "evo-native:";
    public static final String PREFIX_LLAMA_CPP = "llama-cpp:";

    /**
     * Returns the plain model name for an identifier, stripping any known provider
     * prefix. Unprefixed values are returned unchanged.
     */
    public static String plainName(String idOrName) {
        if (idOrName == null) {
            return "";
        }
        if (idOrName.startsWith(PREFIX_OLLAMA)) {
            return idOrName.substring(PREFIX_OLLAMA.length());
        }
        if (idOrName.startsWith(PREFIX_EVO_NATIVE)) {
            return idOrName.substring(PREFIX_EVO_NATIVE.length());
        }
        if (idOrName.startsWith(PREFIX_LLAMA_CPP)) {
            return idOrName.substring(PREFIX_LLAMA_CPP.length());
        }
        return idOrName;
    }
}
