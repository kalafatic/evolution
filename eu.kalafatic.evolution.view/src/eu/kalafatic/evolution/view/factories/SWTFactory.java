package eu.kalafatic.evolution.view.factories;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.SashForm;

import eu.kalafatic.evolution.controller.manager.OllamaService;

public class SWTFactory {

	public static final int LABEL_WIDTH = 130;
	public static final int BUTTON_WIDTH = 100;

	public static Group createGroup(Composite parent, String text, int columns) {
		Group composite = new Group(parent, SWT.NONE);
		composite.setText(text);
		composite.setLayout(new GridLayout(columns, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return composite;
	}

	public static Label createLabel(Composite parent, String text) {
		GridData gd = new GridData();
		gd.widthHint = LABEL_WIDTH;
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(gd);
		label.setText(text);
		return label;
	}
	
	public static Button createButton(Composite parent, String text) {		
		return createButton(parent, text, BUTTON_WIDTH);
	}
	
	public static Button createButton(Composite parent, String text, int widthHint) {
		GridData gd = new GridData();
		gd.widthHint = widthHint;
		Button btn = new Button(parent, SWT.PUSH);
		btn.setLayoutData(gd);
		btn.setText(text);		
		return btn;
	}

	public static Button createEditButton(Composite parent, Text textWidget) {
		GridData gd = new GridData();
		gd.widthHint = BUTTON_WIDTH;
		Button btn = new Button(parent, SWT.PUSH);
		btn.setLayoutData(gd);
		btn.setText("Edit");
		btn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				textWidget.setFocus();
				textWidget.setSelection(0, textWidget.getText().length());
			}
		});
		return btn;
	}
	
	public static Text createText(Composite parent) {
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return text;
	}

	public static Text createPasswordText(Composite parent) {
		Text text = new Text(parent, SWT.BORDER | SWT.PASSWORD);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return text;
	}

	public static Text createMultiLineText(Composite parent) {
		Text text = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		text.setLayoutData(new GridData(GridData.FILL_BOTH));
		return text;
	}

	public static Combo createCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		combo.setLayoutData(gd);
		return combo;
	}

	public static Combo selectModel(Composite parent, OllamaService ollamaService) {
		Combo combo = createCombo(parent);
		
		List<eu.kalafatic.evolution.controller.manager.OllamaModel> models = ollamaService != null
				? ollamaService.loadModels()
				: new ArrayList<>();
		for (eu.kalafatic.evolution.controller.manager.OllamaModel m : models)
			combo.add(m.getName());
		return combo;
	}

	public static Group createMaximizableGroup(Composite parent, String text, int columns) {
		if (!(parent instanceof SashForm)) {
			return createGroup(parent, text, columns);
		}
		SashForm sashForm = (SashForm) parent;
		Composite container = new Composite(sashForm, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		Composite header = new Composite(container, SWT.NONE);
		GridLayout headerLayout = new GridLayout(2, false);
		headerLayout.marginHeight = 0;
		headerLayout.marginWidth = 0;
		header.setLayout(headerLayout);
		header.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(header, SWT.NONE);
		label.setText(text);
		label.setFont(org.eclipse.jface.resource.JFaceResources.getFontRegistry().getBold(org.eclipse.jface.resource.JFaceResources.DEFAULT_FONT));
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button maxBtn = new Button(header, SWT.PUSH | SWT.FLAT);
		GridData maxGd = new GridData();
		maxGd.widthHint = 20;
		maxGd.heightHint = 20;
		maxBtn.setLayoutData(maxGd);
		maxBtn.setText("\u25FB");
		maxBtn.setToolTipText("Maximize");
		Color orange = new Color(header.getDisplay(), 255, 140, 0);
		maxBtn.setBackground(orange);
		maxBtn.setForeground(header.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		maxBtn.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (orange != null && !orange.isDisposed()) orange.dispose();
			}
		});

		Group group = new Group(container, SWT.NONE);
		group.setLayout(new GridLayout(columns, false));
		group.setLayoutData(new GridData(GridData.FILL_BOTH));

		maxBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (sashForm.getMaximizedControl() == container) {
					sashForm.setMaximizedControl(null);
					int[] weights = (int[]) sashForm.getData("lastWeights");
					if (weights != null) {
						sashForm.setWeights(weights);
					}
					maxBtn.setText("\u25FB");
					maxBtn.setToolTipText("Maximize");
				} else {
					sashForm.setData("lastWeights", sashForm.getWeights());
					sashForm.setMaximizedControl(container);
					maxBtn.setText("\u25F2");
					maxBtn.setToolTipText("Restore");
				}
				sashForm.layout(true);
			}
		});

		return group;
	}

}
