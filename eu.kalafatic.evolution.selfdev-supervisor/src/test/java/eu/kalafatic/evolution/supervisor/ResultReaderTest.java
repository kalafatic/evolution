package eu.kalafatic.evolution.supervisor;

import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ResultReaderTest {

    @Test
    public void testReadResult() throws IOException {
        File tempFile = File.createTempFile("result", ".json");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"status\": \"OK\", \"score\": 0.85}");
        }

        ResultReader reader = new ResultReader();
        Result result = reader.readResult(tempFile);

        Assert.assertEquals("OK", result.getStatus());
        Assert.assertEquals(0.85, result.getScore(), 0.001);
        tempFile.delete();
    }

    @Test
    public void testReadState() throws IOException {
        File tempFile = File.createTempFile("state", ".json");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"active\": true, \"iteration\": 5}");
        }

        ResultReader reader = new ResultReader();
        State state = reader.readState(tempFile);

        Assert.assertTrue(state.isActive());
        Assert.assertEquals(5, state.getIteration());
        tempFile.delete();
    }
}
