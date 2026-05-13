package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.FileDescriptor;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.orchestration.mediated.scanner.TargetScanner;

public class SemanticExtractorTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExtract() throws IOException {
        File mainJava = folder.newFile("Main.java");
        Files.writeString(mainJava.toPath(), "public class Main { public static void main(String[] args) {} }");

        File serviceJava = folder.newFile("MyService.java");
        Files.writeString(serviceJava.toPath(), "import org.springframework.stereotype.Service; @Service public class MyService {}");

        TargetScanner scanner = new TargetScanner();
        TargetDescriptor target = scanner.scan(folder.getRoot());

        SemanticExtractor extractor = new SemanticExtractor();
        extractor.extract(target, folder.getRoot());

        assertTrue(target.getFiles().stream().anyMatch(f -> f.getTags().contains("Entry Point")));
        assertTrue(target.getFiles().stream().anyMatch(f -> f.getTags().contains("Spring Component")));
        assertNotNull(target.getArchitectureInference());
        assertTrue(target.getArchitectureInference().contains("Java"));
    }
}
