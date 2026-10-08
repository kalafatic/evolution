package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;

import org.eclipse.swt.SWT;
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
import eu.kalafatic.evolution.view.editors.pages.context.UserMemoryGroup;

public class ContextPage extends Composite {

    private final Orchestrator orchestrator;
    private final File projectRoot;
    private UserMemoryGroup userMemoryGroup;
    private BestPracticesGroup bestPracticesGroup;
    private NeuronContextGroup neuronContextGroup;
    private MultiPageEditor editor;

    public ContextPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator, File projectRoot) {
        super(parent, SWT.NONE);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.projectRoot = projectRoot;

        setLayout(new GridLayout(1, true));

        userMemoryGroup = new UserMemoryGroup(this, editor, orchestrator, projectRoot);
        bestPracticesGroup = new BestPracticesGroup(this, editor, orchestrator, projectRoot);
        neuronContextGroup = new NeuronContextGroup(this, editor, orchestrator, projectRoot);
    }

    public void refreshUI() {
        if (userMemoryGroup != null) userMemoryGroup.updateUI();
        if (bestPracticesGroup != null) bestPracticesGroup.updateUI();
        if (neuronContextGroup != null) neuronContextGroup.updateUI();
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        if (userMemoryGroup != null) userMemoryGroup.setOrchestrator(orchestrator);
        if (bestPracticesGroup != null) bestPracticesGroup.setOrchestrator(orchestrator);
        if (neuronContextGroup != null) neuronContextGroup.setOrchestrator(orchestrator);
    }

    public void setContext(eu.kalafatic.evolution.controller.memory.MemoryScope scope, String associatedId) {
        if (userMemoryGroup != null) {
            userMemoryGroup.setContext(scope, associatedId);
        }
    }
}
