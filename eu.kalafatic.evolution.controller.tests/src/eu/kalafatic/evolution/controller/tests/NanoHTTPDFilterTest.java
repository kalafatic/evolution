package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.logging.*;
import java.net.SocketException;
import eu.kalafatic.evolution.controller.log.Log;

public class NanoHTTPDFilterTest {

    @Test
    public void testFilterApplication() {
        // Log something to ensure static initializer runs
        Log.log("Initializing Log for filter test");

        Logger nanoLogger = Logger.getLogger("fi.iki.elonen.NanoHTTPD");
        Filter filter = nanoLogger.getFilter();

        assertNotNull("Filter should be applied to fi.iki.elonen.NanoHTTPD logger", filter);

        // Test matching record
        LogRecord matchingRecord = new LogRecord(Level.SEVERE, "Could not send response to the client");
        matchingRecord.setThrown(new SocketException("An established connection was aborted by the software in your host machine"));
        assertFalse("Filter should block matching SEVERE record", filter.isLoggable(matchingRecord));

        // Test record with different message
        LogRecord diffMsgRecord = new LogRecord(Level.SEVERE, "Internal Error");
        diffMsgRecord.setThrown(new SocketException("An established connection was aborted by the software in your host machine"));
        assertTrue("Filter should allow SEVERE record with different message", filter.isLoggable(diffMsgRecord));

        // Test record with different exception message
        LogRecord diffExRecord = new LogRecord(Level.SEVERE, "Could not send response to the client");
        diffExRecord.setThrown(new SocketException("Connection refused"));
        assertTrue("Filter should allow SEVERE record with different exception message", filter.isLoggable(diffExRecord));

        // Test non-SEVERE record
        LogRecord infoRecord = new LogRecord(Level.INFO, "Could not send response to the client");
        infoRecord.setThrown(new SocketException("An established connection was aborted by the software in your host machine"));
        assertTrue("Filter should allow non-SEVERE records", filter.isLoggable(infoRecord));
    }
}
