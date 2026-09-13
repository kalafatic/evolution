package eu.kalafatic.evolution.controller.resource;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Descriptor representing a Git repository configured or discovered in EVO.
 */
public class EvoRepository {
    private final String id;
    private final String name;
    private final Path localPath;
    private final String remoteUrl;
    private final String branch;
    private final boolean exists;
    private final boolean valid;

    public EvoRepository(String id, String name, Path localPath, String remoteUrl, String branch) {
        this.id = id != null ? id : "UNKNOWN";
        this.name = name != null ? name : this.id;
        this.localPath = localPath;
        this.remoteUrl = remoteUrl != null ? remoteUrl : "";
        this.branch = branch != null ? branch : "main";

        this.exists = localPath != null && localPath.toFile().exists();
        this.valid = this.exists && (localPath.resolve(".git").toFile().exists() || localPath.resolve("pom.xml").toFile().exists());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getBranch() {
        return branch;
    }

    public boolean isExists() {
        return exists;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvoRepository that = (EvoRepository) o;
        return Objects.equals(id, that.id) && Objects.equals(localPath, that.localPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, localPath);
    }

    @Override
    public String toString() {
        return name + " (" + (localPath != null ? localPath.toString() : "null") + ")";
    }
}
