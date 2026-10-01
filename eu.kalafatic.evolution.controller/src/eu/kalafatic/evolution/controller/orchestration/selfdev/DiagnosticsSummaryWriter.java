package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;

import eu.kalafatic.utils.constants.FUIConstants;

public class DiagnosticsSummaryWriter {

    public static void writeSummary(SelfDevContext context) {
        if (context == null) return;

        File logDir = context.getLogDirectory();
        if (!logDir.exists()) logDir.mkdirs();

        File summaryFile = new File(logDir, "diagnostics-summary.txt");

        TaskResult buildRes = context.getTaskResult("BUILD_EVO");
        if (buildRes == null) buildRes = context.getTaskResult("BUILD");

        TaskResult exportRes = context.getTaskResult("EXPORT_EVO");
        if (exportRes == null) exportRes = context.getTaskResult("EXPORT");

        TaskResult startRes = context.getTaskResult("START_EVO");

        BuildArtifact evoArtifact = context.getArtifact(ArtifactType.EVO_RCP);

        String foiCodeSource = "UNKNOWN";
        try {
            java.security.ProtectionDomain pd = FUIConstants.class.getProtectionDomain();
            if (pd != null && pd.getCodeSource() != null) {
                foiCodeSource = String.valueOf(pd.getCodeSource().getLocation());
            }
        } catch (Throwable t) {
            foiCodeSource = "ERROR: " + t.getMessage();
        }

        String foiRootCause = "NONE";
        File stderrLog = new File(logDir, "start.stderr.log");
        if (stderrLog.exists()) {
            try {
                String content = Files.readString(stderrLog.toPath());
                if (content.contains("FUIConstants")) {
                    foiRootCause = "Detected FUIConstants error in start.stderr.log";
                }
            } catch (Exception ignored) {}
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(summaryFile, false))) {
            pw.println("RUN=" + context.getRunId());
            pw.println("REPOSITORY=" + (context.getRepositoryRoot() != null ? context.getRepositoryRoot().getAbsolutePath() : "null"));
            pw.println("PREPARED_SOURCE=" + (context.getPreparedReactorDirectory() != null ? context.getPreparedReactorDirectory().getAbsolutePath() : "null"));
            pw.println("BUILD=" + (context.getBuildDirectory() != null ? context.getBuildDirectory().getAbsolutePath() : "null"));
            pw.println("EXPORT=" + (context.getExportDirectory() != null ? context.getExportDirectory().getAbsolutePath() : "null"));
            pw.println("RUNTIME=" + (context.getRuntimeDirectory() != null ? context.getRuntimeDirectory().getAbsolutePath() : "null"));

            pw.println("MAVEN_COMMAND=" + (buildRes != null && buildRes.getCommand() != null ? buildRes.getCommand() : "N/A"));
            pw.println("MAVEN_EXIT_CODE=" + (buildRes != null ? buildRes.getExitCode() : "N/A"));

            pw.println("TYCHO_REACTOR=" + (context.getPreparedReactorDirectory() != null ? context.getPreparedReactorDirectory().getAbsolutePath() : "N/A"));
            pw.println("PRODUCT_DEFINITION=" + context.getProductId());
            pw.println("PRODUCT_ARTIFACT=" + (evoArtifact != null && evoArtifact.getPath() != null ? evoArtifact.getPath().getAbsolutePath() : "N/A"));

            File startCmdFile = new File(logDir, "start-command.txt");
            String startCmd = "N/A";
            if (startCmdFile.exists()) {
                try {
                    startCmd = Files.readString(startCmdFile.toPath()).trim();
                } catch (Exception ignored) {}
            }

            pw.println("EXPORTED_EXECUTABLE=" + (context.getExportDirectory() != null ? context.getExportDirectory().getAbsolutePath() : "N/A"));
            pw.println("START_COMMAND=" + startCmd);
            pw.println("PROCESS_PID=" + (startRes != null ? startRes.getDiagnostic("pid", "N/A") : "N/A"));
            pw.println("PROCESS_EXIT_CODE=" + (startRes != null ? startRes.getExitCode() : "N/A"));

            boolean allSuccess = true;
            for (Map.Entry<String, TaskResult> entry : context.getTaskResults().entrySet()) {
                if (!entry.getValue().isSuccess()) {
                    allSuccess = false;
                    break;
                }
            }

            pw.println("OSGI_STATUS=" + (startRes != null && startRes.isSuccess() ? "RUNNING" : "UNKNOWN"));
            pw.println("APPLICATION_STATUS=" + (startRes != null && startRes.isSuccess() ? "INITIALIZED" : "FAILED"));

            pw.println("FUI_CONSTANTS_BUNDLE=eu.kalafatic.utils");
            pw.println("FUI_CONSTANTS_CODE_SOURCE=" + foiCodeSource);
            pw.println("FUI_CONSTANTS_ROOT_CAUSE=" + foiRootCause);

            pw.println("FINAL_STATUS=" + (allSuccess ? "SUCCESS" : "FAILED"));

        } catch (Exception e) {
            System.err.println("[DiagnosticsSummaryWriter] Failed writing summary: " + e.getMessage());
        }
    }
}
