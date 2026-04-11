package eu.kalafatic.evolution.view.editors.pages.context;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Group;

import eu.kalafatic.evolution.controller.services.BestPracticesService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class BestPracticesGroup extends AEvoGroup {

    private final File projectRoot;

    public BestPracticesGroup(Composite parent, MultiPageEditor editor, Orchestrator orchestrator, File projectRoot) {
        super(editor, orchestrator);
        this.projectRoot = projectRoot;

        Group g = new Group(parent, SWT.NONE);
        g.setText("Best Practices Management");
        g.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        this.group = g;
        createContent(g);
    }

    private void createContent(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        Button syncBtn = new Button(parent, SWT.PUSH);
        syncBtn.setText("Initialize Defaults / Sync from Filesystem");
        syncBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                new BestPracticesService(orchestrator, projectRoot);
            }
        });

        Label hint = new Label(parent, SWT.WRAP);
        hint.setText("Markdown files in 'orchestrator/best_practices/' subdirectories will be automatically injected into agent prompts.");
        hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    @Override
    protected void refreshUI() {
    }
}
