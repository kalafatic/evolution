package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

import eu.kalafatic.evolution.controller.application.HeadlessCli;

/**
 * Family UX CLI contract: the mandatory command vocabulary is registered in the
 * single dispatch table, help output is generated from the same table, and the
 * capability commands read live data sources instead of static lists.
 */
public class HeadlessCliTest {

    private static final int PORT = 28999;

    private static String dispatch(String line) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new HeadlessCli(PORT).dispatch(line, new PrintStream(buffer, true));
        return buffer.toString();
    }

    @Test
    public void helpListsTheMandatoryCommandVocabulary() {
        String help = dispatch("/help");
        for (String command : new String[] {"help", "status", "start", "stop",
                "restart", "web", "terminal", "models", "sessions", "providers",
                "agents", "config", "logs", "exit"}) {
            assertTrue("help output missing /" + command, help.contains("/" + command));
        }
    }

    @Test
    public void helpTopicPrintsDetailedHelp() {
        assertTrue(dispatch("/help providers").contains("AiProviders"));
        assertTrue(dispatch("/help models").contains("Ollama"));
        assertTrue(dispatch("/help agents")
                .contains("ConnectedApplicationRegistry"));
    }

    @Test
    public void capabilityCommandsProduceOutput() {
        assertFalse(dispatch("/providers").isBlank());
        assertFalse(dispatch("/models").isBlank());
        assertFalse(dispatch("/agents").isBlank());
    }

    @Test
    public void unknownCommandProducesSuggestions() {
        String output = dispatch("/sessins");
        assertTrue(output, output.contains("Did you mean"));
        assertTrue(output, output.contains("sessions"));
    }

    @Test
    public void exitEndsTheRepl() {
        assertFalse(new HeadlessCli(PORT).dispatch("/exit",
                new PrintStream(new ByteArrayOutputStream())));
    }
}
