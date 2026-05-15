/*******************************************************************************
 * Copyright (c) 2024, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.dialogs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A reusable, generic SWT/JFace dynamic dialog component that generates UI
 * controls from a configuration map and writes edited values back.
 *
 * <p><b>Example Screenshot Structure:</b><br>
 * The dialog follows a standard Eclipse TitleAreaDialog layout:<br>
 * - Title and Description Area at the top.<br>
 * - A scrollable central area containing a two-column grid (Label | Control).<br>
 * - Labels are right-aligned or left-aligned depending on layout, required fields are marked with '*'.<br>
 * - Controls (Text, Combo, Checkbox, etc.) fill the horizontal space.<br>
 * - Browse buttons are placed next to the text fields for Directory/File types.<br>
 * - OK/Cancel buttons at the bottom right.
 * </p>
 *
 * <p><b>Suggested Future Extensions:</b>
 * <ul>
 *   <li>Tabs/Groups support for complex configurations.</li>
 *   <li>Nested sections or expandable composites.</li>
 *   <li>JSON serialization support for easy persistence.</li>
 *   <li>Advanced validation framework integration.</li>
 *   <li>SWT DataBinding support.</li>
 *   <li>Dark mode support enhancements.</li>
 * </ul>
 * </p>
 *
 * @author Petr Kalafatic
 */
public class DynamicMapDialog extends TitleAreaDialog {

	private final LinkedHashMap<String, DynamicField> fields;
	private final Map<String, Object> originalValues = new HashMap<>();
	private final Map<String, Control> controls = new HashMap<>();

	/**
	 * Constructor.
	 *
	 * @param parentShell the parent shell
	 * @param fields the configuration map
	 */
	public DynamicMapDialog(Shell parentShell, LinkedHashMap<String, DynamicField> fields) {
		super(parentShell);
		this.fields = fields;

		// Backup original values
		for (Map.Entry<String, DynamicField> entry : fields.entrySet()) {
			originalValues.put(entry.getKey(), entry.getValue().getValue());
		}

		setShellStyle(getShellStyle() | org.eclipse.swt.SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		ScrolledComposite scrolledComposite = new ScrolledComposite(area, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		Composite container = new Composite(scrolledComposite, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		for (Map.Entry<String, DynamicField> entry : fields.entrySet()) {
			createFieldEditor(container, entry.getKey(), entry.getValue());
		}

		scrolledComposite.setContent(container);
		scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		return area;
	}

	private void createFieldEditor(Composite parent, String key, DynamicField field) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(field.getLabel() + (field.has(DynamicField.REQUIRED) ? " *" : ""));
		if (field.getTooltip() != null) {
			label.setToolTipText(field.getTooltip());
		}

		Control control = null;
		if (field.has(DynamicField.TYPE_TEXT)) {
			int style = SWT.BORDER | (field.has(DynamicField.MULTILINE) ? SWT.MULTI | SWT.V_SCROLL | SWT.WRAP : SWT.SINGLE);
			if (field.has(DynamicField.PASSWORD)) {
				style |= SWT.PASSWORD;
			}

			if (field.has(DynamicField.FILE) || field.has(DynamicField.DIRECTORY)) {
				control = createBrowseField(parent, field);
			} else {
				Text text = createTextField(parent, field, style);
				if (field.has(DynamicField.MULTILINE)) {
					GridData gd = new GridData(GridData.FILL_HORIZONTAL);
					gd.heightHint = 60;
					text.setLayoutData(gd);
				}
				control = text;
			}
		} else if (field.has(DynamicField.TYPE_NUMBER)) {
			control = createTextField(parent, field, SWT.BORDER | SWT.SINGLE);
		} else if (field.has(DynamicField.TYPE_CHECKBOX)) {
			Button check = new Button(parent, SWT.CHECK);
			check.setSelection(field.getValue() instanceof Boolean ? (Boolean) field.getValue() : false);
			control = check;
		} else if (field.has(DynamicField.TYPE_COMBO)) {
			Combo combo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
			if (field.getComboValues() != null) {
				combo.setItems(field.getComboValues().toArray(new String[0]));
			}
			if (field.getValue() != null) {
				combo.setText(field.getValue().toString());
			}
			control = combo;
		}

		if (control != null) {
			if (field.getTooltip() != null) {
				control.setToolTipText(field.getTooltip());
			}
			control.setEnabled(!field.has(DynamicField.READ_ONLY));

			if (!(control instanceof Composite) && control.getLayoutData() == null) {
				GridData gd = new GridData(GridData.FILL_HORIZONTAL);
				if (field.getWidth() > 0) {
					gd.widthHint = field.getWidth();
				}
				control.setLayoutData(gd);
			}

			controls.put(key, control);
		}
	}

	private Text createTextField(Composite parent, DynamicField field, int style) {
		Text text = new Text(parent, style);
		text.setText(field.getValue() != null ? field.getValue().toString() : "");
		return text;
	}

	private Control createBrowseField(Composite parent, DynamicField field) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		comp.setLayout(gl);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Text text = new Text(comp, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.setText(field.getValue() != null ? field.getValue().toString() : "");

		Button browse = new Button(comp, SWT.PUSH);
		browse.setText("Browse...");
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (field.has(DynamicField.DIRECTORY)) {
					DirectoryDialog dialog = new DirectoryDialog(getShell());
					dialog.setFilterPath(text.getText());
					String path = dialog.open();
					if (path != null) {
						text.setText(path);
					}
				} else {
					FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
					dialog.setFileName(text.getText());
					String path = dialog.open();
					if (path != null) {
						text.setText(path);
					}
				}
			}
		});

		return comp;
	}

	@Override
	protected void okPressed() {
		if (validate()) {
			saveValues();
			super.okPressed();
		}
	}

	private boolean validate() {
		for (Map.Entry<String, DynamicField> entry : fields.entrySet()) {
			String key = entry.getKey();
			DynamicField field = entry.getValue();
			Control control = controls.get(key);
			String val = "";

			if (control instanceof Text) {
				val = ((Text) control).getText();
			} else if (control instanceof Combo) {
				val = ((Combo) control).getText();
			} else if (control instanceof Composite) {
				for (Control child : ((Composite) control).getChildren()) {
					if (child instanceof Text) {
						val = ((Text) child).getText();
						break;
					}
				}
			}

			if (field.has(DynamicField.REQUIRED) && (val == null || val.trim().isEmpty())) {
				setErrorMessage("Field '" + field.getLabel() + "' is required.");
				return false;
			}

			if (field.has(DynamicField.TYPE_NUMBER) && !val.isEmpty()) {
				try {
					Integer.parseInt(val);
				} catch (NumberFormatException e) {
					setErrorMessage("Field '" + field.getLabel() + "' must be a number.");
					return false;
				}
			}
		}
		setErrorMessage(null);
		return true;
	}

	private void saveValues() {
		for (Map.Entry<String, DynamicField> entry : fields.entrySet()) {
			String key = entry.getKey();
			DynamicField field = entry.getValue();
			Control control = controls.get(key);
			if (control instanceof Text) {
				field.setValue(((Text) control).getText());
			} else if (control instanceof Button) {
				field.setValue(((Button) control).getSelection());
			} else if (control instanceof Combo) {
				field.setValue(((Combo) control).getText());
			} else if (control instanceof Composite) {
				for (Control child : ((Composite) control).getChildren()) {
					if (child instanceof Text) {
						field.setValue(((Text) child).getText());
						break;
					}
				}
			}
		}
	}

	@Override
	protected void cancelPressed() {
		// Restore original values
		for (Map.Entry<String, DynamicField> entry : fields.entrySet()) {
			entry.getValue().setValue(originalValues.get(entry.getKey()));
		}
		super.cancelPressed();
	}

	/**
	 * Gets the string value of a field.
	 * @param key the field key
	 * @return the string value or null
	 */
	public String getString(String key) {
		Object value = getValue(key);
		return value != null ? value.toString() : null;
	}

	/**
	 * Gets the boolean value of a field.
	 * @param key the field key
	 * @return the boolean value or null
	 */
	public Boolean getBoolean(String key) {
		Object value = getValue(key);
		return value instanceof Boolean ? (Boolean) value : null;
	}

	/**
	 * Gets the integer value of a field.
	 * @param key the field key
	 * @return the integer value or null
	 */
	public Integer getInteger(String key) {
		Object value = getValue(key);
		if (value instanceof Integer) {
			return (Integer) value;
		} else if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Gets the raw value of a field.
	 * @param key the field key
	 * @return the value
	 */
	public Object getValue(String key) {
		DynamicField field = fields.get(key);
		return field != null ? field.getValue() : null;
	}
}
