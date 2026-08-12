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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.OllamaConfigManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
import eu.kalafatic.evolution.view.editors.pages.OllamaViewModel;
import eu.kalafatic.utils.factories.GUIFactory;
import eu.kalafatic.evolution.view.factories.SWTBinding;

public class OllamaSettingsGroup extends AEvoGroup {
	
	OllamaConfigManager.OllamaDefaults defaults = OllamaConfigManager.getDefaults();
	
    private Text ollamaUrlText, ollamaModelText, ollamaPathText, ollamaVersionText;
    private PropertiesPage page;
    private ControlDecoration ollamaUrlDecorator, ollamaPathDecorator, ollamaModelDecorator;
    private Combo modelCombo;
    private OllamaViewModel viewModel;
    private Text terminalCommandText;
    private Text terminalOutputText;

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
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Ollama Settings", 3, false);
        GUIFactory.INSTANCE.createLabel(group, "URL:");
        ollamaUrlText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, ollamaUrlText);

        GUIFactory.INSTANCE.createLabel(group, "Model:");
        ollamaModelText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, ollamaModelText);

        GUIFactory.INSTANCE.createLabel(group, "Select Model:");
        modelCombo = GUIFactory.INSTANCE.createCombo(group);

        GUIFactory.INSTANCE.createLabel(group, "");
        GUIFactory.INSTANCE.createLabel(group, "Model Path:");
        ollamaPathText = GUIFactory.INSTANCE.createText(group);
        
        Button browseOllamaBtn = GUIFactory.INSTANCE.createButton(group, "...");
        browseOllamaBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                org.eclipse.swt.widgets.DirectoryDialog dialog = new org.eclipse.swt.widgets.DirectoryDialog(group.getShell(), SWT.OPEN);
                dialog.setFilterPath(ollamaPathText.getText());
                String p = dialog.open();
                if (p != null) {
                    ollamaPathText.setText(p);
                    if (viewModel != null) viewModel.path.setValue(p);
                }
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "Version:");
        ollamaVersionText = GUIFactory.INSTANCE.createText(group);
        ollamaVersionText.setEditable(false);
        GUIFactory.INSTANCE.createLabel(group, "");

        // Parameters Row
        GUIFactory.INSTANCE.createLabel(group, "Parameters:");
        terminalCommandText = GUIFactory.INSTANCE.createText(group);
        String defaultCmd = System.getProperty("os.name").toLowerCase().contains("win")
            ? "netstat -ano | findstr :11434"
            : "netstat -ano | grep 11434";
        terminalCommandText.setText(defaultCmd);

        Button executeBtn = GUIFactory.INSTANCE.createButton(group, "Execute");
        executeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String command = terminalCommandText.getText().trim();
                if (command.isEmpty()) return;

                executeBtn.setEnabled(false);
                terminalOutputText.setText("Running command: " + command + "...\n");

                org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Terminal Control: " + command) {
                    @Override
                    protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                        StringBuilder output = new StringBuilder();
                        try {
                            String os = System.getProperty("os.name").toLowerCase();
                            ProcessBuilder pb;
                            if (os.contains("win")) {
                                pb = new ProcessBuilder("cmd.exe", "/c", command);
                            } else {
                                pb = new ProcessBuilder("sh", "-c", command);
                            }
                            pb.redirectErrorStream(true);
                            Process process = pb.start();

                            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    output.append(line).append("\n");
                                }
                            }

                            process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                        } catch (Exception ex) {
                            output.append("Error executing command: ").append(ex.getMessage()).append("\n");
                        }

                        Display.getDefault().asyncExec(() -> {
                            if (!terminalOutputText.isDisposed()) {
                                terminalOutputText.setText(output.toString());
                            }
                            if (!executeBtn.isDisposed()) {
                                executeBtn.setEnabled(true);
                            }
                        });
                        return org.eclipse.core.runtime.Status.OK_STATUS;
                    }
                };
                job.schedule();
            }
        });

        // Output Row
        GUIFactory.INSTANCE.createLabel(group, "Output:");
        terminalOutputText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        terminalOutputText.setEditable(false);
        GridData outputGd = new GridData(GridData.FILL_HORIZONTAL);
        outputGd.heightHint = 80;
        terminalOutputText.setLayoutData(outputGd);

        Button copyBtn = GUIFactory.INSTANCE.createButton(group, "Copy");
        copyBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String text = terminalOutputText.getText();
                if (text == null || text.isEmpty()) return;
                org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(group.getDisplay());
                cb.setContents(new Object[] { text }, new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
                cb.dispose();
            }
        });

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
