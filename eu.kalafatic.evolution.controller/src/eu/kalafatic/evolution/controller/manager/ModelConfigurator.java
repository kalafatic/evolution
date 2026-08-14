package eu.kalafatic.evolution.controller.manager;



import eu.kalafatic.evolution.forge.math.api.Tensor;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;

public class ModelConfigurator {
    
    public static EvoLlmModel createModel(ModelSizePreset.Size preset) {
        return new EvoLlmModel(
            preset.getVocabSize(),
            preset.getDModel(),
            preset.getNumHeads(),
            preset.getNumBlocks(),
            preset.getDff(),
            preset.getMaxSeqLen()
        );
    }
    
    public static String getModelInfo(EvoLlmModel model) {
        long params = countParameters(model);
        return String.format(
            "Model: %d params, %d layers, %d heads, %d embedding\n" +
            "Size: %s",
            params,
            model.getNumBlocks(),
            model.getNumHeads(),
            model.getDModel(),
            formatFileSize(params * 4)  // 4 bytes per float
        );
    }
    
    private static long countParameters(EvoLlmModel model) {
        long count = 0;
        for (Tensor t : model.parameters()) {
            count += t.getData().length;
        }
        return count;
    }
    
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }
}