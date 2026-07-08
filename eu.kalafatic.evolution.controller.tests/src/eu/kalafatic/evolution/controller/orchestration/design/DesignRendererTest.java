package eu.kalafatic.evolution.controller.orchestration.design;

import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class DesignRendererTest {

    @Test
    public void testRenderWithTargetInfo() {
        DesignRenderer renderer = new DesignRenderer();
        DesignModel model = new DesignModel();
        model.setName("Test Project");

        String targetPath = "/test/path";
        List<String> history = Arrays.asList("/test/path", "/old/path");

        String html = renderer.render(model, "COMPONENTS", targetPath, history);

        assertTrue("HTML should contain model name", html.contains("Test Project"));
        assertTrue("HTML should contain target path", html.contains("window.TARGET_PATH = \"/test/path\""));
        assertTrue("HTML should contain history", html.contains("\"/old/path\""));
        assertTrue("HTML should contain target-select element", html.contains("id=\"target-select\""));
    }

    @Test
    public void testRenderGenomeMode() {
        DesignRenderer renderer = new DesignRenderer();
        DesignModel model = new DesignModel();
        String genomeJson = "{\"identity\": {\"name\": \"Evo\", \"version\": \"1.0\"}}";

        String html = renderer.render(model, "GENOME", null, null, new java.util.ArrayList<>(), new java.util.ArrayList<>(), null, "", genomeJson);

        assertTrue("HTML should contain Genome title", html.contains("Genome Architecture Map"));
        assertTrue("HTML should contain injected genome data", html.contains("window.INITIAL_GENOME = {\"identity\": {\"name\": \"Evo\", \"version\": \"1.0\"}}"));
        assertTrue("HTML should link genome.js", html.contains("src=\"genome.js\""));
    }
}
