package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ChatMgmtGroup {
    private Composite group;
    private Combo threadCombo;
    private AiChatPage page;

    public ChatMgmtGroup(FormToolkit toolkit, Composite parent, AiChatPage page) {
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Chat Management", 5, true);

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
