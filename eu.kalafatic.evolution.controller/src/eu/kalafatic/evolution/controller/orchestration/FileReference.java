package eu.kalafatic.evolution.controller.orchestration;

/**
 * Represents a reference to a generated or modified file.
 */
public class FileReference {
    private final String path;
    private final String displayName;
    private final String eclipseUri;

    public FileReference(String path, String displayName, String eclipseUri) {
        this.path = path;
        this.displayName = displayName;
        this.eclipseUri = eclipseUri;
    }

    public String getPath() {
        return path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEclipseUri() {
        return eclipseUri;
    }
}
