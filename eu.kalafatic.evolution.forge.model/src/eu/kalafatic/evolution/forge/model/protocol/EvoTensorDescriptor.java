package eu.kalafatic.evolution.forge.model.protocol;

import java.util.Arrays;

/**
 * Descriptor for a single model parameter tensor entry in the EVO Native Model Protocol manifest.
 */
public class EvoTensorDescriptor {

    private final int id;
    private final String canonicalName;
    private final EvoDtype dtype;
    private final int rank;
    private final long[] shape;
    private final int layout; // 0 = ROW_MAJOR
    private final long offset;
    private final long compressedSize;
    private final long uncompressedSize;
    private final int compressionScheme; // 0 = NONE, 1 = GZIP
    private final String checksum;

    public EvoTensorDescriptor(int id, String canonicalName, EvoDtype dtype, long[] shape,
                               int layout, long offset, long compressedSize,
                               long uncompressedSize, int compressionScheme, String checksum) {
        if (canonicalName == null || canonicalName.trim().isEmpty()) {
            throw new IllegalArgumentException("Canonical name cannot be null or empty");
        }
        if (dtype == null) {
            throw new IllegalArgumentException("EvoDtype cannot be null");
        }
        if (shape == null || shape.length == 0) {
            throw new IllegalArgumentException("Shape cannot be null or empty");
        }
        this.id = id;
        this.canonicalName = canonicalName.trim();
        this.dtype = dtype;
        this.rank = shape.length;
        this.shape = shape.clone();
        this.layout = layout;
        this.offset = offset;
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;
        this.compressionScheme = compressionScheme;
        this.checksum = checksum != null ? checksum : "";
    }

    public int getId() { return id; }
    public String getCanonicalName() { return canonicalName; }
    public EvoDtype getDtype() { return dtype; }
    public int getRank() { return rank; }
    public long[] getShape() { return shape.clone(); }
    public int getLayout() { return layout; }
    public long getOffset() { return offset; }
    public long getCompressedSize() { return compressedSize; }
    public long getUncompressedSize() { return uncompressedSize; }
    public int getCompressionScheme() { return compressionScheme; }
    public String getChecksum() { return checksum; }

    public long getElementCount() {
        long count = 1;
        for (long dim : shape) {
            count *= dim;
        }
        return count;
    }

    @Override
    public String toString() {
        return "EvoTensorDescriptor{" +
                "id=" + id +
                ", name='" + canonicalName + '\'' +
                ", dtype=" + dtype +
                ", shape=" + Arrays.toString(shape) +
                ", offset=" + offset +
                ", compressedSize=" + compressedSize +
                ", uncompressedSize=" + uncompressedSize +
                ", checksum='" + checksum + '\'' +
                '}';
    }
}
