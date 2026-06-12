package eu.kalafatic.evolution.media;

import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.media.generator.*;
import eu.kalafatic.evolution.media.model.*;
import eu.kalafatic.evolution.media.render.*;
import eu.kalafatic.evolution.media.video.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;

public class MediaService {

    private final HtmlRenderer htmlRenderer = new HtmlRenderer();
    private final SvgRenderer svgRenderer = new SvgRenderer();
    private final PresentationGenerator presentationGenerator = new PresentationGenerator();
    private final StoryboardGenerator storyboardGenerator = new StoryboardGenerator();
    private final VideoGenerator videoGenerator = new NoOpVideoGenerator();

    public void generateArchitectureHtml(DesignModel model, File outputFile) throws IOException {
        Diagram diagram = convertDesignToDiagram(model);
        String html = htmlRenderer.render(diagram);
        Files.writeString(outputFile.toPath(), html, StandardCharsets.UTF_8);
    }

    public void generateRealityHtml(TargetRealityModel model, File outputFile) throws IOException {
        Diagram diagram = convertRealityToDiagram(model);
        String html = htmlRenderer.render(diagram);
        Files.writeString(outputFile.toPath(), html, StandardCharsets.UTF_8);
    }

    public void generatePresentation(TargetRealityModel model, File outputFile) throws IOException {
        String html = presentationGenerator.generate(model);
        Files.writeString(outputFile.toPath(), html, StandardCharsets.UTF_8);
    }

    public void generateSvg(DesignModel model, File outputFile) throws IOException {
        Diagram diagram = convertDesignToDiagram(model);
        String svg = svgRenderer.render(diagram);
        Files.writeString(outputFile.toPath(), svg, StandardCharsets.UTF_8);
    }

    public Storyboard generateStoryboard(TargetRealityModel model) {
        return storyboardGenerator.generate(model);
    }

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
        diagram.setTitle("Reality: " + model.getDomain());

        Section infoSection = new Section();
        infoSection.setTitle("Summary");
        infoSection.addBlock(new TextBlock(model.getArchitectureSummary()));
        diagram.addSection(infoSection);

        for (var subsystem : model.getSubsystems()) {
            Node node = new Node(subsystem.getName(), subsystem.getName(), "Subsystem");
            node.getProperties().put("Purpose", subsystem.getPurpose());
            diagram.addNode(node);
        }

        for (var fact : model.getArchitecturalFacts()) {
            String label = fact.getSubject() + " " + fact.getPredicate();
            Node node = new Node(fact.getId(), label, "Fact");
            node.getProperties().put("Description", fact.getDescription());
            node.getProperties().put("Confidence", String.valueOf(fact.getConfidence()));
            diagram.addNode(node);

            // Link facts to subsystems if they are related
            for (var subsystem : model.getSubsystems()) {
                if ((fact.getDescription() != null && fact.getDescription().contains(subsystem.getName())) || fact.getSubject().contains(subsystem.getName())) {
                    diagram.addEdge(new Edge(fact.getId(), subsystem.getName(), "relates to", "Relationship"));
                }
            }
        }

        Section hotspotsSection = new Section();
        hotspotsSection.setTitle("Hotspots");
        TableBlock table = new TableBlock();
        table.getHeaders().addAll(Arrays.asList("Name", "Description", "Significance"));
        for (var hotspot : model.getHotspots()) {
            table.getRows().add(Arrays.asList(hotspot.getName(), hotspot.getDescription(), String.valueOf(hotspot.getSignificance())));
        }
        hotspotsSection.addBlock(table);
        diagram.addSection(hotspotsSection);

        return diagram;
    }
}
