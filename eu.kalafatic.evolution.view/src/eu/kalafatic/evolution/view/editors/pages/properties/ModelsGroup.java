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
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
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
        public String stateDescription;
        public String name;
        public boolean local;
        public String pathOrUrl;
        public String token;
        public int rating;
        public eu.kalafatic.evolution.model.orchestration.AIProvider provider;

        public ModelItem(ModelState state, String name, boolean local, String pathOrUrl, String token) {
            this.state = state;
            this.name = name;
            this.local = local;
            this.pathOrUrl = pathOrUrl;
            this.token = token;
        }
    }

    private Color lightRed, lightOrange;

    public ModelsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        this.lightGreen = new Color(Display.getDefault(), 220, 255, 220);
        this.lightRed = new Color(Display.getDefault(), 255, 220, 220);
        this.lightOrange = new Color(Display.getDefault(), 255, 240, 200);
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
        buttonBar.setLayout(new GridLayout(5, false));
        buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        Button addButton = toolkit.createButton(buttonBar, "Add", SWT.PUSH);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddModel();
            }
        });

        Button downloadButton = toolkit.createButton(buttonBar, "Download", SWT.PUSH);
        downloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDownloadModel();
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

        Button saveButton = toolkit.createButton(buttonBar, "Save to Model", SWT.PUSH);
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editor.doSave(null);
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
            if (lightRed != null && !lightRed.isDisposed()) lightRed.dispose();
            if (lightOrange != null && !lightOrange.isDisposed()) lightOrange.dispose();
        });
    }

    private void createColumns() {
        String[] titles = { "State", "Name", "Local", "Path/URL", "Token", "Rating" };
        int[] bounds = { 60, 150, 60, 250, 80, 60 };

        // State
        TableViewerColumn colState = createTableViewerColumn(titles[0], bounds[0]);
        colState.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelItem) element).state.toString();
            }
            @Override
            public String getToolTipText(Object element) {
                return ((ModelItem) element).stateDescription;
            }
            @Override
            public Color getBackground(Object element) {
                ModelItem item = (ModelItem) element;
                if (item.state == ModelState.OK) return lightGreen;
                if (item.state == ModelState.ERR) return lightRed;
                if (item.state == ModelState.NA) return lightOrange;
                return null;
            }
        });
        org.eclipse.jface.viewers.ColumnViewerToolTipSupport.enableFor(tableViewer);

        // Token
        TableViewerColumn colToken = createTableViewerColumn(titles[4], bounds[4]);
        colToken.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                String t = ((ModelItem) element).token;
                if (t == null || t.isEmpty() || t.equals("YOUR_API_KEY")) return "";
                return "****";
            }
            @Override
            public Color getBackground(Object element) {
                ModelItem item = (ModelItem) element;
                if (item.state == ModelState.OK) return lightGreen;
                if (item.state == ModelState.ERR) return lightRed;
                if (item.state == ModelState.NA) return lightOrange;
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
                ModelItem item = (ModelItem) element;
                if (item.state == ModelState.OK) return lightGreen;
                if (item.state == ModelState.ERR) return lightRed;
                if (item.state == ModelState.NA) return lightOrange;
                return null;
            }
        });

        // Rating
        TableViewerColumn colRating = createTableViewerColumn(titles[5], bounds[5]);
        colRating.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return String.valueOf(((ModelItem) element).rating);
            }
            @Override
            public Color getBackground(Object element) {
                ModelItem item = (ModelItem) element;
                if (item.state == ModelState.OK) return lightGreen;
                if (item.state == ModelState.ERR) return lightRed;
                if (item.state == ModelState.NA) return lightOrange;
                return null;
            }
        });

        setupSorting(colName, colState, colRating);
    }

    private void setupSorting(TableViewerColumn colName, TableViewerColumn colState, TableViewerColumn colRating) {
        ModelComparator comparator = new ModelComparator();
        tableViewer.setComparator(comparator);

        colName.getColumn().addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                comparator.setColumn(0);
                tableViewer.refresh();
            }
        });
        colState.getColumn().addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                comparator.setColumn(1);
                tableViewer.refresh();
            }
        });
        colRating.getColumn().addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                comparator.setColumn(2);
                tableViewer.refresh();
            }
        });
    }

    private static class ModelComparator extends org.eclipse.jface.viewers.ViewerComparator {
        private int propertyIndex = 0;
        private static final int DESCENDING = 1;
        private int direction = DESCENDING;

        public void setColumn(int column) {
            if (column == this.propertyIndex) {
                direction = 1 - direction;
            } else {
                this.propertyIndex = column;
                direction = DESCENDING;
            }
        }

        @Override
        public int compare(org.eclipse.jface.viewers.Viewer viewer, Object e1, Object e2) {
            ModelItem m1 = (ModelItem) e1;
            ModelItem m2 = (ModelItem) e2;
            int rc = 0;
            switch (propertyIndex) {
            case 0: // Name
                rc = m1.name.compareToIgnoreCase(m2.name);
                break;
            case 1: // State
                rc = m1.state.toString().compareTo(m2.state.toString());
                break;
            case 2: // Rating
                rc = Integer.compare(m1.rating, m2.rating);
                break;
            default:
                rc = 0;
            }
            if (direction == DESCENDING) rc = -rc;
            return rc;
        }
    }

    private TableViewerColumn createTableViewerColumn(String title, int bound) {
        TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        viewerColumn.getColumn().setText(title);
        viewerColumn.getColumn().setWidth(bound);
        viewerColumn.getColumn().setResizable(true);
        viewerColumn.getColumn().setMoveable(true);
        return viewerColumn;
    }

    private void handleDownloadModel() {
        String ollamaUrl = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
        ModelDownloadDialog dialog = new ModelDownloadDialog(group.getShell(), ollamaUrl);
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            String modelName = dialog.getDownloadedModelName();
            if (modelName != null && !modelName.isEmpty()) {
                // Check if it already exists in aiProviders
                boolean exists = orchestrator.getAiProviders().stream()
                        .anyMatch(p -> modelName.equalsIgnoreCase(p.getName()) && p.isLocal());
                if (!exists) {
                    eu.kalafatic.evolution.model.orchestration.AIProvider newProvider =
                            eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
                    newProvider.setName(modelName);
                    newProvider.setLocal(true);
                    newProvider.setUrl(ollamaUrl);
                    newProvider.setFormat("ollama");
                    orchestrator.getAiProviders().add(newProvider);
                    editor.setDirty(true);
                }
                refreshUI();
            }
        }
    }

    private void handleAddModel() {
        String[] options = { "Local (Ollama)", "Remote" };
        MessageDialog dialog = new MessageDialog(group.getShell(), "Add Model", null,
                "Select the type of model to add:", MessageDialog.QUESTION, options, 0);
        int result = dialog.open();
        if (result == 0) { // Local
            InputDialog input = new InputDialog(group.getShell(), "Add Local Model",
                    "Enter the name of the model to run (e.g., gemma, llama3):", "", null);
            if (input.open() == InputDialog.OK) {
                String modelName = input.getValue();
                if (modelName != null && !modelName.trim().isEmpty()) {
                    // Just add it to providers if it's already there or verified
                    eu.kalafatic.evolution.model.orchestration.AIProvider newProvider =
                            eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
                    newProvider.setName(modelName.trim());
                    newProvider.setLocal(true);
                    String ollamaUrl = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
                    newProvider.setUrl(ollamaUrl);
                    newProvider.setFormat("ollama");
                    orchestrator.getAiProviders().add(newProvider);
                    editor.setDirty(true);
                    refreshUI();
                }
            }
        } else if (result == 1) { // Remote
            eu.kalafatic.evolution.model.orchestration.AIProvider newProvider =
                eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
            newProvider.setName("new-provider");
            newProvider.setFormat("openai");

            ModelDetailsDialog detailsDialog = new ModelDetailsDialog(group.getShell(), newProvider);
            if (detailsDialog.open() == org.eclipse.jface.window.Window.OK) {
                if (orchestrator != null) {
                    orchestrator.getAiProviders().add(newProvider);
                    editor.setDirty(true);
                    refreshUI();
                }
            }
        }
    }

    private void handleEditModel() {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        if (selection.isEmpty()) return;
        ModelItem item = (ModelItem) selection.getFirstElement();
        if (item.local) {
            MessageDialog.openInformation(group.getShell(), "Edit", "Local Ollama models cannot be edited here.");
            return;
        }

        eu.kalafatic.evolution.model.orchestration.AIProvider provider = item.provider;
        boolean isNew = false;
        if (provider == null) {
            // It's a static provider, create a model entry for it
            provider = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
            provider.setName(item.name);
            provider.setUrl(item.pathOrUrl);
            ProviderConfig config = AiProviders.PROVIDERS.get(item.name.toLowerCase());
            if (config != null) {
                provider.setDefaultModel(config.getDefaultModel());
                provider.setFormat(config.getFormat());
            }
            isNew = true;
        }

        ModelDetailsDialog detailsDialog = new ModelDetailsDialog(group.getShell(), provider);
        if (detailsDialog.open() == org.eclipse.jface.window.Window.OK) {
            if (isNew && orchestrator != null) {
                orchestrator.getAiProviders().add(provider);
            }
            editor.setDirty(true);
            refreshUI();
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
            if (MessageDialog.openConfirm(group.getShell(), "Remove Remote Model",
                    "Remove remote model configuration for: " + item.name + "?")) {
                // Remote models are from AiProviders, we don't really 'remove' them from the static map,
                // but we could clear it from the orchestrator if it matches.
                if (orchestrator != null && item.name.equalsIgnoreCase(orchestrator.getRemoteModel())) {
                    orchestrator.setRemoteModel("");
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

        final List<ModelItem> newItems = new ArrayList<>();
        eu.kalafatic.evolution.controller.security.TokenSecurityService security =
                eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance();

        // 1. Load Custom Providers from Model
        for (eu.kalafatic.evolution.model.orchestration.AIProvider p : orchestrator.getAiProviders()) {
            String token = security.getToken(p);
            ModelState state = (token != null && !token.isEmpty() && !token.equals("YOUR_API_KEY")) ? ModelState.OK : ModelState.NA;
            if (p.getState() != null && !p.getState().isEmpty()) {
                try { state = ModelState.valueOf(p.getState()); } catch (Exception e) {}
            }
            ModelItem item = new ModelItem(state, p.getName(), false, p.getUrl(), token);
            item.stateDescription = p.getStateDescription();
            item.rating = p.getRating();
            item.provider = p;
            newItems.add(item);
        }

        // 2. Load Remote Models from static map (if not explicitly in model as custom)
        for (String providerName : AiProviders.PROVIDERS.keySet()) {
            if (newItems.stream().anyMatch(i -> i.name.equalsIgnoreCase(providerName))) continue;

            eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved = security.resolve(orchestrator, providerName);
            ModelState state = (resolved != null && resolved.token != null && !resolved.token.isEmpty() && !"YOUR_API_KEY".equals(resolved.token))
                    ? ModelState.OK : ModelState.NA;

            newItems.add(new ModelItem(state, providerName, false,
                    (resolved != null) ? resolved.url : "",
                    (resolved != null) ? resolved.token : ""));
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

                // Mark EMF local providers status
                for (ModelItem item : newItems) {
                    if (item.local && item.provider != null) {
                        boolean exists = localModels.stream().anyMatch(m -> m.getName().equalsIgnoreCase(item.name) || (item.name.contains("/") && m.getName().contains(item.name)));
                        if (exists) {
                            item.state = ModelState.OK;
                            item.stateDescription = "Model found in Ollama.";
                        } else {
                            item.state = ModelState.ERR;
                            item.stateDescription = "Model NOT found in Ollama. Please download it.";
                        }
                    }
                }

                for (OllamaModel m : localModels) {
                    // Only add if not already in newItems (which contains EMF providers)
                    if (newItems.stream().noneMatch(i -> i.name.equalsIgnoreCase(m.getName()))) {
                        localItems.add(new ModelItem(ModelState.OK, m.getName(), true, ollamaUrl, null));
                    }
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
                        newItems.add(new ModelItem(ModelState.ERR, "Ollama", true, ollamaUrl, null));
                        tableViewer.refresh();
                    }
                });
            }
        });
    }
}
