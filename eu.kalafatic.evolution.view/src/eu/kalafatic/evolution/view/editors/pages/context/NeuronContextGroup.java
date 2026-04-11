package eu.kalafatic.evolution.view.editors.pages.context;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Group;

import eu.kalafatic.evolution.controller.services.NeuronContextService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class NeuronContextGroup extends AEvoGroup {

    private final NeuronContextService service;
    private Text contextPreview;

    public NeuronContextGroup(Composite parent, MultiPageEditor editor, Orchestrator orchestrator, File projectRoot) {
        super(editor, orchestrator);
        this.service = new NeuronContextService(orchestrator, projectRoot);

        Group g = new Group(parent, SWT.NONE);
        g.setText("Neuron Context (Learned Behavior)");
        g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.group = g;
        createContent(g);
    }

    private void createContent(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        Button learnBtn = new Button(parent, SWT.PUSH);
        learnBtn.setText("Learn from History (Analyze Iterations)");
        learnBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                service.learnFromHistory();
                updateUI();
            }
        });

        new Label(parent, SWT.NONE).setText("Current Learned Insights:");
        contextPreview = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 150;
        contextPreview.setLayoutData(gd);

        refreshUI();
    }

    @Override
    protected void refreshUI() {
        if (contextPreview != null && !contextPreview.isDisposed()) {
            contextPreview.setText(service.getLearnedContext());
        }
    }
}
