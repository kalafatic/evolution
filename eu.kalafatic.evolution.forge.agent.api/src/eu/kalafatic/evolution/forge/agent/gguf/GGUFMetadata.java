package eu.kalafatic.evolution.forge.agent.gguf;

public class GGUFMetadata {
    private final String key;
    private final GGUFValueType valueType;
    private final Object value;
    private final long offset;
    private final long length;

    public GGUFMetadata(String key, GGUFValueType valueType, Object value, long offset, long length) {
        this.key = key;
        this.valueType = valueType;
        this.value = value;
        this.offset = offset;
        this.length = length;
    }

    public String getKey() { return key; }
    public GGUFValueType getValueType() { return valueType; }
    public Object getValue() { return value; }
    public long getOffset() { return offset; }
    public long getLength() { return length; }

    @Override
    public String toString() {
        return String.format("GGUFMetadata[key=%s, type=%s, value=%s, offset=0x%X, length=%d]",
                key, valueType, value, offset, length);
    }
}
