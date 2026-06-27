package eu.kalafatic.evolution.controller.mediation.scanner;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;

public class TargetScannerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testScan() throws IOException {
        folder.newFolder("src");
        folder.newFile("src/Main.java");
        folder.newFile("pom.xml");
        folder.newFolder(".git");
        folder.newFile(".git/config");

        TargetScanner scanner = new TargetScanner();
        TargetDescriptor target = scanner.scan(folder.getRoot());

        assertNotNull(target);
        assertEquals(2, target.getFiles().size()); // Main.java and pom.xml
        // Technology detection is now empty by default as it's an emergent property
        assertTrue(target.getDetectedTechnologies().isEmpty());
        assertFalse(target.getFiles().stream().anyMatch(f -> f.getPath().contains(".git")));
    }
}
