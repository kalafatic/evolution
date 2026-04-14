package eu.kalafatic.evolution.controller.tests;

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
}
