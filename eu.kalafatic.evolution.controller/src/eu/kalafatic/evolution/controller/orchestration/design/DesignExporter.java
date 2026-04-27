package eu.kalafatic.evolution.controller.orchestration.design;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @evo:20:A reason=design-exporter
 */
public class DesignExporter {

    public static void exportToHtml(String htmlContent, File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(htmlContent);
        }
    }

    public static void saveModelAsJson(String jsonContent, File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(jsonContent);
        }
    }
}
