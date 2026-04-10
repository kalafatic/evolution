package eu.kalafatic.evolution.view.editors.pages.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
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
        ((GridData)group.getLayoutData()).heightHint = 200;

        tableViewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        createColumns();

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

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

    @Override
    protected void refreshUI() {
        if (orchestrator == null) return;

        final List<ModelItem> newItems = new ArrayList<>();

        // Load Remote Models first (synchronously since it's just from memory)
        for (String providerName : AiProviders.PROVIDERS.keySet()) {
            ProviderConfig config = AiProviders.PROVIDERS.get(providerName);
            ModelState state = ModelState.NA;

            // For remote models, "ok" if we have a valid-looking token
            String token = orchestrator.getOpenAiToken();
            boolean isCurrentProvider = providerName.equalsIgnoreCase(orchestrator.getRemoteModel());

            if (isCurrentProvider && token != null && !token.isEmpty() && !token.equals("YOUR_API_KEY")) {
                state = ModelState.OK;
            } else if (config.getApiKey() != null && !config.getApiKey().equals("YOUR_API_KEY")) {
                 state = ModelState.OK;
            }

            newItems.add(new ModelItem(state, providerName, false, config.getEndpointUrl()));
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
