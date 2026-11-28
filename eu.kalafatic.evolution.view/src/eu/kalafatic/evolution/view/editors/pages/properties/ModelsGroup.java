package eu.kalafatic.evolution.view.editors.pages.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.ModelIdentity;
import eu.kalafatic.evolution.controller.manager.ModelIdentityResolver;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalResult;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.util.EvoTableViewerHelper;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * Professional model management UI for Properties → Models.
 *
 * <p>The table is fed by the reconciled, de-duplicated model dataset. Filtering
 * is applied over the complete dataset, and the filtered subset is the source
 * for bulk selection and bulk operations.</p>
 */
public class ModelsGroup extends AEvoGroup {

    private static final String ALL = "All";

    private CheckboxTableViewer viewer;
    private final List<ModelRow> allRows = new ArrayList<>();
    private final List<ModelRow> filteredRows = new ArrayList<>();
    private final Set<String> checkedKeys = new LinkedHashSet<>();
    private final AtomicBoolean loadPending = new AtomicBoolean(false);

    private eu.kalafatic.evolution.view.editors.pages.PropertiesPage page;
    private Text searchText;
    private Combo providerFilter;
    private Combo typeFilter;
    private Combo statusFilter;
    private Combo registrationFilter;
    private Button showUnregisteredCheckbox;
    private Label countsLabel;

    private Button selectAllButton;
    private Button clearSelectionButton;
    private Button reloadButton;
    private Button editButton;
    private Button renameButton;
    private Button removeButton;
    private Button testButton;
    private Button useButton;
    private Button addButton;
    private Button downloadButton;
    private Button exportButton;
    private Button saveButton;

    private int sortColumn = 1;
    private int sortDirection = 1;
    private Runnable modelChangeListener;

    private String filterPattern = "";

    public ModelsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator,
                       eu.kalafatic.evolution.view.editors.pages.PropertiesPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Models", 1, true, true);

        createFilterToolbar(toolkit);

        viewer = CheckboxTableViewer.newCheckList(group,
                SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        createColumns();

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setComparator(new ModelRowComparator());
        viewer.addDoubleClickListener(event -> handleEditModel());

        viewer.addSelectionChangedListener(event -> updateCounts());
        viewer.addCheckStateListener(event -> {
            if (event.getElement() instanceof ModelRow row) {
                if (event.getChecked()) {
                    checkedKeys.add(row.getKey());
                } else {
                    checkedKeys.remove(row.getKey());
                }
                updateCounts();
            }
        });
        addRightClickSelection();
        createContextMenu();
        createButtons(toolkit);

        modelChangeListener = () -> Display.getDefault().asyncExec(() -> {
            if (group != null && !group.isDisposed()) {
                load();
            }
        });
        ProjectModelManager.getInstance().addModelChangeListener(modelChangeListener);
        group.addDisposeListener(e -> {
            if (modelChangeListener != null) {
                ProjectModelManager.getInstance().removeModelChangeListener(modelChangeListener);
            }
        });

        Display.getDefault().asyncExec(() -> {
            if (!viewer.getControl().isDisposed()) {
                load();
            }
        });
    }

    private void createFilterToolbar(FormToolkit toolkit) {
        Composite searchRow = toolkit.createComposite(group);
        searchRow.setLayout(new GridLayout(3, false));
        searchRow.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(searchRow, "Search:");
        searchText = toolkit.createText(searchRow, "",
                SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        searchText.setMessage("Name, technical ID, provider, type, endpoint, path, tag...");
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        searchText.addModifyListener(e -> {
            filterPattern = searchText.getText();
            applyFilters();
        });

        countsLabel = GUIFactory.INSTANCE.createLabel(searchRow, "");
        countsLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        Composite filterRow = toolkit.createComposite(group);
        filterRow.setLayout(new GridLayout(12, false));
        filterRow.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(filterRow, "Provider:");
        providerFilter = createFilterCombo(filterRow,
                new String[] { ALL, "Ollama", "EVO Native", "GGUF", "Demo", "Remote", "Unknown" });

        GUIFactory.INSTANCE.createLabel(filterRow, "Type:");
        typeFilter = createFilterCombo(filterRow,
                new String[] { ALL, "Local (Ollama)", "EVO Native", "GGUF", "Demo", "Remote" });

        GUIFactory.INSTANCE.createLabel(filterRow, "Status:");
        statusFilter = createFilterCombo(filterRow,
                new String[] { ALL, "Available", "Available (unregistered)",
                        "Missing / not registered", "Offline / error", "Unknown" });

        GUIFactory.INSTANCE.createLabel(filterRow, "Registration:");
        registrationFilter = createFilterCombo(filterRow,
                new String[] { ALL, "Registered", "Discovered", "Unregistered" });

        Button clearButton = GUIFactory.INSTANCE.createButton(filterRow, "Clear Filters");
        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                clearFilters(false);
            }
        });

        Button resetButton = GUIFactory.INSTANCE.createButton(filterRow, "Reset");
        resetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                clearFilters(true);
            }
        });

        showUnregisteredCheckbox = new Button(filterRow, SWT.CHECK);
        showUnregisteredCheckbox.setText("Show unregistered");
        showUnregisteredCheckbox.setToolTipText(
                "Reveal models whose EVO registration was removed but whose artifact/runtime still exists");
        showUnregisteredCheckbox.setSelection(false);
        showUnregisteredCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                applyFilters();
            }
        });
    }

    private Combo createFilterCombo(Composite parent, String[] items) {
        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setItems(items);
        combo.select(0);
        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                applyFilters();
            }
        });
        return combo;
    }

    private void clearFilters(boolean reset) {
        if (searchText != null && !searchText.isDisposed()) {
            filterPattern = "";
            searchText.setText("");
        }
        if (providerFilter != null && !providerFilter.isDisposed()) {
            providerFilter.select(0);
        }
        if (typeFilter != null && !typeFilter.isDisposed()) {
            typeFilter.select(0);
        }
        if (statusFilter != null && !statusFilter.isDisposed()) {
            statusFilter.select(0);
        }
        if (registrationFilter != null && !registrationFilter.isDisposed()) {
            registrationFilter.select(0);
        }
        if (showUnregisteredCheckbox != null && !showUnregisteredCheckbox.isDisposed()) {
            showUnregisteredCheckbox.setSelection(false);
        }
        if (reset) {
            checkedKeys.clear();
            if (viewer != null && !viewer.getControl().isDisposed()) {
                viewer.setAllChecked(false);
                viewer.setSelection(StructuredSelection.EMPTY);
            }
            load();
        } else {
            applyFilters();
        }
    }

    public void createButtons(FormToolkit toolkit) {
        Composite buttonBar = toolkit.createComposite(group);
        buttonBar.setLayout(new GridLayout(7, false));
        buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        selectAllButton = GUIFactory.INSTANCE.createButton(buttonBar, "Select All Filtered");
        selectAllButton.setToolTipText("Select every model currently visible under the active filters");
        selectAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectAllFiltered();
            }
        });

        clearSelectionButton = GUIFactory.INSTANCE.createButton(buttonBar, "Clear Selection");
        clearSelectionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkedKeys.clear();
                if (viewer != null && !viewer.getControl().isDisposed()) {
                    viewer.setAllChecked(false);
                    viewer.setSelection(StructuredSelection.EMPTY);
                }
                updateCounts();
            }
        });

        reloadButton = GUIFactory.INSTANCE.createButton(buttonBar, "Refresh");
        reloadButton.setToolTipText("Re-run model discovery and refresh statuses");
        reloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleReload();
            }
        });

        editButton = GUIFactory.INSTANCE.createButton(buttonBar, "Edit");
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleEditModel();
            }
        });

        renameButton = GUIFactory.INSTANCE.createButton(buttonBar, "Rename");
        renameButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRenameModel();
            }
        });

        removeButton = GUIFactory.INSTANCE.createButton(buttonBar, "Delete Selected");
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemoveModel();
            }
        });

        exportButton = GUIFactory.INSTANCE.createButton(buttonBar, "Export ZIP");
        exportButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleExportToZip();
            }
        });

        testButton = GUIFactory.INSTANCE.createButton(buttonBar, "Test");
        testButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleTestModel();
            }
        });

        useButton = GUIFactory.INSTANCE.createButton(buttonBar, "Use");
        useButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleUseModel();
            }
        });

        addButton = GUIFactory.INSTANCE.createButton(buttonBar, "Add");
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddModel();
            }
        });

        downloadButton = GUIFactory.INSTANCE.createButton(buttonBar, "Download");
        downloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDownloadModel();
            }
        });

        saveButton = GUIFactory.INSTANCE.createButton(buttonBar, "Save to Model");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editor.doSave(null);
            }
        });
    }

    private Color getModelColor(ModelRow row) {
        if (row == null) {
            return null;
        }
        if ("Unregistered".equals(row.getRegistrationState())) {
            return lightPurple;
        }
        if (ProjectModelManager.getInstance().isHybrid(row.getProvider())) {
            return lightCyan;
        }
        String status = row.getStatus();
        if ("Offline / error".equals(status)) {
            return lightRed;
        }
        if ("Missing / not registered".equals(status) || "Unknown".equals(status)) {
            return lightOrange;
        }
        if ("Available".equals(status) || "Available (unregistered)".equals(status)) {
            return lightGreen;
        }
        return null;
    }

    private Color getSafeColor(ModelRow row) {
        Color color = getModelColor(row);
        return color != null ? color : colorWhite;
    }

    private void createColumns() {
        String[] titles = { "Select", "Model", "Technical ID", "Provider", "Type", "Status",
                "Registration", "Endpoint", "Location", "Size", "Modified", "Actions" };
        int[] bounds = { 42, 170, 190, 100, 100, 145, 105, 190, 260, 80, 135, 140 };

        TableViewerColumn selectColumn = createTableViewerColumn(titles[0], bounds[0]);
        selectColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return "";
            }
        });

        TableViewerColumn modelColumn = createTableViewerColumn(titles[1], bounds[1]);
        modelColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getDisplayName();
            }

            @Override
            public String getToolTipText(Object element) {
                ModelRow row = (ModelRow) element;
                return row.getDescription() != null && !row.getDescription().isBlank()
                        ? row.getDescription() : row.getStatusDescription();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn techColumn = createTableViewerColumn(titles[2], bounds[2]);
        techColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getTechnicalId();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn providerColumn = createTableViewerColumn(titles[3], bounds[3]);
        providerColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getProviderLabel();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn typeColumn = createTableViewerColumn(titles[4], bounds[4]);
        typeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getTypeLabel();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn statusColumn = createTableViewerColumn(titles[5], bounds[5]);
        statusColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getStatus();
            }

            @Override
            public String getToolTipText(Object element) {
                return ((ModelRow) element).getStatusDescription();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn registrationColumn = createTableViewerColumn(titles[6], bounds[6]);
        registrationColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getRegistrationState();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn endpointColumn = createTableViewerColumn(titles[7], bounds[7]);
        endpointColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getEndpoint();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn locationColumn = createTableViewerColumn(titles[8], bounds[8]);
        locationColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getLocation();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn sizeColumn = createTableViewerColumn(titles[9], bounds[9]);
        sizeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getFormattedSize();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn modifiedColumn = createTableViewerColumn(titles[10], bounds[10]);
        modifiedColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getFormattedModified();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        TableViewerColumn actionsColumn = createTableViewerColumn(titles[11], bounds[11]);
        actionsColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((ModelRow) element).getActionsLabel();
            }

            @Override
            public Color getBackground(Object element) {
                return getSafeColor((ModelRow) element);
            }
        });

        setupSorting(new TableViewerColumn[] { selectColumn, modelColumn, techColumn, providerColumn,
                typeColumn, statusColumn, registrationColumn, endpointColumn, locationColumn,
                sizeColumn, modifiedColumn, actionsColumn });
        ColumnViewerToolTipSupport.enableFor(viewer);
    }

    private void setupSorting(TableViewerColumn[] columns) {
        for (int i = 1; i < columns.length - 1; i++) {
            final int index = i;
            columns[i].getColumn().addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (sortColumn == index) {
                        sortDirection = -sortDirection;
                    } else {
                        sortColumn = index;
                        sortDirection = 1;
                    }
                    if (viewer != null && !viewer.getControl().isDisposed()) {
                        viewer.refresh();
                    }
                }
            });
        }
    }

    private class ModelRowComparator extends ViewerComparator {
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            ModelRow a = (ModelRow) e1;
            ModelRow b = (ModelRow) e2;
            int result;
            switch (sortColumn) {
                case 2:
                    result = a.getTechnicalId().compareToIgnoreCase(b.getTechnicalId());
                    break;
                case 3:
                    result = a.getProviderLabel().compareToIgnoreCase(b.getProviderLabel());
                    break;
                case 4:
                    result = a.getTypeLabel().compareToIgnoreCase(b.getTypeLabel());
                    break;
                case 5:
                    result = a.getStatus().compareToIgnoreCase(b.getStatus());
                    break;
                case 6:
                    result = a.getRegistrationState().compareToIgnoreCase(b.getRegistrationState());
                    break;
                case 7:
                    result = a.getEndpoint().compareToIgnoreCase(b.getEndpoint());
                    break;
                case 8:
                    result = a.getLocation().compareToIgnoreCase(b.getLocation());
                    break;
                case 9:
                    result = Long.compare(a.getSize(), b.getSize());
                    break;
                case 10:
                    result = Long.compare(a.getModified(), b.getModified());
                    break;
                default:
                    result = a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
                    break;
            }
            return sortDirection >= 0 ? result : -result;
        }
    }

    private TableViewerColumn createTableViewerColumn(String title, int bound) {
        return EvoTableViewerHelper.createColumn(viewer, title, bound, true, true);
    }

    private void addRightClickSelection() {
        viewer.getTable().addListener(SWT.MouseDown, event -> {
            org.eclipse.swt.widgets.TableItem item = viewer.getTable().getItem(
                    new org.eclipse.swt.graphics.Point(event.x, event.y));
            if (item != null) {
                viewer.getTable().setSelection(item);
            }
        });
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
        manager.add(new Action("Select All Filtered") {
            @Override
            public void run() {
                selectAllFiltered();
            }
        });
        manager.add(new Action("Clear Selection") {
            @Override
            public void run() {
                checkedKeys.clear();
                if (viewer != null) {
                    viewer.setAllChecked(false);
                }
                updateCounts();
            }
        });
        manager.add(new Separator());
        manager.add(new Action("Edit...") {
            @Override
            public void run() {
                handleEditModel();
            }
        });
        manager.add(new Action("Rename...") {
            @Override
            public void run() {
                handleRenameModel();
            }
        });
        manager.add(new Action("Delete Selected...") {
            @Override
            public void run() {
                handleRemoveModel();
            }
        });
        manager.add(new Separator());
        manager.add(new Action("Use") {
            @Override
            public void run() {
                handleUseModel();
            }
        });
        manager.add(new Action("Test") {
            @Override
            public void run() {
                handleTestModel();
            }
        });
        manager.add(new Action("Export to ZIP") {
            @Override
            public void run() {
                handleExportToZip();
            }
        });
        manager.add(new Separator());
        manager.add(new Action("Refresh Discovery") {
            @Override
            public void run() {
                handleReload();
            }
        });
        manager.add(new Action("Add") {
            @Override
            public void run() {
                handleAddModel();
            }
        });
        manager.add(new Action("Download") {
            @Override
            public void run() {
                handleDownloadModel();
            }
        });
        manager.add(new Separator());
        manager.add(new Action("Save to Model") {
            @Override
            public void run() {
                editor.doSave(null);
            }
        });
    }

    // ------------------------------------------------------------------
    // Selection helpers
    // ------------------------------------------------------------------

    private List<ModelRow> getVisibleSelection() {
        List<ModelRow> rows = new ArrayList<>();
        if (viewer == null) {
            return rows;
        }
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (selection != null && !selection.isEmpty()) {
            for (Object obj : selection.toArray()) {
                if (obj instanceof ModelRow row) {
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    /**
     * Rows the next action should operate on: the checked filtered selection when
     * present, otherwise the structured table selection.
     */
    private List<ModelRow> getActionRows() {
        List<ModelRow> checkedRows = getCheckedRows();
        if (!checkedRows.isEmpty()) {
            return checkedRows;
        }
        return getVisibleSelection();
    }

    private List<ModelRow> getCheckedRows() {
        List<ModelRow> rows = new ArrayList<>();
        for (ModelRow row : allRows) {
            if (checkedKeys.contains(row.getKey())) {
                rows.add(row);
            }
        }
        return rows;
    }

    private void selectAllFiltered() {
        checkedKeys.clear();
        for (ModelRow row : filteredRows) {
            checkedKeys.add(row.getKey());
        }
        if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.setCheckedElements(filteredRows.toArray());
        }
        updateCounts();
    }

    private void restoreCheckedState() {
        if (viewer == null || viewer.getControl().isDisposed()) {
            return;
        }
        List<ModelRow> toCheck = new ArrayList<>();
        Set<String> visibleKeys = new LinkedHashSet<>();
        for (ModelRow row : filteredRows) {
            visibleKeys.add(row.getKey());
            if (checkedKeys.contains(row.getKey())) {
                toCheck.add(row);
            }
        }
        // Hidden rows are never part of the bulk selection.
        checkedKeys.retainAll(visibleKeys);
        viewer.setCheckedElements(toCheck.toArray());
    }

    // ------------------------------------------------------------------
    // Filtering
    // ------------------------------------------------------------------

    private void applyFilters() {
        if (viewer == null || viewer.getControl().isDisposed()) {
            return;
        }
        List<ModelRow> filtered = new ArrayList<>();
        for (ModelRow row : allRows) {
            if (!matchesRow(row)) {
                continue;
            }
            filtered.add(row);
        }
        filteredRows.clear();
        filteredRows.addAll(filtered);
        viewer.setInput(filteredRows.toArray());
        restoreCheckedState();
        updateCounts();
    }

    private boolean matchesRow(ModelRow row) {
        if (!row.matchesSearch(filterPattern)) {
            return false;
        }
        if (!matchesProvider(providerFilter, row)) {
            return false;
        }
        if (!matchesType(typeFilter, row)) {
            return false;
        }
        if (!matchesCombo(statusFilter, row.getStatus())) {
            return false;
        }
        if (!matchesCombo(registrationFilter, row.getRegistrationState())) {
            return false;
        }
        if (!isShowUnregistered() && "Unregistered".equals(row.getRegistrationState())) {
            return false;
        }
        return true;
    }

    private boolean isShowUnregistered() {
        return showUnregisteredCheckbox != null
                && !showUnregisteredCheckbox.isDisposed()
                && showUnregisteredCheckbox.getSelection();
    }

    private boolean matchesProvider(Combo combo, ModelRow row) {
        if (combo == null || combo.isDisposed()) {
            return true;
        }
        String selected = combo.getText();
        if (selected == null || selected.isBlank() || ALL.equals(selected)) {
            return true;
        }
        if ("Remote".equals(selected)) {
            return row.getIdentity().getKind() == ModelIdentity.Kind.REMOTE;
        }
        return selected.equalsIgnoreCase(row.getProviderLabel());
    }

    private boolean matchesCombo(Combo combo, String value) {
        if (combo == null || combo.isDisposed()) {
            return true;
        }
        String selected = combo.getText();
        if (selected == null || selected.isBlank() || ALL.equals(selected)) {
            return true;
        }
        return selected.equalsIgnoreCase(value);
    }

    private boolean matchesType(Combo combo, ModelRow row) {
        if (combo == null || combo.isDisposed()) {
            return true;
        }
        String selected = combo.getText();
        if (selected == null || selected.isBlank() || ALL.equals(selected)) {
            return true;
        }
        ModelIdentity.Kind kind = row.getIdentity().getKind();
        if ("Local (Ollama)".equals(selected)) {
            return kind == ModelIdentity.Kind.OLLAMA;
        }
        return kind.getLabel().equalsIgnoreCase(selected);
    }

    private void updateCounts() {
        if (countsLabel == null || countsLabel.isDisposed()) {
            return;
        }
        long registered = allRows.stream()
                .filter(r -> "Registered".equals(r.getRegistrationState()))
                .count();
        int selected = 0;
        for (ModelRow row : allRows) {
            if (checkedKeys.contains(row.getKey())) {
                selected++;
            }
        }
        long unregistered = allRows.stream()
                .filter(r -> "Unregistered".equals(r.getRegistrationState()))
                .count();
        String hidden = (!isShowUnregistered() && unregistered > 0)
                ? "  •  Hidden unregistered: " + unregistered : "";
        countsLabel.setText("Showing " + filteredRows.size() + " of " + allRows.size()
                + "  •  Registered: " + registered + "  •  Selected: " + selected + hidden);
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    public void load() {
        if (loadPending.getAndSet(true)) {
            return;
        }
        CompletableFuture.supplyAsync(this::buildRows)
                .thenAcceptAsync(rows -> {
                    loadPending.set(false);
                    if (viewer == null || viewer.getControl().isDisposed()) {
                        return;
                    }
                    allRows.clear();
                    allRows.addAll(rows);
                    Set<String> available = new LinkedHashSet<>();
                    for (ModelRow row : allRows) {
                        available.add(row.getKey());
                    }
                    checkedKeys.retainAll(available);
                    applyFilters();
                }, Display.getDefault()::asyncExec)
                .exceptionally(error -> {
                    loadPending.set(false);
                    eu.kalafatic.evolution.controller.log.Log.log(
                            "[MODEL] Failed to load model list: " + error.getMessage());
                    return null;
                });
    }

    private List<ModelRow> buildRows() {
        List<ModelRow> rows = new ArrayList<>();
        if (orchestrator == null) {
            return rows;
        }
        Set<String> registeredKeys = new LinkedHashSet<>();
        for (AIProvider registration : orchestrator.getAiProviders()) {
            registeredKeys.add(ModelIdentityResolver.resolve(orchestrator, registration).getCanonicalId());
        }
        Map<String, List<OllamaModel>> ollamaByEndpoint = new LinkedHashMap<>();
        Map<String, ModelRow> unique = new LinkedHashMap<>();
        for (AIProvider provider : ProjectModelManager.getInstance().getAllModels(orchestrator, true)) {
            try {
                ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);
                List<OllamaModel> ollamaModels = null;
                if (identity.isOllama()) {
                    String endpoint = identity.getEndpoint();
                    ollamaModels = ollamaByEndpoint.get(endpoint);
                    if (ollamaModels == null) {
                        ollamaModels = OllamaManager.getInstance().getService(endpoint).loadModels();
                        ollamaByEndpoint.put(endpoint, ollamaModels);
                    }
                }
                ModelRow row = ModelRow.from(orchestrator, provider, registeredKeys, ollamaModels);
                unique.putIfAbsent(row.getKey(), row);
            } catch (Exception e) {
                eu.kalafatic.evolution.controller.log.Log.log(
                        "[MODEL] Failed to build row for '" + provider.getName() + "': " + e.getMessage());
            }
        }
        rows.addAll(unique.values());
        return rows;
    }

    private void handleReload() {
        if (orchestrator != null) {
            String ollamaUrl = (orchestrator.getOllama() != null
                    && orchestrator.getOllama().getUrl() != null)
                    ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
            OllamaManager.getInstance().getService(ollamaUrl).clearCache();
        }
        ProjectModelManager.getInstance().refreshModelDiscovery(orchestrator);
        load();
    }

    // ------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------

    private void handleEditModel() {
        List<ModelRow> rows = getActionRows();
        if (rows.isEmpty()) {
            return;
        }
        if (rows.size() > 1) {
            MessageDialog.openInformation(group.getShell(), "Edit Models",
                    "Editing metadata is supported for one model at a time.\n"
                            + "Use the selected single row or narrow the selection.");
            return;
        }
        ModelRow row = rows.get(0);
        ModelEditDialog dialog = new ModelEditDialog(group.getShell(), orchestrator, row, false);
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            editor.setDirty(true);
            persistEditorChanges();
            load();
            refreshConsumers();
            if (dialog.getResultMessage() != null && !dialog.getResultMessage().isBlank()) {
                MessageDialog.openInformation(group.getShell(), "Edit Model", dialog.getResultMessage());
            }
        }
    }

    private void handleRenameModel() {
        List<ModelRow> rows = getActionRows();
        if (rows.isEmpty()) {
            return;
        }
        if (rows.size() > 1) {
            MessageDialog.openInformation(group.getShell(), "Rename Models",
                    "Rename operates on one model at a time because runtime and artifact identity "
                            + "must be updated atomically.");
            return;
        }
        ModelRow row = rows.get(0);
        ModelEditDialog dialog = new ModelEditDialog(group.getShell(), orchestrator, row, true);
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            editor.setDirty(true);
            persistEditorChanges();
            load();
            refreshConsumers();
            MessageDialog.openInformation(group.getShell(), "Rename Model", dialog.getResultMessage());
        }
    }

    private void handleUseModel() {
        List<ModelRow> rows = getActionRows();
        if (rows.isEmpty() || orchestrator == null) {
            return;
        }
        ModelRow row = rows.get(0);
        AIProvider item = row.getProvider();
        ProjectModelManager modelManager = ProjectModelManager.getInstance();
        if (item.isLocal()) {
            modelManager.updateOllamaSettings(orchestrator,
                    orchestrator.getOllama() != null ? orchestrator.getOllama().getUrl() : null,
                    item.getName(),
                    orchestrator.getOllama() != null ? orchestrator.getOllama().getPath() : null);
            modelManager.updateLocalModel(orchestrator, item.getName());
            modelManager.updateLlmSettings(orchestrator, item.getName(),
                    orchestrator.getLlm() != null ? orchestrator.getLlm().getTemperature() : 1.0f);
        } else {
            modelManager.updateRemoteModel(orchestrator, item.getName());
            modelManager.updateLlmSettings(orchestrator, item.getName(),
                    orchestrator.getLlm() != null ? orchestrator.getLlm().getTemperature() : 1.0f);
        }
        editor.setDirty(true);
        refreshConsumers();
    }

    private void handleTestModel() {
        List<ModelRow> rows = getActionRows();
        if (rows.isEmpty()) {
            return;
        }
        for (ModelRow row : rows) {
            testModel(row.getProvider());
        }
    }

    private void testModel(AIProvider item) {
        final AIProvider providerToTest = ProjectModelManager.getInstance()
                .findRegisteredProviders(orchestrator,
                        ModelIdentityResolver.resolve(orchestrator, item))
                .stream().findFirst().orElse(item);

        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job(
                "Testing Model: " + providerToTest.getName()) {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                try {
                    eu.kalafatic.evolution.controller.services.ModelEvaluationService service =
                            new eu.kalafatic.evolution.controller.services.ModelEvaluationService();
                    File projectRoot = null;
                    if (editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
                        projectRoot = ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput())
                                .getFile().getProject().getLocation().toFile();
                    }
                    TaskContext context = new TaskContext(orchestrator, projectRoot);
                    service.evaluateModel(orchestrator, providerToTest, context);
                    Display.getDefault().asyncExec(() -> {
                        load();
                        editor.setDirty(true);
                        MessageDialog.openInformation(group.getShell(), "Test Complete",
                                "Evaluation finished for " + providerToTest.getName() + ".\n"
                                        + "Overall: " + providerToTest.getRating() + "\n"
                                        + "A: " + providerToTest.getRatingAnalyze()
                                        + " CH: " + providerToTest.getRatingChat()
                                        + " P: " + providerToTest.getRatingProgramming());
                    });
                    return org.eclipse.core.runtime.Status.OK_STATUS;
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> MessageDialog.openError(group.getShell(),
                            "Test Error", "Failed to evaluate model: " + e.getMessage()));
                    return org.eclipse.core.runtime.Status.CANCEL_STATUS;
                }
            }
        };
        job.schedule();
    }

    private void handleRemoveModel() {
        List<ModelRow> rows = getActionRows();
        if (rows.isEmpty()) {
            return;
        }
        Map<String, ModelRow> unique = new LinkedHashMap<>();
        for (ModelRow row : rows) {
            unique.putIfAbsent(row.getKey(), row);
        }
        List<ModelRow> selected = new ArrayList<>(unique.values());

        ModelDeleteDialog dialog = new ModelDeleteDialog(group.getShell(), selected);
        if (dialog.open() != org.eclipse.jface.window.Window.OK) {
            return;
        }

        ModelRemovalService.RemovalOptions options = ModelRemovalService.RemovalOptions.defaults()
                .deregisterRuntime(dialog.isDeregisterRuntime())
                .deleteOwnedArtifacts(dialog.isDeleteArtifacts())
                .deleteSharedArtifacts(dialog.isDeleteSharedArtifacts());
        List<AIProvider> providers = new ArrayList<>();
        for (ModelRow row : selected) {
            providers.add(row.getProvider());
        }

        setActionButtonsEnabled(false);
        CompletableFuture
                .supplyAsync(() -> ModelRemovalService.getInstance()
                        .removeModels(orchestrator, providers, options))
                .thenAcceptAsync(results -> {
                    setActionButtonsEnabled(true);
                    if (viewer == null || viewer.getControl().isDisposed()) {
                        return;
                    }
                    editor.setDirty(true);
                    boolean registryChanged = results.stream()
                            .anyMatch(r -> r.getStatus() != eu.kalafatic.evolution.controller.manager.removal.ModelRemovalStatus.FAILED);
                    if (registryChanged) {
                        persistEditorChanges();
                    }
                    ProjectModelManager.getInstance().notifyModelsChanged();
                    load();
                    refreshConsumers();
                    new ModelRemovalReportDialog(group.getShell(), results).open();
                }, Display.getDefault()::asyncExec)
                .exceptionally(error -> {
                    Display.getDefault().asyncExec(() -> {
                        setActionButtonsEnabled(true);
                        MessageDialog.openError(group.getShell(), "Model Removal Error",
                                "An unexpected error occurred during model removal: " + error.getMessage());
                    });
                    return null;
                });
    }

    /**
     * Persists registry/configuration changes immediately so a removed or
     * renamed model does not return after the editor is reopened.
     */
    private void persistEditorChanges() {
        if (editor == null) {
            return;
        }
        try {
            editor.doSave(null);
        } catch (Exception e) {
            eu.kalafatic.evolution.controller.log.Log.log(
                    "[MODEL] Failed to persist model configuration: " + e.getMessage());
        }
    }

    private void refreshConsumers() {
        try {
            ProjectModelManager.getInstance().notifyModelsChanged();
            if (editor != null) {
                editor.refreshPages();
                if (editor.getAiChatPage() != null) {
                    editor.getAiChatPage().refreshModelSelections();
                }
            }
            if (page != null) {
                page.refreshUI();
            }
        } catch (Exception e) {
            eu.kalafatic.evolution.controller.log.Log.log(
                    "[MODEL] Failed to refresh model consumers: " + e.getMessage());
        }
    }

    private void handleExportToZip() {
        List<ModelRow> rows = getActionRows();
        if (rows.isEmpty()) {
            return;
        }
        ModelRow row = rows.get(0);
        String path = row.getLocation();
        if (path == null || path.isEmpty()) {
            MessageDialog.openError(group.getShell(), "Export Model",
                    "Could not find a physical file path for model: " + row.getDisplayName());
            return;
        }
        File file = new File(path);
        if (!file.exists()) {
            MessageDialog.openError(group.getShell(), "Export Model",
                    "Model artifact does not exist on disk: " + path);
            return;
        }

        org.eclipse.swt.widgets.FileDialog fileDialog =
                new org.eclipse.swt.widgets.FileDialog(group.getShell(), SWT.SAVE);
        fileDialog.setFilterExtensions(new String[] { "*.zip" });
        fileDialog.setFileName(row.getTechnicalId().replace("/", "_").replace(":", "_") + ".zip");
        eu.kalafatic.utils.application.FileUtils.configureDialogFilterPath(fileDialog, null);
        String destinationPath = fileDialog.open();
        if (destinationPath == null) {
            return;
        }
        File zipFile = new File(destinationPath);
        try {
            File parentDir = file.getParentFile();
            boolean isSpecificModelFolder = false;
            if (parentDir != null && parentDir.isDirectory()) {
                String parentName = parentDir.getName().toLowerCase(Locale.ROOT);
                if (parentName.startsWith("evo-") || parentName.startsWith("forging-")
                        || (parentDir.getParentFile() != null
                                && parentDir.getParentFile().getName().equalsIgnoreCase("forge-output"))) {
                    isSpecificModelFolder = true;
                }
            }
            if (isSpecificModelFolder) {
                eu.kalafatic.evolution.view.util.ZipUtil.pack(parentDir, zipFile);
            } else {
                try (java.util.zip.ZipOutputStream zos =
                             new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))) {
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);
                    java.nio.file.Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
            MessageDialog.openInformation(group.getShell(), "Export Model",
                    "Model exported successfully to " + zipFile.getAbsolutePath());
        } catch (Exception ex) {
            MessageDialog.openError(group.getShell(), "Export Model Error",
                    "Failed to export model to ZIP: " + ex.getMessage());
        }
    }

    private void handleDownloadModel() {
        if (orchestrator == null) {
            return;
        }
        String ollamaUrl = (orchestrator.getOllama() != null)
                ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
        ModelDownloadDialog dialog = new ModelDownloadDialog(group.getShell(), ollamaUrl);
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            String modelName = dialog.getDownloadedModelName();
            if (modelName != null && !modelName.isEmpty()) {
                boolean exists = orchestrator.getAiProviders().stream()
                        .anyMatch(p -> modelName.equalsIgnoreCase(p.getName()) && p.isLocal());
                if (!exists) {
                    AIProvider newProvider =
                            eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE
                                    .createAIProvider();
                    newProvider.setName(modelName);
                    newProvider.setLocal(true);
                    newProvider.setUrl(ollamaUrl);
                    newProvider.setFormat("ollama");
                    orchestrator.getAiProviders().add(newProvider);
                    editor.setDirty(true);
                }
                // Explicit download is an explicit rediscovery: clear any prior tombstone.
                ProjectModelManager.getInstance().getModelStateStore().unsuppress(
                        ModelIdentityResolver.resolve(orchestrator, orchestrator.getAiProviders().stream()
                                .filter(p -> modelName.equalsIgnoreCase(p.getName()))
                                .findFirst().orElse(createTransient(modelName, ollamaUrl))).getCanonicalId());
                ProjectModelManager.getInstance().refreshModelDiscovery(orchestrator);
                load();
                refreshConsumers();
            }
        }
    }

    private AIProvider createTransient(String name, String url) {
        AIProvider provider = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE
                .createAIProvider();
        provider.setName(name);
        provider.setLocal(true);
        provider.setUrl(url);
        provider.setFormat("ollama");
        return provider;
    }

    private void handleAddModel() {
        if (orchestrator == null) {
            return;
        }
        String[] options = { "Local (Ollama)", "Remote" };
        MessageDialog dialog = new MessageDialog(group.getShell(), "Add Model", null,
                "Select the type of model to add:", MessageDialog.QUESTION, options, 0);
        int result = dialog.open();
        if (result == 0) {
            org.eclipse.jface.dialogs.InputDialog input = new org.eclipse.jface.dialogs.InputDialog(
                    group.getShell(), "Add Local Model",
                    "Enter the name of the model to run (e.g., gemma, llama3):", "", null);
            if (input.open() == org.eclipse.jface.dialogs.InputDialog.OK) {
                String modelName = input.getValue();
                if (modelName != null && !modelName.trim().isEmpty()) {
                    AIProvider newProvider =
                            eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE
                                    .createAIProvider();
                    newProvider.setName(modelName.trim());
                    newProvider.setLocal(true);
                    String ollamaUrl = (orchestrator.getOllama() != null)
                            ? orchestrator.getOllama().getUrl() : "http://localhost:11434";
                    newProvider.setUrl(ollamaUrl);
                    newProvider.setFormat("ollama");
                    orchestrator.getAiProviders().add(newProvider);
                    ProjectModelManager.getInstance().getModelStateStore().unsuppress(
                            ModelIdentityResolver.resolve(orchestrator, newProvider).getCanonicalId());
                    ProjectModelManager.getInstance().refreshModelDiscovery(orchestrator);
                    editor.setDirty(true);
                    load();
                    refreshConsumers();
                }
            }
        } else if (result == 1) {
            AIProvider newProvider =
                    eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE
                            .createAIProvider();
            newProvider.setName("new-provider");
            newProvider.setFormat("openai");
            ModelDetailsDialog detailsDialog = new ModelDetailsDialog(group.getShell(), newProvider);
            if (detailsDialog.open() == org.eclipse.jface.window.Window.OK) {
                if (orchestrator != null) {
                    orchestrator.getAiProviders().add(newProvider);
                    editor.setDirty(true);
                    load();
                    refreshConsumers();
                }
            }
        }
    }

    private void setActionButtonsEnabled(boolean enabled) {
        Button[] buttons = { selectAllButton, clearSelectionButton, reloadButton, editButton,
                renameButton, removeButton, testButton, useButton, addButton, downloadButton,
                exportButton, saveButton };
        for (Button button : buttons) {
            if (button != null && !button.isDisposed()) {
                button.setEnabled(enabled);
            }
        }
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null) {
            return;
        }
        if (allRows.isEmpty()) {
            load();
        } else {
            applyFilters();
        }
    }
}
