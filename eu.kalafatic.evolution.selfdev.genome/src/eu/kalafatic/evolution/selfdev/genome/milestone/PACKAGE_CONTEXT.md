# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.selfdev.genome/src/eu/kalafatic/evolution/selfdev/genome/milestone/

## Domain: general

## Components
* `MilestoneGenerator.java`: package eu.kalafatic.evolution.selfdev.genome.milestone; import java.io.File; import java.io.IOException; import java.nio.file.Files; import java.time.LocalDateTime; import java.time.format.DateTimeFormatter; import java.util.ArrayList; import java.util.Arrays; import java.util.Comparator; import java.util.List; import java.util.stream.Collectors; import org.json.JSONArray; import org.json.JSONObject; import eu.kalafatic.utils.semantic.AIContextTool; import eu.kalafatic.utils.semantic.EvoMetadata; public class MilestoneGenerator { private final AIContextTool contextTool = new AIContextTool(); public void generateMilestone(File root, String projectName, String version) { List<EvoMetadata> allMetadata = scanAllMetadata(root); String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
