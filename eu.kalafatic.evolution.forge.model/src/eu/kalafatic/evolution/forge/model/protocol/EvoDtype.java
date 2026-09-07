package eu.kalafatic.evolution.forge.model.protocol;

/**
 * Supported tensor data types in EVO Native Model Protocol.
 */
public enum EvoDtype {
    F32(0, 4),
    F16(1, 2),
    BF16(2, 2),
    INT8(3, 1),
    INT4(4, 1);

    private final int code;
    private final int elementBytes;

    EvoDtype(int code, int elementBytes) {
        this.code = code;
        this.elementBytes = elementBytes;
    }

    public int getCode() {
        return code;
    }

    public int getElementBytes() {
        return elementBytes;
    }

    public static EvoDtype fromCode(int code) {
        for (EvoDtype type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported EvoDtype code: " + code);
    }
}
