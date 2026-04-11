package eu.kalafatic.evolution.controller.review.model;

import java.util.ArrayList;
import java.util.List;

public class DiffHunk {
    private String header;
    private List<String> lines = new ArrayList<>();

    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }

    public List<String> getLines() { return lines; }
    public void setLines(List<String> lines) { this.lines = lines; }
}
