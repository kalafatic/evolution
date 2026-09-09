package eu.kalafatic.evolution.forge.controller.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.kalafatic.evolution.forge.controller.api.ForgeJob.SourceProfile;

public class ForgeSourceAnalyzer {

    public List<SourceProfile> analyzeSources(List<String> sourcePaths) {
        List<SourceProfile> profiles = new ArrayList<>();
        if (sourcePaths == null) {
            return profiles;
        }

        for (String sourceStr : sourcePaths) {
            if (sourceStr == null || sourceStr.trim().isEmpty()) {
                continue;
            }
            SourceProfile profile = analyzeSource(sourceStr.trim());
            profiles.add(profile);
        }
        return profiles;
    }

    public SourceProfile analyzeSource(String sourceStr) {
        SourceProfile profile = new SourceProfile();
        profile.setSourcePath(sourceStr);

        try {
            Path path = Paths.get(sourceStr);
            if (!Files.exists(path)) {
                if (sourceStr.contains("/") || sourceStr.contains("-")) {
                    profile.setSourceType("HF");
                    profile.setSampleCount(1000);
                    profile.setEstimatedTokens(200000);
                    profile.setQualityScore(0.85);
                    profile.setKnowledgeRatio(0.6);
                    profile.setInstructionRatio(0.4);
                } else {
                    profile.setSourceType("UNKNOWN");
                    profile.setQualityScore(0.0);
                }
                return profile;
            }

            if (Files.isRegularFile(path)) {
                String fileName = path.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".evodata")) {
                    profile.setSourceType("EVODATA");
                    inspectEvodata(path, profile);
                } else if (fileName.endsWith(".evo")) {
                    profile.setSourceType("EVO_MODEL");
                    profile.setQualityScore(0.95);
                    profile.setSampleCount(500);
                    profile.setEstimatedTokens(100000);
                } else if (fileName.endsWith(".json") || fileName.endsWith(".jsonl")) {
                    profile.setSourceType("FILE");
                    inspectTextFile(path, profile);
                } else {
                    profile.setSourceType("FILE");
                    inspectTextFile(path, profile);
                }
            } else if (Files.isDirectory(path)) {
                profile.setSourceType("LOCAL_DIR");
                inspectDirectory(path, profile);
            }
        } catch (Exception e) {
            profile.setSourceType("ERROR");
            profile.setQualityScore(0.0);
        }

        return profile;
    }

    private void inspectEvodata(Path path, SourceProfile profile) {
        try {
            long size = Files.size(path);
            long estSamples = Math.max(1, size / 1024);
            long estTokens = estSamples * 128;
            profile.setSampleCount(estSamples);
            profile.setEstimatedTokens(estTokens);
            profile.setQualityScore(0.92);
            profile.setKnowledgeRatio(0.5);
            profile.setCodeRatio(0.3);
            profile.setInstructionRatio(0.2);
        } catch (Exception e) {
            profile.setQualityScore(0.5);
        }
    }

    private void inspectTextFile(Path path, SourceProfile profile) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            long lines = 0;
            long totalChars = 0;
            long codeLines = 0;
            long chatLines = 0;

            while ((line = reader.readLine()) != null && lines < 5000) {
                lines++;
                totalChars += line.length();
                String trimmed = line.trim();
                if (trimmed.startsWith("def ") || trimmed.startsWith("public ") || trimmed.startsWith("import ") || trimmed.contains("{") || trimmed.contains("}")) {
                    codeLines++;
                }
                if (trimmed.contains("User:") || trimmed.contains("Assistant:") || trimmed.contains("<|im_start|>") || trimmed.contains("System:")) {
                    chatLines++;
                }
            }

            long sampleCount = Math.max(1, lines);
            long estimatedTokens = Math.max(sampleCount * 4, totalChars / 4);
            profile.setSampleCount(sampleCount);
            profile.setEstimatedTokens(estimatedTokens);

            double codeRatio = lines > 0 ? (double) codeLines / lines : 0.0;
            double chatRatio = lines > 0 ? (double) chatLines / lines : 0.0;
            double knowledgeRatio = Math.max(0.0, 1.0 - codeRatio - chatRatio);

            profile.setCodeRatio(codeRatio);
            profile.setChatRatio(chatRatio);
            profile.setInstructionRatio(chatRatio);
            profile.setKnowledgeRatio(knowledgeRatio);
            profile.setQualityScore(0.88);
        } catch (Exception e) {
            profile.setQualityScore(0.6);
        }
    }

    private void inspectDirectory(Path dirPath, SourceProfile profile) {
        try (Stream<Path> walk = Files.walk(dirPath)) {
            List<Path> files = walk.filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains("/.git/") && !p.toString().contains("\\.git\\")
                            && !p.toString().contains("/target/") && !p.toString().contains("\\target\\"))
                    .collect(Collectors.toList());

            long totalBytes = 0;
            long codeFiles = 0;

            for (Path f : files) {
                totalBytes += Files.size(f);
                String name = f.getFileName().toString().toLowerCase();
                if (name.endsWith(".java") || name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".cpp") || name.endsWith(".c")) {
                    codeFiles++;
                }
            }

            long estSamples = Math.max(1, files.size() * 10);
            long estTokens = Math.max(100, totalBytes / 4);

            profile.setSampleCount(estSamples);
            profile.setEstimatedTokens(estTokens);

            double codeRatio = files.size() > 0 ? (double) codeFiles / files.size() : 0.0;
            profile.setCodeRatio(codeRatio);
            profile.setKnowledgeRatio(Math.max(0.0, 1.0 - codeRatio));
            profile.setQualityScore(0.90);
        } catch (Exception e) {
            profile.setQualityScore(0.5);
        }
    }
}
