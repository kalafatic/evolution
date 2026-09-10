package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.services.ModelEvaluationService;
import eu.kalafatic.evolution.controller.services.ModelEvaluationService.EvaluationReport;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.CompositionStrategy;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.ModelObjective;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.SourceProfile;
import eu.kalafatic.evolution.forge.controller.service.impl.ForgeDatasetComposer;
import eu.kalafatic.evolution.forge.controller.service.impl.ForgeSourceAnalyzer;
import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.SourceType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.impl.collector.EvoCodebaseDataCollector;
import eu.kalafatic.evolution.forge.data.impl.collector.EvoCodebaseDataCollector.CodebaseMode;
import eu.kalafatic.evolution.forge.data.impl.source.EvoCodebaseDatasetSource;

public class EvoCodebaseDataSourceTest {

    private Path tempRepoDir;
    private Path javaSourceFile;
    private Path javaTestFile;
    private Path manifestFile;
    private Path pluginXmlFile;
    private Path pomXmlFile;
    private Path productFile;
    private Path readmeFile;
    private Path pdfFile;
    private Path excludedTargetFile;

    @Before
    public void setUp() throws Exception {
        tempRepoDir = Files.createTempDirectory("evo-codebase-test-repo-");

        // 1. Java Source File
        javaSourceFile = tempRepoDir.resolve("plugins/eu.kalafatic.core/src/eu/kalafatic/core/IterationManager.java");
        Files.createDirectories(javaSourceFile.getParent());
        Files.writeString(javaSourceFile,
            "package eu.kalafatic.core;\n" +
            "import java.util.List;\n" +
            "/** Iteration authority */\n" +
            "public class IterationManager extends BaseManager implements TransitionAuthority {\n" +
            "    public void executeIteration() {}\n" +
            "}");

        // 2. Java Test File
        javaTestFile = tempRepoDir.resolve("plugins/eu.kalafatic.core.tests/src/eu/kalafatic/core/tests/IterationManagerTest.java");
        Files.createDirectories(javaTestFile.getParent());
        Files.writeString(javaTestFile,
            "package eu.kalafatic.core.tests;\n" +
            "import org.junit.Test;\n" +
            "public class IterationManagerTest {\n" +
            "    @Test public void testIteration() {}\n" +
            "}");

        // 3. OSGi Manifest
        manifestFile = tempRepoDir.resolve("plugins/eu.kalafatic.core/META-INF/MANIFEST.MF");
        Files.createDirectories(manifestFile.getParent());
        Files.writeString(manifestFile,
            "Manifest-Version: 1.0\n" +
            "Bundle-SymbolicName: eu.kalafatic.core;singleton:=true\n" +
            "Bundle-Version: 2.6.5.qualifier\n" +
            "Require-Bundle: org.eclipse.core.runtime, eu.kalafatic.utils\n" +
            "Export-Package: eu.kalafatic.core\n");

        // 4. Plugin XML
        pluginXmlFile = tempRepoDir.resolve("plugins/eu.kalafatic.core/plugin.xml");
        Files.writeString(pluginXmlFile,
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<plugin>\n" +
            "  <extension point=\"org.eclipse.ui.views\">\n" +
            "    <view class=\"eu.kalafatic.core.IterationView\" id=\"eu.kalafatic.core.view\"/>\n" +
            "  </extension>\n" +
            "</plugin>");

        // 5. Tycho Maven POM
        pomXmlFile = tempRepoDir.resolve("pom.xml");
        Files.writeString(pomXmlFile,
            "<project>\n" +
            "  <groupId>eu.kalafatic</groupId>\n" +
            "  <artifactId>eu.kalafatic.core</artifactId>\n" +
            "  <version>2.6.5-SNAPSHOT</version>\n" +
            "  <build><plugins><plugin><artifactId>tycho-maven-plugin</artifactId></plugin></plugins></build>\n" +
            "</project>");

        // 6. Eclipse Product File
        productFile = tempRepoDir.resolve("evolution.product");
        Files.writeString(productFile,
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<product name=\"EVO Platform\" id=\"eu.kalafatic.evolution.product\" application=\"eu.kalafatic.evolution.application\">\n" +
            "  <configIni use=\"default\"/>\n" +
            "</product>");

        // 7. Markdown Documentation
        readmeFile = tempRepoDir.resolve("docs/architecture.md");
        Files.createDirectories(readmeFile.getParent());
        Files.writeString(readmeFile,
            "# EVO Architecture Overview\n\n" +
            "IterationManager acts as transition authority in EVO platform.");

        // 8. PDF File
        pdfFile = tempRepoDir.resolve("docs/manual.pdf");
        String dummyPdfContent =
            "%PDF-1.4\n" +
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>\nendobj\n" +
            "4 0 obj\n<< /Length 60 >>\nstream\n" +
            "BT\n" +
            "(EVO Platform Operator Manual) Tj\n" +
            "ET\n" +
            "endstream\nendobj\n" +
            "trailer\n<< /Root 1 0 R >>\n%%EOF";
        Files.writeString(pdfFile, dummyPdfContent);

        // 9. Excluded target directory file
        excludedTargetFile = tempRepoDir.resolve("target/classes/eu/kalafatic/core/IterationManager.class");
        Files.createDirectories(excludedTargetFile.getParent());
        Files.writeString(excludedTargetFile, "compiled-binary-data");
    }

    @Test
    public void testEvoCodebaseDataCollectorInference() {
        EvoCodebaseDataCollector collector = new EvoCodebaseDataCollector();
        assertEquals(CollectorType.EVO_CODEBASE, collector.getType());

        CollectionContext context = new CollectionContext();
        context.setProjectDirectory(tempRepoDir.toFile());

        CollectionResult result = collector.collect(context);
        assertEquals(CollectionResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getItems());
        assertFalse(result.getItems().isEmpty());

        boolean foundJava = false;
        boolean foundTest = false;
        boolean foundOsgi = false;
        boolean foundTycho = false;
        boolean foundProduct = false;
        boolean foundDoc = false;
        boolean foundPdf = false;
        boolean foundInstruction = false;

        for (TrainingDataItem item : result.getItems()) {
            assertEquals(SourceType.EVO_CODEBASE, item.getSourceType());
            assertNotNull(item.getMetadata().get("commit"));
            assertNotNull(item.getMetadata().get("branch"));
            assertNotNull(item.getMetadata().get("workingTreeState"));

            String category = (String) item.getMetadata().get("fileType");
            if ("JAVA_SOURCE".equals(category)) foundJava = true;
            if ("JAVA_TEST".equals(category)) foundTest = true;
            if ("OSGI_METADATA".equals(category)) foundOsgi = true;
            if ("TYCHO".equals(category)) foundTycho = true;
            if ("ECLIPSE_RCP".equals(category)) foundProduct = true;
            if ("DOCUMENTATION".equals(category)) foundDoc = true;
            if ("PDF".equals(category)) foundPdf = true;
            if ("INSTRUCTION".equals(category)) foundInstruction = true;

            assertFalse("Target compiled class files must be automatically excluded", item.getSource().startsWith("target/"));
        }

        assertTrue("Java source file classified", foundJava);
        assertTrue("Java test file classified", foundTest);
        assertTrue("OSGi metadata classified", foundOsgi);
        assertTrue("Tycho POM classified", foundTycho);
        assertTrue("Eclipse RCP product classified", foundProduct);
        assertTrue("Markdown documentation classified", foundDoc);
        assertTrue("PDF documentation classified", foundPdf);
        assertTrue("Verified QA instruction pairs generated", foundInstruction);
    }

    @Test
    public void testEvoCodebaseDatasetSourceStreaming() throws Exception {
        DatasetSourceConfig config = new DatasetSourceConfig("EVO_CODEBASE", tempRepoDir.toAbsolutePath().toString());
        config.setMaxSamples(100);

        try (EvoCodebaseDatasetSource source = new EvoCodebaseDatasetSource(config, CodebaseMode.MIXED_AUTO)) {
            source.initialize();
            assertTrue(source.hasNext());

            List<NormalizedSample> collected = new ArrayList<>();
            while (source.hasNext()) {
                NormalizedSample sample = source.next();
                assertNotNull(sample);
                assertNotNull(sample.toFullText());
                assertNotNull(sample.getMetadata().get("commit"));
                collected.add(sample);
            }

            assertFalse(collected.isEmpty());
            assertTrue(source.getStats().getAcceptedRecords() > 0);
            assertTrue(source.getStats().getAcceptedBytes() > 0);
        }
    }

    @Test
    public void testForgeSourceAnalyzerAndComposerForEvoCodebase() {
        ForgeSourceAnalyzer analyzer = new ForgeSourceAnalyzer();
        SourceProfile profile = analyzer.analyzeSource(tempRepoDir.toAbsolutePath().toString());

        assertEquals("EVO_CODEBASE", profile.getSourceType());
        assertTrue(profile.getQualityScore() >= 0.90);
        assertTrue(profile.getEstimatedTokens() > 0);

        ForgeDatasetComposer composer = new ForgeDatasetComposer();
        List<SourceProfile> profiles = List.of(profile);

        CompositionStrategy strategy = composer.computeComposition(profiles, ModelObjective.EVO_DEVELOPER_ASSISTANT, 100_000_000L);
        assertNotNull(strategy);
        assertEquals(100_000_000L, strategy.getTokenBudget());
        assertTrue(strategy.getCodePercent() > 0.30);
        assertTrue(strategy.getKnowledgePercent() > 0.20);
        assertTrue(strategy.getInstructionPercent() > 0.15);
    }

    @Test
    public void testModelEvaluationAndRegressionComparison() {
        ModelEvaluationService evalService = new ModelEvaluationService();

        EvaluationReport v1 = new EvaluationReport();
        v1.setModelName("evo-v1");
        v1.setRatingAnalyze(8);
        v1.setRatingChat(8);
        v1.setRatingProgramming(8);
        v1.setRatingEvoArchitecture(7);
        v1.setRatingEclipseOsgi(7);
        v1.setRatingJavaCompetence(8);
        v1.setOverallRating(8);

        EvaluationReport v2Pass = new EvaluationReport();
        v2Pass.setModelName("evo-v2-improved");
        v2Pass.setRatingAnalyze(9);
        v2Pass.setRatingChat(8);
        v2Pass.setRatingProgramming(9);
        v2Pass.setRatingEvoArchitecture(9);
        v2Pass.setRatingEclipseOsgi(8);
        v2Pass.setRatingJavaCompetence(9);
        v2Pass.setOverallRating(9);

        String passReport = evalService.compareModels(v1, v2Pass);
        assertTrue(passReport.contains("VERDICT: ACCEPTED"));

        EvaluationReport v2Fail = new EvaluationReport();
        v2Fail.setModelName("evo-v2-regressed");
        v2Fail.setRatingAnalyze(8);
        v2Fail.setRatingChat(8);
        v2Fail.setRatingProgramming(8);
        v2Fail.setRatingEvoArchitecture(4); // Regressed
        v2Fail.setRatingEclipseOsgi(3); // Regressed
        v2Fail.setRatingJavaCompetence(8);
        v2Fail.setOverallRating(6);

        String failReport = evalService.compareModels(v1, v2Fail);
        assertTrue(failReport.contains("VERDICT: REGRESSION DETECTED"));
    }
}
