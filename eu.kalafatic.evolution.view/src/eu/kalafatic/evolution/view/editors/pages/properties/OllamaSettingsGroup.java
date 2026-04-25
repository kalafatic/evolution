package eu.kalafatic.evolution.view.editors.pages.properties;

import java.io.File;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.OllamaConfigManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
import eu.kalafatic.evolution.view.editors.pages.OllamaViewModel;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import eu.kalafatic.evolution.view.factories.SWTBinding;

public class OllamaSettingsGroup extends AEvoGroup {
	
	OllamaConfigManager.OllamaDefaults defaults = OllamaConfigManager.getDefaults();
	
    private Text ollamaUrlText, ollamaModelText, ollamaPathText, ollamaVersionText;
    private PropertiesPage page;
    private ControlDecoration ollamaUrlDecorator, ollamaPathDecorator, ollamaModelDecorator;
    private Combo modelCombo;
    private OllamaViewModel viewModel;

    public OllamaSettingsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, PropertiesPage page) {
        super(editor, orchestrator);
        this.page = page;
        if (orchestrator.getOllama() != null) {
            this.viewModel = new OllamaViewModel(orchestrator.getOllama());
        }
        createControl(toolkit, parent);

        group.addDisposeListener(e -> {
            if (viewModel != null) viewModel.dispose();
        });
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Ollama Settings", 3, false);
        SWTFactory.createLabel(group, "URL:");
        ollamaUrlText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, ollamaUrlText);

        SWTFactory.createLabel(group, "Model:");
        ollamaModelText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, ollamaModelText);

        SWTFactory.createLabel(group, "Select Model:");
        modelCombo = SWTFactory.createCombo(group);

        SWTFactory.createLabel(group, "");
        SWTFactory.createLabel(group, "Model Path:");
        ollamaPathText = SWTFactory.createText(group);
        
        Button browseOllamaBtn = SWTFactory.createButton(group, "...");
        browseOllamaBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String p = new org.eclipse.swt.widgets.DirectoryDialog(group.getShell(), SWT.OPEN).open();
                if (p != null) {
                    ollamaPathText.setText(p);
                    if (viewModel != null) viewModel.path.setValue(p);
                }
            }
        });

        SWTFactory.createLabel(group, "Version:");
        ollamaVersionText = SWTFactory.createText(group);
        ollamaVersionText.setEditable(false);
        SWTFactory.createLabel(group, "");

        ollamaUrlDecorator = new ControlDecoration(ollamaUrlText, SWT.TOP | SWT.LEFT);
        ollamaUrlDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        ollamaUrlDecorator.hide();

        ollamaPathDecorator = new ControlDecoration(ollamaPathText, SWT.TOP | SWT.LEFT);
        ollamaPathDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        ollamaPathDecorator.hide();

        ollamaModelDecorator = new ControlDecoration(ollamaModelText, SWT.TOP | SWT.LEFT);
        ollamaModelDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        ollamaModelDecorator.hide();

        if (viewModel != null) {
            SWTBinding.bindText(ollamaUrlText, viewModel.url);
            SWTBinding.bindText(ollamaModelText, viewModel.modelName);
            SWTBinding.bindText(ollamaPathText, viewModel.path);
            SWTBinding.bindText(ollamaVersionText, viewModel.version);
            SWTBinding.bindCombo(modelCombo, viewModel.modelName);
            SWTBinding.bindComboItems(modelCombo, viewModel.availableModels);

            viewModel.isReachable.addChangeListener(e -> {
                boolean reachable = (Boolean) e.getNewValue();
                Display.getDefault().asyncExec(() -> {
                    if (group.isDisposed()) return;
                    if (reachable) {
                        ollamaUrlDecorator.hide();
                    } else {
                        ollamaUrlDecorator.setDescriptionText("Ollama server offline");
                        ollamaUrlDecorator.show();
                    }
                });
            });

            viewModel.availableModels.addChangeListener(e -> {
                Display.getDefault().asyncExec(() -> {
                    if (group.isDisposed()) return;
                    String model = viewModel.modelName.getValue();
                    boolean modelFound = viewModel.availableModels.getList().contains(model);
                    if (model != null && !model.isEmpty() && !modelFound && viewModel.isReachable.getValue()) {
                        ollamaModelDecorator.setDescriptionText("Model not found in Ollama");
                        ollamaModelDecorator.show();
                    } else {
                        ollamaModelDecorator.hide();
                    }
                });
            });

            viewModel.path.addChangeListener(e -> {
               Display.getDefault().asyncExec(() -> {
                   if (group.isDisposed()) return;
                   File f = new File(ollamaPathText.getText());
                   if (!ollamaPathText.getText().isEmpty() && !f.exists()) {
                       ollamaPathDecorator.setDescriptionText("Ollama path does not exist");
                       ollamaPathDecorator.show();
                   } else {
                       ollamaPathDecorator.hide();
                   }
               });
            });
        }
    }

    @Override
    protected void refreshUI() {
        // Handled by bindings
    }

    @Override
    public void updateModel() {
        // Handled by bindings
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { ollamaUrlText, ollamaModelText, ollamaPathText };
    }
}
