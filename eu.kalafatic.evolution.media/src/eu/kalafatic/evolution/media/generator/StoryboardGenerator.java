package eu.kalafatic.evolution.media.generator;

import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import src.eu.kalafatic.evolution.media.model.Storyboard;

public class StoryboardGenerator {

    public Storyboard generate(TargetRealityModel model) {
        Storyboard storyboard = new Storyboard();
        storyboard.setTitle("Video Storyboard for " + model.getDomain());

        Storyboard.Scene intro = new Storyboard.Scene();
        intro.title = "EVO Overview";
        intro.narration = "EVO is a cognitive software understanding platform. Today we explore " + model.getDomain() + ".";
        intro.visual = "Display platform logo and domain overview.";
        intro.animation = "Logo fades in, text appears.";
        intro.durationSeconds = 15;
        storyboard.getScenes().add(intro);

        for (Subsystem subsystem : model.getSubsystems()) {
            Storyboard.Scene scene = new Storyboard.Scene();
            scene.title = "Subsystem: " + subsystem.getName();
            scene.narration = "The " + subsystem.getName() + " subsystem is responsible for " + subsystem.getPurpose() + ".";
            scene.visual = "Diagram of " + subsystem.getName() + " and its components.";
            scene.animation = "Components highlight as mentioned.";
            scene.durationSeconds = 20;
            storyboard.getScenes().add(scene);
        }

        Storyboard.Scene conclusion = new Storyboard.Scene();
        conclusion.title = "Conclusion";
        conclusion.narration = "This concludes our architectural overview of " + model.getDomain() + ".";
        conclusion.visual = "Summary slide with key metrics.";
        conclusion.animation = "Fade to black.";
        conclusion.durationSeconds = 10;
        storyboard.getScenes().add(conclusion);

        return storyboard;
    }
}
