package eu.kalafatic.evolution.view.factories;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

		FontData[] fontData = JFaceResources.getDefaultFont().getFontData();
		for (FontData fd : fontData) {
			fd.setStyle(SWT.BOLD);
		}
		Font boldFont = new Font(parent.getDisplay(), fontData);
		label.setFont(boldFont);
		label.addDisposeListener(e -> boldFont.dispose());
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button maxBtn = new Button(header, SWT.PUSH);
		maxBtn.setText("Maximize");

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
					maxBtn.setText("Maximize");
				} else {
					sashForm.setData("lastWeights", sashForm.getWeights());
					sashForm.setMaximizedControl(container);
					maxBtn.setText("Restore");

					// Revert other buttons in the same sashform
					resetSiblingMaximizeButtons(sashForm, container);
				}
				refreshParentScrolledComposites(sashForm);
			}
		});

		return group;
	}

	private static void resetSiblingMaximizeButtons(SashForm sashForm, Composite currentContainer) {
		for (Control child : sashForm.getChildren()) {
			if (child instanceof Composite && child != currentContainer) {
				Composite container = (Composite) child;
				for (Control containerChild : container.getChildren()) {
					if (containerChild instanceof Composite) { // This is the header
						for (Control headerChild : ((Composite) containerChild).getChildren()) {
							if (headerChild instanceof Button) {
								Button btn = (Button) headerChild;
								if ("Restore".equals(btn.getText())) {
									btn.setText("Maximize");
								}
							}
						}
					}
				}
			}
		}
	}

	private static void refreshParentScrolledComposites(Control control) {
		Composite p = control.getParent();
		while (p != null) {
			if (p instanceof ScrolledComposite) {
				ScrolledComposite sc = (ScrolledComposite) p;
				if (sc.getContent() != null) {
					sc.setMinSize(sc.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}
				sc.layout(true, true);
			}
			p = p.getParent();
		}
	}

}
