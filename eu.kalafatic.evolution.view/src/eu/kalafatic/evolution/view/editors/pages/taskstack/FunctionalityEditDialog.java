package eu.kalafatic.evolution.view.editors.pages.taskstack;

import java.io.File;
import java.util.LinkedHashMap;

import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.controller.orchestration.selfdev.FunctionalityInventoryManager.FunctionalityEntry;
import eu.kalafatic.utils.dialogs.DynamicField;
import eu.kalafatic.utils.dialogs.DynamicMapDialog;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.FunctionalityMetadata;

public class FunctionalityEditDialog extends DynamicMapDialog {

    private FunctionalityEntry entry;
    private File repoRoot;
    private AIContextTool contextTool = new AIContextTool();

    private static final String NAME = "name";
    private static final String STATUS = "status";
    private static final String IMPORTANCE = "importance";
    private static final String USAGE = "usage";
    private static final String COMPLEXITY = "complexity";
    private static final String REFERENCES = "references";
    private static final String MATURITY = "maturity";
    private static final String RISK = "risk";
    private static final String RATIONALE = "rationale";

    public FunctionalityEditDialog(Shell parentShell, FunctionalityEntry entry, File repoRoot) {
        super(parentShell, createFields(entry));
        this.entry = entry;
        this.repoRoot = repoRoot;
        setTitle("Edit Functionality: " + entry.name);
    }

    private static LinkedHashMap<String, DynamicField> createFields(FunctionalityEntry entry) {
        LinkedHashMap<String, DynamicField> fields = new LinkedHashMap<>();
        fields.put(NAME, new DynamicField("Name:", DynamicField.TYPE_TEXT, entry.name));
        fields.put(STATUS, new DynamicField("Status:", DynamicField.TYPE_TEXT, entry.status));

        fields.put(IMPORTANCE, new DynamicField("Importance (0-100):", DynamicField.TYPE_TEXT, String.valueOf(entry.importance >= 0 ? entry.importance : 50)));
        fields.put(USAGE, new DynamicField("Usage Estimate (0-100):", DynamicField.TYPE_TEXT, String.valueOf(entry.usage >= 0 ? entry.usage : 50)));
        fields.put(COMPLEXITY, new DynamicField("Size / Complexity (0-100):", DynamicField.TYPE_TEXT, String.valueOf(entry.complexity >= 0 ? entry.complexity : 50)));
        fields.put(REFERENCES, new DynamicField("References / Centrality (0-100):", DynamicField.TYPE_TEXT, String.valueOf(entry.references >= 0 ? entry.references : 50)));
        fields.put(MATURITY, new DynamicField("Maturity Score (0-100):", DynamicField.TYPE_TEXT, String.valueOf(entry.maturity >= 0 ? entry.maturity : 50)));
        fields.put(RISK, new DynamicField("Maintainability Risk (0-100):", DynamicField.TYPE_TEXT, String.valueOf(entry.risk >= 0 ? entry.risk : 50)));

        fields.put(RATIONALE, new DynamicField("Evaluation Rationale:", DynamicField.TYPE_TEXT, entry.rationale != null ? entry.rationale : ""));

        return fields;
    }

    @Override
    protected void okPressed() {
        if (!validate()) return;
        saveValues();

        entry.name = getString(NAME);
        entry.status = getString(STATUS);

        entry.importance = parseScore(getString(IMPORTANCE), entry.importance);
        entry.usage = parseScore(getString(USAGE), entry.usage);
        entry.complexity = parseScore(getString(COMPLEXITY), entry.complexity);
        entry.references = parseScore(getString(REFERENCES), entry.references);
        entry.maturity = parseScore(getString(MATURITY), entry.maturity);
        entry.risk = parseScore(getString(RISK), entry.risk);

        entry.rationale = getString(RATIONALE);
        entry.method = "USER_EDIT";

        String nowStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        entry.timestamp = nowStr;

        // Persist updated metadata sidecar
        if (repoRoot != null && entry.sourcePath != null) {
            File srcFile = new File(repoRoot, entry.sourcePath);
            if (srcFile.exists()) {
                FunctionalityMetadata meta = new FunctionalityMetadata();
                meta.setFunctionalityId(entry.id);
                meta.setFunctionalityName(entry.name);
                meta.setPrimaryClass(entry.primaryClass);
                meta.setFqcn(entry.fqcn);
                meta.setModuleName(entry.module);
                meta.setSourcePath(entry.sourcePath);
                meta.setImplementationStatus(entry.status);

                meta.setImportanceScore0To100(entry.importance);
                meta.setUsageEstimate0To100(entry.usage);
                meta.setComplexityScore0To100(entry.complexity);
                meta.setCentralityScore0To100(entry.references);
                meta.setMaturityScore0To100(entry.maturity);
                meta.setRiskScore0To100(entry.risk);

                meta.setEvaluationRationale(entry.rationale);
                meta.setEvaluationMethod("USER_EDIT");
                meta.setConfidenceLevel(1.0);
                meta.setEvaluationTimestamp(nowStr);

                contextTool.saveFunctionalityMetadata(srcFile, meta);
                entry.metadataFresh = true;
            }
        }

        super.okPressed();
    }

    private int parseScore(String str, int defaultScore) {
        try {
            int score = Integer.parseInt(str.trim());
            if (score < 0) return 0;
            if (score > 100) return 100;
            return score;
        } catch (Exception e) {
            return defaultScore >= 0 ? defaultScore : 50;
        }
    }
}
