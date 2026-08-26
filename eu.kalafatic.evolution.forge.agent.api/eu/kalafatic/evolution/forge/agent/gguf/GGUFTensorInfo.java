package eu.kalafatic.evolution.forge.agent.gguf;

import java.util.Arrays;

public class GGUFTensorInfo {
    private final String name;
    private final long[] dimensions;
    private final GGMLType ggmlType;
    private final long ggufTensorOffset; // offset relative to data section start
    private final long absoluteFileOffset; // absolute offset in file where tensor data begins
    private final long byteSize;
    private final long elementCount;
    private final long descriptorOffset; // offset in file where tensor header descriptor starts
    private final long descriptorLength; // length of tensor header descriptor

    public GGUFTensorInfo(String name, long[] dimensions, GGMLType ggmlType,
                          long ggufTensorOffset, long absoluteFileOffset,
                          long byteSize, long elementCount,
                          long descriptorOffset, long descriptorLength) {
        this.name = name;
        this.dimensions = dimensions;
        this.ggmlType = ggmlType;
        this.ggufTensorOffset = ggufTensorOffset;
        this.absoluteFileOffset = absoluteFileOffset;
        this.byteSize = byteSize;
        this.elementCount = elementCount;
        this.descriptorOffset = descriptorOffset;
        this.descriptorLength = descriptorLength;
    }

    public String getName() { return name; }
    public long[] getDimensions() { return dimensions; }
    public GGMLType getGgmlType() { return ggmlType; }
    public long getGgufTensorOffset() { return ggufTensorOffset; }
    public long getAbsoluteFileOffset() { return absoluteFileOffset; }
    public long getByteSize() { return byteSize; }
    public long getElementCount() { return elementCount; }
    public long getDescriptorOffset() { return descriptorOffset; }
    public long getDescriptorLength() { return descriptorLength; }

    @Override
    public String toString() {
        return String.format("GGUFTensorInfo[name=%s, dims=%s, type=%s, offset=0x%X, fileOffset=0x%X, size=%d bytes]",
                name, Arrays.toString(dimensions), ggmlType, ggufTensorOffset, absoluteFileOffset, byteSize);
    }
}
