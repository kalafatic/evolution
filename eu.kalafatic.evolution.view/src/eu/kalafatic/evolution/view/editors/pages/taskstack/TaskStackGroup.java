package eu.kalafatic.evolution.view.editors.pages.taskstack;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;
import eu.kalafatic.utils.factories.GUIFactory;

import eu.kalafatic.evolution.controller.orchestration.behavior.BitState;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TaskStackGroup extends AEvoGroup {
    private FormToolkit toolkit;
    private TaskStackPage page;
    private CheckboxTreeViewer treeViewer;

    public TaskStackGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, TaskStackPage page) {
        super(editor, orchestrator);
        this.toolkit = toolkit;
        this.page = page;
        createControl(parent);
    }

    @Override
    public void refreshUI() {
        if (treeViewer != null && !treeViewer.getControl().isDisposed()) {
            treeViewer.refresh();
        }
    }

    public void scheduleRefresh() {
        if (refreshPending.compareAndSet(false, true)) {
            org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
                refreshPending.set(false);
                if (treeViewer != null && !treeViewer.getControl().isDisposed()) {
                    treeViewer.refresh();
                }
            });
        }
    }

    public void openTaskEditDialog(Task task) {
        TaskEditDialog dialog = new TaskEditDialog(treeViewer.getControl().getShell(), task, page);
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            treeViewer.refresh(task);
            page.setDirty(true);
        }
    }

    private void createControl(Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Task/Prompt Stack", 1, true, true);
        group.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 400;
        group.setLayoutData(gd);

        Tree tree = toolkit.createTree(group, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));

        treeViewer = new CheckboxTreeViewer(tree);
        treeViewer.setContentProvider(new TaskTreeContentProvider());
        treeViewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(Object element) {
                return ((Task) element).isSelected();
            }
            @Override
            public boolean isGrayed(Object element) {
                return false;
            }
        });

        createColumns();

        treeViewer.addCheckStateListener(event -> {
            Task task = (Task) event.getElement();
            task.setSelected(event.getChecked());
            page.setDirty(true);
        });

        treeViewer.setInput(orchestrator);
        treeViewer.expandAll();
        
        // Add listener for checkbox changes
        treeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                Object element = event.getElement();
                boolean checked = event.getChecked();
                
                Task task = (Task) element;
                task.setSelected(checked);
               
            }
        });

        tree.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
            @Override
            public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
                org.eclipse.swt.widgets.TreeItem item = tree.getItem(new org.eclipse.swt.graphics.Point(e.x, e.y));
                if (item != null) {
                    Task task = (Task) item.getData();
                    int column = -1;
                    for (int i = 0; i < tree.getColumnCount(); i++) {
                        if (item.getBounds(i).contains(e.x, e.y)) {
                            column = i;
                            break;
                        }
                    }
                    if (column == 0) { // Run
                        page.runSingleTask(task);
                    } else if (column == 12) { // Result
                        editor.openTaskResult(task);
                    } else if (column == 1) { // Edit
                        openTaskEditDialog(task);
                    }
                }
            }
        });
    }

    private void createColumns() {
        // Run Column
        TreeViewerColumn runCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        runCol.getColumn().setText("Run");
        runCol.getColumn().setWidth(200);
        runCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
            	String name =((Task) element).getId();
            	name = name != null ? name : ((Task) element).getName();
                return "\u25B6" + "			"+ name; // Play icon +id
            }
        });

        // Edit Column
        TreeViewerColumn editCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        editCol.getColumn().setText("Edit");
        editCol.getColumn().setWidth(40);
        editCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return "\u270E"; // Pencil icon
            }
        });
        
        // Prompt Column
        TreeViewerColumn promptCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
        promptCol.getColumn().setText("Prompt");
        promptCol.getColumn().setWidth(200);
        promptCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                String prompt = ((Task) element).getPrompt();
                return prompt != null ? prompt : "";
            }
        });
        promptCol.setEditingSupport(new TaskAttributeEditingSupport(treeViewer, "prompt"));
        
        
        // Attachments Column
        TreeViewerColumn attachmentsCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
        attachmentsCol.getColumn().setText("Attachments");
        attachmentsCol.getColumn().setWidth(100);
        attachmentsCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return String.join(", ", ((Task) element).getAttachments());
            }
        });

        
        // Timer Column (Commented out)
        /*
        TreeViewerColumn timerCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        timerCol.getColumn().setText("Timer");
        timerCol.getColumn().setWidth(60);
        timerCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return page.getCountdown((Task) element);
            }
            @Override
            public org.eclipse.swt.graphics.Color getForeground(Object element) {
                return treeViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_RED);
            }
        });
        */

        // Date Column
        TreeViewerColumn dateCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
        dateCol.getColumn().setText("Date");
        dateCol.getColumn().setWidth(100);
        dateCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Task) element).getScheduledTime();
            }
        });
        dateCol.setEditingSupport(new DateEditingSupport(treeViewer));

//        // Session ID Column
//        TreeViewerColumn idCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
//        idCol.getColumn().setText("Session ID");
//        idCol.getColumn().setWidth(40);
//        idCol.setLabelProvider(new ColumnLabelProvider() {
//            @Override
//            public String getText(Object element) {
//                return ((Task) element).getId();
//            }
//        });
//        idCol.setEditingSupport(new TaskAttributeEditingSupport(treeViewer, "id"));
//
//       
//        // Tasks Column
//        TreeViewerColumn nameCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
//        nameCol.getColumn().setText("Tasks");
//        nameCol.getColumn().setWidth(300);
//        nameCol.setLabelProvider(new ColumnLabelProvider() {
//            @Override
//            public String getText(Object element) {
//                return ((Task) element).getName();
//            }
//        });
//        nameCol.setEditingSupport(new TaskAttributeEditingSupport(treeViewer, "name"));

        
        // Type Column
        TreeViewerColumn typeCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
        typeCol.getColumn().setText("Type");
        typeCol.getColumn().setWidth(80);
        typeCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                String type = ((Task) element).getType();
                return type != null ? type : "";
            }
        });
        typeCol.setEditingSupport(new TaskAttributeEditingSupport(treeViewer, "type"));

        // Mode Column
        TreeViewerColumn modeCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
        modeCol.getColumn().setText("Mode");
        modeCol.getColumn().setWidth(80);
        modeCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                int mode = BitState.getMode(((Task) element).getBitState());
                if (mode >= 0 && mode < BitState.MODES.length) {
                    return BitState.MODES[mode];
                }
                return BitState.MODES[0];
            }
        });
        modeCol.setEditingSupport(new EditingSupport(treeViewer) {
            @Override protected CellEditor getCellEditor(Object element) {
                return new org.eclipse.jface.viewers.ComboBoxCellEditor(treeViewer.getTree(), BitState.MODES, SWT.READ_ONLY);
            }
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected Object getValue(Object element) {
                return BitState.getMode(((Task) element).getBitState());
            }
            @Override protected void setValue(Object element, Object value) {
                Task task = (Task) element;
                int selectedMode = (Integer) value;
                long currentBitState = task.getBitState();
                task.setBitState(BitState.encode(
                    selectedMode,
                    BitState.getSupervision(currentBitState),
                    BitState.getInteraction(currentBitState),
                    BitState.getReasoning(currentBitState),
                    BitState.getWorkflow(currentBitState)
                ));
                treeViewer.update(element, null);
                page.setDirty(true);
            }
        });

        // Iterative Column
        TreeViewerColumn iterativeCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        iterativeCol.getColumn().setText("Iterative");
        iterativeCol.getColumn().setWidth(70);
        iterativeCol.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return ((Task) element).isIterativeMode() ? "YES" : "NO"; }
        });
        iterativeCol.setEditingSupport(new EditingSupport(treeViewer) {
            @Override protected CellEditor getCellEditor(Object element) { return new CheckboxCellEditor(treeViewer.getTree()); }
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected Object getValue(Object element) { return ((Task) element).isIterativeMode(); }
            @Override protected void setValue(Object element, Object value) {
                ((Task) element).setIterativeMode((Boolean) value);
                treeViewer.update(element, null);
                page.setDirty(true);
            }
        });

        // Self-Dev Column
        TreeViewerColumn selfDevCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        selfDevCol.getColumn().setText("Self-Dev");
        selfDevCol.getColumn().setWidth(70);
        selfDevCol.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return ((Task) element).isSelfIterativeMode() ? "YES" : "NO"; }
        });
        selfDevCol.setEditingSupport(new EditingSupport(treeViewer) {
            @Override protected CellEditor getCellEditor(Object element) { return new CheckboxCellEditor(treeViewer.getTree()); }
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected Object getValue(Object element) { return ((Task) element).isSelfIterativeMode(); }
            @Override protected void setValue(Object element, Object value) {
                ((Task) element).setSelfIterativeMode((Boolean) value);
                treeViewer.update(element, null);
                page.setDirty(true);
            }
        });

        // Darwin Column
        TreeViewerColumn darwinCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        darwinCol.getColumn().setText("Darwin");
        darwinCol.getColumn().setWidth(70);
        darwinCol.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return ((Task) element).isDarwinMode() ? "YES" : "NO"; }
        });
        darwinCol.setEditingSupport(new EditingSupport(treeViewer) {
            @Override protected CellEditor getCellEditor(Object element) { return new CheckboxCellEditor(treeViewer.getTree()); }
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected Object getValue(Object element) { return ((Task) element).isDarwinMode(); }
            @Override protected void setValue(Object element, Object value) {
                ((Task) element).setDarwinMode((Boolean) value);
                treeViewer.update(element, null);
                page.setDirty(true);
            }
        });

        // Auto-Git Column
        TreeViewerColumn autoGitCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        autoGitCol.getColumn().setText("Auto-Git");
        autoGitCol.getColumn().setWidth(70);
        autoGitCol.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return ((Task) element).isGitAutomation() ? "YES" : "NO"; }
        });
        autoGitCol.setEditingSupport(new EditingSupport(treeViewer) {
            @Override protected CellEditor getCellEditor(Object element) { return new CheckboxCellEditor(treeViewer.getTree()); }
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected Object getValue(Object element) { return ((Task) element).isGitAutomation(); }
            @Override protected void setValue(Object element, Object value) {
                ((Task) element).setGitAutomation((Boolean) value);
                treeViewer.update(element, null);
                page.setDirty(true);
            }
        });

        // Step Mode Column
        TreeViewerColumn stepModeCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        stepModeCol.getColumn().setText("Step Mode");
        stepModeCol.getColumn().setWidth(70);
        stepModeCol.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return ((Task) element).isStepMode() ? "YES" : "NO"; }
        });
        stepModeCol.setEditingSupport(new EditingSupport(treeViewer) {
            @Override protected CellEditor getCellEditor(Object element) { return new CheckboxCellEditor(treeViewer.getTree()); }
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected Object getValue(Object element) { return ((Task) element).isStepMode(); }
            @Override protected void setValue(Object element, Object value) {
                ((Task) element).setStepMode((Boolean) value);
                treeViewer.update(element, null);
                page.setDirty(true);
            }
        });

        // Max Iterations Column
        TreeViewerColumn maxIterCol = new TreeViewerColumn(treeViewer, SWT.CENTER);
        maxIterCol.getColumn().setText("Max Iter");
        maxIterCol.getColumn().setWidth(70);
        maxIterCol.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) { return String.valueOf(((Task) element).getMaxIterations()); }
        });
        maxIterCol.setEditingSupport(new EditingSupport(treeViewer) {
            @Override protected CellEditor getCellEditor(Object element) { return new TextCellEditor(treeViewer.getTree()); }
            @Override protected boolean canEdit(Object element) { return true; }
            @Override protected Object getValue(Object element) { return String.valueOf(((Task) element).getMaxIterations()); }
            @Override protected void setValue(Object element, Object value) {
                try {
                    ((Task) element).setMaxIterations(Integer.parseInt(String.valueOf(value)));
                    treeViewer.update(element, null);
                    page.setDirty(true);
                } catch (NumberFormatException e) {}
            }
        });

        // State Column
        TreeViewerColumn stateCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
        stateCol.getColumn().setText("State");
        stateCol.getColumn().setWidth(50);
        stateCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Task) element).getStatus().toString();
            }
            @Override
            public org.eclipse.swt.graphics.Color getForeground(Object element) {
                return getStatusColor(((Task) element).getStatus());
            }
        });

        // Result Column
        TreeViewerColumn resultCol = new TreeViewerColumn(treeViewer, SWT.LEFT);
        resultCol.getColumn().setText("Result");
        resultCol.getColumn().setWidth(200);
        resultCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                String res = ((Task) element).getResultSummary();
                return res != null ? res : "";
            }
            @Override
            public org.eclipse.swt.graphics.Color getForeground(Object element) {
                return treeViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_BLUE);
            }
        });
    }
   
    
    private org.eclipse.swt.graphics.Color getStatusColor(TaskStatus status) {
        int color = SWT.COLOR_BLACK;
        switch (status) {
            case READY: color = SWT.COLOR_DARK_GRAY; break;
            case PENDING: color = SWT.COLOR_BLACK; break;
            case RUNNING: color = SWT.COLOR_BLUE; break;
            case DONE: color = SWT.COLOR_DARK_GREEN; break;
            case FAILED: color = SWT.COLOR_RED; break;
            case WAITING_FOR_APPROVAL: color = SWT.COLOR_DARK_YELLOW; break;
            default: break;
        }
        return treeViewer.getControl().getDisplay().getSystemColor(color);
    }

    public void clear() {}
    public void createTaskRow(Task task) {}
    public void updateStatusColor(org.eclipse.swt.widgets.Label label, TaskStatus status) {}

    public void layout() {
        group.layout(true, true);
    }

    private class TaskTreeContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Orchestrator) {
                return ((Orchestrator) inputElement).getTasks().toArray();
            }
            return new Object[0];
        }
        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof Task) {
                return ((Task) parentElement).getSubTasks().toArray();
            }
            return new Object[0];
        }
        @Override
        public Object getParent(Object element) {
            if (element instanceof Task) {
                return ((Task) element).eContainer();
            }
            return null;
        }
        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof Task) {
                return !((Task) element).getSubTasks().isEmpty();
            }
            return false;
        }
    }

    private class TaskAttributeEditingSupport extends EditingSupport {
        private String attribute;
        private TextCellEditor editor;
        public TaskAttributeEditingSupport(CheckboxTreeViewer viewer, String attribute) {
            super(viewer);
            this.attribute = attribute;
            this.editor = new TextCellEditor(viewer.getTree());
        }
        @Override protected CellEditor getCellEditor(Object element) { return editor; }
        @Override protected boolean canEdit(Object element) { return true; }
        @Override protected Object getValue(Object element) {
            Task task = (Task) element;
            return switch (attribute) {
                case "id" -> task.getId() != null ? task.getId() : "";
                case "name" -> task.getName() != null ? task.getName() : "";
                case "type" -> task.getType() != null ? task.getType() : "";
                case "prompt" -> task.getPrompt() != null ? task.getPrompt() : "";
                default -> "";
            };
        }
        @Override protected void setValue(Object element, Object value) {
            Task task = (Task) element;
            String strValue = (String) value;
            switch (attribute) {
                case "id" -> task.setId(strValue);
                case "name" -> task.setName(strValue);
                case "type" -> task.setType(strValue);
                case "prompt" -> task.setPrompt(strValue);
            }
            getViewer().update(element, null);
            page.setDirty(true);
        }
    }

    private class DateEditingSupport extends EditingSupport {
        public DateEditingSupport(CheckboxTreeViewer viewer) { super(viewer); }
        @Override protected CellEditor getCellEditor(Object element) { return new DateCellEditor(treeViewer.getTree()); }
        @Override protected boolean canEdit(Object element) { return true; }
        @Override protected Object getValue(Object element) { return ((Task) element).getScheduledTime(); }
        @Override protected void setValue(Object element, Object value) {
            ((Task) element).setScheduledTime((String) value);
            getViewer().update(element, null);
            page.setDirty(true);
        }
    }

    private class DateCellEditor extends DialogCellEditor {
        public DateCellEditor(Composite parent) { super(parent); }
        @Override
        protected Object openDialogBox(Control cellEditorWindow) {
            DateSelectionDialog dialog = new DateSelectionDialog(cellEditorWindow.getShell());
            if (dialog.open() == Dialog.OK) {
                return dialog.getSelectedDate();
            }
            return null;
        }
    }

    private class DateSelectionDialog extends Dialog {
        private DateTime calendar;
        private DateTime time;
        private String selectedDate;
        public DateSelectionDialog(Shell parentShell) { super(parentShell); }
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(1, false));
            calendar = new DateTime(container, SWT.CALENDAR | SWT.BORDER);
            time = new DateTime(container, SWT.TIME | SWT.SHORT);
            return container;
        }
        @Override
        protected void okPressed() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
            Calendar cal = Calendar.getInstance();
            cal.set(calendar.getYear(), calendar.getMonth(), calendar.getDay(), time.getHours(), time.getMinutes());
            selectedDate = sdf.format(cal.getTime());
            super.okPressed();
        }
        public String getSelectedDate() { return selectedDate; }
    }

	public CheckboxTreeViewer getTreeViewer() {
		return treeViewer;
	}
}
