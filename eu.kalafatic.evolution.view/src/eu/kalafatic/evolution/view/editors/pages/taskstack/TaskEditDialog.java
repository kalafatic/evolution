package eu.kalafatic.evolution.view.editors.pages.taskstack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.controller.orchestration.behavior.BitState;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;
import eu.kalafatic.utils.dialogs.DynamicField;
import eu.kalafatic.utils.dialogs.DynamicMapDialog;
import eu.kalafatic.utils.factories.GUIFactory;

public class TaskEditDialog extends DynamicMapDialog {
    private Task task;
    private TaskStackPage page;

    private ListViewer attachmentsViewer;
    private List<String> attachmentsList;

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MODE = "mode";
    private static final String SUPERVISION = "supervision";
    private static final String REASONING = "reasoning";
    private static final String WORKFLOW = "workflow";
    private static final String PROMPT = "prompt";
    private static final String GOAL = "goal";
    private static final String DESCRIPTION = "description";
    private static final String APPROVAL_REQUIRED = "approvalRequired";
    private static final String ITERATIVE_MODE = "iterativeMode";
    private static final String SELF_ITERATIVE_MODE = "selfIterativeMode";
    private static final String DARWIN_MODE = "darwinMode";
    private static final String GIT_AUTOMATION = "gitAutomation";
    private static final String STEP_MODE = "stepMode";
    private static final String MAX_ITERATIONS = "maxIterations";
    private static final String ATTACHMENTS = "attachments";

    public TaskEditDialog(Shell parentShell, Task task, TaskStackPage page) {
        super(parentShell, createFields(task));
        this.task = task;
        this.page = page;
        this.attachmentsList = new ArrayList<>(task.getAttachments());
        setTitle("Edit Task: " + task.getName());
        setContainerWidth(700);
    }

    private static LinkedHashMap<String, DynamicField> createFields(Task task) {
        LinkedHashMap<String, DynamicField> fields = new LinkedHashMap<>();
        fields.put(NAME, new DynamicField("Name:", DynamicField.TYPE_TEXT, task.getName()));
        fields.put(TYPE, new DynamicField("Type:", DynamicField.TYPE_COMBO, task.getType(), "chat", "coding", "llm", "file", "shell", "git", "maven", "approval"));
        
        int mode = BitState.getMode(task.getBitState());
        fields.put(MODE, new DynamicField("Mode:", DynamicField.TYPE_COMBO, BitState.MODES[mode >= 0 && mode < BitState.MODES.length ? mode : 0], (Object[]) BitState.MODES));

        int supervision = BitState.getSupervision(task.getBitState());
        fields.put(SUPERVISION, new DynamicField("Supervision:", DynamicField.TYPE_COMBO, BitState.SUPERVISIONS[supervision >= 0 && supervision < BitState.SUPERVISIONS.length ? supervision : 0], (Object[]) BitState.SUPERVISIONS));

        int reasoning = BitState.getReasoning(task.getBitState());
        fields.put(REASONING, new DynamicField("Reasoning:", DynamicField.TYPE_COMBO, BitState.REASONINGS[reasoning >= 0 && reasoning < BitState.REASONINGS.length ? reasoning : 0], (Object[]) BitState.REASONINGS));

        int workflow = BitState.getWorkflow(task.getBitState());
        fields.put(WORKFLOW, new DynamicField("Workflow:", DynamicField.TYPE_COMBO, BitState.WORKFLOWS[workflow >= 0 && workflow < BitState.WORKFLOWS.length ? workflow : 0], (Object[]) BitState.WORKFLOWS));
        
        fields.put(PROMPT, new DynamicField("Prompt:", DynamicField.TYPE_TEXT | DynamicField.MULTILINE, task.getPrompt() != null ? task.getPrompt() : ""));
        fields.put(GOAL, new DynamicField("Goal:", DynamicField.TYPE_TEXT, task.getGoal() != null ? task.getGoal() : ""));
        fields.put(DESCRIPTION, new DynamicField("Description:", DynamicField.TYPE_TEXT, task.getDescription() != null ? task.getDescription() : ""));
        
        // Execution Controls - we'll handle them specially in createFieldEditor or just use checkboxes
        fields.put(APPROVAL_REQUIRED, new DynamicField("Approval Required", DynamicField.TYPE_CHECKBOX, task.isApprovalRequired()));
        fields.put(ITERATIVE_MODE, new DynamicField("Iterative Mode", DynamicField.TYPE_CHECKBOX, task.isIterativeMode()));
        fields.put(SELF_ITERATIVE_MODE, new DynamicField("Self-Dev Mode", DynamicField.TYPE_CHECKBOX, task.isSelfIterativeMode()));
        fields.put(DARWIN_MODE, new DynamicField("Darwin Mode", DynamicField.TYPE_CHECKBOX, task.isDarwinMode()));
        fields.put(GIT_AUTOMATION, new DynamicField("Git Automation", DynamicField.TYPE_CHECKBOX, task.isGitAutomation()));
        fields.put(STEP_MODE, new DynamicField("Step Mode", DynamicField.TYPE_CHECKBOX, task.isStepMode()));
        
        fields.put(MAX_ITERATIONS, new DynamicField("Max Iterations:", DynamicField.TYPE_SPINNER, task.getMaxIterations()));
        
        fields.put(ATTACHMENTS, new DynamicField("Attachments:", DynamicField.TYPE_TEXT, "")); // Dummy for position

        return fields;
    }

    @Override
    protected void createFieldEditor(Composite parent, String key, DynamicField field) {
        if (ATTACHMENTS.equals(key)) {
            GUIFactory.INSTANCE.createLabel(parent, "Attachments:");
            Composite attachComp = GUIFactory.INSTANCE.createComposite(parent, 2, SWT.NONE);
            attachComp.setLayoutData(new GridData(GridData.FILL_BOTH));

            attachmentsViewer = new ListViewer(attachComp, SWT.BORDER | SWT.V_SCROLL);
            attachmentsViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            attachmentsViewer.setContentProvider(ArrayContentProvider.getInstance());
            attachmentsViewer.setInput(attachmentsList);

            Composite btnComp = GUIFactory.INSTANCE.createComposite(attachComp);

            Button addBtn = GUIFactory.INSTANCE.createButton(btnComp, "Add");
            addBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override
                public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                    InputDialog dlg = new InputDialog(getShell(), "Add Attachment", "Enter file path:", "", null);
                    if (dlg.open() == Window.OK) {
                        attachmentsList.add(dlg.getValue());
                        attachmentsViewer.refresh();
                    }
                }
            });

            Button removeBtn = GUIFactory.INSTANCE.createButton(btnComp, "Remove");
            removeBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override
                public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                    int index = attachmentsViewer.getList().getSelectionIndex();
                    if (index != -1) {
                        attachmentsList.remove(index);
                        attachmentsViewer.refresh();
                    }
                }
            });

            controls.put(ATTACHMENTS, attachComp);
            return;
        }

        super.createFieldEditor(parent, key, field);
    }

    @Override
    protected void okPressed() {
        if (!validate()) return;
        saveValues();

        task.setName(getString(NAME));
        task.setType(getString(TYPE));

        // Find indices for BitState
        int selectedMode = findIndex(BitState.MODES, getString(MODE));
        int selectedSupervision = findIndex(BitState.SUPERVISIONS, getString(SUPERVISION));
        int selectedReasoning = findIndex(BitState.REASONINGS, getString(REASONING));
        int selectedWorkflow = findIndex(BitState.WORKFLOWS, getString(WORKFLOW));

        long currentBitState = task.getBitState();
        long newBitState = BitState.encode(
            selectedMode != -1 ? selectedMode : 0,
            selectedSupervision != -1 ? selectedSupervision : 0,
            BitState.getInteraction(currentBitState),
            selectedReasoning != -1 ? selectedReasoning : 0,
            selectedWorkflow != -1 ? selectedWorkflow : 0
        );
        task.setBitState(newBitState);

        task.setPrompt(getString(PROMPT));
        task.setGoal(getString(GOAL));
        task.setDescription(getString(DESCRIPTION));

        task.setApprovalRequired(getBoolean(APPROVAL_REQUIRED));
        task.setIterativeMode(getBoolean(ITERATIVE_MODE));
        task.setSelfIterativeMode(getBoolean(SELF_ITERATIVE_MODE));
        task.setDarwinMode(getBoolean(DARWIN_MODE));
        task.setGitAutomation(getBoolean(GIT_AUTOMATION));
        task.setStepMode(getBoolean(STEP_MODE));

        task.setMaxIterations(getInteger(MAX_ITERATIONS));

        task.getAttachments().clear();
        task.getAttachments().addAll(attachmentsList);

        page.setDirty(true);
        super.okPressed();
    }

    private int findIndex(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) return i;
        }
        return -1;
    }
}
