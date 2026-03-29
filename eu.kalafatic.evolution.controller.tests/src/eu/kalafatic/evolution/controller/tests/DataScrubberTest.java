package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.util.DataScrubber;

public class DataScrubberTest {

    @Test
    public void testScrubOpenAiKey() {
        String input = "My key is sk-123456789012345678901234567890123456789012345678 and it's secret.";
        String expected = "My key is [SECRET_API_KEY] and it's secret.";
        assertEquals(expected, DataScrubber.scrub(input));
    }

    @Test
    public void testScrubEmail() {
        String input = "Contact me at user@example.com for info.";
        String expected = "Contact me at [EMAIL_REDACTED] for info.";
        assertEquals(expected, DataScrubber.scrub(input));
    }

    @Test
    public void testScrubAbsolutePath() {
        String input = "The file is at /home/user/project/secret.txt or C:\\Users\\Name\\secret.txt";
        String scrubbed = DataScrubber.scrub(input);
        assertNotEquals(input, scrubbed);
        assertEquals("The file is at [PATH_REDACTED] or [PATH_REDACTED]", scrubbed);
    }

    @Test
    public void testNoScrubbingNeeded() {
        String input = "This is a normal sentence with no secrets.";
        assertEquals(input, DataScrubber.scrub(input));
    }
}
