package eu.kalafatic.evolution.forge.agent.gguf;

public class GGUFException extends Exception {
    private final String section;
    private final long offset;

    public GGUFException(String message) {
        this("unknown", -1, message);
    }

    public GGUFException(String section, long offset, String message) {
        super(formatMessage(section, offset, message));
        this.section = section;
        this.offset = offset;
    }

    public GGUFException(String section, long offset, String message, Throwable cause) {
        super(formatMessage(section, offset, message), cause);
        this.section = section;
        this.offset = offset;
    }

    private static String formatMessage(String section, long offset, String message) {
        if (offset >= 0) {
            return String.format("GGUF error in section '%s' at offset 0x%X (%d): %s", section, offset, offset, message);
        } else {
            return String.format("GGUF error in section '%s': %s", section, message);
        }
    }

    public String getSection() {
        return section;
    }

    public long getOffset() {
        return offset;
    }
}
