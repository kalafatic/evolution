package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import eu.kalafatic.evolution.controller.orchestration.selfdev.MavenErrorCategory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MavenErrorClassifier;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MavenErrorClassifier.ClassificationResult;

public class MavenErrorClassifierTest {

    @Test
    public void testSurefireFailureClassificationNotTargetPlatform() {
        String output = "[INFO] --- target-platform-configuration:4.0.5:target-platform (default-target-platform) @ eu.kalafatic.evolution.model ---\n" +
                "[INFO] Running eu.kalafatic.evolution.model.orchestration.tests.OrchestrationAllTests\n" +
                "[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0\n" +
                "[INFO] ------------------------------------------------------------------------\n" +
                "[INFO] BUILD FAILURE\n" +
                "[INFO] ------------------------------------------------------------------------\n" +
                "[ERROR] Failed to execute goal org.eclipse.tycho:tycho-surefire-plugin:4.0.5:test (default-test) on project eu.kalafatic.evolution.model.tests: There are test failures.\n" +
                "[ERROR] Please refer to C:\\workspace\\eu.kalafatic.evolution.model.tests\\target\\surefire-reports for the individual test results.\n";

        ClassificationResult res = MavenErrorClassifier.classify(output);

        assertEquals(MavenErrorCategory.TEST_FAILURE, res.getCategory());
        assertEquals("eu.kalafatic.evolution.model.tests", res.getFailingModule());
        assertEquals("org.eclipse.tycho:tycho-surefire-plugin:4.0.5", res.getFailingPlugin());
        assertEquals("test", res.getFailingGoal());
        assertEquals("org.eclipse.tycho:tycho-surefire-plugin:4.0.5:test", res.getFailedPhase());
        assertEquals("There are test failures.", res.getRootCause());
        assertEquals("C:\\workspace\\eu.kalafatic.evolution.model.tests\\target\\surefire-reports", res.getReportDirectory());
    }

    @Test
    public void testSurefireReportDirectoryParsing() throws Exception {
        File tempDir = Files.createTempDirectory("surefire_report_test").toFile();
        File reportsDir = new File(tempDir, "target/surefire-reports");
        reportsDir.mkdirs();

        File xmlReport = new File(reportsDir, "TEST-eu.kalafatic.evolution.model.tests.ModelTest.xml");
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<testsuite name=\"eu.kalafatic.evolution.model.tests.ModelTest\" tests=\"1\" failures=\"1\" errors=\"0\">\n" +
                "  <testcase classname=\"eu.kalafatic.evolution.model.tests.ModelTest\" name=\"testModelFixture\">\n" +
                "    <failure message=\"AssertionFailedError: expected failure\">junit.framework.AssertionFailedError</failure>\n" +
                "  </testcase>\n" +
                "</testsuite>\n";
        Files.writeString(xmlReport.toPath(), xmlContent);

        try {
            String output = "[ERROR] Failed to execute goal org.eclipse.tycho:tycho-surefire-plugin:4.0.5:test (default-test) on project eu.kalafatic.evolution.model.tests: There are test failures.\n";
            ClassificationResult res = MavenErrorClassifier.classify(output, tempDir);

            assertEquals(MavenErrorCategory.TEST_FAILURE, res.getCategory());
            assertNotNull(res.getTestSummary());
            assertTrue(res.getTestSummary().contains("ModelTest#testModelFixture: AssertionFailedError: expected failure"));
        } finally {
            xmlReport.delete();
            reportsDir.delete();
            tempDir.delete();
        }
    }
}
