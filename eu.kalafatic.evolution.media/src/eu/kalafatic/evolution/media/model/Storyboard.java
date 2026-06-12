package eu.kalafatic.evolution.media.model;

import java.util.ArrayList;
import java.util.List;

public class Storyboard {
    private String title;
    private List<Scene> scenes = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<Scene> getScenes() { return scenes; }

    public static class Scene {
        public String title;
        public String narration;
        public String visual;
        public String animation;
        public int durationSeconds;
        public List<String> referencedDiagrams = new ArrayList<>();
    }
}
