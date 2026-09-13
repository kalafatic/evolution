package eu.kalafatic.evolution.controller.resource;

import java.io.File;
import java.util.Objects;

/**
 * Encapsulates product definition properties.
 */
public class ProductDefinition {
    private final String productId;
    private final String launcherName;
    private final String rootFolder;
    private final String repositoryModule;
    private final File productFile;

    public ProductDefinition(String productId, String launcherName, String rootFolder, String repositoryModule, File productFile) {
        this.productId = productId != null && !productId.trim().isEmpty() ? productId.trim() : "evolution";
        this.launcherName = launcherName != null && !launcherName.trim().isEmpty() ? launcherName.trim() : "evo";
        this.rootFolder = rootFolder != null && !rootFolder.trim().isEmpty() ? rootFolder.trim() : "evolution";
        this.repositoryModule = repositoryModule != null && !repositoryModule.trim().isEmpty() ? repositoryModule.trim() : "eu.kalafatic.evolution.repository";
        this.productFile = productFile;
    }

    public ProductDefinition(String productId, String launcherName, String rootFolder, String repositoryModule) {
        this(productId, launcherName, rootFolder, repositoryModule, null);
    }

    public String getProductId() { return productId; }
    public String getLauncherName() { return launcherName; }
    public String getRootFolder() { return rootFolder; }
    public String getRepositoryModule() { return repositoryModule; }
    public File getProductFile() { return productFile; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDefinition that = (ProductDefinition) o;
        return Objects.equals(productId, that.productId) && Objects.equals(launcherName, that.launcherName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, launcherName);
    }

    @Override
    public String toString() {
        return "ProductDefinition{" +
                "productId='" + productId + '\'' +
                ", launcherName='" + launcherName + '\'' +
                ", rootFolder='" + rootFolder + '\'' +
                ", repositoryModule='" + repositoryModule + '\'' +
                '}';
    }
}
