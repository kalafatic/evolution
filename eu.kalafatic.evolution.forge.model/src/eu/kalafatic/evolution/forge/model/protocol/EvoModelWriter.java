package eu.kalafatic.evolution.forge.model.protocol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Deterministic writer for the EVO Native Model Protocol (.evo binary container format v2).
 * Writes 32-bit length prefixed UTF-8 strings to eliminate 64 KB writeUTF limitations.
 */
public class EvoModelWriter {

    public static final String MAGIC_V2 = "EVO_NAT2"; // 8 Bytes ASCII
    public static final String MAGIC_V1 = "EVO_ARTIFACT_V1";
    public static final short FORMAT_VERSION_MAJOR = 2;
    public static final short FORMAT_VERSION_MINOR = 0;
    public static final short PROTOCOL_VERSION = 1;

    private static final int CHUNK_SIZE = 8192;

    public void writeModel(Path path,
                           String modelName,
                           EvoArchitectureDescriptor architecture,
                           EvoTokenizerDescriptor tokenizer,
                           List<String> tensorNames,
                           List<long[]> tensorShapes,
                           List<EvoDtype> tensorDtypes,
                           List<float[]> tensorData,
                           Map<String, String> metadata) throws IOException {

        if (path == null) {
            throw new IllegalArgumentException("Target file path cannot be null");
        }
        if (architecture == null) {
            throw new IllegalArgumentException("Architecture descriptor cannot be null");
        }
        if (tokenizer == null) {
            throw new IllegalArgumentException("Tokenizer descriptor cannot be null");
        }
        if (tensorNames == null || tensorData == null || tensorNames.size() != tensorData.size()) {
            throw new IllegalArgumentException("Tensor names and data must be non-null and match in size");
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tempFile = parent != null ?
                parent.resolve(path.getFileName().toString() + ".tmp") :
                Path.of(path.toString() + ".tmp");

        List<EvoTensorDescriptor> manifest = new ArrayList<>();
        byte[][] compressedTensorBytes = new byte[tensorData.size()][];
        long currentOffset = 0;

        for (int i = 0; i < tensorData.size(); i++) {
            String name = tensorNames.get(i);
            long[] shape = tensorShapes.get(i);
            EvoDtype dtype = tensorDtypes != null && i < tensorDtypes.size() ? tensorDtypes.get(i) : EvoDtype.F32;
            float[] data = tensorData.get(i);

            String tensorChecksum = EvoModelContentHash.calculateFloatArrayChecksum(data);
            byte[] rawBytes = floatArrayToByteArrayChunked(data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(rawBytes);
            }
            byte[] compBytes = baos.toByteArray();
            compressedTensorBytes[i] = compBytes;

            EvoTensorDescriptor descriptor = new EvoTensorDescriptor(
                    i,
                    name,
                    dtype,
                    shape,
                    0, // ROW_MAJOR
                    currentOffset,
                    compBytes.length,
                    rawBytes.length,
                    1, // GZIP
                    tensorChecksum
            );
            manifest.add(descriptor);
            currentOffset += compBytes.length;
        }

        String contentHash = EvoModelContentHash.calculateContentHash(architecture, tokenizer, manifest, tensorData);

        JSONObject archJson = architecture.toJsonObject();
        JSONObject tokJson = tokenizer.toJsonObject();

        JSONObject vocabJson = new JSONObject();
        if (tokenizer.getTokenToId() != null) {
            for (Map.Entry<String, Integer> entry : tokenizer.getTokenToId().entrySet()) {
                vocabJson.put(entry.getKey(), entry.getValue());
            }
        }

        JSONArray manifestArr = new JSONArray();
        for (EvoTensorDescriptor desc : manifest) {
            JSONObject tObj = new JSONObject();
            tObj.put("id", desc.getId());
            tObj.put("name", desc.getCanonicalName());
            tObj.put("dtype", desc.getDtype().getCode());
            JSONArray shapeArr = new JSONArray();
            for (long s : desc.getShape()) shapeArr.put(s);
            tObj.put("shape", shapeArr);
            tObj.put("layout", desc.getLayout());
            tObj.put("offset", desc.getOffset());
            tObj.put("compressed_size", desc.getCompressedSize());
            tObj.put("uncompressed_size", desc.getUncompressedSize());
            tObj.put("compression", desc.getCompressionScheme());
            tObj.put("checksum", desc.getChecksum());
            manifestArr.put(tObj);
        }

        JSONObject metaJson = new JSONObject();
        if (metadata != null) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                metaJson.put(entry.getKey(), entry.getValue());
            }
        }

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile.toFile())))) {
            dos.write(MAGIC_V2.getBytes(StandardCharsets.US_ASCII));
            dos.writeShort(FORMAT_VERSION_MAJOR);
            dos.writeShort(FORMAT_VERSION_MINOR);
            dos.writeShort(PROTOCOL_VERSION);
            dos.writeShort(0); // flags
            dos.writeInt(64);  // header size
            dos.writeInt(5);   // section count
            dos.writeLong(System.currentTimeMillis());

            byte[] hashBytes = contentHash.getBytes(StandardCharsets.UTF_8);
            byte[] hashPadded = new byte[32];
            System.arraycopy(hashBytes, 0, hashPadded, 0, Math.min(hashBytes.length, 32));
            dos.write(hashPadded);

            writeString32(dos, modelName != null ? modelName : "evo_model");
            writeString32(dos, archJson.toString());
            writeString32(dos, tokJson.toString());
            writeString32(dos, vocabJson.toString());
            writeString32(dos, manifestArr.toString());
            writeString32(dos, metaJson.toString());

            dos.writeInt(compressedTensorBytes.length);
            for (byte[] cBytes : compressedTensorBytes) {
                dos.writeInt(cBytes.length);
                dos.write(cBytes);
            }
        }

        Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void writeString32(DataOutputStream dos, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }

    private byte[] floatArrayToByteArrayChunked(float[] floats) {
        ByteBuffer bb = ByteBuffer.allocate(floats.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
        for (float f : floats) {
            bb.putFloat(f);
        }
        return bb.array();
    }
}
