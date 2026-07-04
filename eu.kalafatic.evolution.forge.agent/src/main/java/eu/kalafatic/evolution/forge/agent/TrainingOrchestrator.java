package eu.kalafatic.evolution.forge.agent;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class TrainingOrchestrator {
    private final Path projectPath;
    private final SmartScanner scanner;
    private final DataForgeEngine forge;
    private final AiDataEnhancer enhancer;

    public TrainingOrchestrator(Path projectPath, AiDataEnhancer enhancer) {
        this.projectPath = projectPath;
        this.scanner = new SmartScanner(projectPath);
        this.forge = new DataForgeEngine();
        this.enhancer = enhancer;
    }

    public void runPipeline() throws Exception {
        System.out.println("Step 1: Scanning project data...");
        List<Path> files = scanner.scan();
        System.out.println("Found " + files.size() + " relevant files.");

        Path dataDir = projectPath.resolve("evo-forge-data");
        Files.createDirectories(dataDir);

        System.out.println("Step 2: Forging raw dataset...");
        Path rawDataset = dataDir.resolve("raw_data.jsonl");
        forge.buildDataset(files, rawDataset);

        System.out.println("Step 3: Creating instruction pairs...");
        Path instructionDataset = dataDir.resolve("instructions.jsonl");
        forge.createInstructionPairs(files, instructionDataset);

        if (enhancer != null) {
            System.out.println("Step 4: AI Data Enhancement...");
            Path enhancedDataset = dataDir.resolve("evo_train.jsonl");
            enhancer.enhance(instructionDataset, enhancedDataset);
        }

        System.out.println("Step 5: Configuring training parameters...");
        generateAxolotlConfig(dataDir);

        System.out.println("Pipeline complete. Ready for training.");
    }

    private void generateAxolotlConfig(Path dataDir) throws IOException {
        String config = "base_model: unsloth/llama-3-8b-bnb-4bit\n" +
                        "model_type: LlamaForCausalLM\n" +
                        "tokenizer_type: LlamaTokenizer\n" +
                        "dataset_prepared_path: " + dataDir.resolve("prepared").toString() + "\n" +
                        "datasets:\n" +
                        "  - path: " + dataDir.resolve("evo_train.jsonl").toString() + "\n" +
                        "    type: alpaca\n" +
                        "adapter: lora\n" +
                        "lora_r: 32\n" +
                        "lora_alpha: 16\n" +
                        "lora_dropout: 0.05\n" +
                        "learning_rate: 0.0002\n" +
                        "num_epochs: 3\n";
        Files.writeString(dataDir.resolve("axolotl_config.yaml"), config);
    }
}
