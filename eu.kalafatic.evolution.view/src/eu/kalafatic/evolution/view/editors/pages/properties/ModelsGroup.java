package eu.kalafatic.evolution.view.editors.pages.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

import eu.kalafatic.evolution.controller.manager.ModelInfo;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

/**
 * @evo.lastModified: 13:A
 * @evo.origin: user
 */
public class ModelsGroup extends AEvoGroup {

    private TableViewer viewer;
    private List<ModelInfo> modelItems = new ArrayList<>();
    private eu.kalafatic.evolution.view.editors.pages.PropertiesPage page;

    public ModelsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, eu.kalafatic.evolution.view.editors.pages.PropertiesPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Models", 1, true, true);

        viewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        createColumns();

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.addDoubleClickListener(event -> handleUseModel());

        createButtons(toolkit);
        
        Display.getDefault().asyncExec(() -> {
            if (!viewer.getControl().isDisposed()) {
            	load();
            }
        });
    }

	public void createButtons(FormToolkit toolkit) {
		Composite buttonBar = toolkit.createComposite(group);
        buttonBar.setLayout(new GridLayout(8, false));
        buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        
        Button reloadButton = SWTFactory.createButton(buttonBar, "Reload");
        reloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String ollamaUrl = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
                OllamaManager.getInstance().getService(ollamaUrl).refreshModels();
                load();
            }
        });

        Button testButton = SWTFactory.createButton(buttonBar, "Test Model");
        testButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleTestModel();
            }
        });

        Button useButton = SWTFactory.createButton(buttonBar, "Use");
        useButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleUseModel();
            }
        });

        Button addButton = SWTFactory.createButton(buttonBar, "Add");
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddModel();
            }
        });

        Button downloadButton = SWTFactory.createButton(buttonBar, "Download");
        downloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDownloadModel();
            }
        });

        Button editButton = SWTFactory.createButton(buttonBar, "Edit");
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleEditModel();
            }
        });

        Button removeButton = SWTFactory.createButton(buttonBar, "Remove");
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemoveModel();
            }
        });

        Button saveButton = SWTFactory.createButton(buttonBar, "Save to Model");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editor.doSave(null);
            }
        });
	}

    private Color getModelColor(ModelInfo item) {
        if (item.getState() == ModelInfo.ModelState.ERR) return lightRed;
        if (item.getState() == ModelInfo.ModelState.NA) return lightOrange;
        if (item.getState() == ModelInfo.ModelState.OK) {
            String name = item.getName().toLowerCase();
            if (item.isHybrid()) return lightCyan;
            if (name.endsWith(":instruct")) return lightPurple;
            if (name.endsWith(":chat")) return lightBlue;
            return lightGreen;
        }
        return null;
    }

    private void createColumns() {
        String[] titles = { "State", "Name", "Type", "Path/URL", "Token", "Rating (A/CH/P)" };
        int[] bounds = { 60, 150, 60, 250, 80, 120 };

        // State
        TableViewerColumn colState = createTableViewerColumn(titles[0], bounds[0]);
        colState.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelInfo) element).getState().toString();
            }
            @Override
            public String getToolTipText(Object element) {
                return ((ModelInfo) element).getStateDescription();
            }
            @Override
            public Color getBackground(Object element) {
                return getModelColor((ModelInfo) element);
            }
        });
        org.eclipse.jface.viewers.ColumnViewerToolTipSupport.enableFor(viewer);

        // Name
        TableViewerColumn colName = createTableViewerColumn(titles[1], bounds[1]);
        colName.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelInfo) element).getName();
            }
            @Override
            public Color getBackground(Object element) {
                return getModelColor((ModelInfo) element);
            }
        });

        // Local
        TableViewerColumn colLocal = createTableViewerColumn(titles[2], bounds[2]);
        colLocal.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                ModelInfo item = (ModelInfo) element;
                if (item.isHybrid()) return "Hybrid";
                return (item.isLocal()) ? "Local" : "Remote";
            }
            @Override
            public Color getBackground(Object element) {
                return getModelColor((ModelInfo) element);
            }
        });

        // Path/URL
        TableViewerColumn colPath = createTableViewerColumn(titles[3], bounds[3]);
        colPath.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelInfo) element).getPathOrUrl();
            }
            @Override
            public Color getBackground(Object element) {
                return getModelColor((ModelInfo) element);
            }
        });

        // Token
        TableViewerColumn colToken = createTableViewerColumn(titles[4], bounds[4]);
        colToken.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                String t = ((ModelInfo) element).getToken();
                if (t == null || t.isEmpty() || t.equals("YOUR_API_KEY")) return "";
                return "****";
            }
            @Override
            public Color getBackground(Object element) {
                return getModelColor((ModelInfo) element);
            }
        });

        // Rating
        TableViewerColumn colRating = createTableViewerColumn(titles[5], bounds[5]);
        colRating.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                ModelInfo item = (ModelInfo) element;
                return String.format("%d (A:%d/CH:%d/P:%d)", item.getRating(), item.getRatingAnalyze(), item.getRatingChat(), item.getRatingProgramming());
            }
            @Override
            public Color getBackground(Object element) {
                return getModelColor((ModelInfo) element);
            }
        });

        setupSorting(colName, colState, colRating);
    }

    private void setupSorting(TableViewerColumn colName, TableViewerColumn colState, TableViewerColumn colRating) {
        ModelComparator comparator = new ModelComparator();
        viewer.setComparator(comparator);

        colName.getColumn().addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                comparator.setColumn(0);
                viewer.refresh();
            }
        });
        colState.getColumn().addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                comparator.setColumn(1);
                viewer.refresh();
            }
        });
        colRating.getColumn().addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                comparator.setColumn(2);
                viewer.refresh();
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
            ModelInfo m1 = (ModelInfo) e1;
            ModelInfo m2 = (ModelInfo) e2;
            int rc = 0;
            switch (propertyIndex) {
            case 0: // Name
                rc = m1.getName().compareToIgnoreCase(m2.getName());
                break;
            case 1: // State
                rc = m1.getState().toString().compareTo(m2.getState().toString());
                break;
            case 2: // Rating
                rc = Integer.compare(m1.getRating(), m2.getRating());
                break;
            default:
                rc = 0;
            }
            if (direction == DESCENDING) rc = -rc;
            return rc;
        }
    }

    private TableViewerColumn createTableViewerColumn(String title, int bound) {
        TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
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
            org.eclipse.jface.dialogs.InputDialog input = new org.eclipse.jface.dialogs.InputDialog(group.getShell(), "Add Local Model",
                    "Enter the name of the model to run (e.g., gemma, llama3):", "", null);
            if (input.open() == org.eclipse.jface.dialogs.InputDialog.OK) {
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
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (selection.isEmpty()) return;
        ModelInfo item = (ModelInfo) selection.getFirstElement();
        if (item.isLocal()) {
            MessageDialog.openInformation(group.getShell(), "Edit", "Local Ollama models cannot be edited here.");
            return;
        }

        eu.kalafatic.evolution.model.orchestration.AIProvider provider = item.getProvider();
        boolean isNew = false;
        if (provider == null) {
            // It's a static provider, create a model entry for it
            provider = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
            provider.setName(item.getName());
            provider.setUrl(item.getPathOrUrl());
            ProviderConfig config = AiProviders.PROVIDERS.get(item.getName().toLowerCase());
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

    private void handleTestModel() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (selection.isEmpty()) return;
        ModelInfo item = (ModelInfo) selection.getFirstElement();
        if (item.getProvider() == null) {
            MessageDialog.openWarning(group.getShell(), "Test Model", "Only custom or local models with provider entries can be tested.");
            return;
        }

        final eu.kalafatic.evolution.model.orchestration.AIProvider provider = item.getProvider();

        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Testing Model: " + provider.getName()) {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                try {
                    eu.kalafatic.evolution.controller.services.ModelEvaluationService service =
                            new eu.kalafatic.evolution.controller.services.ModelEvaluationService();

                    File projectRoot = null;
                    if (editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
                        projectRoot = ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput()).getFile().getProject().getLocation().toFile();
                    }

                    TaskContext context = new TaskContext(orchestrator, projectRoot);
                    service.evaluateModel(orchestrator, provider, context);

                    Display.getDefault().asyncExec(() -> {
                        refreshUI();
                        editor.setDirty(true);
                        MessageDialog.openInformation(group.getShell(), "Test Complete",
                                "Evaluation finished for " + provider.getName() + ".\n" +
                                "Overall: " + provider.getRating() + "\n" +
                                "A: " + provider.getRatingAnalyze() + " CH: " + provider.getRatingChat() + " P: " + provider.getRatingProgramming());
                    });
                    return org.eclipse.core.runtime.Status.OK_STATUS;
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        MessageDialog.openError(group.getShell(), "Test Error", "Failed to evaluate model: " + e.getMessage());
                    });
                    return org.eclipse.core.runtime.Status.CANCEL_STATUS;
                }
            }
        };
        job.schedule();
    }

    private void handleUseModel() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (selection.isEmpty()) return;
        ModelInfo item = (ModelInfo) selection.getFirstElement();

        if (orchestrator != null) {
            ProjectModelManager modelManager = ProjectModelManager.getInstance();
            if (item.isLocal()) {
                modelManager.updateOllamaSettings(orchestrator, (orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : null, item.getName(), (orchestrator.getOllama() != null) ? orchestrator.getOllama().getPath() : null);
                modelManager.updateLocalModel(orchestrator, item.getName());
                modelManager.updateLlmSettings(orchestrator, item.getName(), (orchestrator.getLlm() != null) ? orchestrator.getLlm().getTemperature() : 1.0f);
            } else {
                modelManager.updateRemoteModel(orchestrator, item.getName());
                modelManager.updateLlmSettings(orchestrator, item.getName(), (orchestrator.getLlm() != null) ? orchestrator.getLlm().getTemperature() : 1.0f);
            }
            editor.setDirty(true);
            if (page != null) {
                page.refreshUI();
            }
        }
    }

    private void handleRemoveModel() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (selection.isEmpty()) return;
        ModelInfo item = (ModelInfo) selection.getFirstElement();

        if (item.isLocal()) {
            if (MessageDialog.openConfirm(group.getShell(), "Remove Local Model",
                    "Are you sure you want to remove the local model: " + item.getName() + "?")) {
                runTerminalCommand("ollama rm " + item.getName());
            }
        } else {
            if (MessageDialog.openConfirm(group.getShell(), "Remove Remote Model",
                    "Remove remote model configuration for: " + item.getName() + "?")) {
                if (orchestrator != null) {
                    if (item.getProvider() != null) {
                        orchestrator.getAiProviders().remove(item.getProvider());
                        editor.setDirty(true);
                        refreshUI();
                    } else if (item.getName().equalsIgnoreCase(orchestrator.getRemoteModel())) {
                        ProjectModelManager.getInstance().updateRemoteModel(orchestrator, "");
                        editor.setDirty(true);
                        refreshUI();
                    }
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
        if (viewer == null || viewer.getControl().isDisposed()) return;
        viewer.refresh();
    }

	public void load() {
        CompletableFuture.runAsync(() -> {
            List<ModelInfo> allModels = ProjectModelManager.getInstance().getAllModels(orchestrator);
            Display.getDefault().asyncExec(() -> {
                if (viewer.getControl().isDisposed()) return;
                this.modelItems = allModels;
                viewer.setInput(modelItems);
                viewer.refresh();
            });
        });
	}
}
