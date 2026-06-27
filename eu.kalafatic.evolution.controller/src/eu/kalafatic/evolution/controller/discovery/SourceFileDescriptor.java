package eu.kalafatic.evolution.controller.discovery;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.File;

public class SourceFileDescriptor {
    private File file;
    private File repositoryRoot;
    private File projectRoot;
    private String relativePath;
    private String extension;

    public SourceFileDescriptor(File file, File repositoryRoot, File projectRoot, String relativePath, String extension) {
        this.file = file;
        this.repositoryRoot = repositoryRoot;
        this.projectRoot = projectRoot;
        this.relativePath = relativePath;
        this.extension = extension;
    }

    public File getFile() { return file; }
    public File getRepositoryRoot() { return repositoryRoot; }
    public File getProjectRoot() { return projectRoot; }
    public String getRelativePath() { return relativePath; }
    public String getExtension() { return extension; }
}
