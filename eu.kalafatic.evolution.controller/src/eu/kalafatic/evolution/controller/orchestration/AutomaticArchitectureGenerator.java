package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Responsible for automatically generating complete computational graphs for
 * AI models based on high-level palette items.
 */
public class AutomaticArchitectureGenerator {

    public static class ArchitectureResult {
        public final String graph;
        public final JSONObject defaults;

        public ArchitectureResult(String graph, JSONObject defaults) {
            this.graph = graph;
            this.defaults = defaults;
        }
    }

    public ArchitectureResult generate(String modelType, JSONObject params) {
        String graph = "{}";
        JSONObject defaults = new JSONObject();

        switch (modelType != null ? modelType.toUpperCase() : "") {
            case "SEED":
                graph = generateSeedGraph();
                defaults.put("lr", 0.001).put("optimizer", "adam").put("epochs", 5);
                break;
            case "NEURON":
                graph = generateNeuronGraph();
                defaults.put("lr", 0.01).put("optimizer", "sgd").put("epochs", 10);
                break;
            case "MLP":
                int layers = params.optInt("layers", 1);
                int hiddenSize = params.optInt("hidden_size", 64);
                graph = generateMlpGraph(layers, hiddenSize);
                defaults.put("layers", layers).put("hidden_size", hiddenSize).put("activation", "relu").put("lr", 0.001).put("batch", 32).put("optimizer", "adam").put("epochs", 20);
                break;
            case "CNN":
                int filters = params.optInt("filters", 32);
                int kernelSize = params.optInt("kernel_size", 3);
                graph = generateCnnGraph(filters, kernelSize);
                defaults.put("filters", filters).put("kernel_size", kernelSize).put("lr", 0.001).put("batch", 64).put("optimizer", "adam").put("epochs", 15);
                break;
            case "TRANSFORMER":
                int tLayers = params.optInt("layers", 6);
                int heads = params.optInt("heads", 8);
                int dModel = params.optInt("d_model", 512);
                graph = generateTransformerGraph(tLayers, heads, dModel);
                defaults.put("heads", heads).put("d_model", dModel).put("layers", tLayers).put("lr", 0.0001).put("batch", 16).put("optimizer", "adam").put("epochs", 10);
                break;
            case "LLM":
                int llmLayers = params.optInt("layers", 12);
                int llmHeads = params.optInt("heads", 12);
                int vocabSize = params.optInt("vocab_size", 32000);
                int contextLength = params.optInt("context_length", 2048);
                graph = generateLlmGraph(llmLayers, llmHeads, vocabSize, contextLength);
                defaults.put("vocab_size", vocabSize).put("context_length", contextLength).put("layers", llmLayers).put("heads", llmHeads).put("lr", 0.00005).put("batch", 8).put("optimizer", "adam").put("epochs", 5);
                break;
        }

        return new ArchitectureResult(graph, defaults);
    }

    private String generateSeedGraph() {
        return "{\"nodes\":[" +
                "{\"id\":\"s_in\",\"name\":\"seed_input\",\"type\":\"LAYER\",\"x\":100,\"y\":200}," +
                "{\"id\":\"s_h\",\"name\":\"seed_hidden\",\"type\":\"LAYER\",\"x\":300,\"y\":200}," +
                "{\"id\":\"s_out\",\"name\":\"seed_output\",\"type\":\"LAYER\",\"x\":500,\"y\":200}" +
                "],\"links\":[" +
                "{\"source\":\"s_in\",\"target\":\"s_h\"}," +
                "{\"source\":\"s_h\",\"target\":\"s_out\"}" +
                "]}";
    }

    private String generateNeuronGraph() {
        return "{\"nodes\":[" +
                "{\"id\":\"n1\",\"name\":\"input_1\",\"type\":\"NEURON\",\"x\":100,\"y\":100}," +
                "{\"id\":\"n2\",\"name\":\"bias\",\"type\":\"NEURON\",\"x\":100,\"y\":200}," +
                "{\"id\":\"n3\",\"name\":\"output\",\"type\":\"NEURON\",\"x\":300,\"y\":150}" +
                "],\"links\":[" +
                "{\"source\":\"n1\",\"target\":\"n3\"}," +
                "{\"source\":\"n2\",\"target\":\"n3\"}" +
                "]}";
    }

    private String generateMlpGraph(int layers, int hiddenSize) {
        JSONArray nodes = new JSONArray();
        JSONArray links = new JSONArray();

        JSONObject inputNode = new JSONObject().put("id", "m_in").put("name", "input").put("type", "LAYER").put("x", 50).put("y", 150);
        nodes.put(inputNode);

        String lastId = "m_in";
        for (int i = 0; i < layers; i++) {
            String currentId = "m_h" + i;
            nodes.put(new JSONObject().put("id", currentId).put("name", "hidden_" + (i+1) + " (" + hiddenSize + ")").put("type", "LAYER").put("x", 200 + i * 150).put("y", 150));
            links.put(new JSONObject().put("source", lastId).put("target", currentId));
            lastId = currentId;
        }

        String outputId = "m_out";
        nodes.put(new JSONObject().put("id", outputId).put("name", "output").put("type", "LAYER").put("x", 200 + layers * 150).put("y", 150));
        links.put(new JSONObject().put("source", lastId).put("target", outputId));

        return new JSONObject().put("nodes", nodes).put("links", links).toString();
    }

    private String generateCnnGraph(int filters, int kernelSize) {
        return "{\"nodes\":[" +
                "{\"id\":\"c1\",\"name\":\"conv_2d_" + filters + "f_" + kernelSize + "k\",\"type\":\"LAYER\",\"x\":100,\"y\":100}," +
                "{\"id\":\"c2\",\"name\":\"max_pool\",\"type\":\"LAYER\",\"x\":100,\"y\":200}," +
                "{\"id\":\"c3\",\"name\":\"flatten\",\"type\":\"LAYER\",\"x\":300,\"y\":100}," +
                "{\"id\":\"c4\",\"name\":\"dense_out\",\"type\":\"LAYER\",\"x\":300,\"y\":200}" +
                "],\"links\":[" +
                "{\"source\":\"c1\",\"target\":\"c2\"}," +
                "{\"source\":\"c2\",\"target\":\"c3\"}," +
                "{\"source\":\"c3\",\"target\":\"c4\"}" +
                "]}";
    }

    private String generateTransformerGraph(int layers, int heads, int dModel) {
        return "{\"nodes\":[" +
                "{\"id\":\"t1\",\"name\":\"embedding_" + dModel + "\",\"type\":\"LAYER\",\"x\":50,\"y\":200}," +
                "{\"id\":\"t2\",\"name\":\"attn_" + heads + "heads_x" + layers + "l\",\"type\":\"ATTENTION\",\"x\":200,\"y\":200}," +
                "{\"id\":\"t3\",\"name\":\"ffn_1\",\"type\":\"LAYER\",\"x\":350,\"y\":200}," +
                "{\"id\":\"t4\",\"name\":\"head\",\"type\":\"LAYER\",\"x\":500,\"y\":200}" +
                "],\"links\":[" +
                "{\"source\":\"t1\",\"target\":\"t2\"}," +
                "{\"source\":\"t2\",\"target\":\"t3\"}," +
                "{\"source\":\"t3\",\"target\":\"t4\"}" +
                "]}";
    }

    private String generateLlmGraph(int layers, int heads, int vocabSize, int contextLength) {
        return "{\"nodes\":[" +
                "{\"id\":\"l1\",\"name\":\"tokenizer_v" + vocabSize + "\",\"type\":\"CUSTOM\",\"x\":50,\"y\":250}," +
                "{\"id\":\"l2\",\"name\":\"embeddings_c" + contextLength + "\",\"type\":\"LAYER\",\"x\":150,\"y\":250}," +
                "{\"id\":\"l3\",\"name\":\"blocks_x" + layers + "_h" + heads + "\",\"type\":\"TRANSFORMER\",\"x\":300,\"y\":250}," +
                "{\"id\":\"l4\",\"name\":\"norm\",\"type\":\"LAYER\",\"x\":450,\"y\":250}," +
                "{\"id\":\"l5\",\"name\":\"head\",\"type\":\"LAYER\",\"x\":600,\"y\":250}" +
                "],\"links\":[" +
                "{\"source\":\"l1\",\"target\":\"l2\"}," +
                "{\"source\":\"l2\",\"target\":\"l3\"}," +
                "{\"source\":\"l3\",\"target\":\"l4\"}," +
                "{\"source\":\"l4\",\"target\":\"l5\"}" +
                "]}";
    }
}
