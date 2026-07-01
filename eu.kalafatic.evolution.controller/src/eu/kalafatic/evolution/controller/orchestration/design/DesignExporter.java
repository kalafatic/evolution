package eu.kalafatic.evolution.controller.orchestration.design;

import java.io.File;
import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * @evo:22:A reason=tool-based-exporter
 */
public class DesignExporter {

    public static void exportToHtml(String htmlContent, File outputFile, TaskContext context) throws Exception {
        FileTool tool = new FileTool();
        String relPath = outputFile.getName(); // Simple for now, context handles workingDir
        tool.execute("WRITE " + relPath + "\n" + htmlContent, outputFile.getParentFile(), context);
    }

    public static void saveModelAsJson(String jsonContent, File outputFile, TaskContext context) throws Exception {
        FileTool tool = new FileTool();
        String relPath = outputFile.getName();
        tool.execute("WRITE " + relPath + "\n" + jsonContent, outputFile.getParentFile(), context);
    }
}
