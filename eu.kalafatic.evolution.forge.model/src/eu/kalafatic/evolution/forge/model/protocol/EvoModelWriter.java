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
            byte[] rawBytes = floatArrayToByteArray(data, dtype, resolveQuantizationScale(name, dtype, data, metadata));

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
        // Full 64-hex-char content hash stored in metadata for integrity verification
        // (the 32-byte header field keeps only the prefix for backwards compatibility).
        metaJson.put("evo_content_hash", contentHash);

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile.toFile())))) {
            dos.write(MAGIC_V2.getBytes(StandardCharsets.US_ASCII));
            dos.writeShort(FORMAT_VERSION_MAJOR);
            dos.writeShort(FORMAT_VERSION_MINOR);
            dos.writeShort(PROTOCOL_VERSION);
            dos.writeShort(0); // flags
            dos.writeInt(64);  // header size
            dos.writeInt(6);   // section count (modelName, arch, tokenizer, vocab, manifest, metadata)
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

    /**
     * Serializes float values into the compact on-disk representation of the given dtype:
     * F32 = 4-byte big-endian floats, F16 = 2-byte half-precision, BF16 = 2-byte truncated
     * floats, INT8 = 4-byte big-endian float scale followed by 1-byte quantized values.
     * INT4 packed storage is not implemented and is rejected at write time.
     */
    private byte[] floatArrayToByteArray(float[] floats, EvoDtype dtype, float int8Scale) {
        ByteBuffer bb;
        switch (dtype) {
            case F32 -> {
                bb = ByteBuffer.allocate(floats.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
                for (float f : floats) {
                    bb.putFloat(f);
                }
            }
            case F16 -> {
                bb = ByteBuffer.allocate(floats.length * Short.BYTES).order(ByteOrder.BIG_ENDIAN);
                for (float f : floats) {
                    bb.putShort(floatToFloat16(f));
                }
            }
            case BF16 -> {
                bb = ByteBuffer.allocate(floats.length * Short.BYTES).order(ByteOrder.BIG_ENDIAN);
                for (float f : floats) {
                    int bits = Float.floatToIntBits(f);
                    bb.putShort((short) (bits >>> 16));
                }
            }
            case INT8 -> {
                float scale = int8Scale > 0.0f ? int8Scale : 1.0f;
                bb = ByteBuffer.allocate(Float.BYTES + floats.length).order(ByteOrder.BIG_ENDIAN);
                bb.putFloat(scale);
                for (float f : floats) {
                    int q = Math.round(f / scale);
                    q = Math.max(-127, Math.min(127, q));
                    bb.put((byte) q);
                }
            }
            case INT4 -> {
                throw new IllegalArgumentException("INT4 tensor storage requires packed nibble encoding which is not implemented; tensor must use F32/F16/BF16/INT8");
            }
            default -> {
                bb = ByteBuffer.allocate(floats.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
                for (float f : floats) {
                    bb.putFloat(f);
                }
            }
        }
        return bb.array();
    }

    private float resolveQuantizationScale(String tensorName, EvoDtype dtype, float[] data, Map<String, String> metadata) {
        if (dtype != EvoDtype.INT8) return 0.0f;
        if (metadata != null && tensorName != null) {
            String stored = metadata.get("tensor_scale_" + tensorName);
            if (stored != null) {
                try {
                    return Float.parseFloat(stored);
                } catch (NumberFormatException ignored) {
                    // fall through to recomputation
                }
            }
        }
        float maxAbs = 0.0f;
        for (float v : data) {
            maxAbs = Math.max(maxAbs, Math.abs(v));
        }
        return maxAbs == 0.0f ? 1.0f : maxAbs / 127.0f;
    }

    private static short floatToFloat16(float val) {
        int bits = Float.floatToIntBits(val);
        int sign = (bits >>> 16) & 0x8000;
        int exp = ((bits >>> 23) & 0xFF) - 127 + 15;
        int mant = bits & 0x007FFFFF;

        if (exp <= 0) {
            return (short) sign;
        } else if (exp >= 31) {
            return (short) (sign | 0x7C00);
        } else {
            return (short) (sign | (exp << 10) | (mant >>> 13));
        }
    }
}
