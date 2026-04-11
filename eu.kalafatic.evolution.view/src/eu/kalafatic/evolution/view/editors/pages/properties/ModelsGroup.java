package eu.kalafatic.evolution.view.editors.pages.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.orchestration.ShellTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ModelsGroup extends AEvoGroup {

    private TableViewer tableViewer;
    private List<ModelItem> modelItems = new ArrayList<>();
    private Color lightGreen;

    public enum ModelState { OK, ERR, NA }

    public static class ModelItem {
        public ModelState state;
        public String name;
        public boolean local;
        public String pathOrUrl;

        public ModelItem(ModelState state, String name, boolean local, String pathOrUrl) {
            this.state = state;
            this.name = name;
            this.local = local;
            this.pathOrUrl = pathOrUrl;
        }
    }

    public ModelsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        this.lightGreen = new Color(Display.getDefault(), 220, 255, 220);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Models", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)group.getLayoutData()).heightHint = 250;

        tableViewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        createColumns();

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        Composite buttonBar = toolkit.createComposite(group);
        buttonBar.setLayout(new GridLayout(3, false));
        buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        Button addButton = toolkit.createButton(buttonBar, "Add", SWT.PUSH);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddModel();
            }
        });

        Button editButton = toolkit.createButton(buttonBar, "Edit", SWT.PUSH);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleEditModel();
            }
        });

        Button removeButton = toolkit.createButton(buttonBar, "Remove", SWT.PUSH);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemoveModel();
            }
        });

        Button reloadButton = toolkit.createButton(buttonBar, "Reload", SWT.PUSH);
        reloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshUI();
            }
        });

        group.addDisposeListener(e -> {
            if (lightGreen != null && !lightGreen.isDisposed()) lightGreen.dispose();
        });
    }

    private void createColumns() {
        String[] titles = { "State", "Name", "Local", "Path/URL" };
        int[] bounds = { 60, 150, 60, 300 };

        // State
        TableViewerColumn colState = createTableViewerColumn(titles[0], bounds[0]);
        colState.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelItem) element).state.toString();
            }
            @Override
            public Color getBackground(Object element) {
                if (((ModelItem) element).state == ModelState.OK) {
                    return lightGreen;
                }
                return null;
            }
        });

        // Name
        TableViewerColumn colName = createTableViewerColumn(titles[1], bounds[1]);
        colName.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelItem) element).name;
            }
            @Override
            public Color getBackground(Object element) {
                if (((ModelItem) element).state == ModelState.OK) {
                    return lightGreen;
                }
                return null;
            }
        });

        // Local
        TableViewerColumn colLocal = createTableViewerColumn(titles[2], bounds[2]);
        colLocal.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return String.valueOf(((ModelItem) element).local);
            }
            @Override
            public Color getBackground(Object element) {
                if (((ModelItem) element).state == ModelState.OK) {
                    return lightGreen;
                }
                return null;
            }
        });

        // Path/URL
        TableViewerColumn colPath = createTableViewerColumn(titles[3], bounds[3]);
        colPath.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelItem) element).pathOrUrl;
            }
            @Override
            public Color getBackground(Object element) {
                if (((ModelItem) element).state == ModelState.OK) {
                    return lightGreen;
                }
                return null;
            }
        });
    }

    private TableViewerColumn createTableViewerColumn(String title, int bound) {
        TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        viewerColumn.getColumn().setText(title);
        viewerColumn.getColumn().setWidth(bound);
        viewerColumn.getColumn().setResizable(true);
        viewerColumn.getColumn().setMoveable(true);
        return viewerColumn;
    }

    private void handleAddModel() {
        String[] options = { "Local (Ollama)", "Remote Provider" };
        MessageDialog dialog = new MessageDialog(group.getShell(), "Add Model", null,
                "Select the type of model to add:", MessageDialog.QUESTION, options, 0);
        int result = dialog.open();
        if (result == 0) { // Local
            InputDialog input = new InputDialog(group.getShell(), "Add Local Model",
                    "Enter the name of the model to run (e.g., gemma, llama3):", "", null);
            if (input.open() == InputDialog.OK) {
                String modelName = input.getValue();
                if (modelName != null && !modelName.trim().isEmpty()) {
                    runTerminalCommand("ollama run " + modelName.trim());
                }
            }
        } else if (result == 1) { // Remote
            InputDialog input = new InputDialog(group.getShell(), "Add Provider",
                    "Enter provider name (e.g. custom-openai):", "", null);
            if (input.open() == InputDialog.OK) {
                String providerName = input.getValue();
                if (providerName != null && !providerName.trim().isEmpty()) {
                    InputDialog urlInput = new InputDialog(group.getShell(), "Provider URL",
                            "Enter the API URL:", "", null);
                    if (urlInput.open() == InputDialog.OK) {
                        String url = urlInput.getValue();
                        InputDialog tokenInput = new InputDialog(group.getShell(), "API Token",
                                "Enter the API Token:", "", null);
                        if (tokenInput.open() == InputDialog.OK) {
                            String token = tokenInput.getValue();

                            AIProvider provider = OrchestrationFactory.eINSTANCE.createAIProvider();
                            provider.setName(providerName.trim());
                            provider.setUrl(url);
                            provider.setApiKey(token);
                            provider.setFormat("openai"); // Default
                            provider.setLocal(false);

                            orchestrator.getAiProviders().add(provider);
                            editor.setDirty(true);
                            refreshUI();
                        }
                    }
                }
            }
        }
    }

    private void handleEditModel() {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        if (selection.isEmpty()) return;
        ModelItem item = (ModelItem) selection.getFirstElement();
        if (item.local) {
             MessageDialog.openInformation(group.getShell(), "Edit", "Local models cannot be edited here.");
             return;
        }

        AIProvider provider = orchestrator.getAiProviders().stream()
                .filter(p -> p.getName().equalsIgnoreCase(item.name))
                .findFirst().orElse(null);

        if (provider == null) return;

        InputDialog urlInput = new InputDialog(group.getShell(), "Edit Provider URL",
                "Update the API URL for " + provider.getName() + ":", provider.getUrl(), null);
        if (urlInput.open() == InputDialog.OK) {
            provider.setUrl(urlInput.getValue());
            InputDialog tokenInput = new InputDialog(group.getShell(), "Edit API Token",
                    "Update the API Token for " + provider.getName() + ":", provider.getApiKey(), null);
            if (tokenInput.open() == InputDialog.OK) {
                provider.setApiKey(tokenInput.getValue());
                editor.setDirty(true);
                refreshUI();
            }
        }
    }

    private void handleRemoveModel() {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        if (selection.isEmpty()) return;
        ModelItem item = (ModelItem) selection.getFirstElement();

        if (item.local) {
            if (MessageDialog.openConfirm(group.getShell(), "Remove Local Model",
                    "Are you sure you want to remove the local model: " + item.name + "?")) {
                runTerminalCommand("ollama rm " + item.name);
            }
        } else {
            if (MessageDialog.openConfirm(group.getShell(), "Remove Provider",
                    "Are you sure you want to remove the provider: " + item.name + "?")) {
                AIProvider provider = orchestrator.getAiProviders().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(item.name))
                        .findFirst().orElse(null);
                if (provider != null) {
                    orchestrator.getAiProviders().remove(provider);
                    if (item.name.equalsIgnoreCase(orchestrator.getRemoteModel())) {
                        orchestrator.setRemoteModel("");
                    }
                    editor.setDirty(true);
                    refreshUI();
                }
            }
        }
    }

    private void runTerminalCommand(String command) {
        new Thread(() -> {
            try {
                ShellTool shell = new ShellTool();
                File workingDir = null;
                if (editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
                    workingDir = ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput()).getFile().getProject()
                            .getLocation().toFile();
                }
                String output = shell.execute(command, workingDir, null);
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openInformation(group.getShell(), "Terminal Output", output);
                    refreshUI();
                });
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openError(group.getShell(), "Command Error", e.getMessage());
                });
            }
        }).start();
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null) return;
        AiProviders.initializeProviders(orchestrator);

        final List<ModelItem> newItems = new ArrayList<>();

        // Load Remote Models from Ecore model
        for (AIProvider provider : orchestrator.getAiProviders()) {
            ModelState state = ModelState.NA;

            // For remote models, "ok" if we have a valid-looking token
            String token = orchestrator.getOpenAiToken();
            boolean isCurrentProvider = provider.getName().equalsIgnoreCase(orchestrator.getRemoteModel());

            if (isCurrentProvider && token != null && !token.isEmpty() && !token.equals("YOUR_API_KEY")) {
                state = ModelState.OK;
            } else if (provider.getApiKey() != null && !provider.getApiKey().isEmpty() && !provider.getApiKey().equals("YOUR_API_KEY")) {
                 state = ModelState.OK;
            }

            newItems.add(new ModelItem(state, provider.getName(), provider.isLocal(), provider.getUrl()));
        }

        this.modelItems = newItems;
        tableViewer.setInput(modelItems);

        // Load Local Models (asynchronously)
        String ollamaUrl = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
        OllamaService ollamaService = new OllamaService(ollamaUrl, null);

        CompletableFuture.runAsync(() -> {
            try {
                List<OllamaModel> localModels = ollamaService.loadModels();
                List<ModelItem> localItems = new ArrayList<>();
                for (OllamaModel m : localModels) {
                    localItems.add(new ModelItem(ModelState.OK, m.getName(), true, ollamaUrl));
                }

                Display.getDefault().asyncExec(() -> {
                    if (tableViewer.getTable().isDisposed()) return;
                    // Check if we are still dealing with the same list to avoid race conditions
                    if (this.modelItems == newItems) {
                        newItems.addAll(localItems);
                        tableViewer.refresh();
                    }
                });
            } catch (Exception e) {
                // If it fails, maybe Ollama is offline
                Display.getDefault().asyncExec(() -> {
                    if (tableViewer.getTable().isDisposed()) return;
                    if (this.modelItems == newItems) {
                        newItems.add(new ModelItem(ModelState.ERR, "Ollama", true, ollamaUrl));
                        tableViewer.refresh();
                    }
                });
            }
        });
    }
}
