package eu.kalafatic.evolution.forge.agent.gguf;

public class GGUFHeader {
    private final String magic;
    private final int version;
    private final long tensorCount;
    private final long metadataCount;
    private final long headerOffset;
    private final long headerLength;

    public GGUFHeader(String magic, int version, long tensorCount, long metadataCount, long headerOffset, long headerLength) {
        this.magic = magic;
        this.version = version;
        this.tensorCount = tensorCount;
        this.metadataCount = metadataCount;
        this.headerOffset = headerOffset;
        this.headerLength = headerLength;
    }

    public String getMagic() { return magic; }
    public int getVersion() { return version; }
    public long getTensorCount() { return tensorCount; }
    public long getMetadataCount() { return metadataCount; }
    public long getHeaderOffset() { return headerOffset; }
    public long getHeaderLength() { return headerLength; }

    @Override
    public String toString() {
        return String.format("GGUFHeader[magic=%s, version=%d, tensors=%d, metadata=%d, offset=0x%X, length=%d]",
                magic, version, tensorCount, metadataCount, headerOffset, headerLength);
    }
}
