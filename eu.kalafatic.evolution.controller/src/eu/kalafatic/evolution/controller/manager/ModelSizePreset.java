package eu.kalafatic.evolution.controller.manager;

import java.util.*;

/**
 * ModelSizePreset - Defines predefined model architectures
 * Optimized for: 128GB RAM + RTX 5070 Ti 16GB VRAM
 */
public class ModelSizePreset {
    
    public enum Category {
        TESTING("🧪 Testing", "Quick validation, 1-5 min training"),
        EDGE("📱 Edge", "Mobile/lightweight deployment"),
        DESKTOP("💻 Desktop", "Balanced performance, GPU only"),
        POWER("🚀 Power", "CPU offloading, massive models"),
        CUSTOM("⚙️ Custom", "User-defined architecture");
        
        private final String icon;
        private final String description;
        
        Category(String icon, String description) {
            this.icon = icon;
            this.description = description;
        }
        
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
        public String getDisplayName() { return icon + " " + name().charAt(0) + name().substring(1).toLowerCase(); }
    }
    
    public enum Size {
        // ============ 🧪 TESTING ============
        NANO("Nano (1M)", Category.TESTING, 
             1000, 128, 4, 3, 256, 128, 
             "Ultra-tiny for quick testing (1-2 min)"),
        
        MICRO("Micro (5M)", Category.TESTING,
             2000, 256, 8, 4, 512, 256,
             "Very fast iteration (5-10 min)"),
        
        // ============ 📱 EDGE ============
        TINY("Tiny (15M)", Category.EDGE,
             8000, 384, 8, 6, 1024, 512,
             "Mobile/Edge deployment, decent quality",
             "CPU only, 2GB RAM"),
        
        SMALL("Small (50M)", Category.EDGE,
              15000, 512, 8, 8, 2048, 1024,
              "Lightweight model, good for most tasks",
              "CPU or low-end GPU, 4GB RAM"),
        
        // ============ 💻 DESKTOP ============
        MEDIUM("Medium (250M)", Category.DESKTOP,
               32000, 768, 12, 12, 3072, 2048,
               "GPT-2 Medium equivalent, solid performance",
               "Entry GPU (4-8GB VRAM)"),
        
        LARGE("Large (750M)", Category.DESKTOP,
              50000, 1024, 16, 16, 4096, 4096,
              "LLaMA-style, requires decent hardware",
              "Mid-range GPU (8-16GB VRAM)"),
        
        // ============ 🚀 POWER (Your System!) ============
        GRANITE("Granite 6B", Category.POWER,
                50000, 4096, 32, 32, 14336, 8192,
                "6.4B - Granite equivalent, GPU only",
                "GPU only: 12GB VRAM | RTX 5070 Ti ✅"),
        
        GRANITE_PLUS("Granite+ 8B", Category.POWER,
                     50000, 4096, 32, 40, 14336, 8192,
                     "8B - pushes VRAM to limit, still fast",
                     "GPU only: 15.8GB VRAM | RTX 5070 Ti ⚠️"),
        
        MASSIVE_13B("Massive 13B", Category.POWER,
                    50000, 5120, 40, 40, 16384, 16384,
                    "13B - CPU offloading, uses ~4GB VRAM",
                    "GPU: 4GB + RAM: 22GB | 128GB RAM ✅"),
        
        MASSIVE_20B("Massive 20B", Category.POWER,
                    50000, 6144, 48, 48, 20480, 16384,
                    "20B - CPU offloading, uses ~5GB VRAM",
                    "GPU: 5GB + RAM: 35GB | 128GB RAM ✅"),
        
        MASSIVE_30B("Massive 30B", Category.POWER,
                    50000, 7680, 64, 48, 24576, 16384,
                    "30B - CPU offloading, uses ~6GB VRAM",
                    "GPU: 6GB + RAM: 54GB | 128GB RAM ✅"),
        
        BEAST_70B("Beast 70B", Category.POWER,
                  50000, 8192, 64, 80, 28672, 32768,
                  "70B - LLaMA 3 level! Uses CPU offloading",
                  "GPU: 8GB + RAM: 62GB | 128GB RAM ✅"),
        
        // ============ ⚙️ CUSTOM ============
        CUSTOM("Custom", Category.CUSTOM,
               0, 0, 0, 0, 0, 0,
               "User-defined architecture");
        
        private final String displayName;
        private final Category category;
        private final int vocabSize;
        private final int dModel;
        private final int numHeads;
        private final int numBlocks;
        private final int dff;
        private final int maxSeqLen;
        private final String description;
        private final String hardwareNote;
        
        Size(String displayName, Category category,
             int vocabSize, int dModel, int numHeads, 
             int numBlocks, int dff, int maxSeqLen, 
             String description) {
            this(displayName, category, vocabSize, dModel, numHeads, 
                 numBlocks, dff, maxSeqLen, description, null);
        }
        
        Size(String displayName, Category category,
             int vocabSize, int dModel, int numHeads, 
             int numBlocks, int dff, int maxSeqLen, 
             String description, String hardwareNote) {
            this.displayName = displayName;
            this.category = category;
            this.vocabSize = vocabSize;
            this.dModel = dModel;
            this.numHeads = numHeads;
            this.numBlocks = numBlocks;
            this.dff = dff;
            this.maxSeqLen = maxSeqLen;
            this.description = description;
            this.hardwareNote = hardwareNote;
        }
        
        // ============ GETTERS ============
        public String getDisplayName() { return displayName; }
        public Category getCategory() { return category; }
        public int getVocabSize() { return vocabSize; }
        public int getDModel() { return dModel; }
        public int getNumHeads() { return numHeads; }
        public int getNumBlocks() { return numBlocks; }
        public int getDff() { return dff; }
        public int getMaxSeqLen() { return maxSeqLen; }
        public String getDescription() { return description; }
        public String getHardwareNote() { return hardwareNote; }
        
        // ============ CALCULATIONS ============
        public long getParameterCount() {
            if (this == CUSTOM) return 0;
            long params = (long) vocabSize * dModel;
            params += (long) numBlocks * (
                4L * dModel * dModel +
                3L * dModel * dff +
                2L * dModel
            );
            params += dModel;
            params += (long) dModel * vocabSize;
            return params / 1_000_000;
        }
        
        public long getFileSizeMB() {
            return getParameterCount() * 4;
        }
        
        public String getFormattedSize() {
            long params = getParameterCount();
            if (params < 1) return "<1M";
            if (params < 1000) return params + "M";
            return String.format("%.1fB", params / 1000.0);
        }
        
        public String getVRAMUsage() {
            long params = getParameterCount();
            if (params < 10) return "~" + (params * 4 / 1024) + "MB";
            if (params < 1000) return "~" + (params * 4 / 1024) + "GB";
            return "~" + String.format("%.1f", params * 4 / 1024.0) + "GB";
        }
        
        public String getRAMUsage() {
            long params = getParameterCount();
            if (params < 10) return "~" + (params * 4 / 1024) + "MB";
            if (params < 1000) return "~" + (params * 4 / 1024) + "GB";
            return "~" + String.format("%.1f", params * 4 / 1024.0) + "GB";
        }
        
        public String getTrainingTimeEstimate() {
            long params = getParameterCount();
            if (params < 10) return "~1-2 minutes";
            if (params < 50) return "~5-15 minutes";
            if (params < 250) return "~30-60 minutes";
            if (params < 750) return "~2-4 hours";
            if (params < 1500) return "~4-8 hours";
            if (params < 3000) return "~8-16 hours";
            if (params < 6000) return "~16-24 hours";
            return "~24-48 hours";
        }
        
        public boolean isCpuOffloadingRecommended() {
            long params = getParameterCount();
            return params > 3000; // >3B params
        }
        
        public boolean isGpuOnly() {
            long params = getParameterCount();
            return params <= 3000; // <=3B params
        }
        
        // ============ UTILITY ============
        public static List<Size> getByCategory(Category category) {
            List<Size> result = new ArrayList<>();
            for (Size size : values()) {
                if (size.category == category) {
                    result.add(size);
                }
            }
            return result;
        }
        
        public static List<Size> getRecommendedForHardware(long ramGB, long vramGB) {
            List<Size> result = new ArrayList<>();
            for (Size size : values()) {
                if (size == CUSTOM) continue;
                long params = size.getParameterCount();
                // Check if model fits in VRAM (GPU only)
                boolean fitsVRAM = params * 4 / 1024 <= vramGB;
                // Check if model fits in RAM (CPU offloading)
                boolean fitsRAM = params * 4 / 1024 <= ramGB;
                if (fitsVRAM || fitsRAM) {
                    result.add(size);
                }
            }
            return result;
        }
        
        // ============ DEFAULT ============
        public static Size getDefault() {
            return GRANITE; // 6B - sweet spot for RTX 5070 Ti
        }
        
        public static Size getMaximumForSystem() {
            return BEAST_70B; // 70B - max for 128GB RAM
        }
    }
}