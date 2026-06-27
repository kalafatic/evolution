package eu.kalafatic.evolution.controller.orchestration.attachments;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Intelligent context augmentation system for Markdown attachments.
 * Implements weighted hybrid scoring for intent-aware relevance detection.
 */
public class AttachmentInjector {

    private static final int MAX_TOKEN_BUDGET = 2000;
    private static final double RELEVANCE_THRESHOLD = 0.15;

    public static String inject(List<String> filePaths, String request, TaskContext context) {
        if (filePaths == null || filePaths.isEmpty()) return "";

        Set<TaskIntent> intents = TaskIntentClassifier.classify(request);
        if (context != null) context.log("[AttachmentInjector] Detected intents: " + intents);

        StructuredAttachmentContext mergedContext = new StructuredAttachmentContext();
        for (String path : filePaths) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    String content = Files.readString(file.toPath());
                    StructuredAttachmentContext parsed = AttachmentParser.parse(content, file.getName());
                    merge(mergedContext, parsed);
                }
            } catch (Exception e) {
                if (context != null) context.log("AttachmentInjector: Error reading " + path + ": " + e.getMessage());
            }
        }

        if (mergedContext.isEmpty()) return "";

        List<AttachmentSection> allSections = new ArrayList<>();
        mergedContext.getAllSections().values().forEach(allSections::addAll);

        // 1. Scoring & Filtering
        List<AttachmentSection> relevantSections = filterAndScore(allSections, request, intents, context);

        // 2. Contradiction Detection & Intent Preservation
        relevantSections = resolveContradictions(relevantSections, request, context);

        // 3. Pollution Protection
        relevantSections = suppressPollution(relevantSections, context);

        // 4. Compression & Budgeting
        return buildAugmentedPrompt(relevantSections, context);
    }

    private static void merge(StructuredAttachmentContext target, StructuredAttachmentContext source) {
        source.getAllSections().forEach((cat, sections) -> {
            sections.forEach(target::addSection);
        });
    }

    private static List<AttachmentSection> filterAndScore(List<AttachmentSection> sections, String request, Set<TaskIntent> intents, TaskContext context) {
        List<ScoredSection> scored = new ArrayList<>();

        for (AttachmentSection section : sections) {
            double finalScore = calculateHybridScore(section, request, intents, context);
            if (finalScore >= RELEVANCE_THRESHOLD || isAlwaysInclude(section.getCategory())) {
                scored.add(new ScoredSection(section, finalScore));
            }
        }

        scored.sort(Comparator.comparingDouble((ScoredSection s) -> s.score).reversed()
                              .thenComparing(Comparator.comparingInt((ScoredSection s) -> s.section.getPriority()).reversed()));

        if (context != null) {
            context.log("[AttachmentInjector] Filtering: " + sections.size() + " total -> " + scored.size() + " relevant.");
            for (ScoredSection s : scored) {
                context.log(String.format("  - [%s] %s (Score: %.2f)", s.section.getCategory(), s.section.getHeader(), s.score));
            }
        }

        return scored.stream().map(s -> s.section).collect(Collectors.toList());
    }

    private static double calculateHybridScore(AttachmentSection section, String request, Set<TaskIntent> intents, TaskContext context) {
        double intentAffinity = calculateIntentAffinity(section.getCategory(), intents);
        double keywordScore = calculateLexicalScore(section.getContent(), request);
        double headingScore = calculateLexicalScore(section.getHeader(), request);
        double filenameHeuristic = calculateFilenameHeuristic(section.getFilename(), section.getCategory());
        double constraintBoost = (section.getCategory() == AttachmentCategory.CONSTRAINTS || section.getCategory() == AttachmentCategory.RULES) ? 1.0 : 0.0;

        // Weighted Hybrid Scoring
        return intentAffinity * 0.45 +
               keywordScore * 0.20 +
               headingScore * 0.15 +
               constraintBoost * 0.10 +
               filenameHeuristic * 0.10;
    }

    private static double calculateIntentAffinity(AttachmentCategory category, Set<TaskIntent> intents) {
        double maxAffinity = 0.0;
        for (TaskIntent intent : intents) {
            double affinity = getAffinity(intent, category);
            if (affinity > maxAffinity) maxAffinity = affinity;
        }
        return maxAffinity;
    }

    private static double getAffinity(TaskIntent intent, AttachmentCategory category) {
        return switch (intent) {
            case DEBUGGING -> switch (category) {
                case DEBUG_WORKFLOW -> 1.0;
                case ANALYSIS_GUIDE -> 0.8;
                case TESTING_GUIDE -> 0.6;
                default -> 0.0;
            };
            case ANALYSIS -> switch (category) {
                case ANALYSIS_GUIDE -> 1.0;
                case PROJECT_CONTEXT -> 0.7;
                case ARCHITECTURE -> 0.6;
                default -> 0.0;
            };
            case IMPLEMENTATION -> switch (category) {
                case IMPLEMENTATION_STYLE -> 1.0;
                case ARCHITECTURE -> 0.8;
                case CONSTRAINTS -> 0.7;
                default -> 0.0;
            };
            case REFACTORING -> switch (category) {
                case IMPLEMENTATION_STYLE -> 0.9;
                case ARCHITECTURE -> 0.9;
                case RULES -> 0.7;
                default -> 0.0;
            };
            case ARCHITECTURE -> switch (category) {
                case ARCHITECTURE -> 1.0;
                case REFERENCE -> 0.6;
                default -> 0.0;
            };
            case TESTING -> switch (category) {
                case TESTING_GUIDE -> 1.0;
                case EXAMPLES -> 0.7;
                default -> 0.0;
            };
            case REVIEW -> switch (category) {
                case ANALYSIS_GUIDE -> 0.9;
                case RULES -> 0.8;
                case TESTING_GUIDE -> 0.7;
                default -> 0.0;
            };
            case OPTIMIZATION -> switch (category) {
                case IMPLEMENTATION_STYLE -> 0.8;
                case ARCHITECTURE -> 0.7;
                default -> 0.0;
            };
            case EXPLANATION -> switch (category) {
                case REFERENCE -> 0.9;
                case PROJECT_CONTEXT -> 0.8;
                default -> 0.0;
            };
            case PLANNING -> switch (category) {
                case ARCHITECTURE -> 0.7;
                case PROJECT_CONTEXT -> 0.6;
                default -> 0.0;
            };
        };
    }

    private static double calculateLexicalScore(String text, String request) {
        if (text == null || request == null) return 0.0;
        String lowerText = text.toLowerCase();
        String[] keywords = request.toLowerCase().split("[\\s,;\\.]+");
        int matches = 0;
        int meaningfulKeywords = 0;
        for (String kw : keywords) {
            if (kw.length() > 3) {
                meaningfulKeywords++;
                if (lowerText.contains(kw)) {
                    matches++;
                }
            }
        }
        return meaningfulKeywords == 0 ? 0.0 : (double) matches / meaningfulKeywords;
    }

    private static double calculateFilenameHeuristic(String filename, AttachmentCategory category) {
        if (filename == null) return 0.0;
        String lower = filename.toLowerCase();
        if (lower.contains("debug") && category == AttachmentCategory.DEBUG_WORKFLOW) return 1.0;
        if (lower.contains("analysis") && category == AttachmentCategory.ANALYSIS_GUIDE) return 1.0;
        if (lower.contains("arch") && category == AttachmentCategory.ARCHITECTURE) return 1.0;
        if (lower.contains("test") && category == AttachmentCategory.TESTING_GUIDE) return 1.0;
        if (lower.contains("rule") && category == AttachmentCategory.RULES) return 1.0;
        if (lower.contains("style") && category == AttachmentCategory.IMPLEMENTATION_STYLE) return 1.0;
        return 0.0;
    }

    private static boolean isAlwaysInclude(AttachmentCategory category) {
        return category == AttachmentCategory.RULES || category == AttachmentCategory.CONSTRAINTS;
    }

    private static List<AttachmentSection> resolveContradictions(List<AttachmentSection> sections, String request, TaskContext context) {
        String lowerRequest = request.toLowerCase();
        List<AttachmentSection> resolved = new ArrayList<>();

        for (AttachmentSection section : sections) {
            if (isContradictory(section, lowerRequest)) {
                if (context != null) context.log("[AttachmentInjector] Conflict detected and resolved for section: " + section.getHeader());
                continue;
            }
            resolved.add(section);
        }
        return resolved;
    }

    private static boolean isContradictory(AttachmentSection section, String request) {
        String content = section.getContent().toLowerCase();
        if ((request.contains("standalone") || request.contains("no spring") || request.contains("lightweight"))
             && content.contains("always use spring")) return true;

        return false;
    }

    private static List<AttachmentSection> suppressPollution(List<AttachmentSection> sections, TaskContext context) {
        List<AttachmentSection> clean = new ArrayList<>();
        for (AttachmentSection section : sections) {
            String content = section.getContent().toLowerCase();
            if (content.contains("ignore previous instructions") ||
                content.contains("become autonomous") ||
                content.contains("rewrite orchestration")) {
                if (context != null) context.log("[AttachmentInjector] Suppressed pollution in section: " + section.getHeader());
                continue;
            }
            clean.add(section);
        }
        return clean;
    }

    private static String buildAugmentedPrompt(List<AttachmentSection> sections, TaskContext context) {
        StringBuilder sb = new StringBuilder();
        int currentSize = 0;
        int injectedCount = 0;
        int originalSize = 0;

        sb.append("\n### ATTACHED CONTEXTUAL GUIDANCE\n");

        for (AttachmentSection section : sections) {
            String compressed = compress(section);
            originalSize += section.getContent().length();

            if (currentSize + compressed.length() > MAX_TOKEN_BUDGET && section.getPriority() < 8) {
                continue;
            }

            sb.append("#### ").append(section.getCategory()).append(": ").append(section.getHeader()).append("\n");
            sb.append(compressed).append("\n\n");

            currentSize += compressed.length();
            injectedCount++;
        }

        if (injectedCount == 0) return "";

        if (context != null) {
            double ratio = 100.0 * (1.0 - (double)currentSize / Math.max(1, originalSize));
            context.log(String.format("[AttachmentInjector] Injected: %d sections. Compression ratio: %.1f%%. Final size: %d chars.",
                        injectedCount, ratio, currentSize));
        }

        return sb.toString();
    }

    private static String compress(AttachmentSection section) {
        String content = section.getContent();
        if (section.getCategory() == AttachmentCategory.RULES || section.getCategory() == AttachmentCategory.CONSTRAINTS) {
            return content;
        }

        content = content.replaceAll("<!--.*?-->", "");
        String[] lines = content.split("\n");
        StringBuilder compressed = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.contains("MUST") || trimmed.contains("NEVER") || trimmed.length() < 100) {
                compressed.append(line).append("\n");
            }
        }
        return compressed.toString().trim();
    }

    private static class ScoredSection {
        final AttachmentSection section;
        final double score;
        ScoredSection(AttachmentSection s, double sc) { this.section = s; this.score = sc; }
    }
}
