package eu.kalafatic.evolution.forge.agent.gguf;

public enum GGMLType {
    F32(0, 4, 1),
    F16(1, 2, 1),
    Q4_0(2, 18, 32),
    Q4_1(3, 20, 32),
    Q8_0(8, 34, 32),
    Q8_1(9, 36, 32),
    Q2_K(10, 256, 256),
    Q3_K(11, 256, 256),
    Q4_K(12, 256, 256),
    Q5_K(13, 256, 256),
    Q6_K(14, 256, 256),
    Q8_K(15, 256, 256),
    IQ2_XXS(16, 256, 256),
    IQ2_XS(17, 256, 256),
    IQ3_XXS(18, 256, 256),
    I8(24, 1, 1),
    I16(25, 2, 1),
    I32(26, 4, 1),
    I64(27, 8, 1),
    F64(28, 8, 1),
    IQ1_S(29, 256, 256),
    CPU_Q4_0(30, 18, 32);

    private final int id;
    private final int typeSize; // size in bytes for one block
    private final int blockSize; // number of elements per block

    GGMLType(int id, int typeSize, int blockSize) {
        this.id = id;
        this.typeSize = typeSize;
        this.blockSize = blockSize;
    }

    public int getId() {
        return id;
    }

    public int getTypeSize() {
        return typeSize;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public static GGMLType fromId(int id) throws GGUFException {
        for (GGMLType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new GGUFException("tensor_info", -1, "Unsupported or unknown GGML type ID: " + id);
    }

    /**
     * Calculate byte size for a given element count using overflow-safe arithmetic.
     */
    public long calculateByteSize(long elementCount) throws GGUFException {
        if (elementCount < 0) {
            throw new GGUFException("tensor_info", -1, "Negative element count: " + elementCount);
        }
        if (elementCount % blockSize != 0) {
            throw new GGUFException("tensor_info", -1, String.format(
                "Element count %d is not a multiple of GGML type %s block size %d",
                elementCount, name(), blockSize));
        }
        long blocks = elementCount / blockSize;
        try {
            return Math.multiplyExact(blocks, (long) typeSize);
        } catch (ArithmeticException e) {
            throw new GGUFException("tensor_info", -1, "Byte size calculation integer overflow for elements: " + elementCount);
        }
    }
}
