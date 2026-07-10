package eu.kalafatic.evolution.view.editors.pages.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.controller.tools.ShellTool;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * @evo.lastModified: 13:A
 * @evo.origin: user
 */
public class ModelsGroup extends AEvoGroup {

    private CheckboxTableViewer viewer;
    private List<AIProvider> modelItems = new ArrayList<>();
    private eu.kalafatic.evolution.view.editors.pages.PropertiesPage page;
    private Font boldFont;
    private Color orangeColor;

    public ModelsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, eu.kalafatic.evolution.view.editors.pages.PropertiesPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Models", 1, true, true);

        viewer = CheckboxTableViewer.newCheckList(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.addDisposeListener(e -> {
            if (boldFont != null && !boldFont.isDisposed()) {
                boldFont.dispose();
            }
            if (orangeColor != null && !orangeColor.isDisposed()) {
                orangeColor.dispose();
            }
        });

        createColumns();

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.addDoubleClickListener(event -> handleUseModel());

        createContextMenu();
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
        
        Button reloadButton = GUIFactory.INSTANCE.createButton(buttonBar, "Reload");
        reloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String ollamaUrl = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
                OllamaManager.getInstance().getService(ollamaUrl).refreshModels();
                load();
            }
        });

        Button testButton = GUIFactory.INSTANCE.createButton(buttonBar, "Test Model");
        testButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleTestModel();
            }
        });

        Button useButton = GUIFactory.INSTANCE.createButton(buttonBar, "Use");
        useButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleUseModel();
            }
        });

        Button addButton = GUIFactory.INSTANCE.createButton(buttonBar, "Add");
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddModel();
            }
        });

        Button downloadButton = GUIFactory.INSTANCE.createButton(buttonBar, "Download");
        downloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDownloadModel();
            }
        });

        Button editButton = GUIFactory.INSTANCE.createButton(buttonBar, "Edit");
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleEditModel();
            }
        });

        Button removeButton = GUIFactory.INSTANCE.createButton(buttonBar, "Remove");
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemoveModel();
            }
        });

        Button saveButton = GUIFactory.INSTANCE.createButton(buttonBar, "Save to Model");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editor.doSave(null);
            }
        });
	}

    private Color getModelColor(AIProvider item) {
        if ("ERR".equals(item.getState())) return lightRed;
        if ("NA".equals(item.getState())) return lightOrange;
        if ("OK".equals(item.getState())) {
            String name = item.getName().toLowerCase();
            if (ProjectModelManager.getInstance().isHybrid(item)) return lightCyan;
            if (name.endsWith(":instruct")) return lightPurple;
            if (name.endsWith(":chat")) return lightBlue;
            return lightGreen;
        }
        return null;
    }

    private boolean isEvoModel(AIProvider item) {
        if (item == null || item.getName() == null) return false;
        String name = item.getName().toLowerCase();
        return name.equals("evo") || name.contains("evo");
    }

    private Font getBoldFont() {
        if (boldFont == null || boldFont.isDisposed()) {
            Font defaultFont = viewer.getTable().getFont();
            FontData[] fontData = defaultFont.getFontData();
            for (FontData fd : fontData) {
                fd.setStyle(SWT.BOLD);
            }
            boldFont = new Font(Display.getDefault(), fontData);
        }
        return boldFont;
    }

    private Color getOrangeColor() {
        if (orangeColor == null || orangeColor.isDisposed()) {
            orangeColor = new Color(Display.getDefault(), 237, 108, 2);
        }
        return orangeColor;
    }

    private class ModelLabelProvider extends ColumnLabelProvider {
        @Override
        public Font getFont(Object element) {
            if (isEvoModel((AIProvider) element)) {
                return getBoldFont();
            }
            return super.getFont(element);
        }

        @Override
        public Color getForeground(Object element) {
            if (isEvoModel((AIProvider) element)) {
                return getOrangeColor();
            }
            return super.getForeground(element);
        }
    }

    public static String getModelAbsolutePath(Orchestrator orchestrator, AIProvider provider) {
        if (provider == null) return "";
        String url = provider.getUrl();
        if (url != null) {
            File file = new File(url);
            if (file.isAbsolute() && file.exists()) {
                return file.getAbsolutePath();
            }
        }
        if (provider.isLocal()) {
            String name = provider.getName();
            if (name != null && !name.isEmpty()) {
                File file = new File(name);
                if (file.isAbsolute() && file.exists()) {
                    return file.getAbsolutePath();
                }
                String modelName = name;
                String tag = "latest";
                if (name.contains(":")) {
                    int colonIdx = name.indexOf(":");
                    modelName = name.substring(0, colonIdx);
                    tag = name.substring(colonIdx + 1);
                }
                String userHome = System.getProperty("user.home");
                File ollamaModelsDir = null;
                if (orchestrator != null && orchestrator.getOllama() != null && orchestrator.getOllama().getPath() != null && !orchestrator.getOllama().getPath().isEmpty()) {
                    ollamaModelsDir = new File(orchestrator.getOllama().getPath());
                } else {
                    ollamaModelsDir = new File(userHome, ".ollama/models");
                }

                File manifestFile = new File(ollamaModelsDir, "manifests/registry.ollama.ai/library/" + modelName + "/" + tag);
                if (manifestFile.exists()) {
                    return manifestFile.getAbsolutePath();
                }
                return manifestFile.getAbsolutePath();
            }
        }
        return "";
    }

    private void createColumns() {
        String[] titles = { "Select", "State", "Name", "Type", "Path/URL", "Path", "Token", "Rating (A/CH/P)" };
        int[] bounds = { 50, 60, 150, 60, 250, 250, 80, 120 };

        // Select (Checkbox column)
        TableViewerColumn colSelect = createTableViewerColumn(titles[0], bounds[0]);
        colSelect.setLabelProvider(new ModelLabelProvider() {
            @Override
            public String getText(Object element) {
                return "";
            }
        });

        // State
        TableViewerColumn colState = createTableViewerColumn(titles[1], bounds[1]);
        colState.setLabelProvider(new ModelLabelProvider() {
            @Override
            public String getText(Object element) {
                String s = ((AIProvider) element).getState();
                return s != null ? s : "NA";
            }
            @Override
            public String getToolTipText(Object element) {
                return ((AIProvider) element).getStateDescription();
            }
            @Override
            public Color getBackground(Object element) {
                return getSafeColor(getModelColor((AIProvider) element));
            }
        });
        org.eclipse.jface.viewers.ColumnViewerToolTipSupport.enableFor(viewer);

        // Name
        TableViewerColumn colName = createTableViewerColumn(titles[2], bounds[2]);
        colName.setLabelProvider(new ModelLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((AIProvider) element).getName();
            }
            @Override
            public Color getBackground(Object element) {
                return getSafeColor(getModelColor((AIProvider) element));
            }
        });

        // Local
        TableViewerColumn colLocal = createTableViewerColumn(titles[3], bounds[3]);
        colLocal.setLabelProvider(new ModelLabelProvider() {
            @Override
            public String getText(Object element) {
                AIProvider item = (AIProvider) element;
                if (ProjectModelManager.getInstance().isHybrid(item)) return "Hybrid";
                return (item.isLocal()) ? "Local" : "Remote";
            }
            @Override
            public Color getBackground(Object element) {
                return getSafeColor(getModelColor((AIProvider) element));
            }
        });

        // Path/URL
        TableViewerColumn colPath = createTableViewerColumn(titles[4], bounds[4]);
        colPath.setLabelProvider(new ModelLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((AIProvider) element).getUrl();
            }
            @Override
            public Color getBackground(Object element) {
                return getSafeColor(getModelColor((AIProvider) element));
            }
        });

        // Path (Absolute File Path)
        TableViewerColumn colAbsPath = createTableViewerColumn(titles[5], bounds[5]);
        colAbsPath.setLabelProvider(new ModelLabelProvider() {
            @Override
            public String getText(Object element) {
                return getModelAbsolutePath(orchestrator, (AIProvider) element);
            }
            @Override
            public Color getBackground(Object element) {
                return getSafeColor(getModelColor((AIProvider) element));
            }
        });

        // Token
        TableViewerColumn colToken = createTableViewerColumn(titles[6], bounds[6]);
        colToken.setLabelProvider(new ModelLabelProvider() {
            @Override
            public String getText(Object element) {
                String t = ((AIProvider) element).getApiKey();
                if (t == null || t.isEmpty() || t.equals("YOUR_API_KEY")) return "";
                return "****";
            }
            @Override
            public Color getBackground(Object element) {
                return getSafeColor(getModelColor((AIProvider) element));
            }
        });

        // Rating
        TableViewerColumn colRating = createTableViewerColumn(titles[7], bounds[7]);
        colRating.setLabelProvider(new ModelLabelProvider() {
            @Override
            public String getText(Object element) {
                AIProvider item = (AIProvider) element;
                return String.format("%d (A:%d/CH:%d/P:%d)", item.getRating(), item.getRatingAnalyze(), item.getRatingChat(), item.getRatingProgramming());
            }
            @Override
            public Color getBackground(Object element) {
                return getSafeColor(getModelColor((AIProvider) element));
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
            AIProvider m1 = (AIProvider) e1;
            AIProvider m2 = (AIProvider) e2;
            int rc = 0;
            switch (propertyIndex) {
            case 0: // Name
                rc = m1.getName().compareToIgnoreCase(m2.getName());
                break;
            case 1: // State
                String s1 = m1.getState() != null ? m1.getState() : "";
                String s2 = m2.getState() != null ? m2.getState() : "";
                rc = s1.compareTo(s2);
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

    private void createContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        Control control = viewer.getControl();
        Menu menu = menuMgr.createContextMenu(control);
        control.setMenu(menu);
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(new Action("Reload") {
            @Override public void run() {
                String ollamaUrl = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
                OllamaManager.getInstance().getService(ollamaUrl).refreshModels();
                load();
            }
        });
        manager.add(new Action("Test Model") {
            @Override public void run() { handleTestModel(); }
        });
        manager.add(new Action("Use") {
            @Override public void run() { handleUseModel(); }
        });
        manager.add(new Separator());
        manager.add(new Action("Add") {
            @Override public void run() { handleAddModel(); }
        });
        manager.add(new Action("Download") {
            @Override public void run() { handleDownloadModel(); }
        });
        manager.add(new Separator());
        manager.add(new Action("Edit") {
            @Override public void run() { handleEditModel(); }
        });
        manager.add(new Action("Remove") {
            @Override public void run() { handleRemoveModel(); }
        });
        manager.add(new Separator());
        manager.add(new Action("Save to Model") {
            @Override public void run() { editor.doSave(null); }
        });
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
        Object[] checked = viewer.getCheckedElements();
        if (checked.length == 0) {
            IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            if (selection.isEmpty()) return;
            checked = new Object[] { selection.getFirstElement() };
        }
        AIProvider item = (AIProvider) checked[0];
        if (item.isLocal()) {
            MessageDialog.openInformation(group.getShell(), "Edit", "Local Ollama models cannot be edited here.");
            return;
        }

        // We need to find the REAL provider in the model if it exists
        AIProvider provider = orchestrator.getAiProviders().stream()
                .filter(p -> p.getName().equalsIgnoreCase(item.getName()))
                .findFirst().orElse(null);

        boolean isNew = false;
        if (provider == null) {
            // It's a static provider, create a model entry for it
            provider = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
            provider.setName(item.getName());
            provider.setUrl(item.getUrl());
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
        Object[] checked = viewer.getCheckedElements();
        if (checked.length == 0) {
            IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            if (!selection.isEmpty()) {
                checked = selection.toArray();
            }
        }
        if (checked.length == 0) return;

        for (Object obj : checked) {
            AIProvider item = (AIProvider) obj;
            testModel(item);
        }
    }

    private void testModel(AIProvider item) {
        // Find real provider or use a temporary one for testing
        final AIProvider providerToTest = orchestrator.getAiProviders().stream()
                .filter(p -> p.getName().equalsIgnoreCase(item.getName()))
                .findFirst().orElse(item);

        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Testing Model: " + providerToTest.getName()) {
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
                    service.evaluateModel(orchestrator, providerToTest, context);

                    Display.getDefault().asyncExec(() -> {
                        refreshUI();
                        editor.setDirty(true);
                        MessageDialog.openInformation(group.getShell(), "Test Complete",
                                "Evaluation finished for " + providerToTest.getName() + ".\n" +
                                "Overall: " + providerToTest.getRating() + "\n" +
                                "A: " + providerToTest.getRatingAnalyze() + " CH: " + providerToTest.getRatingChat() + " P: " + providerToTest.getRatingProgramming());
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
        Object[] checked = viewer.getCheckedElements();
        if (checked.length == 0) {
            IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            if (selection.isEmpty()) return;
            checked = new Object[] { selection.getFirstElement() };
        }
        AIProvider item = (AIProvider) checked[0];

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
        Object[] checked = viewer.getCheckedElements();
        if (checked.length == 0) {
            IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            if (!selection.isEmpty()) {
                checked = selection.toArray();
            }
        }
        if (checked.length == 0) return;

        if (!MessageDialog.openConfirm(group.getShell(), "Remove Models",
                "Are you sure you want to remove " + checked.length + " selected model(s)?")) {
            return;
        }

        for (Object obj : checked) {
            AIProvider item = (AIProvider) obj;
            if (item.isLocal()) {
                runTerminalCommand("ollama rm " + item.getName());
            } else {
                if (orchestrator != null) {
                    AIProvider realProvider = orchestrator.getAiProviders().stream()
                            .filter(p -> p.getName().equalsIgnoreCase(item.getName()))
                            .findFirst().orElse(null);

                    if (realProvider != null) {
                        orchestrator.getAiProviders().remove(realProvider);
                        editor.setDirty(true);
                    } else if (item.getName().equalsIgnoreCase(orchestrator.getRemoteModel())) {
                        ProjectModelManager.getInstance().updateRemoteModel(orchestrator, "");
                        editor.setDirty(true);
                    }
                }
            }
        }
        refreshUI();
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
        try {
			if (orchestrator == null) return;
			if (viewer == null || viewer.getControl().isDisposed()) return;
			viewer.refresh();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	public void load() {
        CompletableFuture.runAsync(() -> {
            List<AIProvider> allModels = ProjectModelManager.getInstance().getAllModels(orchestrator);
            Display.getDefault().asyncExec(() -> {
                if (viewer.getControl().isDisposed()) return;
                this.modelItems = allModels;
                viewer.setInput(modelItems);
                refreshUI();
            });
        });
	}
}
