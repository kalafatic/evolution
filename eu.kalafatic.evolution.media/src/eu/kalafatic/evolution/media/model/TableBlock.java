package eu.kalafatic.evolution.media.model;

import java.util.ArrayList;
import java.util.List;

public class TableBlock {
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();

    public List<String> getHeaders() { return headers; }
    public List<List<String>> getRows() { return rows; }
}
