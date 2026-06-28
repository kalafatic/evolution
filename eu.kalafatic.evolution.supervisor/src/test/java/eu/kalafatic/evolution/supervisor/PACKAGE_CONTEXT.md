# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.supervisor/src/test/java/eu/kalafatic/evolution/supervisor/

## Domain: general

## Components
* `ResultReaderTest.java`: package eu.kalafatic.evolution.supervisor; import org.junit.Assert; import org.junit.Test; import java.io.File; import java.io.FileWriter; import java.io.IOException; public class ResultReaderTest { @Test public void testReadResult() throws IOException { File tempFile = File.createTempFile("result", ".json"); try (FileWriter writer = new FileWriter(tempFile)) { writer.write("{\"status\": \"OK\", \"score\": 0.85}"); } ResultReader reader = new ResultReader(); Result result = reader.readResult(tempFile); Assert.assertEquals("OK", result.getStatus()); Assert.assertEquals(0.85, result.getScore(), 0.001); tempFile.delete(); } @Test
* `EvoValidatorTest.java`: package eu.kalafatic.evolution.supervisor; import org.junit.Test; import java.io.File; import java.io.IOException; import java.nio.file.Files; import java.util.Arrays; import java.util.Collections; public class EvoValidatorTest { @Test public void testValidMarker() throws IOException { File tempDir = createTempDir("test-variant"); File javaFile = new File(tempDir, "TestFile.java"); Files.write(javaFile.toPath(), Collections.singletonList("// @evo:12:A reason=test")); EvoPlan plan = new EvoPlan(); plan.setIteration(12); plan.setVariant("A"); plan.setFiles(Collections.singletonList("TestFile.java")); new EvoValidator().validate(tempDir, plan); } @Test
