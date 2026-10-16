package eu.kalafatic.evolution.view.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import eu.kalafatic.evolution.controller.ui.EvoTooltipManager;
import eu.kalafatic.evolution.controller.ui.EvoUiActionDescriptor;
import eu.kalafatic.evolution.controller.ui.EvoUiActionLogger;

/**
 * Utility helper to attach UI action listeners, guard against duplicate events,
 * invoke detailed UI action logging, and keep SWT control tooltips dynamically updated.
 */
public class EvoUiListenerHelper {

    public static final String ACTION_DESCRIPTOR_KEY = "EVO_UI_ACTION_DESCRIPTOR";
    public static final String LISTENER_ATTACHED_KEY = "EVO_UI_LISTENER_ATTACHED";

    public static void attachActionDescriptor(Control control, EvoUiActionDescriptor descriptor) {
        if (control == null || control.isDisposed()) return;
        control.setData(ACTION_DESCRIPTOR_KEY, descriptor);
        updateControlTooltip(control, descriptor);
    }

    public static EvoUiActionDescriptor getActionDescriptor(Control control) {
        if (control == null || control.isDisposed()) return null;
        Object data = control.getData(ACTION_DESCRIPTOR_KEY);
        if (data instanceof EvoUiActionDescriptor) {
            return (EvoUiActionDescriptor) data;
        }
        return null;
    }

    public static void updateControlTooltip(Control control, EvoUiActionDescriptor descriptor) {
        if (control == null || control.isDisposed() || descriptor == null) return;
        String formattedTooltip = EvoTooltipManager.getInstance().formatTooltip(descriptor);
        control.setToolTipText(formattedTooltip);
    }

    public static void updateControlTooltip(Control control) {
        EvoUiActionDescriptor descriptor = getActionDescriptor(control);
        if (descriptor != null) {
            updateControlTooltip(control, descriptor);
        }
    }

    public static void attachButtonLogging(Button button, SelectionListener userListener) {
        if (button == null || button.isDisposed()) return;
        if (Boolean.TRUE.equals(button.getData(LISTENER_ATTACHED_KEY))) {
            if (userListener != null) {
                button.addSelectionListener(userListener);
            }
            return;
        }
        button.setData(LISTENER_ATTACHED_KEY, Boolean.TRUE);

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EvoUiActionDescriptor descriptor = getActionDescriptor(button);
                if (descriptor != null) {
                    if ((button.getStyle() & SWT.CHECK) != 0 || (button.getStyle() & SWT.TOGGLE) != 0 || (button.getStyle() & SWT.RADIO) != 0) {
                        boolean sel = button.getSelection();
                        descriptor.setTrigger(EvoUiActionDescriptor.TriggerType.VALUE_CHANGE);
                        descriptor.setOldValue(String.valueOf(!sel));
                        descriptor.setNewValue(String.valueOf(sel));
                        EvoUiActionLogger.getInstance().logSingleInteraction(descriptor, "Selection changed to " + sel);
                    } else {
                        descriptor.setTrigger(EvoUiActionDescriptor.TriggerType.USER_CLICK);
                        EvoUiActionLogger.getInstance().logStart(descriptor);
                    }
                    updateControlTooltip(button, descriptor);
                }
            }
        });

        if (userListener != null) {
            button.addSelectionListener(userListener);
        }
    }

    public static void attachComboLogging(Combo combo, SelectionListener userListener) {
        if (combo == null || combo.isDisposed()) return;
        if (Boolean.TRUE.equals(combo.getData(LISTENER_ATTACHED_KEY))) {
            if (userListener != null) {
                combo.addSelectionListener(userListener);
            }
            return;
        }
        combo.setData(LISTENER_ATTACHED_KEY, Boolean.TRUE);

        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EvoUiActionDescriptor descriptor = getActionDescriptor(combo);
                if (descriptor != null) {
                    descriptor.setTrigger(EvoUiActionDescriptor.TriggerType.SELECTION);
                    descriptor.setNewValue(combo.getText());
                    EvoUiActionLogger.getInstance().logSingleInteraction(descriptor, "Selected: " + combo.getText());
                    updateControlTooltip(combo, descriptor);
                }
            }
        });

        if (userListener != null) {
            combo.addSelectionListener(userListener);
        }
    }

    public static void attachTextLogging(Text text) {
        if (text == null || text.isDisposed()) return;
        if (Boolean.TRUE.equals(text.getData(LISTENER_ATTACHED_KEY))) return;
        text.setData(LISTENER_ATTACHED_KEY, Boolean.TRUE);

        text.addFocusListener(new FocusAdapter() {
            private String initialText;

            @Override
            public void focusGained(FocusEvent e) {
                initialText = text.getText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                String currentText = text.getText();
                if (initialText != null && !initialText.equals(currentText)) {
                    EvoUiActionDescriptor descriptor = getActionDescriptor(text);
                    if (descriptor != null) {
                        descriptor.setTrigger(EvoUiActionDescriptor.TriggerType.VALUE_CHANGE);
                        descriptor.setOldValue(EvoUiActionDescriptor.sanitizeValue("text", initialText));
                        descriptor.setNewValue(EvoUiActionDescriptor.sanitizeValue("text", currentText));
                        EvoUiActionLogger.getInstance().logSingleInteraction(descriptor, "Value updated");
                        updateControlTooltip(text, descriptor);
                    }
                }
            }
        });
    }

    public static void attachTableLogging(Table table, SelectionListener userListener) {
        if (table == null || table.isDisposed()) return;
        if (Boolean.TRUE.equals(table.getData(LISTENER_ATTACHED_KEY))) {
            if (userListener != null) {
                table.addSelectionListener(userListener);
            }
            return;
        }
        table.setData(LISTENER_ATTACHED_KEY, Boolean.TRUE);

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EvoUiActionDescriptor descriptor = getActionDescriptor(table);
                if (descriptor != null && e.item instanceof Item item) {
                    descriptor.setTrigger(EvoUiActionDescriptor.TriggerType.SELECTION);
                    descriptor.setNewValue(item.getText());
                    EvoUiActionLogger.getInstance().logSingleInteraction(descriptor, "Row selected: " + item.getText());
                    updateControlTooltip(table, descriptor);
                }
            }
        });

        if (userListener != null) {
            table.addSelectionListener(userListener);
        }
    }

    public static void attachTreeLogging(Tree tree, SelectionListener userListener) {
        if (tree == null || tree.isDisposed()) return;
        if (Boolean.TRUE.equals(tree.getData(LISTENER_ATTACHED_KEY))) {
            if (userListener != null) {
                tree.addSelectionListener(userListener);
            }
            return;
        }
        tree.setData(LISTENER_ATTACHED_KEY, Boolean.TRUE);

        tree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EvoUiActionDescriptor descriptor = getActionDescriptor(tree);
                if (descriptor != null && e.item instanceof Item item) {
                    descriptor.setTrigger(EvoUiActionDescriptor.TriggerType.SELECTION);
                    descriptor.setNewValue(item.getText());
                    EvoUiActionLogger.getInstance().logSingleInteraction(descriptor, "Node selected: " + item.getText());
                    updateControlTooltip(tree, descriptor);
                }
            }
        });

        if (userListener != null) {
            tree.addSelectionListener(userListener);
        }
    }
}
