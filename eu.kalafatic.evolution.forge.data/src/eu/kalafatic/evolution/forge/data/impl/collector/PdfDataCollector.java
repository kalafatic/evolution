package eu.kalafatic.evolution.forge.data.impl.collector;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.SourceType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataCollector;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects and extracts text content from PDF documents.
 */
public class PdfDataCollector extends TrainingDataCollector {

    private static final Pattern TJ_SINGLE_PATTERN = Pattern.compile("\\(([^)]+)\\)\\s*Tj");
    private static final Pattern TJ_ARRAY_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\s*TJ");
    private static final Pattern STRING_IN_ARRAY_PATTERN = Pattern.compile("\\(([^)]+)\\)");

    @Override
    public CollectorType getType() {
        return CollectorType.PDF;
    }

    @Override
    public String getName() {
        return "PDF Data Collector";
    }

    @Override
    public CollectionResult collect(CollectionContext context) {
        long startTime = System.currentTimeMillis();
        List<TrainingDataItem> items = new ArrayList<>();
        List<File> targetPaths = new ArrayList<>();

        if (context.getProjectDirectory() != null && context.getProjectDirectory().exists()) {
            targetPaths.add(context.getProjectDirectory());
        }
        if (context.getDiskPaths() != null) {
            targetPaths.addAll(context.getDiskPaths());
        }

        if (targetPaths.isEmpty()) {
            return CollectionResult.success(getType(), items, System.currentTimeMillis() - startTime);
        }

        List<String> exclusions = context.getExclusions();

        for (File pathFile : targetPaths) {
            if (pathFile == null || !pathFile.exists()) continue;

            if (pathFile.isFile() && pathFile.getName().toLowerCase().endsWith(".pdf")) {
                processPdfFile(pathFile.toPath(), context, exclusions, items);
            } else if (pathFile.isDirectory()) {
                try (Stream<Path> walk = Files.walk(pathFile.toPath())) {
                    List<Path> pdfFiles = walk
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                            .sorted()
                            .collect(Collectors.toList());

                    for (Path pdf : pdfFiles) {
                        processPdfFile(pdf, context, exclusions, items);
                    }
                } catch (Exception e) {
                    // Continue with remaining paths
                }
            }
        }

        return CollectionResult.success(getType(), items, System.currentTimeMillis() - startTime);
    }

    private void processPdfFile(Path pdfPath, CollectionContext context, List<String> exclusions, List<TrainingDataItem> items) {
        try {
            String absPath = pdfPath.toAbsolutePath().toString().replace('\\', '/');
            if (exclusions != null) {
                for (String exclusion : exclusions) {
                    if (absPath.contains(exclusion)) return;
                }
            }

            long size = Files.size(pdfPath);
            if (context.getMaxFileSize() > 0 && size > context.getMaxFileSize()) {
                return;
            }

            byte[] bytes = Files.readAllBytes(pdfPath);
            String rawContent = new String(bytes, StandardCharsets.ISO_8859_1);

            String extractedText = extractTextFromPdfRaw(rawContent);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                extractedText = fallbackCleanPrintableText(bytes);
            }

            if (extractedText == null || extractedText.trim().length() < 5) {
                return;
            }

            String fileName = pdfPath.getFileName().toString();
            TrainingDataItem item = new TrainingDataItem();
            item.setSourceType(SourceType.PDF);
            item.setSource(absPath);
            item.setTitle(fileName);
            item.setContent(extractedText.trim());

            item.addMetadata("fileSize", size);
            item.addMetadata("format", "PDF");
            item.addMetadata("sourcePath", absPath);

            items.add(item);
        } catch (Exception e) {
            // Unreadable PDF skipped gracefully
        }
    }

    /**
     * Extracts text from PDF stream operators (BT...ET text blocks, Tj, and TJ operators).
     */
    private String extractTextFromPdfRaw(String pdfRaw) {
        StringBuilder sb = new StringBuilder();

        int btIndex = pdfRaw.indexOf("BT");
        while (btIndex != -1) {
            int etIndex = pdfRaw.indexOf("ET", btIndex);
            if (etIndex == -1) break;

            String block = pdfRaw.substring(btIndex + 2, etIndex);

            // Match single Tj operators
            Matcher tjMatcher = TJ_SINGLE_PATTERN.matcher(block);
            while (tjMatcher.find()) {
                String str = tjMatcher.group(1);
                sb.append(cleanPdfString(str)).append(" ");
            }

            // Match array TJ operators
            Matcher arrayMatcher = TJ_ARRAY_PATTERN.matcher(block);
            while (arrayMatcher.find()) {
                String arrContent = arrayMatcher.group(1);
                Matcher strMatcher = STRING_IN_ARRAY_PATTERN.matcher(arrContent);
                while (strMatcher.find()) {
                    sb.append(cleanPdfString(strMatcher.group(1)));
                }
                sb.append(" ");
            }

            sb.append("\n");
            btIndex = pdfRaw.indexOf("BT", etIndex + 2);
        }

        return sb.toString();
    }

    private String cleanPdfString(String str) {
        if (str == null) return "";
        return str.replace("\\(", "(")
                  .replace("\\)", ")")
                  .replace("\\\\", "\\")
                  .replace("\\r", "\n")
                  .replace("\\n", "\n");
    }

    private String fallbackCleanPrintableText(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int printableCount = 0;
        for (byte b : bytes) {
            if ((b >= 32 && b <= 126) || b == 10 || b == 13 || b == 9) {
                char c = (char) (b & 0xFF);
                sb.append(c);
                if (Character.isLetterOrDigit(c)) printableCount++;
            } else {
                sb.append(' ');
            }
        }
        if (printableCount < 20) return "";
        return sb.toString().replaceAll("\\s+", " ").trim();
    }
}
