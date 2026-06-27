package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import static org.junit.Assert.assertTrue;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.junit.Test;

public class RegexRegressionTest {

    @Test
    public void testApprovedPattern() {
        String regex = "\\[(APPROVED|REJECTED|KEPT):([^]]+)\\]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher("[APPROVED:variant123]");
        assertTrue(matcher.find());
        org.junit.Assert.assertEquals("APPROVED", matcher.group(1));
        org.junit.Assert.assertEquals("variant123", matcher.group(2));
    }

    @Test
    public void testFileTagPattern() {
        String regex = "\\[FILE:([^]]+)\\]";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher("Check [FILE:src/Main.java]");
        assertTrue(matcher.find());
        org.junit.Assert.assertEquals("src/Main.java", matcher.group(1));
    }
}
