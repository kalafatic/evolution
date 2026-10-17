package eu.kalafatic.evolution.view.editors.pages.context;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.orchestration.ContextualHelpService;
import eu.kalafatic.evolution.controller.orchestration.ContextualHelpService.ContextualHelpResult;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

/**
 * Composite group displaying 3-level contextual assistance and live process awareness in ContextPage.
 */
public class ContextualHelpGroup extends AEvoGroup {

    private Button autoHelpToggle;
    private Button refreshBtn;
    private Button saveMemoryBtn;

    private Text selectionHelpText;
    private Text processAwarenessText;
    private Text cognitiveAssistanceText;
    private Label statusLabel;

    private volatile ContextualHelpResult lastResult;

    public ContextualHelpGroup(Composite parent, MultiPageEditor editor, Orchestrator orchestrator, File projectRoot) {
        super(editor, orchestrator);

        Group g = new Group(parent, SWT.NONE);
        g.setText("Contextual Help & Live Process Awareness");
        g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.group = g;
        createContent(g);
    }

    private void createContent(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        // Control Bar
        Composite ctrlComp = new Composite(parent, SWT.NONE);
        ctrlComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ctrlComp.setLayout(new GridLayout(3, false));

        autoHelpToggle = new Button(ctrlComp, SWT.CHECK);
        autoHelpToggle.setText("Enable Automatic Contextual Help");
        autoHelpToggle.setSelection(true);

        refreshBtn = new Button(ctrlComp, SWT.PUSH);
        refreshBtn.setText("Refresh Assistance");
        refreshBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                refreshUI();
            }
        });

        saveMemoryBtn = new Button(ctrlComp, SWT.PUSH);
        saveMemoryBtn.setText("Save Insight as Memory");
        saveMemoryBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleSaveInsightAsMemory();
            }
        });

        // Level A: Selection Help Area
        new Label(parent, SWT.NONE).setText("A. UI Selection Help:");
        selectionHelpText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        GridData selGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        selGd.heightHint = 80;
        selectionHelpText.setLayoutData(selGd);

        // Level B: Process Awareness Area
        new Label(parent, SWT.NONE).setText("B. Live Process Awareness:");
        processAwarenessText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        GridData procGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        procGd.heightHint = 80;
        processAwarenessText.setLayoutData(procGd);

        // Level C: Cognitive Assistance Area
        new Label(parent, SWT.NONE).setText("C. Cognitive Assistance & Evidence:");
        cognitiveAssistanceText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        GridData cogGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        cogGd.heightHint = 100;
        cognitiveAssistanceText.setLayoutData(cogGd);

        // Status Footer
        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        statusLabel.setText("Status: Ready.");

        refreshUI();
    }

    public boolean isAutoHelpEnabled() {
        return autoHelpToggle != null && !autoHelpToggle.isDisposed() && autoHelpToggle.getSelection();
    }

    public void updateAssistance(ContextualHelpResult result) {
        if (result == null) return;
        this.lastResult = result;

        Display.getDefault().asyncExec(() -> {
            if (group == null || group.isDisposed()) return;

            setTextSafe(selectionHelpText, result.getSelectionHelp());
            setTextSafe(processAwarenessText, result.getProcessAwareness());
            setTextSafe(cognitiveAssistanceText, result.getCognitiveAssistance());

            if (statusLabel != null && !statusLabel.isDisposed()) {
                String sid = result.getSnapshot() != null ? result.getSnapshot().getOwningSessionId() : "GLOBAL";
                statusLabel.setText("Resolved in " + result.getDurationMs() + "ms | Session: " + sid);
            }
        });
    }

    private void handleSaveInsightAsMemory() {
        if (lastResult == null || lastResult.getSnapshot() == null) {
            MessageDialog.openInformation(group.getShell(), "Save Memory", "No active contextual snapshot available to save.");
            return;
        }

        String content = lastResult.getCognitiveAssistance();
        if (content.isEmpty()) {
            content = lastResult.getSelectionHelp();
        }

        MemoryEntry entry = new MemoryEntry();
        entry.setContent(content);
        entry.setScope(MemoryScope.PROJECT);
        entry.setType(MemoryType.SOLUTION);
        entry.setSource(MemorySource.INFERENCE);
        entry.setImportance(MemoryImportance.MEDIUM);
        entry.setAssociatedId(lastResult.getSnapshot().getProjectName());

        MemoryEntryDialog dialog = new MemoryEntryDialog(group.getShell(), entry);
        if (dialog.open() == MemoryEntryDialog.OK) {
            MemoryService.getInstance().addEntry(dialog.getEntry());
            MessageDialog.openInformation(group.getShell(), "Save Memory", "Contextual insight saved to persistent user memory.");
            if (editor != null) editor.setDirty(true);
        }
    }

    @Override
    protected void refreshUI() {
        String sid = orchestrator != null ? orchestrator.getId() : "GLOBAL";
        ContextualHelpService.getInstance().requestAssistanceDebounced(
                "ContextPage", "Manual Context Refresh", "UICommand", "", "", "", sid, 0,
                this::updateAssistance
        );
    }
}
