package eu.kalafatic.evolution.controller.manager;

import java.util.*;

/**
 * ModelSizePreset - Defines predefined model architectures
 */
public class ModelSizePreset {
    public enum Size {
        TINY("Tiny (0.5M params)", 50, 64, 4, 3, 256, 128, "Ultra-light for edge devices"),
        VERY_SMALL("Very Small (2M params)", 100, 128, 4, 4, 512, 128, "Very fast testing"),
        SMALL("Small (6M params)", 8000, 256, 8, 6, 1024, 128, "Current default - fast"),
        MEDIUM("Medium (15M params)", 15000, 384, 8, 8, 1536, 256, "Balanced performance"),
        LARGE("Large (50M params)", 32000, 512, 8, 12, 2048, 512, "Better quality, more data"),
        XL("XL (125M params)", 50000, 768, 12, 12, 3072, 1024, "GPT-2 Small equivalent"),
        XXL("XXL (250M params)", 50000, 1024, 16, 16, 4096, 2048, "GPT-2 Medium equivalent"),
        CUSTOM("Custom", 0, 0, 0, 0, 0, 0, "User-defined architecture");
        
        private final String displayName;
        private final int vocabSize;
        private final int dModel;
        private final int numHeads;
        private final int numBlocks;
        private final int dff;
        private final int maxSeqLen;
        private final String description;
        
        Size(String displayName, int vocabSize, int dModel, int numHeads, 
             int numBlocks, int dff, int maxSeqLen, String description) {
            this.displayName = displayName;
            this.vocabSize = vocabSize;
            this.dModel = dModel;
            this.numHeads = numHeads;
            this.numBlocks = numBlocks;
            this.dff = dff;
            this.maxSeqLen = maxSeqLen;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public int getVocabSize() { return vocabSize; }
        public int getDModel() { return dModel; }
        public int getNumHeads() { return numHeads; }
        public int getNumBlocks() { return numBlocks; }
        public int getDff() { return dff; }
        public int getMaxSeqLen() { return maxSeqLen; }
        public String getDescription() { return description; }
        
        public long getParameterCount() {
            if (this == CUSTOM) return 0;
            long params = (long) vocabSize * dModel; // embedding
            params += (long) numBlocks * (
                4L * dModel * dModel +      // Q,K,V,O
                3L * dModel * dff +          // Gate, Up, Down
                2L * dModel                   // Norms
            );
            params += dModel;                // output norm
            params += (long) dModel * vocabSize; // lm head
            return params / 1_000_000;       // in millions
        }
    }
}