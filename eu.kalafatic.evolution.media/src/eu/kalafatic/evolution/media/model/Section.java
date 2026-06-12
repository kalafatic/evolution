package eu.kalafatic.evolution.media.model;

import java.util.ArrayList;
import java.util.List;

public class Section {
    private String title;
    private List<Object> blocks = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<Object> getBlocks() { return blocks; }

    public void addBlock(Object block) { blocks.add(block); }
}
