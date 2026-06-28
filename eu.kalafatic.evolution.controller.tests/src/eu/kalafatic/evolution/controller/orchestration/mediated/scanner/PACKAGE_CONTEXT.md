# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller.tests/src/eu/kalafatic/evolution/controller/orchestration/mediated/scanner/

## Domain: general

## Components
* `TargetScannerTest.java`: package eu.kalafatic.evolution.controller.mediation.scanner; import static org.junit.Assert.*; import java.io.File; import java.io.IOException; import java.nio.file.Files; import org.junit.Rule; import org.junit.Test; import org.junit.rules.TemporaryFolder; import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor; public class TargetScannerTest { @Rule public TemporaryFolder folder = new TemporaryFolder(); @Test public void testScan() throws IOException { folder.newFolder("src"); folder.newFile("src/Main.java"); folder.newFile("pom.xml"); folder.newFolder(".git"); folder.newFile(".git/config"); TargetScanner scanner = new TargetScanner();
