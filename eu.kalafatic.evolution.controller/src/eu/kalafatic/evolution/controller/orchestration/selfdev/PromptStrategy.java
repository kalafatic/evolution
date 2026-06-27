package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

public class PromptStrategy {
    public IntentProfile intent;
    public String format; // STEP_BY_STEP, JSON_SCHEMA, SIMPLE_TEXT, CODE_ONLY
    public int siblingCount;
    public String tone;
    public String promptTemplate;
    public String expectedOutputFormat;
    public List<String> fields = new ArrayList<>();
    public List<String> examples = new ArrayList<>();
    public List<String> constraints = new ArrayList<>();
    public List<String> validationRules = new ArrayList<>();
}