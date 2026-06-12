package eu.kalafatic.evolution.media.video;

import eu.kalafatic.evolution.media.model.Storyboard;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for future video generation from storyboards.
 */
public interface VideoGenerator {
    /**
     * Generates a video file from a storyboard.
     * @param storyboard The storyboard to visualize.
     * @param outputFile The destination file (e.g. .mp4).
     * @return A future that completes when the video is generated.
     */
    CompletableFuture<File> generateVideo(Storyboard storyboard, File outputFile);
}
