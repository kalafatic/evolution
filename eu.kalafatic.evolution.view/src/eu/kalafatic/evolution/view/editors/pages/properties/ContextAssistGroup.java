package eu.kalafatic.evolution.view.editors.pages.properties;

import java.util.Map;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.manager.NeuronService;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.NeuronType;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * UI group for Neuron Network Context Assist properties and statistics.
 */
public class ContextAssistGroup extends AEvoGroup {
    private Text neuronAiUrlText, neuronAiModelText;
    private Combo neuronTypeCombo;
    private Label globalStatsLabel, localStatsLabel;
    private Button clearGlobalBtn, clearLocalBtn;

    public ContextAssistGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Neuron Context Assist", 3, true);

        GUIFactory.INSTANCE.createLabel(group, "Neuron AI URL:");
        neuronAiUrlText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, neuronAiUrlText);

        GUIFactory.INSTANCE.createLabel(group, "Neuron Model:");
        neuronAiModelText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, neuronAiModelText);

        GUIFactory.INSTANCE.createLabel(group, "Neuron Type:");
        neuronTypeCombo = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
        for (NeuronType type : NeuronType.VALUES) {
            neuronTypeCombo.add(type.getName());
        }
        neuronTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.adapt(neuronTypeCombo, true, true);
        new Label(group, SWT.NONE); // Filler

        GUIFactory.INSTANCE.createLabel(group, "Global Stats:");
        globalStatsLabel = GUIFactory.INSTANCE.createLabel(group, "N/A");
        globalStatsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        clearGlobalBtn = GUIFactory.INSTANCE.createButton(group, "Clear Global");
        clearGlobalBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                NeuronService.getInstance().clearGlobalMemory();
                refreshUI();
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "Local Stats:");
        localStatsLabel = GUIFactory.INSTANCE.createLabel(group, "N/A");
        localStatsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        clearLocalBtn = GUIFactory.INSTANCE.createButton(group, "Clear Local");
        clearLocalBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                NeuronService.getInstance().clearLocalMemory(orchestrator);
                refreshUI();
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            NeuronAI neuronAI = orchestrator.getNeuronAI();
            if (neuronAI != null) {
                setTextSafe(neuronAiUrlText, neuronAI.getUrl());
                setTextSafe(neuronAiModelText, neuronAI.getModel());
                if (neuronAI.getType() != null) {
                    selectSafe(neuronTypeCombo, neuronAI.getType().getName());
                }
            }

            Map<String, Integer> globalStats = NeuronService.getInstance().getGlobalStats();
            setTextSafe(globalStatsLabel, formatStats(globalStats));

            Map<String, Integer> localStats = NeuronService.getInstance().getLocalStats(orchestrator);
            setTextSafe(localStatsLabel, formatStats(localStats));
        }
    }

    private String formatStats(Map<String, Integer> stats) {
        if (stats == null || stats.isEmpty()) return "Empty";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return sb.toString();
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
            NeuronType type = null;
            int selectionIndex = neuronTypeCombo.getSelectionIndex();
            if (selectionIndex != -1) {
                type = NeuronType.getByName(neuronTypeCombo.getItem(selectionIndex));
            }
            ProjectModelManager.getInstance().updateNeuronAISettings(orchestrator,
                neuronAiUrlText.getText(),
                neuronAiModelText.getText(),
                type);
        }
    }

    @Override
    public Control[] getControls() {
        return new Control[] { neuronAiUrlText, neuronAiModelText, neuronTypeCombo };
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { neuronAiUrlText, neuronAiModelText };
    }
}
