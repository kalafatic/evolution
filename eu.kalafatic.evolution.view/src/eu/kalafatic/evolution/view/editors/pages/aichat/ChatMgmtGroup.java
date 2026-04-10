package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class ChatMgmtGroup extends AEvoGroup {
    private Combo threadCombo;
    private AiChatPage page;

    public ChatMgmtGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, AiChatPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        // No dynamic model data to refresh
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Chat Management", 5, false);

        Button cleanButton = SWTFactory.createButton(group, "Clean");
        cleanButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.cleanChat();
            }
        });

        Button saveButton = SWTFactory.createButton(group, "Save");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.saveChatToFile();
            }
        });

        Button copyAllButton = SWTFactory.createButton(group, "Copy All");
        copyAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.copyConversationToClipboard();
            }
        });

        SWTFactory.createLabel(group, "Select Thread:");
        threadCombo = SWTFactory.createCombo(group);
        threadCombo.add(page.getCurrentThreadName());
        threadCombo.select(0);
        threadCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.switchThread(threadCombo.getText());
            }
        });

        Button newThreadButton = SWTFactory.createButton(group, "New Thread");
        newThreadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.createNewThread();
            }
        });
    }

    public void updateThreadCombo(String[] threads, String current) {
        if (threadCombo.isDisposed()) return;
        threadCombo.setItems(threads);
        for (int i = 0; i < threads.length; i++) {
            if (threads[i].equals(current)) {
                threadCombo.select(i);
                break;
            }
        }
    }
}
