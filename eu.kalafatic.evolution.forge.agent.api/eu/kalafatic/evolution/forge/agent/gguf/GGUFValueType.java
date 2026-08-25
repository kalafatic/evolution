package eu.kalafatic.evolution.forge.agent.gguf;

public enum GGUFValueType {
    UINT8(0, 1),
    INT8(1, 1),
    UINT16(2, 2),
    INT16(3, 2),
    UINT32(4, 4),
    INT32(5, 4),
    FLOAT32(6, 4),
    BOOL(7, 1),
    STRING(8, -1),
    ARRAY(9, -1),
    UINT64(10, 8),
    INT64(11, 8),
    FLOAT64(12, 8);

    private final int id;
    private final int fixedSize;

    GGUFValueType(int id, int fixedSize) {
        this.id = id;
        this.fixedSize = fixedSize;
    }

    public int getId() {
        return id;
    }

    public int getFixedSize() {
        return fixedSize;
    }

    public static GGUFValueType fromId(int id) throws GGUFException {
        for (GGUFValueType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new GGUFException("metadata_value_type", -1, "Unknown GGUF metadata value type ID: " + id);
    }
}
