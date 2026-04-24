package eu.kalafatic.evolution.supervisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class ResultReader {
    private final ObjectMapper mapper = new ObjectMapper();

    public Result readResult(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("Result file missing: " + file.getAbsolutePath());
        }
        return mapper.readValue(file, Result.class);
    }

    public State readState(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("State file missing: " + file.getAbsolutePath());
        }
        return mapper.readValue(file, State.class);
    }

    public EvoPlan readPlan(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("Plan file missing: " + file.getAbsolutePath());
        }
        return mapper.readValue(file, EvoPlan.class);
    }

    public void writeState(File file, State state) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, state);
    }
}
