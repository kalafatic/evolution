package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.context.BestPracticesGroup;
import eu.kalafatic.evolution.view.editors.pages.context.NeuronContextGroup;

public class ContextPage extends FormPage {

    private final Orchestrator orchestrator;
    private final File projectRoot;
    private BestPracticesGroup bestPracticesGroup;
    private NeuronContextGroup neuronContextGroup;

    public ContextPage(FormEditor editor, Orchestrator orchestrator, File projectRoot) {
        super(editor, "context", "Context & Behavior");
        this.orchestrator = orchestrator;
        this.projectRoot = projectRoot;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText("Instruction Context & Neural Learning");
        Composite body = form.getBody();
        body.setLayout(new GridLayout(1, true));

        MultiPageEditor editor = (MultiPageEditor) getEditor();
        bestPracticesGroup = new BestPracticesGroup(body, editor, orchestrator, projectRoot);
        neuronContextGroup = new NeuronContextGroup(body, editor, orchestrator, projectRoot);
    }

    public void refreshUI() {
        if (bestPracticesGroup != null) bestPracticesGroup.updateUI();
        if (neuronContextGroup != null) neuronContextGroup.updateUI();
    }
}
