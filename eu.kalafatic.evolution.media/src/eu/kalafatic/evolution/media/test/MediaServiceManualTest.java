package eu.kalafatic.evolution.media.test;

import eu.kalafatic.evolution.media.MediaService;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import java.io.File;
import java.io.IOException;

public class MediaServiceManualTest {
    public static void main(String[] args) throws IOException {
        MediaService mediaService = new MediaService();

        // Test Architecture HTML
        DesignModel designModel = new DesignModel();
        designModel.setName("Test System");

        ComponentRecord c1 = new ComponentRecord();
        c1.setId("c1");
        c1.setName("Core");
        designModel.getComponents().add(c1);

        ComponentRecord c2 = new ComponentRecord();
        c2.setId("c2");
        c2.setName("UI \"Widget\"");
        designModel.getComponents().add(c2);

        RelationshipRecord r1 = new RelationshipRecord();
        r1.setFrom("c2");
        r1.setTo("c1");
        r1.setType("depends");
        designModel.getRelationships().add(r1);

        File archFile = new File("test-architecture.html");
        mediaService.generateArchitectureHtml(designModel, archFile);
        System.out.println("Generated architecture HTML: " + archFile.getAbsolutePath());

        // Test Reality HTML & Presentation
        TargetRealityModel realityModel = new TargetRealityModel();
        realityModel.setDomain("Testing 'Media'");
        realityModel.setPurpose("Verify media module");
        realityModel.setArchitectureSummary("A simple model for testing.");

        File realityFile = new File("test-reality.html");
        mediaService.generateRealityHtml(realityModel, realityFile);
        System.out.println("Generated reality HTML: " + realityFile.getAbsolutePath());

        File presFile = new File("test-presentation.html");
        mediaService.generatePresentation(realityModel, presFile);
        System.out.println("Generated presentation HTML: " + presFile.getAbsolutePath());
    }
}
