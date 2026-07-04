package eu.kalafatic.evolution.forge.agent;

import java.io.IOException;
import java.nio.file.*;

public class ModelExporter {
    public void createModelfile(Path outputPath, String ggufPath) throws IOException {
        String content = "FROM " + ggufPath + "\n" +
                         "PARAMETER temperature 0.7\n" +
                         "PARAMETER top_p 0.9\n" +
                         "SYSTEM \"\"\"You are Evo, a specialized coding assistant trained on this specific project. " +
                         "You have deep knowledge of its architecture, modules, and coding patterns.\"\"\"\n";
        Files.writeString(outputPath.resolve("Modelfile"), content);
    }

    public void createExportScript(Path outputPath) throws IOException {
        String script = "#!/bin/bash\n" +
                        "python3 convert_gguf.py --adapter-path ./output --base-model llama-3-8b --output-file evo.gguf\n" +
                        "ollama create evo -f Modelfile\n";
        Files.writeString(outputPath.resolve("export_model.sh"), script);
    }
}
