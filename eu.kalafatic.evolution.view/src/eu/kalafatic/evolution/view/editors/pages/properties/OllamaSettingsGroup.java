package eu.kalafatic.evolution.view.editors.pages.properties;

import java.io.File;
import java.util.List;
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
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class OllamaSettingsGroup extends AEvoGroup {
    private Text ollamaUrlText, ollamaModelText, ollamaPathText, ollamaVersionText;
    private PropertiesPage page;
    private ControlDecoration ollamaUrlDecorator, ollamaPathDecorator;
    private OllamaService ollamaService;
    private Combo modelCombo;

    public OllamaSettingsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, PropertiesPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
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
        modelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = modelCombo.getSelectionIndex();
                if (idx >= 0 && ollamaService != null) {
                    List<OllamaModel> models = ollamaService.loadModels();
                    if (idx < models.size()) {
                        ollamaModelText.setText(models.get(idx).getName());
                        page.syncModelWithUI();
                    }
                }
            }
        });

        SWTFactory.createLabel(group, "");
        SWTFactory.createLabel(group, "Model Path:");
        ollamaPathText = SWTFactory.createText(group);
        Button browseOllamaBtn = SWTFactory.createButton(group, "...");
        browseOllamaBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String p = new org.eclipse.swt.widgets.DirectoryDialog(group.getShell(), SWT.OPEN).open();
                if (p != null) ollamaPathText.setText(p);
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
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null && orchestrator.getOllama() != null) {
            ollamaUrlText.setText(orchestrator.getOllama().getUrl() != null ? orchestrator.getOllama().getUrl() : "");
            ollamaModelText.setText(orchestrator.getOllama().getModel() != null ? orchestrator.getOllama().getModel() : "");
            ollamaPathText.setText(orchestrator.getOllama().getPath() != null ? orchestrator.getOllama().getPath() : "");

            ollamaService = new OllamaService(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel());

            // Populate combo
            modelCombo.removeAll();
            new Thread(() -> {
                if (ollamaService.ping()) {
                    List<OllamaModel> models = ollamaService.loadModels();
                    Display.getDefault().asyncExec(() -> {
                        if (!modelCombo.isDisposed()) {
                            for (OllamaModel m : models) modelCombo.add(m.getName());
                        }
                    });
                }
            }).start();

            new Thread(() -> {
                String v = (ollamaService != null && ollamaService.ping()) ? ollamaService.getVersion() : "Offline";
                Display.getDefault().asyncExec(() -> {
                    if (!ollamaVersionText.isDisposed()) ollamaVersionText.setText(v);
                });
            }).start();
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
            if (orchestrator.getOllama() == null) {
                orchestrator.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
            }
            orchestrator.getOllama().setUrl(ollamaUrlText.getText());
            orchestrator.getOllama().setModel(ollamaModelText.getText());
            orchestrator.getOllama().setPath(ollamaPathText.getText());

            boolean urlValid = !ollamaUrlText.getText().isEmpty() && ollamaUrlText.getText().startsWith("http");
            if (!urlValid) { ollamaUrlDecorator.setDescriptionText("Invalid Ollama URL"); ollamaUrlDecorator.show(); }
            else ollamaUrlDecorator.hide();

            File f = new File(ollamaPathText.getText());
            if (!ollamaPathText.getText().isEmpty() && !f.exists()) { ollamaPathDecorator.setDescriptionText("Ollama path does not exist"); ollamaPathDecorator.show(); }
            else ollamaPathDecorator.hide();
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { ollamaUrlText, ollamaModelText, ollamaPathText };
    }
}
