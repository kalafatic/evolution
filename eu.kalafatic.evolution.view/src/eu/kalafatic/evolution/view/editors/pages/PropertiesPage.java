package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import eu.kalafatic.evolution.view.project.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.manager.*;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class PropertiesPage extends ScrolledComposite {
    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private boolean isUpdating = false;
    private Canvas statusCanvas;
    private Text orchIdText, orchNameText, llmModelText, llmTempText, ollamaUrlText, ollamaModelText, ollamaPathText, ollamaVersionText;
    private Table agentsTable;
    private Text gitRepoText, gitBranchText, mavenGoalsText, mavenProfilesText, aiChatUrlText, neuronAiUrlText, compilerSourceText;
    private ControlDecoration ollamaUrlDecorator, ollamaPathDecorator, llmTempDecorator, gitRepoDecorator;
    private OllamaService ollamaService;

    public PropertiesPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        createControl();
    }

    private void createControl() {
        Composite comp = new Composite(this, SWT.NONE);
        comp.setLayout(new GridLayout(1, false));
        Group statusGroup = new Group(comp, SWT.NONE);
        statusGroup.setText("Orchestration Status");
        statusGroup.setLayout(new GridLayout(1, false));
        statusGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        statusCanvas = new Canvas(statusGroup, SWT.DOUBLE_BUFFERED | SWT.BORDER);
        statusCanvas.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)statusCanvas.getLayoutData()).heightHint = 80;
        statusCanvas.addPaintListener(e -> {
            if (orchestrator == null) { e.gc.drawString("No Orchestrator selected", 10, 10); return; }
            String id = orchestrator.getId();
            double progress = OrchestrationStatusManager.getInstance().getProgress(id);
            String status = OrchestrationStatusManager.getInstance().getStatus(id);
            e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            e.gc.fillRectangle(0, 0, statusCanvas.getClientArea().width, statusCanvas.getClientArea().height);
            e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
            e.gc.fillRectangle(10, 30, statusCanvas.getClientArea().width - 20, 20);
            e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
            e.gc.fillRectangle(10, 30, (int) ((statusCanvas.getClientArea().width - 20) * progress), 20);
            e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            e.gc.drawRectangle(10, 30, statusCanvas.getClientArea().width - 20, 20);
            e.gc.drawString("Status: " + status, 10, 60);
            e.gc.drawString("Progress: " + (int) (progress * 100) + "%", 10, 10);
        });
        Runnable timer = new Runnable() { @Override public void run() { if (!statusCanvas.isDisposed()) { statusCanvas.redraw(); Display.getDefault().timerExec(1000, this); } } };
        Display.getDefault().timerExec(1000, timer);

        Group orchGroup = new Group(comp, SWT.NONE);
        orchGroup.setText("Orchestrator");
        orchGroup.setLayout(new GridLayout(3, false));
        orchGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createLabel(orchGroup, "ID:"); orchIdText = new Text(orchGroup, SWT.BORDER); orchIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(orchGroup, orchIdText);
        createLabel(orchGroup, "Name:"); orchNameText = new Text(orchGroup, SWT.BORDER); orchNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(orchGroup, orchNameText);

        Group llmGroup = new Group(comp, SWT.NONE);
        llmGroup.setText("LLM Settings");
        llmGroup.setLayout(new GridLayout(3, false));
        llmGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createLabel(llmGroup, "Model:"); llmModelText = new Text(llmGroup, SWT.BORDER); llmModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(llmGroup, llmModelText);
        createLabel(llmGroup, "Temperature:"); llmTempText = new Text(llmGroup, SWT.BORDER); llmTempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(llmGroup, llmTempText);

        Group ollamaGroup = new Group(comp, SWT.NONE);
        ollamaGroup.setText("Ollama Settings");
        ollamaGroup.setLayout(new GridLayout(3, false));
        ollamaGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createLabel(ollamaGroup,"URL:"); ollamaUrlText = new Text(ollamaGroup, SWT.BORDER); ollamaUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(ollamaGroup, ollamaUrlText);
        createLabel(ollamaGroup, "Model:"); ollamaModelText = new Text(ollamaGroup, SWT.BORDER); ollamaModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(ollamaGroup, ollamaModelText);
        createLabel(ollamaGroup, "Select Model:"); selectModel(ollamaGroup); createLabel(ollamaGroup, "");
        createLabel(ollamaGroup, "Model Path:"); ollamaPathText = new Text(ollamaGroup, SWT.BORDER); ollamaPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Button browseOllamaBtn = new Button(ollamaGroup, SWT.PUSH); browseOllamaBtn.setText("...");
        browseOllamaBtn.addSelectionListener(new SelectionAdapter() { @Override public void widgetSelected(SelectionEvent e) { String p = new org.eclipse.swt.widgets.DirectoryDialog(getShell(), SWT.OPEN).open(); if (p != null) ollamaPathText.setText(p); } });
        createLabel(ollamaGroup, "Version:"); ollamaVersionText = new Text(ollamaGroup, SWT.BORDER | SWT.READ_ONLY); ollamaVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createLabel(ollamaGroup, "");

        Group agentsGroup = new Group(comp, SWT.NONE);
        agentsGroup.setText("Agents");
        agentsGroup.setLayout(new GridLayout(1, false));
        agentsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        ((GridData)agentsGroup.getLayoutData()).heightHint = 150;
        agentsTable = new Table(agentsGroup, SWT.BORDER | SWT.FULL_SELECTION);
        agentsTable.setHeaderVisible(true); agentsTable.setLinesVisible(true);
        agentsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        String[] headers = {"ID", "Type", "Execution Mode"};
        int[] widths = {100, 100, 120};
        for(int i=0; i<headers.length; i++) { TableColumn col = new TableColumn(agentsTable, SWT.NONE); col.setText(headers[i]); col.setWidth(widths[i]); }

        Group gitMavenGroup = new Group(comp, SWT.NONE);
        gitMavenGroup.setText("Git & Maven");
        gitMavenGroup.setLayout(new GridLayout(3, false));
        gitMavenGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createLabel(gitMavenGroup, "Git Repo:"); gitRepoText = new Text(gitMavenGroup, SWT.BORDER); gitRepoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(gitMavenGroup, gitRepoText);
        createLabel(gitMavenGroup, "Git Branch:"); gitBranchText = new Text(gitMavenGroup, SWT.BORDER); gitBranchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(gitMavenGroup, gitBranchText);
        createLabel(gitMavenGroup, "Maven Goals:"); mavenGoalsText = new Text(gitMavenGroup, SWT.BORDER); mavenGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(gitMavenGroup, mavenGoalsText);
        createLabel(gitMavenGroup, "Maven Profiles:"); mavenProfilesText = new Text(gitMavenGroup, SWT.BORDER); mavenProfilesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(gitMavenGroup, mavenProfilesText);

        Group othersGroup = new Group(comp, SWT.NONE);
        othersGroup.setText("Additional AI & Tools");
        othersGroup.setLayout(new GridLayout(3, false));
        othersGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createLabel(othersGroup, "AI Chat URL:"); aiChatUrlText = new Text(othersGroup, SWT.BORDER); aiChatUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(othersGroup, aiChatUrlText);
        createLabel(othersGroup, "Neuron AI URL:"); neuronAiUrlText = new Text(othersGroup, SWT.BORDER); neuronAiUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(othersGroup, neuronAiUrlText);
        createLabel(othersGroup, "Compiler Source:"); compilerSourceText = new Text(othersGroup, SWT.BORDER); compilerSourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); createEditButton(othersGroup, compilerSourceText);

        ollamaUrlDecorator = new ControlDecoration(ollamaUrlText, SWT.TOP | SWT.LEFT);
        ollamaUrlDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        ollamaUrlDecorator.hide();
        ollamaPathDecorator = new ControlDecoration(ollamaPathText, SWT.TOP | SWT.LEFT);
        ollamaPathDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        ollamaPathDecorator.hide();
        llmTempDecorator = new ControlDecoration(llmTempText, SWT.TOP | SWT.LEFT);
        llmTempDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        llmTempDecorator.hide();
        gitRepoDecorator = new ControlDecoration(gitRepoText, SWT.TOP | SWT.LEFT);
        gitRepoDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        gitRepoDecorator.hide();

        ModifyListener ml = e -> { if (orchestrator != null && !isUpdating) { updateModelFromFields(); validateFields(); editor.setDirty(true); } };
        Text[] texts = {orchIdText, orchNameText, llmModelText, llmTempText, ollamaUrlText, ollamaModelText, ollamaPathText, gitRepoText, gitBranchText, mavenGoalsText, mavenProfilesText, aiChatUrlText, neuronAiUrlText, compilerSourceText};
        for(Text t : texts) t.addModifyListener(ml);

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updatePropertiesInfo();
    }

    public void updatePropertiesInfo() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        orchIdText.setText(orchestrator.getId() != null ? orchestrator.getId() : "");
        orchNameText.setText(orchestrator.getName() != null ? orchestrator.getName() : "");
        if (orchestrator.getLlm() != null) {
            llmModelText.setText(orchestrator.getLlm().getModel() != null ? orchestrator.getLlm().getModel() : "");
            llmTempText.setText(String.valueOf(orchestrator.getLlm().getTemperature()));
        }
        if (orchestrator.getOllama() != null) {
            ollamaUrlText.setText(orchestrator.getOllama().getUrl() != null ? orchestrator.getOllama().getUrl() : "");
            ollamaModelText.setText(orchestrator.getOllama().getModel() != null ? orchestrator.getOllama().getModel() : "");
            ollamaPathText.setText(orchestrator.getOllama().getPath() != null ? orchestrator.getOllama().getPath() : "");
            ollamaService = new OllamaService(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel());
            new Thread(() -> {
                String v = (ollamaService != null && ollamaService.ping()) ? ollamaService.getVersion() : "Offline";
                Display.getDefault().asyncExec(() -> { if (!ollamaVersionText.isDisposed()) ollamaVersionText.setText(v); });
            }).start();
        }
        agentsTable.removeAll();
        for (Agent a : orchestrator.getAgents()) {
            TableItem item = new TableItem(agentsTable, SWT.NONE);
            item.setText(0, a.getId() != null ? a.getId() : "");
            item.setText(1, a.getType() != null ? a.getType() : "");
            item.setText(2, a.getExecutionMode() != null ? a.getExecutionMode().name() : "");
        }
        if (orchestrator.getGit() != null) {
            gitRepoText.setText(orchestrator.getGit().getRepositoryUrl() != null ? orchestrator.getGit().getRepositoryUrl() : "");
            gitBranchText.setText(orchestrator.getGit().getBranch() != null ? orchestrator.getGit().getBranch() : "");
        }
        if (orchestrator.getMaven() != null) {
            mavenGoalsText.setText(orchestrator.getMaven().getGoals().toString());
            mavenProfilesText.setText(orchestrator.getMaven().getProfiles().toString());
        }
        if (orchestrator.getAiChat() != null) aiChatUrlText.setText(orchestrator.getAiChat().getUrl() != null ? orchestrator.getAiChat().getUrl() : "");
        if (orchestrator.getNeuronAI() != null) neuronAiUrlText.setText(orchestrator.getNeuronAI().getUrl() != null ? orchestrator.getNeuronAI().getUrl() : "");
        if (orchestrator.getCompiler() != null) compilerSourceText.setText(orchestrator.getCompiler().getSourceVersion() != null ? orchestrator.getCompiler().getSourceVersion() : "");
        isUpdating = false;
    }

    private void validateFields() {
        boolean urlValid = !ollamaUrlText.getText().isEmpty() && ollamaUrlText.getText().startsWith("http");
        if(!urlValid) { ollamaUrlDecorator.setDescriptionText("Invalid Ollama URL"); ollamaUrlDecorator.show(); } else ollamaUrlDecorator.hide();
        File f = new File(ollamaPathText.getText());
        if (!ollamaPathText.getText().isEmpty() && !f.exists()) { ollamaPathDecorator.setDescriptionText("Ollama path does not exist"); ollamaPathDecorator.show(); } else ollamaPathDecorator.hide();
        try { Float.parseFloat(llmTempText.getText()); llmTempDecorator.hide(); } catch (NumberFormatException e) { llmTempDecorator.setDescriptionText("Temperature must be a number"); llmTempDecorator.show(); }
        boolean gitValid = !gitRepoText.getText().isEmpty() && (gitRepoText.getText().startsWith("http") || gitRepoText.getText().startsWith("git@"));
        if(!gitValid) { gitRepoDecorator.setDescriptionText("Invalid Git Repository URL"); gitRepoDecorator.show(); } else gitRepoDecorator.hide();
    }

    private void updateModelFromFields() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        orchestrator.setId(orchIdText.getText());
        orchestrator.setName(orchNameText.getText());
        if (orchestrator.getLlm() != null) {
            orchestrator.getLlm().setModel(llmModelText.getText());
            try { orchestrator.getLlm().setTemperature(Float.parseFloat(llmTempText.getText())); } catch (NumberFormatException e) {}
        }
        if (orchestrator.getOllama() != null) {
            orchestrator.getOllama().setUrl(ollamaUrlText.getText());
            orchestrator.getOllama().setModel(ollamaModelText.getText());
            orchestrator.getOllama().setPath(ollamaPathText.getText());
        }
        if (orchestrator.getGit() != null) {
            orchestrator.getGit().setRepositoryUrl(gitRepoText.getText());
            orchestrator.getGit().setBranch(gitBranchText.getText());
        }
        if (orchestrator.getMaven() != null) {
            orchestrator.getMaven().getGoals().clear();
            for (String goal : mavenGoalsText.getText().replace("[", "").replace("]", "").split("[,\\s]+")) if (!goal.trim().isEmpty()) orchestrator.getMaven().getGoals().add(goal.trim());
            orchestrator.getMaven().getProfiles().clear();
            for (String prof : mavenProfilesText.getText().replace("[", "").replace("]", "").split("[,\\s]+")) if (!prof.trim().isEmpty()) orchestrator.getMaven().getProfiles().add(prof.trim());
        }
        if (orchestrator.getAiChat() != null) orchestrator.getAiChat().setUrl(aiChatUrlText.getText());
        if (orchestrator.getNeuronAI() != null) orchestrator.getNeuronAI().setUrl(neuronAiUrlText.getText());
        if (orchestrator.getCompiler() != null) orchestrator.getCompiler().setSourceVersion(compilerSourceText.getText());
        ollamaService = new OllamaService(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel());
        isUpdating = false;
    }

    private void createLabel(Composite parent, String text) {
        GridData gd = new GridData(); gd.widthHint = 100;
        Label label = new Label(parent, SWT.NONE); label.setLayoutData(gd); label.setText(text);
    }

    private void createEditButton(Composite parent, Text textWidget) {
        GridData gd = new GridData(); gd.widthHint = 100;
        Button btn = new Button(parent, SWT.PUSH); btn.setLayoutData(gd); btn.setText("Edit");
        btn.addSelectionListener(new SelectionAdapter() { @Override public void widgetSelected(SelectionEvent e) { textWidget.setFocus(); textWidget.setSelection(0, textWidget.getText().length()); } });
    }

    private void selectModel(Composite parent) {
        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        List<eu.kalafatic.evolution.controller.manager.OllamaModel> models = ollamaService != null ? ollamaService.loadModels() : new ArrayList<>();
        for (eu.kalafatic.evolution.controller.manager.OllamaModel m : models) combo.add(m.getName());
        combo.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                int idx = combo.getSelectionIndex();
                if (idx >= 0) {
                    eu.kalafatic.evolution.controller.manager.OllamaModel m = models.get(idx);
                    ollamaModelText.setText(m.getName()); llmModelText.setText(m.getName()); llmTempText.setText("0.7");
                    updateModelFromFields(); updatePropertiesInfo();
                }
            }
        });
    }

    public void setOrchestrator(Orchestrator orchestrator) { this.orchestrator = orchestrator; updatePropertiesInfo(); }
}
