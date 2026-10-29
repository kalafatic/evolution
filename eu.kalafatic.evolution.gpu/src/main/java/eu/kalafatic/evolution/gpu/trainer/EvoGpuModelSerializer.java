package eu.kalafatic.evolution.gpu.trainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Independent binary .evo (EVO_NAT2) format reader and writer for GPU module.
 * Fully compatible with EVO Native Model Protocol version 2.
 */
public class EvoGpuModelSerializer {

    public static final String MAGIC_V2 = "EVO_NAT2";
    public static final short FORMAT_VERSION_MAJOR = 2;
    public static final short FORMAT_VERSION_MINOR = 0;
    public static final short PROTOCOL_VERSION = 1;

    public static class ModelContainer {
        private String modelName;
        private String architectureJson;
        private String tokenizerJson;
        private Map<String, Integer> tokenToId = new HashMap<>();
        private Map<String, String> metadata = new HashMap<>();
        private List<String> tensorNames = new ArrayList<>();
        private List<long[]> tensorShapes = new ArrayList<>();
        private List<float[]> tensorData = new ArrayList<>();

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getArchitectureJson() { return architectureJson; }
        public void setArchitectureJson(String architectureJson) { this.architectureJson = architectureJson; }

        public String getTokenizerJson() { return tokenizerJson; }
        public void setTokenizerJson(String tokenizerJson) { this.tokenizerJson = tokenizerJson; }

        public Map<String, Integer> getTokenToId() { return tokenToId; }
        public void setTokenToId(Map<String, Integer> tokenToId) { this.tokenToId = tokenToId; }

        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

        public List<String> getTensorNames() { return tensorNames; }
        public List<long[]> getTensorShapes() { return tensorShapes; }
        public List<float[]> getTensorData() { return tensorData; }

        public void addTensor(String name, long[] shape, float[] data) {
            this.tensorNames.add(name);
            this.tensorShapes.add(shape);
            this.tensorData.add(data);
        }
    }

    public void writeModel(Path path, ModelContainer container) throws IOException {
        if (path == null) throw new IllegalArgumentException("Path cannot be null");
        if (container == null) throw new IllegalArgumentException("Container cannot be null");

        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);

        Path tempFile = parent != null ?
                parent.resolve(path.getFileName().toString() + ".tmp") :
                Path.of(path.toString() + ".tmp");

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode vocabJson = mapper.createObjectNode();
        if (container.getTokenToId() != null) {
            container.getTokenToId().forEach(vocabJson::put);
        }

        ArrayNode manifestArr = mapper.createArrayNode();
        byte[][] compressedTensors = new byte[container.getTensorData().size()][];
        long currentOffset = 0;

        for (int i = 0; i < container.getTensorData().size(); i++) {
            String name = container.getTensorNames().get(i);
            long[] shape = container.getTensorShapes().get(i);
            float[] data = container.getTensorData().get(i);

            byte[] rawBytes = floatArrayToByteArray(data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(rawBytes);
            }
            byte[] compBytes = baos.toByteArray();
            compressedTensors[i] = compBytes;

            ObjectNode tObj = mapper.createObjectNode();
            tObj.put("id", i);
            tObj.put("name", name);
            tObj.put("dtype", "F32");
            ArrayNode shapeArr = mapper.createArrayNode();
            for (long s : shape) shapeArr.add(s);
            tObj.set("shape", shapeArr);
            tObj.put("layout", 0);
            tObj.put("offset", currentOffset);
            tObj.put("compressed_size", compBytes.length);
            tObj.put("uncompressed_size", rawBytes.length);
            tObj.put("compression", 1);
            tObj.put("checksum", calculateChecksum(data));
            manifestArr.add(tObj);

            currentOffset += compBytes.length;
        }

        ObjectNode metaJson = mapper.createObjectNode();
        if (container.getMetadata() != null) {
            container.getMetadata().forEach(metaJson::put);
        }

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile.toFile())))) {
            dos.write(MAGIC_V2.getBytes(StandardCharsets.US_ASCII));
            dos.writeShort(FORMAT_VERSION_MAJOR);
            dos.writeShort(FORMAT_VERSION_MINOR);
            dos.writeShort(PROTOCOL_VERSION);
            dos.writeShort(0);
            dos.writeInt(64);
            dos.writeInt(5);
            dos.writeLong(System.currentTimeMillis());

            byte[] hashPadded = new byte[32];
            dos.write(hashPadded);

            writeString32(dos, container.getModelName() != null ? container.getModelName() : "evo_gpu_model");
            writeString32(dos, container.getArchitectureJson() != null ? container.getArchitectureJson() : "{}");
            writeString32(dos, container.getTokenizerJson() != null ? container.getTokenizerJson() : "{}");
            writeString32(dos, vocabJson.toString());
            writeString32(dos, manifestArr.toString());
            writeString32(dos, metaJson.toString());

            dos.writeInt(compressedTensors.length);
            for (byte[] cBytes : compressedTensors) {
                dos.writeInt(cBytes.length);
                dos.write(cBytes);
            }
        }

        Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public ModelContainer readModel(Path path) throws IOException {
        if (!Files.exists(path)) throw new IOException("File not found: " + path);

        ModelContainer container = new ModelContainer();
        ObjectMapper mapper = new ObjectMapper();

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path.toFile())))) {
            byte[] magic = new byte[8];
            dis.readFully(magic);
            String magicStr = new String(magic, StandardCharsets.US_ASCII);
            if (!MAGIC_V2.equals(magicStr)) {
                throw new IOException("Invalid magic header: " + magicStr + ", expected " + MAGIC_V2);
            }

            short major = dis.readShort();
            short minor = dis.readShort();
            short proto = dis.readShort();
            short flags = dis.readShort();
            int headerSize = dis.readInt();
            int sectionCount = dis.readInt();
            long timestamp = dis.readLong();
            byte[] hashBytes = new byte[32];
            dis.readFully(hashBytes);

            container.setModelName(readString32(dis));
            container.setArchitectureJson(readString32(dis));
            container.setTokenizerJson(readString32(dis));

            String vocabStr = readString32(dis);
            JsonNode vocabNode = mapper.readTree(vocabStr);
            Map<String, Integer> vocabMap = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = vocabNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                vocabMap.put(entry.getKey(), entry.getValue().asInt());
            }
            container.setTokenToId(vocabMap);

            String manifestStr = readString32(dis);
            JsonNode manifestNode = mapper.readTree(manifestStr);

            String metaStr = readString32(dis);
            JsonNode metaNode = mapper.readTree(metaStr);
            Map<String, String> metaMap = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> metaFields = metaNode.fields();
            while (metaFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = metaFields.next();
                metaMap.put(entry.getKey(), entry.getValue().asText());
            }
            container.setMetadata(metaMap);

            int tensorCount = dis.readInt();
            for (int i = 0; i < tensorCount; i++) {
                int len = dis.readInt();
                byte[] cBytes = new byte[len];
                dis.readFully(cBytes);

                JsonNode tObj = manifestNode.get(i);
                String tName = tObj.get("name").asText();
                JsonNode shapeNode = tObj.get("shape");
                long[] shape = new long[shapeNode.size()];
                for (int s = 0; s < shapeNode.size(); s++) {
                    shape[s] = shapeNode.get(s).asLong();
                }

                int uncompSize = tObj.get("uncompressed_size").asInt();
                byte[] rawBytes = new byte[uncompSize];

                try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(cBytes))) {
                    int read = 0;
                    while (read < uncompSize) {
                        int r = gzis.read(rawBytes, read, uncompSize - read);
                        if (r < 0) break;
                        read += r;
                    }
                }

                float[] data = byteArrayToFloatArray(rawBytes);
                container.addTensor(tName, shape, data);
            }
        }

        return container;
    }

    private void writeString32(DataOutputStream dos, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }

    private String readString32(DataInputStream dis) throws IOException {
        int len = dis.readInt();
        byte[] bytes = new byte[len];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] floatArrayToByteArray(float[] floats) {
        ByteBuffer bb = ByteBuffer.allocate(floats.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
        for (float f : floats) bb.putFloat(f);
        return bb.array();
    }

    private float[] byteArrayToFloatArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        float[] floats = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < floats.length; i++) floats[i] = bb.getFloat();
        return floats;
    }

    private String calculateChecksum(float[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = floatArrayToByteArray(data);
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "0000000000000000";
        }
    }
}
