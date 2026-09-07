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

        JSONObject vocabObj = new JSONObject(vocabJsonStr);
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

        int tensorCount = dis.readInt();
        tensorDataPayloads.clear();
        for (int i = 0; i < tensorCount; i++) {
            int compLen = dis.readInt();
            byte[] compBytes = new byte[compLen];
            dis.readFully(compBytes);

            byte[] rawBytes;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(compBytes);
                 GZIPInputStream gzis = new GZIPInputStream(bais)) {
                rawBytes = gzis.readAllBytes();
            }
            float[] floats = byteArrayToFloatArray(rawBytes);
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

    private float[] byteArrayToFloatArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        float[] floats = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = bb.getFloat();
        }
        return floats;
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
}
