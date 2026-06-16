package eu.kalafatic.evolution.media.video;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import src.eu.kalafatic.evolution.media.model.Storyboard;

public class NoOpVideoGenerator implements VideoGenerator {
    @Override
    public CompletableFuture<File> generateVideo(Storyboard storyboard, File outputFile) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Video generation not yet implemented."));
    }
}
