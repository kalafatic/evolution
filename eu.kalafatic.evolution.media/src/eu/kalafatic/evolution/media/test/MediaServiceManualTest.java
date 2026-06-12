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
        String projectName = "TestProject";

        // 1. Test Architecture HTML (Discovery simulation)
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
        mediaService.generateArchitectureHtml(projectName, designModel, archFile);
        System.out.println("Generated architecture HTML and CACHED: " + archFile.getAbsolutePath());

        // 2. Test Reality HTML & Presentation
        TargetRealityModel realityModel = new TargetRealityModel();
        realityModel.setDomain("Testing 'Media'");
        realityModel.setPurpose("Verify media module");
        realityModel.setArchitectureSummary("A simple model for testing.");

        File realityFile = new File("test-reality.html");
        mediaService.generateRealityHtml(projectName, realityModel, realityFile);
        System.out.println("Generated reality HTML and CACHED: " + realityFile.getAbsolutePath());

        File presFile = new File("test-presentation.html");
        mediaService.generatePresentation(projectName, realityModel, presFile);
        System.out.println("Generated presentation HTML: " + presFile.getAbsolutePath());

        // 3. Test CACHE REUSE
        String commitHash = getCurrentCommitHash();
        DesignModel cachedModel = mediaService.loadArtifact(projectName, "architecture", commitHash, DesignModel.class);
        if (cachedModel != null) {
            System.out.println("SUCCESS: Loaded cached architecture model for " + commitHash);
            System.out.println("Cached model name: " + cachedModel.getName());
        } else {
            System.err.println("FAILURE: Could not load cached architecture model for " + commitHash);
        }

        TargetRealityModel cachedReality = mediaService.loadArtifact(projectName, "reality", commitHash, TargetRealityModel.class);
        if (cachedReality != null) {
            System.out.println("SUCCESS: Loaded cached reality model for " + commitHash);
            System.out.println("Cached reality domain: " + cachedReality.getDomain());
        } else {
            System.err.println("FAILURE: Could not load cached reality model for " + commitHash);
        }
    }

    private static String getCurrentCommitHash() {
        try {
            Process process = Runtime.getRuntime().exec("git rev-parse --short HEAD");
            process.waitFor();
            return new String(process.getInputStream().readAllBytes()).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
