package eu.kalafatic.evolution.media;

import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.media.generator.*;
import eu.kalafatic.evolution.media.model.*;
import eu.kalafatic.evolution.media.render.*;
import eu.kalafatic.evolution.media.video.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MediaService {

    private final HtmlRenderer htmlRenderer = new HtmlRenderer();
    private final SvgRenderer svgRenderer = new SvgRenderer();
    private final PresentationGenerator presentationGenerator = new PresentationGenerator();
    private final StoryboardGenerator storyboardGenerator = new StoryboardGenerator();
    private final VideoGenerator videoGenerator = new NoOpVideoGenerator();
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final String ARTIFACTS_ROOT = "evo-artifacts";

    // --- Legacy / Direct API ---

    public void generateArchitectureHtml(String projectName, DesignModel model, File outputFile) throws IOException {
        Diagram diagram = convertDesignToDiagram(model);
        String html = htmlRenderer.render(diagram);
        Files.writeString(outputFile.toPath(), html, StandardCharsets.UTF_8);
        saveArtifact(projectName, "architecture", model, html, svgRenderer.render(diagram));
    }

    public void generateRealityHtml(String projectName, TargetRealityModel model, File outputFile) throws IOException {
        Diagram diagram = convertRealityToDiagram(model);
        String html = htmlRenderer.render(diagram);
        Files.writeString(outputFile.toPath(), html, StandardCharsets.UTF_8);
        saveArtifact(projectName, "reality", model, html, null);
    }

    public void generatePresentation(String projectName, TargetRealityModel model, File outputFile) throws IOException {
        String html = presentationGenerator.generate(model);
        Files.writeString(outputFile.toPath(), html, StandardCharsets.UTF_8);
    }

    public void generateSvg(String projectName, DesignModel model, File outputFile) throws IOException {
        Diagram diagram = convertDesignToDiagram(model);
        String svg = svgRenderer.render(diagram);
        Files.writeString(outputFile.toPath(), svg, StandardCharsets.UTF_8);
    }

    // --- New Caching-Aware Public API ---

    public DesignModel getOrCreateArchitectureModel(String projectName, String commitHash, java.util.function.Supplier<DesignModel> discovery) throws IOException {
        DesignModel cached = loadArtifact(projectName, "architecture", commitHash, DesignModel.class);
        if (cached != null) return cached;

        DesignModel discovered = discovery.get();
        saveArtifact(projectName, "architecture", discovered);
        return discovered;
    }

    public TargetRealityModel getOrCreateRealityModel(String projectName, String commitHash, java.util.function.Supplier<TargetRealityModel> discovery) throws IOException {
        TargetRealityModel cached = loadArtifact(projectName, "reality", commitHash, TargetRealityModel.class);
        if (cached != null) return cached;

        TargetRealityModel discovered = discovery.get();
        saveArtifact(projectName, "reality", discovered);
        return discovered;
    }

    public <T> T loadArtifact(String projectName, String artifactType, String commitHash, Class<T> modelClass) throws IOException {
        File artifactDir = findArtifactDir(projectName, artifactType, commitHash);
        if (artifactDir == null) return null;

        File modelFile = new File(artifactDir, "model.json");
        if (!modelFile.exists()) return null;

        return mapper.readValue(modelFile, modelClass);
    }

    public void saveArtifact(String projectName, String artifactType, Object model) throws IOException {
        saveArtifact(projectName, artifactType, model, null, null, null);
    }

    public void saveArtifact(String projectName, String artifactType, Object model, String html, String svg) throws IOException {
        saveArtifact(projectName, artifactType, model, html, svg, null);
    }

    public void saveArtifact(String projectName, String artifactType, Object model, String html, String svg, Map<String, Object> layout) throws IOException {
        String commitHash = getCurrentCommitHash();
        File root = new File(ARTIFACTS_ROOT);
        File projectDir = new File(root, projectName);
        File typeDir = new File(projectDir, artifactType);

        if (!typeDir.exists()) Files.createDirectories(typeDir.toPath());

        int version = 1;
        File artifactDir = null;
        File[] versionDirs = typeDir.listFiles(File::isDirectory);
        if (versionDirs != null) {
            for (File v : versionDirs) {
                File metaFile = new File(v, "meta.json");
                if (metaFile.exists()) {
                    try {
                        Map meta = mapper.readValue(metaFile, Map.class);
                        if (commitHash.equals(meta.get("commitHash"))) {
                            artifactDir = v;
                            break;
                        }
                    } catch (Exception e) {}
                }
                String name = v.getName();
                if (name.startsWith("v")) {
                    try {
                        int vNum = Integer.parseInt(name.substring(1));
                        if (vNum >= version) version = vNum + 1;
                    } catch (NumberFormatException e) {}
                }
            }
        }

        if (artifactDir == null) {
            artifactDir = new File(typeDir, "v" + version);
            Files.createDirectories(artifactDir.toPath());
        }

        System.out.println("Saving artifact to: " + artifactDir.getAbsolutePath());

        // 1. Semantic Model (model.json)
        mapper.writeValue(new File(artifactDir, "model.json"), model);

        // 2. Layout Cache (layout.json)
        Map<String, Object> layoutToSave = (layout != null) ? layout : new HashMap<>();
        mapper.writeValue(new File(artifactDir, "layout.json"), layoutToSave);

        // 3. Rendered Output Cache
        if (html != null) {
            Files.writeString(new File(artifactDir, "artifact.html").toPath(), html, StandardCharsets.UTF_8);
        } else {
            String autoHtml = null;
            if (model instanceof DesignModel dm) autoHtml = htmlRenderer.render(convertDesignToDiagram(dm));
            else if (model instanceof TargetRealityModel rm) autoHtml = htmlRenderer.render(convertRealityToDiagram(rm));
            if (autoHtml != null) {
                Files.writeString(new File(artifactDir, "artifact.html").toPath(), autoHtml, StandardCharsets.UTF_8);
            }
        }

        if (svg != null) {
            Files.writeString(new File(artifactDir, "artifact.svg").toPath(), svg, StandardCharsets.UTF_8);
        } else {
            String autoSvg = null;
            if (model instanceof DesignModel dm) autoSvg = svgRenderer.render(convertDesignToDiagram(dm));
            else if (model instanceof TargetRealityModel rm) autoSvg = svgRenderer.render(convertRealityToDiagram(rm));
            if (autoSvg != null) {
                Files.writeString(new File(artifactDir, "artifact.svg").toPath(), autoSvg, StandardCharsets.UTF_8);
            }
        }

        if (model instanceof TargetRealityModel rm) {
            Files.writeString(new File(artifactDir, "presentation.html").toPath(), presentationGenerator.generate(rm), StandardCharsets.UTF_8);
            mapper.writeValue(new File(artifactDir, "storyboard.json"), storyboardGenerator.generate(rm));
        }

        // 4. Metadata (meta.json)
        Map<String, Object> meta = new HashMap<>();
        meta.put("project", projectName);
        meta.put("type", artifactType);
        meta.put("commitHash", commitHash);
        meta.put("timestamp", System.currentTimeMillis());
        meta.put("version", artifactDir.getName());
        meta.put("model_version", "v1");
        meta.put("generator_version", "v1");
        mapper.writeValue(new File(artifactDir, "meta.json"), meta);

        System.out.println("Artifact saved successfully.");
    }

    private File findArtifactDir(String projectName, String artifactType, String commitHash) throws IOException {
        File root = new File(ARTIFACTS_ROOT);
        File projectDir = new File(root, projectName);
        File typeDir = new File(projectDir, artifactType);
        if (!typeDir.exists()) return null;

        File[] versions = typeDir.listFiles(File::isDirectory);
        if (versions == null) return null;

        for (File v : versions) {
            File metaFile = new File(v, "meta.json");
            if (metaFile.exists()) {
                try {
                    Map meta = mapper.readValue(metaFile, Map.class);
                    if (commitHash.equals(meta.get("commitHash"))) {
                        return v;
                    }
                } catch (Exception e) {}
            }
        }
        return null;
    }

    private String getCurrentCommitHash() {
        try {
            Process process = Runtime.getRuntime().exec("git rev-parse --short HEAD");
            process.waitFor();
            return new String(process.getInputStream().readAllBytes()).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // --- Conversion Logic ---

    private Diagram convertDesignToDiagram(DesignModel model) {
        Diagram diagram = new Diagram();
        diagram.setTitle("Architecture: " + model.getName());
        for (ComponentRecord comp : model.getComponents()) {
            diagram.addNode(new Node(comp.getId(), comp.getName(), "Component"));
        }
        for (RelationshipRecord rel : model.getRelationships()) {
            diagram.addEdge(new Edge(rel.getFrom(), rel.getTo(), rel.getType(), rel.getType()));
        }
        return diagram;
    }

    private Diagram convertRealityToDiagram(TargetRealityModel model) {
        Diagram diagram = new Diagram();
        diagram.setTitle("Reality: " + (model.getDomain() != null ? model.getDomain() : "Unknown"));

        Section infoSection = new Section();
        infoSection.setTitle("Summary");
        infoSection.addBlock(new TextBlock(model.getArchitectureSummary()));
        diagram.addSection(infoSection);

        if (model.getSubsystems() != null) {
            for (var subsystem : model.getSubsystems()) {
                Node node = new Node(subsystem.getName(), subsystem.getName(), "Subsystem");
                node.getProperties().put("Purpose", subsystem.getPurpose());
                diagram.addNode(node);
            }
        }

        if (model.getArchitecturalFacts() != null) {
            for (var fact : model.getArchitecturalFacts()) {
                String label = fact.getSubject() + " " + fact.getPredicate();
                Node node = new Node(fact.getId(), label, "Fact");
                node.getProperties().put("Description", fact.getDescription());
                node.getProperties().put("Confidence", String.valueOf(fact.getConfidence()));
                diagram.addNode(node);

                if (model.getSubsystems() != null) {
                    for (var subsystem : model.getSubsystems()) {
                        if ((fact.getDescription() != null && fact.getDescription().contains(subsystem.getName())) || fact.getSubject().contains(subsystem.getName())) {
                            diagram.addEdge(new Edge(fact.getId(), subsystem.getName(), "relates to", "Relationship"));
                        }
                    }
                }
            }
        }

        if (model.getHotspots() != null) {
            Section hotspotsSection = new Section();
            hotspotsSection.setTitle("Hotspots");
            TableBlock table = new TableBlock();
            table.getHeaders().addAll(Arrays.asList("Name", "Description", "Significance"));
            for (var hotspot : model.getHotspots()) {
                table.getRows().add(Arrays.asList(hotspot.getName(), hotspot.getDescription(), String.valueOf(hotspot.getSignificance())));
            }
            hotspotsSection.addBlock(table);
            diagram.addSection(hotspotsSection);
        }

        return diagram;
    }
}
