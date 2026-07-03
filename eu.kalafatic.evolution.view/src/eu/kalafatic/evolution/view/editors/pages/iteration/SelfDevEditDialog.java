package eu.kalafatic.evolution.view.editors.pages.iteration;

import java.util.LinkedHashMap;

import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.view.editors.pages.DevelopmentPage;
import eu.kalafatic.utils.dialogs.DynamicField;
import eu.kalafatic.utils.dialogs.DynamicMapDialog;

public class SelfDevEditDialog extends DynamicMapDialog {
    private SelfDevSession session;
    private DevelopmentPage page;

    private static final String INITIAL_REQUEST = "initialRequest";
    private static final String MAX_ITERATIONS = "maxIterations";
    private static final String RATIONALE = "rationale";
    private static final String SOURCE_PATH = "sourcePath";
    private static final String TARGET_PATH = "targetPath";
    private static final String BRANCH = "branch";
    private static final String LLM_MODEL = "llmModel";
    private static final String MAVEN_GOALS = "mavenGoals";

    public SelfDevEditDialog(Shell parentShell, SelfDevSession session, DevelopmentPage page) {
        super(parentShell, createFields(session));
        this.session = session;
        this.page = page;
        setTitle("Edit Self-Dev Session");
        setContainerWidth(600);
    }

    private static LinkedHashMap<String, DynamicField> createFields(SelfDevSession session) {
        LinkedHashMap<String, DynamicField> fields = new LinkedHashMap<>();
        fields.put(INITIAL_REQUEST, new DynamicField("Initial Request:", DynamicField.TYPE_TEXT | DynamicField.MULTILINE, session.getInitialRequest() != null ? session.getInitialRequest() : ""));
        fields.put(MAX_ITERATIONS, new DynamicField("Max Iterations:", DynamicField.TYPE_NUMBER, String.valueOf(session.getMaxIterations())));
        fields.put(RATIONALE, new DynamicField("Rationale:", DynamicField.TYPE_TEXT | DynamicField.MULTILINE, session.getRationale() != null ? session.getRationale() : ""));

        String src = "";
        String tgt = "";
        String br = "evo/self-dev";
        String llm = "llama3";
        String mvn = "clean install";

        if (session.eContainer() instanceof eu.kalafatic.evolution.model.orchestration.Orchestrator orch) {
            if (orch.getSupervisorSettings() != null) {
                src = orch.getSupervisorSettings().getSourcePath();
                tgt = orch.getSupervisorSettings().getExecutablePath();
            }
            if (orch.getGit() != null) {
                br = orch.getGit().getBranchName();
            }
            if (orch.getLlm() != null) {
                llm = orch.getLlm().getModel();
            }
            if (orch.getMaven() != null) {
                mvn = String.join(" ", orch.getMaven().getGoals());
            }
        }

        fields.put(SOURCE_PATH, new DynamicField("Source Path:", DynamicField.TYPE_TEXT | DynamicField.DIRECTORY, src));
        fields.put(TARGET_PATH, new DynamicField("Target Path (Sandbox):", DynamicField.TYPE_TEXT | DynamicField.DIRECTORY, tgt));
        fields.put(BRANCH, new DynamicField("Git Branch:", DynamicField.TYPE_TEXT, br));
        fields.put(LLM_MODEL, new DynamicField("LLM Model:", DynamicField.TYPE_TEXT, llm));
        fields.put(MAVEN_GOALS, new DynamicField("Maven Goals:", DynamicField.TYPE_TEXT, mvn));

        return fields;
    }

    @Override
    protected void okPressed() {
        if (!validate()) return;

        saveValues();

        session.setInitialRequest(getString(INITIAL_REQUEST));
        session.setMaxIterations(getInteger(MAX_ITERATIONS));
        session.setRationale(getString(RATIONALE));

        if (session.eContainer() instanceof eu.kalafatic.evolution.model.orchestration.Orchestrator orch) {
            if (orch.getSupervisorSettings() == null) {
                orch.setSupervisorSettings(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createSupervisorSettings());
            }
            orch.getSupervisorSettings().setSourcePath(getString(SOURCE_PATH));
            orch.getSupervisorSettings().setExecutablePath(getString(TARGET_PATH));

            if (orch.getGit() == null) {
                orch.setGit(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createGit());
            }
            orch.getGit().setBranchName(getString(BRANCH));

            if (orch.getLlm() == null) {
                orch.setLlm(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createLLM());
            }
            orch.getLlm().setModel(getString(LLM_MODEL));

            if (orch.getMaven() == null) {
                orch.setMaven(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createMaven());
            }
            orch.getMaven().getGoals().clear();
            for (String g : getString(MAVEN_GOALS).split(" ")) if (!g.trim().isEmpty()) orch.getMaven().getGoals().add(g.trim());
        }

        page.getEditor().setDirty(true);
        super.okPressed();
    }
}
