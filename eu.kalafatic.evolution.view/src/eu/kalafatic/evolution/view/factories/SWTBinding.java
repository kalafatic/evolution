package eu.kalafatic.evolution.view.factories;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import eu.kalafatic.utils.model.ObservableProperty;
import eu.kalafatic.utils.model.ObservableList;
import eu.kalafatic.utils.model.ModelChangeEvent;

public class SWTBinding {

    public static void bindText(Text text, ObservableProperty<String> property) {
        // Model -> View
        property.addChangeListener(event -> {
            Display.getDefault().asyncExec(() -> {
                if (!text.isDisposed()) {
                    String newValue = (String) event.getNewValue();
                    if (!text.getText().equals(newValue)) {
                        text.setText(newValue != null ? newValue : "");
                    }
                }
            });
        });

        // View -> Model
        text.addModifyListener(e -> {
            property.setValue(text.getText());
        });

        // Initial sync
        String val = property.getValue();
        text.setText(val != null ? val : "");
    }

    public static void bindCombo(Combo combo, ObservableProperty<String> property) {
        // Model -> View
        property.addChangeListener(event -> {
            Display.getDefault().asyncExec(() -> {
                if (!combo.isDisposed()) {
                    String newValue = (String) event.getNewValue();
                    int index = combo.indexOf(newValue != null ? newValue : "");
                    if (index >= 0 && combo.getSelectionIndex() != index) {
                        combo.select(index);
                    }
                }
            });
        });

        // View -> Model
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = combo.getSelectionIndex();
                if (index >= 0) {
                    property.setValue(combo.getItem(index));
                }
            }
        });

        // Initial sync
        String val = property.getValue();
        if (val != null) {
            int index = combo.indexOf(val);
            if (index >= 0) combo.select(index);
        }
    }

    public static void bindComboItems(Combo combo, ObservableList<String> list) {
        list.addChangeListener(event -> {
            Display.getDefault().asyncExec(() -> {
                if (!combo.isDisposed()) {
                    String currentSelection = combo.getText();
                    combo.setItems(list.getList().toArray(new String[0]));
                    int index = combo.indexOf(currentSelection);
                    if (index >= 0) combo.select(index);
                }
            });
        });

        combo.setItems(list.getList().toArray(new String[0]));
    }

    public static void bindCheckbox(Button checkbox, ObservableProperty<Boolean> property) {
        // Model -> View
        property.addChangeListener(event -> {
            Display.getDefault().asyncExec(() -> {
                if (!checkbox.isDisposed()) {
                    Boolean newValue = (Boolean) event.getNewValue();
                    boolean val = newValue != null && newValue;
                    if (checkbox.getSelection() != val) {
                        checkbox.setSelection(val);
                    }
                }
            });
        });

        // View -> Model
        checkbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                property.setValue(checkbox.getSelection());
            }
        });

        // Initial sync
        Boolean val = property.getValue();
        checkbox.setSelection(val != null && val);
    }

    public static <T> void bindTable(TableViewer tableViewer, ObservableList<T> list) {
        tableViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return list.getList().toArray();
            }
        });

        list.addChangeListener(event -> {
            Display.getDefault().asyncExec(() -> {
                if (!tableViewer.getControl().isDisposed()) {
                    tableViewer.refresh();
                }
            });
        });

        tableViewer.setInput(list);
    }
}
