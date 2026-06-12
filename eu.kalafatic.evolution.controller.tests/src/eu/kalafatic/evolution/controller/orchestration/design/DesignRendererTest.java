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
}
