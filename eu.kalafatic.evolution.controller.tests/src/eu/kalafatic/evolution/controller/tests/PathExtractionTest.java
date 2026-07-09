package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PathExtractionTest {

    @Test
    public void testPathExtractionRegex() {
        String regex = "(?i)^(.+:\\s*)?(Write|Create|Generate|Update|Modify|Delete)(\\s+file)?\\s+";

        assertEquals("Main.java", "Operational: Create Main.java".replaceFirst(regex, "").trim());
        assertEquals("Main.java", "Create Main.java".replaceFirst(regex, "").trim());
        assertEquals("src/App.java", "CODING: Write src/App.java".replaceFirst(regex, "").trim());
        assertEquals("README.md", "Update file README.md".replaceFirst(regex, "").trim());
        assertEquals("test.txt", "Generate test.txt".replaceFirst(regex, "").trim());
        assertEquals("old.txt", "Delete file 'old.txt'".replaceFirst(regex, "").trim().replaceAll("^['\"]|['\"]$", ""));
        assertEquals("file.java", "Modify file.java".replaceFirst(regex, "").trim());

        // Mixed cases
        assertEquals("Main.java", "oPeRaTiOnAl: cReAtE Main.java".replaceFirst(regex, "").trim());
    }

    @Test
    public void testFileUriDecoding() throws Exception {
        String[] cases = {
            "file:/C:/Users/abc/mediated_export.zip",
            "file:///C:/Users/abc/mediated_export.zip",
            "file://C:/Users/abc/mediated_export.zip",
            "file:/C:/My%20Folder/mediated_export.zip"
        };
        String[] expected = {
            "C:/Users/abc/mediated_export.zip",
            "C:/Users/abc/mediated_export.zip",
            "C:/Users/abc/mediated_export.zip",
            "C:/My Folder/mediated_export.zip"
        };

        for (int i = 0; i < cases.length; i++) {
            String path = cases[i];
            if (path.startsWith("file://") && !path.startsWith("file:///")) {
                path = path.replaceFirst("file://", "file:///");
            }
            if (path.startsWith("file:")) {
                try {
                    path = new java.net.URI(path).getPath();
                } catch (Exception e) {
                    path = path.replaceFirst("^file:/+", "");
                }
            }
            if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                path = path.substring(1);
            }
            assertEquals(expected[i], path);
        }
    }
}
