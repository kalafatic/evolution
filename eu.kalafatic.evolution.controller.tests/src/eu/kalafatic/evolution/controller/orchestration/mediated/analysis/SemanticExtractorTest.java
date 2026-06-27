package eu.kalafatic.evolution.controller.mediation.analysis;
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
import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;

public class SemanticExtractorTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExtract() throws IOException {
        File mainJava = folder.newFile("Main.java");
        Files.write(mainJava.toPath(), "public class Main { public static void main(String[] args) {} }\n".getBytes());

        File serviceJava = folder.newFile("MyService.java");
        Files.write(serviceJava.toPath(), "import org.springframework.stereotype.Service; @Service public class MyService {}\n".getBytes());

        TargetScanner scanner = new TargetScanner();
        TargetDescriptor target = scanner.scan(folder.getRoot());

        System.out.println("Files scanned: " + target.getFiles().size());
        for (FileDescriptor fd : target.getFiles()) {
            System.out.println(" - " + fd.getPath());
        }

        SemanticExtractor extractor = new SemanticExtractor();
        extractor.extract(target, folder.getRoot());

        for (FileDescriptor fd : target.getFiles()) {
            System.out.println("File: " + fd.getPath() + " Tags: " + fd.getTags());
        }

        // Executory and Annotated are the new abstract tags
        assertTrue("Should have Executory tag", target.getFiles().stream().anyMatch(f -> f.getTags().contains("Executory")));
        assertTrue("Should have Annotated tag", target.getFiles().stream().anyMatch(f -> f.getTags().contains("Annotated")));
    }
}
