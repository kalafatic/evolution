package eu.kalafatic.evolution.forge.data.api.source;

/**
 * Inspection report describing metadata and readiness of a dataset candidate source.
 */
public class DatasetInspection {

    private DatasetItem item;
    private String adapterName;
    private boolean exists;
    private long estimatedBytes;
    private String format;
    private boolean supported;
    private String details;

    public DatasetInspection() {}

    public DatasetInspection(DatasetItem item, String adapterName, boolean exists, long estimatedBytes, String format, boolean supported, String details) {
        this.item = item;
        this.adapterName = adapterName;
        this.exists = exists;
        this.estimatedBytes = estimatedBytes;
        this.format = format;
        this.supported = supported;
        this.details = details;
    }

    public DatasetItem getItem() { return item; }
    public void setItem(DatasetItem item) { this.item = item; }

    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }

    public boolean isExists() { return exists; }
    public void setExists(boolean exists) { this.exists = exists; }

    public long getEstimatedBytes() { return estimatedBytes; }
    public void setEstimatedBytes(long estimatedBytes) { this.estimatedBytes = estimatedBytes; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public boolean isSupported() { return supported; }
    public void setSupported(boolean supported) { this.supported = supported; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Override
    public String toString() {
        return "DatasetInspection{" +
                "adapterName='" + adapterName + '\'' +
                ", exists=" + exists +
                ", estimatedBytes=" + estimatedBytes +
                ", format='" + format + '\'' +
                ", supported=" + supported +
                ", details='" + details + '\'' +
                '}';
    }
}
