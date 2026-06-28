# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/parsers/

## Domain: general

## Components
* `JsonUtils.java`: package eu.kalafatic.evolution.controller.parsers; import org.json.JSONArray; import org.json.JSONObject; import org.json.JSONException; import java.util.ArrayList; import java.util.Collections; import java.util.List; public class JsonUtils { public static JSONObject extractJsonObject(String text) { if (text == null) return null; text = text.replaceAll("(?is)<think>.*?</think>", ""); int firstStart = text.indexOf("{"); int lastEnd = text.lastIndexOf("}"); if (firstStart != -1 && lastEnd != -1 && lastEnd > firstStart) { String fullPart = text.substring(firstStart, lastEnd + 1); try { return new JSONObject(fullPart); } catch (JSONException e) { int searchPos = firstStart; while (searchPos <= lastEnd) {
* `RuleParser.java`: package eu.kalafatic.evolution.controller.parsers; import eu.kalafatic.evolution.model.orchestration.*; public class RuleParser { public static void parseAndAddRules(Agent agent, String rulesData) { if (rulesData == null || rulesData.isEmpty()) return; String[] ruleSpecs = rulesData.split(";"); OrchestrationFactory factory = OrchestrationFactory.eINSTANCE; for (String spec : ruleSpecs) { String[] parts = spec.split("=", 2); if (parts.length < 2) continue; String type = parts[0].trim(); String config = parts[1].trim(); Rule rule = null; if ("access".equalsIgnoreCase(type)) { AccessRule ar = factory.createAccessRule(); parseKeyValuePairs(ar, config); rule = ar; } else if ("network".equalsIgnoreCase(type)) { NetworkRule nr = factory.createNetworkRule(); parseKeyValuePairs(nr, config);
