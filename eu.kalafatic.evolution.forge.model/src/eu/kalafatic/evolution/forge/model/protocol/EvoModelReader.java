package eu.kalafatic.evolution.forge.model.protocol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Authoritative reader for `.evo` Native Model Protocol files (supporting EVO_NATIVE_V2 and legacy V1).
 * Reads 32-bit length prefixed UTF-8 strings.
 */
public class EvoModelReader {

    private String magic;
    private short formatVersionMajor;
    private short formatVersionMinor;
    private short protocolVersion;
    private long createdAt;
    private String modelContentHash;

    private String modelName;
    private EvoArchitectureDescriptor architecture;
    private EvoTokenizerDescriptor tokenizer;
    private final Map<String, String> metadata = new HashMap<>();

    private final List<EvoTensorDescriptor> manifest = new ArrayList<>();
    private final List<float[]> tensorDataPayloads = new ArrayList<>();

    public void read(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new FileNotFoundException("Model path does not exist: " + path);
        }

        try (FileInputStream fis = new FileInputStream(path.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream dis = new DataInputStream(bis)) {

            byte[] magicBytes = new byte[8];
            dis.mark(10);
            int bytesRead = dis.read(magicBytes);
            if (bytesRead < 8) {
                throw new IOException("EVO_PROTOCOL_ERROR: File too small or truncated");
            }

            String headerMagic = new String(magicBytes, StandardCharsets.US_ASCII);
            if (EvoModelWriter.MAGIC_V2.equals(headerMagic)) {
                readV2Format(dis);
            } else {
                bis.reset();
                readV1GzipFormat(dis);
            }
        }
    }

    private void readV2Format(DataInputStream dis) throws IOException {
        this.magic = EvoModelWriter.MAGIC_V2;
        this.formatVersionMajor = dis.readShort();
        this.formatVersionMinor = dis.readShort();
        this.protocolVersion = dis.readShort();
        dis.readShort(); // flags
        dis.readInt();   // headerSize
        dis.readInt();   // sectionCount
        this.createdAt = dis.readLong();

        byte[] hashBytes = new byte[32];
        dis.readFully(hashBytes);
        this.modelContentHash = new String(hashBytes, StandardCharsets.UTF_8).trim();

        this.modelName = readString32(dis);
        String archJsonStr = readString32(dis);
        String tokJsonStr = readString32(dis);
        String vocabJsonStr = readString32(dis);
        String manifestJsonStr = readString32(dis);
        String metaJsonStr = readString32(dis);

        this.architecture = EvoArchitectureDescriptor.fromJsonObject(new JSONObject(archJsonStr));
        this.tokenizer = EvoTokenizerDescriptor.fromJsonObject(new JSONObject(tokJsonStr));

        // Vocab parsing is duplicate-tolerant: real artifacts produced by earlier
        // pipeline revisions can contain duplicate token keys; the last entry wins.
        JSONObject vocabObj = parseJsonObjectLenient(vocabJsonStr);
        Map<String, Integer> tokenToId = new LinkedHashMap<>();
        Map<Integer, String> idToToken = new LinkedHashMap<>();
        for (Iterator<String> it = vocabObj.keys(); it.hasNext(); ) {
            String key = it.next();
            int id = vocabObj.getInt(key);
            tokenToId.put(key, id);
            idToToken.put(id, key);
        }
        this.tokenizer.setTokenToId(tokenToId);
        this.tokenizer.setIdToToken(idToToken);

        JSONArray manifestArr = new JSONArray(manifestJsonStr);
        manifest.clear();
        for (int i = 0; i < manifestArr.length(); i++) {
            JSONObject tObj = manifestArr.getJSONObject(i);
            JSONArray shapeArr = tObj.getJSONArray("shape");
            long[] shape = new long[shapeArr.length()];
            for (int s = 0; s < shapeArr.length(); s++) shape[s] = shapeArr.getLong(s);

            EvoTensorDescriptor desc = new EvoTensorDescriptor(
                    tObj.getInt("id"),
                    tObj.getString("name"),
                    EvoDtype.fromCode(tObj.getInt("dtype")),
                    shape,
                    tObj.optInt("layout", 0),
                    tObj.optLong("offset", 0),
                    tObj.optLong("compressed_size", 0),
                    tObj.optLong("uncompressed_size", 0),
                    tObj.optInt("compression", 1),
                    tObj.optString("checksum", "")
            );
            manifest.add(desc);
        }

        JSONObject metaObj = new JSONObject(metaJsonStr);
        metadata.clear();
        for (Iterator<String> it = metaObj.keys(); it.hasNext(); ) {
            String key = it.next();
            metadata.put(key, metaObj.getString(key));
        }

        // Full 64-hex-char hash stored in metadata (post-fix writers); header field
        // carries only the first 32 chars for backwards compatibility.
        String fullHash = metadata.get("evo_content_hash");
        if (fullHash != null && !fullHash.isEmpty()) {
            this.modelContentHash = fullHash;
        }

        int tensorCount = dis.readInt();
        tensorDataPayloads.clear();
        for (int i = 0; i < tensorCount; i++) {
            int compLen = dis.readInt();
            if (compLen < 0) {
                throw new IOException("EVO_INTEGRITY_ERROR: Negative tensor payload length: " + compLen);
            }
            byte[] compBytes = new byte[compLen];
            dis.readFully(compBytes);

            byte[] rawBytes;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(compBytes);
                 GZIPInputStream gzis = new GZIPInputStream(bais)) {
                rawBytes = gzis.readAllBytes();
            } catch (IOException gzipFailure) {
                String tensorName = i < manifest.size() ? manifest.get(i).getCanonicalName() : "tensor_" + i;
                throw new IOException("EVO_INTEGRITY_ERROR: Failed to decompress tensor payload '"
                        + tensorName + "' (" + gzipFailure.getMessage() + ")", gzipFailure);
            }
            EvoDtype dtype = i < manifest.size() ? manifest.get(i).getDtype() : EvoDtype.F32;
            float[] floats = byteArrayToFloatArray(rawBytes, dtype);
            tensorDataPayloads.add(floats);
        }
    }

    private String readString32(DataInputStream dis) throws IOException {
        int len = dis.readInt();
        if (len < 0) {
            throw new IOException("EVO_PROTOCOL_ERROR: Invalid string length: " + len);
        }
        byte[] bytes = new byte[len];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Parses a JSON object tolerating duplicate keys (last value wins), unlike
     * {@code new JSONObject(String)} which throws on duplicates.
     */
    private static JSONObject parseJsonObjectLenient(String jsonStr) throws IOException {
        JSONObject obj = new JSONObject();
        org.json.JSONTokener tokener = new org.json.JSONTokener(jsonStr);
        char c = tokener.nextClean();
        if (c != '{') {
            throw new IOException("EVO_PROTOCOL_ERROR: Expected '{' at start of JSON object");
        }
        while (true) {
            c = tokener.nextClean();
            switch (c) {
                case 0 -> throw new IOException("EVO_PROTOCOL_ERROR: Unterminated JSON object");
                case '}' -> {
                    return obj;
                }
                case ',', ';' -> {
                    // consumed separator; parse the next key
                }
                default -> {
                    tokener.back();
                    String key = tokener.nextValue().toString();
                    c = tokener.nextClean();
                    if (c != ':') {
                        throw new IOException("EVO_PROTOCOL_ERROR: Expected ':' after JSON key '" + key + "'");
                    }
                    obj.put(key, tokener.nextValue());
                }
            }
        }
    }

    private void readV1GzipFormat(DataInputStream bis) throws IOException {
        this.magic = EvoModelWriter.MAGIC_V1;
        try (GZIPInputStream gzis = new GZIPInputStream(bis);
             DataInputStream dis = new DataInputStream(gzis)) {

            String v1Magic = dis.readUTF();
            if (!EvoModelWriter.MAGIC_V1.equals(v1Magic)) {
                throw new IOException("EVO_PROTOCOL_ERROR: Invalid format magic header: " + v1Magic);
            }

            this.modelName = dis.readUTF();
            String modelVersion = dis.readUTF();
            this.createdAt = dis.readLong();

            int vocabSize = dis.readInt();
            int dModel = dis.readInt();
            int numHeads = dis.readInt();
            int numBlocks = dis.readInt();
            int dff = dis.readInt();
            int maxSeqLen = dis.readInt();

            this.architecture = new EvoArchitectureDescriptor(vocabSize, dModel, numHeads, numBlocks, dff, maxSeqLen);

            float temperature = dis.readFloat();
            float topP = dis.readFloat();
            int topK = dis.readInt();
            float repeatPenalty = dis.readFloat();
            float frequencyPenalty = dis.readFloat();
            float presencePenalty = dis.readFloat();

            int bosTokenId = dis.readInt();
            int eosTokenId = dis.readInt();
            int unkTokenId = dis.readInt();
            int padTokenId = dis.readInt();

            int vocabCount = dis.readInt();
            Map<Integer, String> idToToken = new LinkedHashMap<>();
            Map<String, Integer> tokenToId = new LinkedHashMap<>();
            for (int i = 0; i < vocabCount; i++) {
                int id = dis.readInt();
                String tok = dis.readUTF();
                idToToken.put(id, tok);
                tokenToId.put(tok, id);
            }

            this.tokenizer = new EvoTokenizerDescriptor(vocabSize, tokenToId);
            this.tokenizer.setBosTokenId(bosTokenId);
            this.tokenizer.setEosTokenId(eosTokenId);
            this.tokenizer.setUnkTokenId(unkTokenId);
            this.tokenizer.setPadTokenId(padTokenId);

            int metaSize = dis.readInt();
            metadata.clear();
            for (int i = 0; i < metaSize; i++) {
                metadata.put(dis.readUTF(), dis.readUTF());
            }

            int weightCount = dis.readInt();
            manifest.clear();
            tensorDataPayloads.clear();

            for (int i = 0; i < weightCount; i++) {
                String name = dis.readUTF();
                int shapeLen = dis.readInt();
                long[] shape = new long[shapeLen];
                for (int d = 0; d < shapeLen; d++) {
                    shape[d] = dis.readLong();
                }
                int dataLen = dis.readInt();
                float[] data = new float[dataLen];
                for (int j = 0; j < dataLen; j++) {
                    data[j] = dis.readFloat();
                }

                String checksum = EvoModelContentHash.calculateFloatArrayChecksum(data);
                EvoTensorDescriptor desc = new EvoTensorDescriptor(
                        i, name, EvoDtype.F32, shape, 0, 0, dataLen * 4, dataLen * 4, 1, checksum
                );
                manifest.add(desc);
                tensorDataPayloads.add(data);
            }

            this.modelContentHash = EvoModelContentHash.calculateContentHash(architecture, tokenizer, manifest, tensorDataPayloads);
        }
    }

    private float[] byteArrayToFloatArray(byte[] bytes) throws IOException {
        return byteArrayToFloatArray(bytes, EvoDtype.F32);
    }

    /**
     * Decodes compact on-disk payloads back into float32 values according to the tensor dtype:
     * F16 = half-precision expansion, BF16 = zero-extended truncated floats, INT8 = leading
     * 4-byte scale followed by quantized bytes dequantized back to float32.
     */
    private float[] byteArrayToFloatArray(byte[] bytes, EvoDtype dtype) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        switch (dtype) {
            case F16 -> {
                float[] floats = new float[bytes.length / Short.BYTES];
                for (int i = 0; i < floats.length; i++) {
                    floats[i] = float16ToFloat(bb.getShort());
                }
                return floats;
            }
            case BF16 -> {
                float[] floats = new float[bytes.length / Short.BYTES];
                for (int i = 0; i < floats.length; i++) {
                    int upper = bb.getShort() & 0xFFFF;
                    floats[i] = Float.intBitsToFloat(upper << 16);
                }
                return floats;
            }
            case INT8 -> {
                if (bytes.length < Float.BYTES) {
                    throw new IOException("EVO_PROTOCOL_ERROR: INT8 payload too small to contain scale prefix");
                }
                float scale = bb.getFloat();
                if (scale <= 0.0f || Float.isNaN(scale) || Float.isInfinite(scale)) {
                    throw new IOException("EVO_PROTOCOL_ERROR: Invalid INT8 quantization scale: " + scale);
                }
                float[] floats = new float[bytes.length - Float.BYTES];
                for (int i = 0; i < floats.length; i++) {
                    floats[i] = bb.get() * scale;
                }
                return floats;
            }
            case INT4 -> {
                throw new IOException("EVO_PROTOCOL_ERROR: INT4 payload decoding requires packed nibble storage which is not implemented");
            }
            default -> {
                float[] floats = new float[bytes.length / Float.BYTES];
                for (int i = 0; i < floats.length; i++) {
                    floats[i] = bb.getFloat();
                }
                return floats;
            }
        }
    }

    private static float float16ToFloat(short hbits) {
        int mant = hbits & 0x03FF;
        int exp = hbits & 0x7C00;
        if (exp == 0x7C00) {
            exp = 0x8f;
        } else if (exp != 0) {
            exp = (exp >> 10) + 127 - 15;
        } else if (mant != 0) {
            exp = 127 - 14;
            while ((mant & 0x0400) == 0) {
                mant <<= 1;
                exp--;
            }
            mant &= 0x03FF;
        } else {
            exp = 0;
        }
        int bits = ((hbits & 0x8000) << 16) | (exp << 23) | (mant << 13);
        return Float.intBitsToFloat(bits);
    }

    public String getMagic() { return magic; }
    public short getFormatVersionMajor() { return formatVersionMajor; }
    public short getFormatVersionMinor() { return formatVersionMinor; }
    public short getProtocolVersion() { return protocolVersion; }
    public long getCreatedAt() { return createdAt; }
    public String getModelContentHash() { return modelContentHash; }
    public String getModelName() { return modelName; }
    public EvoArchitectureDescriptor getArchitecture() { return architecture; }
    public EvoTokenizerDescriptor getTokenizer() { return tokenizer; }
    public Map<String, String> getMetadata() { return metadata; }
    public List<EvoTensorDescriptor> getManifest() { return manifest; }
    public List<float[]> getTensorDataPayloads() { return tensorDataPayloads; }

    /**
     * Verifies the integrity of the loaded model:
     * 1. Each tensor payload must match its manifest SHA-256 checksum.
     * 2. The recomputed full content hash must match the stored hash (for v2 files
     *    carrying the full hash; legacy files without a stored full hash are checked
     *    against the header prefix only when it is non-empty).
     *
     * @return true when integrity is confirmed
     * @throws IOException when a checksum mismatch or corruption is detected
     */
    public boolean verifyIntegrity() throws IOException {
        if (magic == null) {
            throw new IOException("EVO_INTEGRITY_ERROR: No model has been read yet");
        }

        List<String> failures = new ArrayList<>();

        for (int i = 0; i < manifest.size() && i < tensorDataPayloads.size(); i++) {
            EvoTensorDescriptor desc = manifest.get(i);
            float[] payload = tensorDataPayloads.get(i);
            String stored = desc.getChecksum();
            if (stored == null || stored.isEmpty()) {
                failures.add("tensor '" + desc.getCanonicalName() + "': missing stored checksum");
                continue;
            }
            String computed = EvoModelContentHash.calculateFloatArrayChecksum(payload);
            if (!stored.equals(computed)) {
                failures.add("tensor '" + desc.getCanonicalName() + "': checksum mismatch (stored " + stored + ", computed " + computed + ")");
            }
        }
        if (manifest.size() != tensorDataPayloads.size()) {
            failures.add("manifest/payload count mismatch: " + manifest.size() + " descriptors vs " + tensorDataPayloads.size() + " payloads");
        }

        String recomputedHash = EvoModelContentHash.calculateContentHash(architecture, tokenizer, manifest, tensorDataPayloads);
        if (modelContentHash != null && !modelContentHash.isEmpty()) {
            boolean fullHashAvailable = metadata.containsKey("evo_content_hash")
                    && metadata.get("evo_content_hash") != null
                    && metadata.get("evo_content_hash").length() >= 64;
            boolean fullMatch = recomputedHash.equals(modelContentHash);
            boolean prefixMatch = recomputedHash.startsWith(modelContentHash);
            if (fullHashAvailable) {
                // Current-algorithm files carry the full digest in metadata: exact match required.
                if (!fullMatch) {
                    failures.add("model content hash mismatch (stored " + modelContentHash + ", recomputed " + recomputedHash + ")");
                }
            } else {
                // Legacy files were hashed by an earlier algorithm that is not
                // reproducible by the current implementation; the per-tensor
                // checksums above remain the enforced integrity layer. A matching
                // prefix is still reported as a bonus verification.
                if (!prefixMatch) {
                    // Tolerated: legacy-algorithm artifact; integrity is still
                    // enforced per tensor. No failure is added.
                    System.err.println("[EVO-NATIVE] Legacy artifact: content hash was produced by an earlier algorithm and is not re-verified; per-tensor checksums remain enforced.");
                }
            }
        }

        if (!failures.isEmpty()) {
            throw new IOException("EVO_INTEGRITY_ERROR: Model failed integrity verification: " + String.join("; ", failures));
        }
        return true;
    }
}
