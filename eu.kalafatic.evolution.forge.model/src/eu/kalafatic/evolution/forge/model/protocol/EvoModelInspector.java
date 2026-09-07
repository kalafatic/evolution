package eu.kalafatic.evolution.forge.model.protocol;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Inspection utility for inspecting `.evo` Native Model Protocol files.
 */
public class EvoModelInspector {

    public static String inspect(Path modelPath) throws IOException {
        return inspectTensor(modelPath, null);
    }

    public static String inspectTensor(Path modelPath, String targetTensorName) throws IOException {
        EvoModelReader reader = new EvoModelReader();
        reader.read(modelPath);

        StringBuilder sb = new StringBuilder();
        sb.append("EVO MODEL INSPECTION REPORT\n");
        sb.append("============================\n");
        sb.append("File Path:            ").append(modelPath.toAbsolutePath()).append("\n");
        sb.append("Magic Format:         ").append(reader.getMagic()).append("\n");
        sb.append("Model Name:           ").append(reader.getModelName()).append("\n");
        sb.append("Protocol Version:     ").append(reader.getProtocolVersion()).append("\n");
        sb.append("Model Content Hash:   ").append(reader.getModelContentHash()).append("\n");

        EvoArchitectureDescriptor arch = reader.getArchitecture();
        if (arch != null) {
            sb.append("\n[Architecture Descriptor]\n");
            sb.append("  Family:             ").append(arch.getArchitectureFamily()).append("\n");
            sb.append("  Vocab Size:         ").append(arch.getVocabSize()).append("\n");
            sb.append("  d_model:            ").append(arch.getDModel()).append("\n");
            sb.append("  num_heads:          ").append(arch.getNumHeads()).append("\n");
            sb.append("  num_blocks:         ").append(arch.getNumBlocks()).append("\n");
            sb.append("  dff:                ").append(arch.getDff()).append("\n");
            sb.append("  max_seq_len:        ").append(arch.getMaxSeqLen()).append("\n");
        }

        EvoTokenizerDescriptor tok = reader.getTokenizer();
        if (tok != null) {
            sb.append("\n[Tokenizer Descriptor]\n");
            sb.append("  Type:               ").append(tok.getTokenizerType()).append("\n");
            sb.append("  Vocab Size:         ").append(tok.getVocabSize()).append("\n");
            sb.append("  BOS / EOS / UNK:    ").append(tok.getBosTokenId()).append(" / ")
                    .append(tok.getEosTokenId()).append(" / ").append(tok.getUnkTokenId()).append("\n");
        }

        List<EvoTensorDescriptor> manifest = reader.getManifest();
        List<float[]> payloads = reader.getTensorDataPayloads();

        sb.append("\n[Tensor Manifest (").append(manifest.size()).append(" Tensors)]\n");

        long totalParams = 0;
        for (int i = 0; i < manifest.size(); i++) {
            EvoTensorDescriptor desc = manifest.get(i);
            totalParams += desc.getElementCount();

            if (targetTensorName == null || desc.getCanonicalName().equalsIgnoreCase(targetTensorName)) {
                sb.append(String.format("  [%02d] %-30s | Shape: %-15s | Dtype: %-4s | Count: %d\n",
                        i, desc.getCanonicalName(), Arrays.toString(desc.getShape()), desc.getDtype(), desc.getElementCount()));

                if (targetTensorName != null && targetTensorName.equalsIgnoreCase(desc.getCanonicalName())) {
                    float[] data = payloads.get(i);
                    sb.append("\n--- Tensor Details: ").append(desc.getCanonicalName()).append(" ---\n");
                    sb.append("Checksum: ").append(desc.getChecksum()).append("\n");
                    sb.append("First 10 values: ");
                    for (int k = 0; k < Math.min(10, data.length); k++) {
                        sb.append(data[k]).append(" ");
                    }
                    sb.append("\n");
                }
            }
        }

        sb.append("\nTotal Parameter Count: ").append(totalParams).append("\n");
        return sb.toString();
    }
}
