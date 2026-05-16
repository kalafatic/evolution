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
        return fields;
    }

    @Override
    protected void okPressed() {
        if (!validate()) return;

        saveValues();

        session.setInitialRequest(getString(INITIAL_REQUEST));
        session.setMaxIterations(getInteger(MAX_ITERATIONS));
        session.setRationale(getString(RATIONALE));

        page.setDirty(true);
        super.okPressed();
    }
}
