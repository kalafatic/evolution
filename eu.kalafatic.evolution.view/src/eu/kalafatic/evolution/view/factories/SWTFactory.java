package eu.kalafatic.evolution.view.factories;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.manager.OllamaService;

public class SWTFactory {

	public static Group createGroup(Composite parent, String text, int columns) {
		Group composite = new Group(parent, SWT.NONE);
		composite.setText(text);
		composite.setLayout(new GridLayout(columns, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return composite;
	}

	public static Label createLabel(Composite parent, String text) {
		GridData gd = new GridData();
		gd.widthHint = 120;
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(gd);
		label.setText(text);
		return label;
	}
	

	public static Button createButton(Composite parent, String text) {		
		return createButton( parent,  text, 100) ;
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
		gd.widthHint = 100;
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
	
	public static Combo createCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.widthHint = 200;
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

}
