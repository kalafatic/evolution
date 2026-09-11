package eu.kalafatic.evolution.forge.agent.gguf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Standalone GGUFReader that sequentially parses GGUF binary files from disk.
 *
 * Tracks explicit file positions, offsets, lengths, and value types.
 * Rejects malformed files instead of silently continuing or guessing.
 */
public class GGUFReader implements AutoCloseable {

    private final Path filePath;
    private final RandomAccessFile file;
    private final FileChannel channel;
    private final long fileSize;

    private GGUFHeader header;
    private final Map<String, GGUFMetadata> metadataMap = new LinkedHashMap<>();
    private final List<GGUFMetadata> metadataList = new ArrayList<>();
    private final Map<String, GGUFTensorInfo> tensorMap = new LinkedHashMap<>();
    private final List<GGUFTensorInfo> tensorList = new ArrayList<>();

    private long alignment = 32; // Default alignment
    private long dataSectionStart = -1;

    public static class StringReadResult {
        public final String value;
        public final long rawLength;

        public StringReadResult(String value, long rawLength) {
            this.value = value;
            this.rawLength = rawLength;
        }
    }

    public GGUFReader(Path filePath) throws IOException, GGUFException {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        if (!Files.exists(filePath)) {
            throw new GGUFException("file", 0, "GGUF file does not exist: " + filePath.toAbsolutePath());
        }
        this.fileSize = Files.size(filePath);
        if (fileSize < 12) {
            throw new GGUFException("header", 0, "File size (" + fileSize + " bytes) is too small to contain a valid GGUF header");
        }

        this.file = new RandomAccessFile(filePath.toFile(), "r");
        this.channel = file.getChannel();

        parse();
    }

    private void parse() throws IOException, GGUFException {
        long currentOffset = 0;

        // 1. Header parsing
        ByteBuffer headerBuf = readBuffer(currentOffset, 24, "header");
        byte[] magicBytes = new byte[4];
        headerBuf.get(magicBytes);
        String magic = new String(magicBytes, StandardCharsets.UTF_8);

        if (!"GGUF".equals(magic)) {
            throw new GGUFException("header", currentOffset,
                String.format("Invalid GGUF magic header. Expected 'GGUF', found '%s' (bytes: 0x%02X 0x%02X 0x%02X 0x%02X)",
                    magic, magicBytes[0], magicBytes[1], magicBytes[2], magicBytes[3]));
        }

        int version = headerBuf.getInt();
        if (version != 2 && version != 3) {
            throw new GGUFException("header", currentOffset + 4, "Unsupported GGUF version: " + version + ". Supported versions are 2 and 3");
        }

        long tensorCount = headerBuf.getLong();
        long metadataCount = headerBuf.getLong();

        if (tensorCount < 0) {
            throw new GGUFException("header", currentOffset + 8, "Negative tensor count: " + tensorCount);
        }
        if (metadataCount < 0) {
            throw new GGUFException("header", currentOffset + 16, "Negative metadata count: " + metadataCount);
        }

        this.header = new GGUFHeader(magic, version, tensorCount, metadataCount, currentOffset, 24);
        currentOffset += 24;

        // 2. Metadata parsing
        for (long i = 0; i < metadataCount; i++) {
            long metaStartOffset = currentOffset;
            StringReadResult keyResult = readGGUFString("metadata_key", currentOffset);
            String key = keyResult.value;
            long keyLen = safeAdd(8, keyResult.rawLength);
            currentOffset = safeAdd(currentOffset, keyLen);

            ByteBuffer typeBuf = readBuffer(currentOffset, 4, "metadata_type");
            int typeId = typeBuf.getInt();
            currentOffset = safeAdd(currentOffset, 4);

            GGUFValueType valueType = GGUFValueType.fromId(typeId);
            ParseValueResult valResult = readMetadataValue(valueType, currentOffset);
            currentOffset = valResult.nextOffset;

            long metaLen = currentOffset - metaStartOffset;
            GGUFMetadata metadata = new GGUFMetadata(key, valueType, valResult.value, metaStartOffset, metaLen);

            if (metadataMap.containsKey(key)) {
                throw new GGUFException("metadata", metaStartOffset, "Duplicate metadata key detected: " + key);
            }
            metadataMap.put(key, metadata);
            metadataList.add(metadata);

            // Extract alignment if present
            if ("general.alignment".equals(key)) {
                if (valResult.value instanceof Number) {
                    this.alignment = ((Number) valResult.value).longValue();
                    if (alignment <= 0 || (alignment & (alignment - 1)) != 0) {
                        throw new GGUFException("metadata", metaStartOffset, "Invalid alignment value (must be positive power of 2): " + alignment);
                    }
                }
            }
        }

        // 3. Tensor info parsing
        for (long i = 0; i < tensorCount; i++) {
            long tensorDescriptorStart = currentOffset;

            StringReadResult nameResult = readGGUFString("tensor_descriptor", currentOffset);
            String tensorName = nameResult.value;
            long nameLen = safeAdd(8, nameResult.rawLength);
            currentOffset = safeAdd(currentOffset, nameLen);

            ByteBuffer dimsBuf = readBuffer(currentOffset, 4, "tensor_dimensions_count");
            int nDims = dimsBuf.getInt();
            currentOffset = safeAdd(currentOffset, 4);

            if (nDims < 0 || nDims > 8) {
                throw new GGUFException("tensor_info", currentOffset - 4,
                    String.format("Invalid dimension count (%d) for tensor '%s'", nDims, tensorName));
            }

            long[] dimensions = new long[nDims];
            long elementCount = 1;
            if (nDims > 0) {
                ByteBuffer shapeBuf = readBuffer(currentOffset, nDims * 8, "tensor_shape");
                for (int d = 0; d < nDims; d++) {
                    long dim = shapeBuf.getLong();
                    if (dim <= 0) {
                        throw new GGUFException("tensor_info", currentOffset + (d * 8),
                            String.format("Invalid dimension [%d] = %d for tensor '%s'", d, dim, tensorName));
                    }
                    dimensions[d] = dim;
                    try {
                        elementCount = Math.multiplyExact(elementCount, dim);
                    } catch (ArithmeticException e) {
                        throw new GGUFException("tensor_info", currentOffset, "Integer overflow calculating element count for tensor: " + tensorName);
                    }
                }
                currentOffset = safeAdd(currentOffset, nDims * 8);
            }

            ByteBuffer typeAndOffsetBuf = readBuffer(currentOffset, 12, "tensor_type_and_offset");
            int typeId = typeAndOffsetBuf.getInt();
            long ggufTensorOffset = typeAndOffsetBuf.getLong();
            currentOffset = safeAdd(currentOffset, 12);

            GGMLType ggmlType = GGMLType.fromId(typeId);
            long byteSize = ggmlType.calculateByteSize(elementCount);

            long descriptorLength = currentOffset - tensorDescriptorStart;

            GGUFTensorInfo tensorInfo = new GGUFTensorInfo(
                tensorName, dimensions, ggmlType,
                ggufTensorOffset, -1, // absolute file offset determined after data section alignment
                byteSize, elementCount,
                tensorDescriptorStart, descriptorLength
            );

            if (tensorMap.containsKey(tensorName)) {
                throw new GGUFException("tensor_info", tensorDescriptorStart, "Duplicate tensor name detected: " + tensorName);
            }
            tensorMap.put(tensorName, tensorInfo);
            tensorList.add(tensorInfo);
        }

        // 4. Align to data section start
        this.dataSectionStart = alignOffset(currentOffset, alignment);

        if (dataSectionStart > fileSize) {
            throw new GGUFException("alignment", currentOffset,
                String.format("Data section start (0x%X) exceeds file size (0x%X)", dataSectionStart, fileSize));
        }

        // Update absolute file offsets in tensor list and validate offset boundaries
        for (int i = 0; i < tensorList.size(); i++) {
            GGUFTensorInfo rawInfo = tensorList.get(i);
            long absoluteFileOffset = safeAdd(dataSectionStart, rawInfo.getGgufTensorOffset());

            GGUFTensorInfo updatedInfo = new GGUFTensorInfo(
                rawInfo.getName(), rawInfo.getDimensions(), rawInfo.getGgmlType(),
                rawInfo.getGgufTensorOffset(), absoluteFileOffset,
                rawInfo.getByteSize(), rawInfo.getElementCount(),
                rawInfo.getDescriptorOffset(), rawInfo.getDescriptorLength()
            );

            tensorList.set(i, updatedInfo);
            tensorMap.put(rawInfo.getName(), updatedInfo);
        }

        validateTensorOffsetsAndRanges();
    }

    /**
     * Strict verification of tensor offsets, payload boundaries, overflow, and overlapping regions.
     */
    private void validateTensorOffsetsAndRanges() throws GGUFException {
        for (GGUFTensorInfo tensor : tensorList) {
            long start = tensor.getAbsoluteFileOffset();
            long size = tensor.getByteSize();

            if (start < dataSectionStart) {
                throw new GGUFException("tensor_offset", tensor.getDescriptorOffset(),
                    String.format("Tensor '%s' start offset (0x%X) is before data section start (0x%X)",
                        tensor.getName(), start, dataSectionStart));
            }

            if (start >= fileSize && size > 0) {
                throw new GGUFException("tensor_offset", tensor.getDescriptorOffset(),
                    String.format("Tensor '%s' start offset (0x%X) exceeds file size (0x%X)",
                        tensor.getName(), start, fileSize));
            }

            long end = safeAdd(start, size);
            if (end > fileSize) {
                throw new GGUFException("tensor_payload", tensor.getDescriptorOffset(),
                    String.format("Tensor '%s' payload range [0x%X, 0x%X) extends past end of file (0x%X)",
                        tensor.getName(), start, end, fileSize));
            }

            if (start % alignment != 0) {
                throw new GGUFException("alignment", tensor.getDescriptorOffset(),
                    String.format("Tensor '%s' absolute offset 0x%X is not aligned to %d bytes",
                        tensor.getName(), start, alignment));
            }
        }

        // Check for overlapping tensors
        List<GGUFTensorInfo> sortedByOffset = new ArrayList<>(tensorList);
        sortedByOffset.sort(Comparator.comparingLong(GGUFTensorInfo::getAbsoluteFileOffset));

        for (int i = 0; i < sortedByOffset.size() - 1; i++) {
            GGUFTensorInfo current = sortedByOffset.get(i);
            GGUFTensorInfo next = sortedByOffset.get(i + 1);

            long currentEnd = current.getAbsoluteFileOffset() + current.getByteSize();
            long nextStart = next.getAbsoluteFileOffset();

            if (currentEnd > nextStart) {
                throw new GGUFException("tensor_overlap", current.getDescriptorOffset(),
                    String.format("Overlapping tensor payload detected: Tensor '%s' range [0x%X, 0x%X) overlaps with Tensor '%s' range [0x%X, 0x%X)",
                        current.getName(), current.getAbsoluteFileOffset(), currentEnd,
                        next.getName(), nextStart, nextStart + next.getByteSize()));
            }
        }
    }

    /**
     * Read raw bytes for a named tensor safely from disk.
     */
    public byte[] readTensorBytes(String name) throws IOException, GGUFException {
        GGUFTensorInfo info = tensorMap.get(name);
        if (info == null) {
            throw new GGUFException("tensor_data", -1, "Tensor not found in GGUF file: " + name);
        }
        long offset = info.getAbsoluteFileOffset();
        long size = info.getByteSize();

        if (size > Integer.MAX_VALUE) {
            throw new GGUFException("tensor_data", offset, "Tensor '" + name + "' byte size (" + size + ") exceeds Java byte array maximum size");
        }

        ByteBuffer buf = readBuffer(offset, (int) size, "tensor_data");
        byte[] bytes = new byte[(int) size];
        buf.get(bytes);
        return bytes;
    }

    /**
     * Decode float values for floating-point tensors (F32 / F16).
     */
    public float[] readTensorFloatData(String name) throws IOException, GGUFException {
        GGUFTensorInfo info = tensorMap.get(name);
        if (info == null) {
            throw new GGUFException("tensor_data", -1, "Tensor not found in GGUF file: " + name);
        }

        byte[] rawBytes = readTensorBytes(name);
        ByteBuffer buf = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN);
        long elemCount = info.getElementCount();

        if (elemCount > Integer.MAX_VALUE) {
            throw new GGUFException("tensor_data", info.getAbsoluteFileOffset(), "Tensor element count exceeds Integer.MAX_VALUE: " + elemCount);
        }

        float[] floats = new float[(int) elemCount];

        if (info.getGgmlType() == GGMLType.F32) {
            for (int i = 0; i < elemCount; i++) {
                floats[i] = buf.getFloat();
            }
        } else if (info.getGgmlType() == GGMLType.F16) {
            for (int i = 0; i < elemCount; i++) {
                short h = buf.getShort();
                floats[i] = fp16ToFloat(h);
            }
        } else {
            throw new GGUFException("tensor_data", info.getAbsoluteFileOffset(),
                "Decoding float values for GGML type " + info.getGgmlType() + " is not supported");
        }

        return floats;
    }

    private static float fp16ToFloat(short halfPrecision) {
        int bits = halfPrecision & 0xffff;
        int sign = (bits >>> 15) & 1;
        int exp = (bits >>> 10) & 0x1f;
        int mant = bits & 0x03ff;

        if (exp == 0) {
            if (mant == 0) {
                return sign == 0 ? 0.0f : -0.0f;
            } else {
                // Denormalized number
                float val = (float) (Math.scalb(mant, -24));
                return sign == 0 ? val : -val;
            }
        } else if (exp == 31) {
            if (mant == 0) {
                return sign == 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            } else {
                return Float.NaN;
            }
        } else {
            int fExp = exp - 15 + 127;
            int fMant = mant << 13;
            int fBits = (sign << 31) | (fExp << 23) | fMant;
            return Float.intBitsToFloat(fBits);
        }
    }

    private StringReadResult readGGUFString(String section, long offset) throws IOException, GGUFException {
        ByteBuffer buf = readBuffer(offset, 8, section);
        long len = buf.getLong();
        if (len < 0 || len > 100_000_000) { // Safety cap
            throw new GGUFException(section, offset, "String length invalid or excessive: " + len);
        }
        ByteBuffer strBuf = readBuffer(safeAdd(offset, 8), (int) len, section);
        byte[] bytes = new byte[(int) len];
        strBuf.get(bytes);
        return new StringReadResult(new String(bytes, StandardCharsets.UTF_8), len);
    }

    private static class ParseValueResult {
        final Object value;
        final long nextOffset;

        ParseValueResult(Object value, long nextOffset) {
            this.value = value;
            this.nextOffset = nextOffset;
        }
    }

    private ParseValueResult readMetadataValue(GGUFValueType type, long offset) throws IOException, GGUFException {
        switch (type) {
            case UINT8: {
                ByteBuffer buf = readBuffer(offset, 1, "metadata_val_uint8");
                return new ParseValueResult(Byte.toUnsignedInt(buf.get()), safeAdd(offset, 1));
            }
            case INT8: {
                ByteBuffer buf = readBuffer(offset, 1, "metadata_val_int8");
                return new ParseValueResult((int) buf.get(), safeAdd(offset, 1));
            }
            case UINT16: {
                ByteBuffer buf = readBuffer(offset, 2, "metadata_val_uint16");
                return new ParseValueResult(Short.toUnsignedInt(buf.getShort()), safeAdd(offset, 2));
            }
            case INT16: {
                ByteBuffer buf = readBuffer(offset, 2, "metadata_val_int16");
                return new ParseValueResult((int) buf.getShort(), safeAdd(offset, 2));
            }
            case UINT32: {
                ByteBuffer buf = readBuffer(offset, 4, "metadata_val_uint32");
                return new ParseValueResult(Integer.toUnsignedLong(buf.getInt()), safeAdd(offset, 4));
            }
            case INT32: {
                ByteBuffer buf = readBuffer(offset, 4, "metadata_val_int32");
                return new ParseValueResult(buf.getInt(), safeAdd(offset, 4));
            }
            case FLOAT32: {
                ByteBuffer buf = readBuffer(offset, 4, "metadata_val_float32");
                return new ParseValueResult(buf.getFloat(), safeAdd(offset, 4));
            }
            case BOOL: {
                ByteBuffer buf = readBuffer(offset, 1, "metadata_val_bool");
                byte b = buf.get();
                if (b != 0 && b != 1) {
                    throw new GGUFException("metadata_val_bool", offset, "Invalid boolean byte value (expected 0 or 1): " + b);
                }
                return new ParseValueResult(b != 0, safeAdd(offset, 1));
            }
            case STRING: {
                StringReadResult strResult = readGGUFString("metadata_val_string", offset);
                long strLen = safeAdd(8, strResult.rawLength);
                return new ParseValueResult(strResult.value, safeAdd(offset, strLen));
            }
            case ARRAY: {
                ByteBuffer buf = readBuffer(offset, 12, "metadata_val_array_header");
                int elemTypeId = buf.getInt();
                long arrayLen = buf.getLong();

                GGUFValueType elemType = GGUFValueType.fromId(elemTypeId);
                if (arrayLen < 0 || arrayLen > 10_000_000) {
                    throw new GGUFException("metadata_val_array", offset, "Invalid array length: " + arrayLen);
                }

                long curOffset = safeAdd(offset, 12);
                List<Object> list = new ArrayList<>();
                for (long i = 0; i < arrayLen; i++) {
                    ParseValueResult elemResult = readMetadataValue(elemType, curOffset);
                    list.add(elemResult.value);
                    curOffset = elemResult.nextOffset;
                }
                return new ParseValueResult(list, curOffset);
            }
            case UINT64: {
                ByteBuffer buf = readBuffer(offset, 8, "metadata_val_uint64");
                return new ParseValueResult(buf.getLong(), safeAdd(offset, 8)); // Note: Java long is signed, but retains bit pattern
            }
            case INT64: {
                ByteBuffer buf = readBuffer(offset, 8, "metadata_val_int64");
                return new ParseValueResult(buf.getLong(), safeAdd(offset, 8));
            }
            case FLOAT64: {
                ByteBuffer buf = readBuffer(offset, 8, "metadata_val_float64");
                return new ParseValueResult(buf.getDouble(), safeAdd(offset, 8));
            }
            default:
                throw new GGUFException("metadata_val", offset, "Unhandled GGUF value type: " + type);
        }
    }

    private ByteBuffer readBuffer(long offset, int length, String section) throws IOException, GGUFException {
        if (offset < 0 || length < 0) {
            throw new GGUFException(section, offset, "Negative read offset or length");
        }
        if (safeAdd(offset, length) > fileSize) {
            throw new GGUFException(section, offset,
                String.format("Read request for %d bytes at offset 0x%X exceeds file size (0x%X)", length, offset, fileSize));
        }

        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int readBytes = channel.read(buf, offset);
        if (readBytes != length) {
            throw new GGUFException(section, offset,
                String.format("Unexpected EOF while reading %d bytes at offset 0x%X (read %d bytes)", length, offset, readBytes));
        }
        buf.flip();
        return buf;
    }

    private static long safeAdd(long a, long b) throws GGUFException {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            throw new GGUFException("overflow", a, "Integer overflow adding " + a + " and " + b);
        }
    }

    private static long alignOffset(long offset, long alignment) throws GGUFException {
        long remainder = offset % alignment;
        if (remainder == 0) return offset;
        long padding = alignment - remainder;
        return safeAdd(offset, padding);
    }

    // Getters
    public Path getFilePath() { return filePath; }
    public long getFileSize() { return fileSize; }
    public GGUFHeader getHeader() { return header; }
    public Map<String, GGUFMetadata> getMetadataMap() { return Collections.unmodifiableMap(metadataMap); }
    public List<GGUFMetadata> getMetadataList() { return Collections.unmodifiableList(metadataList); }
    public Map<String, GGUFTensorInfo> getTensorMap() { return Collections.unmodifiableMap(tensorMap); }
    public List<GGUFTensorInfo> getTensorList() { return Collections.unmodifiableList(tensorList); }
    public long getAlignment() { return alignment; }
    public long getDataSectionStart() { return dataSectionStart; }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
