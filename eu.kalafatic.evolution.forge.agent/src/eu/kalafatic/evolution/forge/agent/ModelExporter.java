package eu.kalafatic.evolution.forge.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModelExporter {
    public void createModelfile(Path outputPath, String ggufPath) throws IOException {
        String content = "FROM " + ggufPath + "\n" +
                         "PARAMETER temperature 0.7\n" +
                         "PARAMETER top_p 0.9\n" +
                         "SYSTEM \"\"\"You are Evo, a specialized coding assistant trained on this specific project. \"\"\"\n";
        Files.writeString(outputPath.resolve("Modelfile"), content);
    }

    public void createExportScript(Path outputPath) throws IOException {
        String script = "#!/bin/bash\npython3 convert_gguf.py --adapter-path ./output --base-model llama-3-8b --output-file evo.gguf\nollama create evo -f Modelfile\n";
        Files.writeString(outputPath.resolve("export_model.sh"), script);
    }
}
