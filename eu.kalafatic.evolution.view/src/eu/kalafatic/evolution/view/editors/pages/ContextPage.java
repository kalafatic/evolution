package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.List;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.controller.orchestration.BestPracticesService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.NeuronContextService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IFileEditorInput;

/**
 * Page for managing Best Practices and Neuron Context.
 */
public class ContextPage extends Composite {

    private final MultiPageEditor editor;
    private final Orchestrator orchestrator;
    private BestPracticesService bpService;
    private NeuronContextService ncService;
    private IterationMemoryService imService;

    private List learnedPatternsList;
    private Label lastUpdatedLabel;
    private Text bpArchitectText;
    private Text bpPlannerText;
    private Text bpAgentText;
    private Text bpToolsText;

    public ContextPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.editor = editor;
        this.orchestrator = orchestrator;

        File projectRoot = getProjectRoot();
        if (projectRoot != null) {
            this.bpService = new BestPracticesService(projectRoot);
            this.ncService = new NeuronContextService(projectRoot);
            this.imService = new IterationMemoryService(projectRoot);
        }

        setLayout(new GridLayout(2, true));
        createBestPracticesGroup();
        createNeuronContextGroup();

        refreshUI();
    }

    private void createBestPracticesGroup() {
        Composite group = new Composite(this, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        new Label(group, SWT.NONE).setText("Best Practices (Markdown Files Sync)");

        new Label(group, SWT.NONE).setText("Architect:");
        bpArchitectText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        bpArchitectText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        bpArchitectText.setEditable(false);

        new Label(group, SWT.NONE).setText("Planner:");
        bpPlannerText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        bpPlannerText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        bpPlannerText.setEditable(false);

        new Label(group, SWT.NONE).setText("Agent:");
        bpAgentText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        bpAgentText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        bpAgentText.setEditable(false);

        new Label(group, SWT.NONE).setText("Tools:");
        bpToolsText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        bpToolsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        bpToolsText.setEditable(false);

        Button syncButton = new Button(group, SWT.PUSH);
        syncButton.setText("Sync from Filesystem");
        syncButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (bpService != null) {
                    bpService.reload();
                    refreshUI();
                }
            }
        });
    }

    private void createNeuronContextGroup() {
        Composite group = new Composite(this, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        new Label(group, SWT.NONE).setText("Neuron Context Model (Learned User Behavior)");

        lastUpdatedLabel = new Label(group, SWT.NONE);
        lastUpdatedLabel.setText("Last Updated: Never");

        learnedPatternsList = new List(group, SWT.BORDER | SWT.V_SCROLL);
        learnedPatternsList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Button learnButton = new Button(group, SWT.PUSH);
        learnButton.setText("Learn from History");
        learnButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (ncService != null && imService != null) {
                    ncService.learn(imService.getRecords());
                    refreshUI();
                }
            }
        });

        Composite btnBox = new Composite(group, SWT.NONE);
        btnBox.setLayout(new GridLayout(2, true));

        Button loadButton = new Button(btnBox, SWT.PUSH);
        loadButton.setText("Load Context");
        loadButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (ncService != null) {
                    ncService.load();
                    refreshUI();
                }
            }
        });

        Button saveButton = new Button(btnBox, SWT.PUSH);
        saveButton.setText("Save Context");
        saveButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (ncService != null) {
                    ncService.save();
                }
            }
        });
    }

    public void refreshUI() {
        if (bpService != null) {
            bpArchitectText.setText(bpService.getPractices("architect"));
            bpPlannerText.setText(bpService.getPractices("planner"));
            bpAgentText.setText(bpService.getPractices("agent"));
            bpToolsText.setText(bpService.getPractices("tools"));
        }

        if (ncService != null) {
            learnedPatternsList.removeAll();
            for (String pattern : ncService.getModel().getLearnedPatterns()) {
                learnedPatternsList.add(pattern);
            }
            long lastUpdated = ncService.getModel().getLastUpdated();
            if (lastUpdated > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                lastUpdatedLabel.setText("Last Updated: " + sdf.format(new Date(lastUpdated)));
            }
        }
        layout();
    }

    private File getProjectRoot() {
        if (editor.getEditorInput() instanceof IFileEditorInput) {
            IProject project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
            return project.getLocation().toFile();
        }
        return null;
    }
}
