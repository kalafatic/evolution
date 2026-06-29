package eu.kalafatic.evolution.supervisor.bootstrap;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class SelfImprovementAnalyzerTest {

    @Test
    public void testAnalyze() {
        SelfImprovementAnalyzer analyzer = new SelfImprovementAnalyzer();
        AnalysisRequest request = new AnalysisRequest();

        request.addInput("build.log", "some text\n[ERROR] Compilation failed in MyClass.java\nsome more text");
        request.addInput("Source.java", "public class Source {\n  // TODO: refactor this\n}");

        AnalysisResult result = analyzer.analyze(request);

        assertNotNull(result);
        assertEquals(2, result.getCandidates().size());

        boolean foundBuildFailure = false;
        boolean foundTechDebt = false;

        for (ImprovementCandidate candidate : result.getCandidates()) {
            if (candidate.getCategory().equals("Build failures")) {
                foundBuildFailure = true;
                assertEquals(0.9, candidate.getPriority(), 0.01);
            } else if (candidate.getCategory().equals("Technical debt")) {
                foundTechDebt = true;
                assertEquals(0.4, candidate.getPriority(), 0.01);
            }
        }

        assertTrue(foundBuildFailure);
        assertTrue(foundTechDebt);
        assertTrue(result.getSummary().contains("Discovered 2 candidates"));
    }
}
