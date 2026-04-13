/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.factories;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.constants.EDateFormat;
import eu.kalafatic.utils.constants.FTextConstants;
import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.utils.hack.ImageCombo;
import eu.kalafatic.utils.log.Log;
import eu.kalafatic.utils.model.ComboData;
import eu.kalafatic.utils.preferences.ECorePreferences;
import eu.kalafatic.utils.time.DateUtils;

/**
 * A factory for creating GUI objects.
 */
public class GUIFactory {

	/** The Constant NAME. */
	public static final String NAME = "name";

	/** The Constant DATA. */
	public static final String DATA = "data";

	/** The Constant ORIGINAL. */
	public static final String ORIGINAL = "original";

	/** The shell. */
	private static Shell shell;

	/** The grid layout. */
	private GridLayout gridLayout;

	/** The grid data. */
	private GridData gridData;

	/** The toolkit. */
	private FormToolkit toolkit;

	/** The form. */
	private ScrolledForm form;

	/** The font registry. */
	private static FontRegistry fontRegistry;

	/** The SECTIO n_ style. */
	public final int SECTION_STYLE = Section.TITLE_BAR | Section.TWISTIE | Section.COMPACT;

	public static final GUIFactory INSTANCE = new GUIFactory();

	/** The GridLayout Constants. */
	public static final GridLayout DEF_GL, CONTAINER_GL, SECTION_GL;

	/** The GridData Constants. */
	public static final GridData DEF_GD, H_GD, LBL_GD, BTN_GD, TXT_GD, MULTI_TXT_GD, CONTAINER_GD, VIEWER_GD;
	static {
		DEF_GL = new GridLayout(1, false);

		DEF_GD = new GridData(GridData.FILL_BOTH);
		H_GD = new GridData(GridData.FILL_HORIZONTAL);

		CONTAINER_GL = new GridLayout(FUIConstants.BLOCK_COLS, false);
		CONTAINER_GL.horizontalSpacing = 0;
		CONTAINER_GL.verticalSpacing = 0;
		CONTAINER_GL.marginLeft = 0;
		CONTAINER_GL.marginRight = 0;
		CONTAINER_GL.marginWidth = 0;
		CONTAINER_GL.marginHeight = 2;
		CONTAINER_GL.marginTop = 0;
		CONTAINER_GL.marginBottom = 2;

		// height should be computed
		CONTAINER_GD = new GridData(GridData.FILL_HORIZONTAL);
		// WIDGET_GD.heightHint = WIDGET_HEIGHT;

		SECTION_GL = new GridLayout();
		SECTION_GL.horizontalSpacing = 0;
		SECTION_GL.verticalSpacing = 0;
		SECTION_GL.marginLeft = 0;
		SECTION_GL.marginRight = 0;
		SECTION_GL.marginWidth = 0;
		SECTION_GL.marginHeight = 0;
		SECTION_GL.marginTop = 0;
		SECTION_GL.marginBottom = 0;

		LBL_GD = new GridData(SWT.LEFT, SWT.TOP, false, false);
		LBL_GD.horizontalIndent = 0;
		LBL_GD.verticalIndent = 0;
		LBL_GD.widthHint = FUIConstants.LABEL_WIDTH;
		LBL_GD.heightHint = FUIConstants.BUTTON_HEIGHT;

		BTN_GD = new GridData(SWT.LEFT, SWT.TOP, false, false);
		BTN_GD.widthHint = FUIConstants.BUTTON_WIDTH;
		BTN_GD.heightHint = FUIConstants.BUTTON_HEIGHT;

		TXT_GD = new GridData(SWT.LEFT, SWT.TOP, false, false);
		TXT_GD.horizontalIndent = 5;
		TXT_GD.verticalIndent = 0;
		TXT_GD.widthHint = FUIConstants.CONTROL_WIDTH;
		TXT_GD.heightHint = FUIConstants.BUTTON_HEIGHT;

		MULTI_TXT_GD = new GridData(SWT.LEFT, SWT.TOP, false, false);
		MULTI_TXT_GD.horizontalIndent = 5;
		MULTI_TXT_GD.verticalIndent = 0;
		MULTI_TXT_GD.widthHint = FUIConstants.CONTROL_WIDTH - FUIConstants.SCROLL_WIDTH;
		MULTI_TXT_GD.heightHint = 40;

		VIEWER_GD = new GridData(GridData.FILL_HORIZONTAL);
		VIEWER_GD.heightHint = 200;
	}

	/**
	 * Instantiates a new gUI factory.
	 */
	public GUIFactory() {
		toolkit = new FormToolkit(Display.getDefault());
		shell = new Shell(Display.getDefault());

		fontRegistry = new FontRegistry(Display.getDefault());
		fontRegistry.put("button-text", new FontData[] { new FontData("Arial", 8, SWT.NORMAL) });
		fontRegistry.put("label-text", new FontData[] { new FontData("Arial", 8, SWT.NORMAL) });
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param name the name
	 * @param description the description
	 * @param style the style
	 * @return the section
	 */
	public Section createSection(String name, String description, int style) {

		final Section section = toolkit.createSection(form.getBody(), style);

		section.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {

				form.setLayoutData(new GridData(GridData.FILL_BOTH));
				form.reflow(true);
			}
		});
		section.setText(name);
		if (description != null) {
			section.setDescription(description);
		}
		gridLayout = new GridLayout(1, true);
		gridLayout.verticalSpacing = 8;

		section.setLayout(gridLayout);

		section.setLayoutData(new ColumnLayoutData());

		return section;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param section the section
	 * @param col the col
	 * @return the composite
	 */
	public Composite createSectionClient(Section section, int col) {

		Composite sectionClient = toolkit.createComposite(section);

		gridLayout = new GridLayout(col, true);
		gridLayout.verticalSpacing = 8;
		sectionClient.setLayout(gridLayout);

		gridData = new GridData(GridData.FILL_BOTH);
		gridData.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;

		sectionClient.setLayoutData(gridData);
		return sectionClient;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param col the col
	 * @return the composite
	 */
	public Composite createComposite(Composite parent, int col) {
		return createComposite(parent, col, SWT.SHADOW_IN);
	}

	// public static Composite createComposite(Composite parent) {
	// return createComposite(parent, SWT.NONE, 1);
	// }

	public Composite createComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		return composite;
	}

	public Composite createContainer(Composite parent, int numColumns) {
		return createContainer(parent, numColumns, 1, 0);
	}

	public Composite createContainer(Composite parent, int numColumns, int horizontalSpan, int height) {
		Composite control = createComposite(parent);
		GridLayout gridLayout = new GridLayout(numColumns, false);
		gridLayout.horizontalSpacing = 10;
//		gridLayout.verticalSpacing = 0;
//		gridLayout.marginLeft = 10;
//		gridLayout.marginRight = 10;
//		gridLayout.marginWidth = 0;
//		gridLayout.marginHeight = 0;
//		gridLayout.marginTop = 0;
//		gridLayout.marginBottom = 0;

		control.setLayout(gridLayout);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		if (height > 0) {
			gridData.heightHint = height;
		}
		gridData.horizontalSpan = horizontalSpan;
		control.setLayoutData(gridData);
		return control;
	}

	// public static Group createGroup(Composite parent, String name, int style, int columns) {
	// Group control = new Group(parent, style);
	// control.setText(name);
	// control.setLayout(new GridLayout(columns, false));
	// control.setLayoutData(new GridData(GridData.FILL_BOTH));
	// return control;
	// }
	//
	// public static Label createLabel(Composite parent, String text) {
	// return createLabel(parent, text, "", 0);
	// }

	public Label createLabel(Composite parent, String text, String tooltip, int width) {
		Label control = new Label(parent, SWT.NONE);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		if (width > 0) {
			gridData.widthHint = width;
		}

		control.setLayoutData(gridData);

		control.setText((text == null) ? "" : text);
		control.setToolTipText((tooltip == null) ? "" : tooltip);
		return control;
	}

	public Label createLabel(Composite parent, GridData gridData, String text) {
		Label control = new Label(parent, SWT.BORDER);
		control.setLayoutData(gridData);
		control.setText((text == null) ? "" : text);
		return control;
	}

	public Text createText(Composite parent, int style, String text, String tooltip, int width, boolean enabled, boolean editable) {
		return createText(parent, style, text, tooltip, width, 0, enabled, editable);
	}

	public Text createText(Composite parent, int style, String text, String tooltip, int width, int height, boolean enabled, boolean editable) {
		final Text control = new Text(parent, style);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_BOTH);
		if (width > 0) {
			gridData.widthHint = width;
		}
		if (height > 0) {
			gridData.heightHint = height;
		}
		control.setLayoutData(gridData);
		control.setEnabled(enabled);
		control.setEditable(editable);

		control.setText((text == null) ? "" : text);
		control.setToolTipText((tooltip == null) ? "" : tooltip);
		return control;
	}

	public Combo createCombo(Composite parent, int style, String tooltip, int width, Object... input) {
		Combo control = new Combo(parent, style);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		if (width > 0) {
			gridData.widthHint = width;
		}
		control.setLayoutData(gridData);

		for (Object object : input) {
			control.add(object.toString());
		}
		control.setToolTipText((tooltip == null) ? "" : tooltip);
		control.setEnabled(true);
		return control;
	}

	public Button createButton(Composite parent, int style, String text, String tooltip, int width, boolean enabled) {
		Button control = new Button(parent, style);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		if (width > 0) {
			gridData.widthHint = width;
		}
		control.setLayoutData(gridData);

		control.setText((text == null) ? "" : text);
		control.setToolTipText((tooltip == null) ? "" : tooltip);
		control.setEnabled(enabled);
		return control;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param col the col
	 * @param style the style
	 * @return the composite
	 */
	public Composite createComposite(Composite parent, int col, int style) {
		Composite composite = new Composite(parent, style);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = col;
		composite.setLayout(gridLayout);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gridData);

		return composite;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param col the col
	 * @param style the style
	 * @param width the width
	 * @return the composite
	 */
	public Composite createComposite(Composite parent, int col, int style, int width) {
		Composite composite = new Composite(parent, style);

		gridLayout = new GridLayout();
		gridLayout.numColumns = col;
		composite.setLayout(gridLayout);

		gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = width;
		composite.setLayoutData(gridData);

		return composite;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param col the col
	 * @return the group
	 */
	public Group createGroup(Composite parent, String name, int col) {
		return createGroup(parent, name, col, SWT.SHADOW_IN | SWT.WRAP);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param col the col
	 * @param style the style
	 * @return the group
	 */
	public Group createGroup(Composite parent, String name, int col, int style) {
		Group group = new Group(parent, style);
		group.setText(name);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = col;
		group.setLayout(gridLayout);

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);

		group.setLayoutData(gridData);

		return group;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param col the col
	 * @param orientation the orientation
	 * @return the sash form
	 */
	public SashForm createSashForm(Composite parent, int col, int orientation) {
		SashForm form = new SashForm(parent, SWT.BORDER | orientation);
		form.setLayout(new GridLayout(col, true));

		GridData gridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL | GridData.GRAB_HORIZONTAL);
		form.setLayoutData(gridData);
		return form;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param col the col
	 * @param orientation the orientation
	 * @param width the width
	 * @return the sash form
	 */
	public SashForm createSashForm(Composite parent, int col, int orientation, int width) {
		SashForm form = new SashForm(parent, SWT.BORDER | orientation);
		form.setLayout(new GridLayout(col, true));

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = width;
		parent.setLayoutData(gridData);

		form.setLayoutData(gridData);
		return form;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param col the col
	 * @param style the style
	 * @param width the width
	 * @return the group
	 */
	public Group createGroup(Composite parent, String name, int col, int style, int width) {
		Group group = createGroup(parent, name, col);
		((GridData) group.getLayoutData()).widthHint = width;
		return group;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @return the label
	 */
	public Label createLabel(Composite composite) {
		return createLabel(composite, "", SWT.NONE);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param width the width
	 * @return the label
	 */
	public Label createLabel(Composite composite, int width) {
		return createLabel(composite, "", SWT.NONE, width);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @return the label
	 */
	public Label createLabel(Composite composite, String name) {
		return createLabel(composite, name, SWT.SHADOW_IN);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param span the span
	 * @return the label
	 */
	public Label createLabel(Composite composite, String name, byte span) {
		return createLabel(composite, name, SWT.SHADOW_IN, span);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param span the span
	 * @return the label
	 */
	public Label createLabel(Composite composite, String name, int style, byte span) {
		Label label = createLabel(composite, name, style);
		if (span > 0) {
			((GridData) label.getLayoutData()).horizontalSpan = span;
		} else {
			((GridData) label.getLayoutData()).verticalSpan = -span;
		}
		return label;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @return the label
	 */
	public Label createLabel(Composite composite, String name, int style) {
		Label label = new Label(composite, style);
		label.setText(name);
		label.setToolTipText(name);

		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
		gridData.widthHint = FUIConstants.LABEL_WIDTH;
		label.setLayoutData(gridData);

		return label;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param width the width
	 * @return the label
	 */
	public Label createLabel(Composite composite, String name, int style, int width) {
		Label label = createLabel(composite, name, style);
		((GridData) label.getLayoutData()).widthHint = width;
		return label;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param sectionClient the section client
	 * @param name the name
	 * @param value the value
	 * @throws Exception the exception
	 */
	public void createLabels(final Composite sectionClient, String name, String value) throws Exception {
		Composite composite = createComposite(sectionClient, 2);
		GUIFactory.INSTANCE.createLabel(composite, name, SWT.NONE);
		final Text text = createText(composite, "", true);
		text.setText(value);
		text.setEditable(false);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param href the href
	 * @return the hyperlink
	 */
	public Hyperlink createHyperlink(Composite composite, String name, final String href) {

		Hyperlink link = toolkit.createHyperlink(composite, name, SWT.NORMAL);
		link.setText(name);
		link.setHref(href);

		link.addHyperlinkListener(new IHyperlinkListener() {

			@Override
			public void linkExited(HyperlinkEvent e) {

			}

			@Override
			public void linkEntered(HyperlinkEvent e) {

			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
				try {
					support.getExternalBrowser().openURL(new URL(href));
				} catch (Exception ex) {
					Log.log(ECorePreferences.MODULE, ex);
				}

			}
		});

		gridData = new GridData();
		gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		gridData.widthHint = FUIConstants.LABEL_WIDTH;

		link.setLayoutData(gridData);

		link.setToolTipText(href);

		link.setForeground(new Color(Display.getDefault(), 0, 0, 0));
		link.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		link.setFont(fontRegistry.get("label-text"));

		return link;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param editable the editable
	 * @return the text
	 */
	public Text createText(Composite composite, String name, boolean editable) {
		return createText(composite, name, editable, SWT.SINGLE | SWT.BORDER);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param editable the editable
	 * @param span the span
	 * @return the text
	 */
	public Text createText(Composite composite, String name, boolean editable, byte span) {
		Text text = createText(composite, name, editable, SWT.SINGLE | SWT.BORDER);
		if (span > 0) {
			((GridData) text.getLayoutData()).horizontalSpan = span;
		} else {
			((GridData) text.getLayoutData()).verticalSpan = span;
		}
		return text;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param editable the editable
	 * @param style the style
	 * @return the text
	 */
	public Text createText(Composite composite, String name, boolean editable, int style) {
		Text text = new Text(composite, style);
		text.setEditable(editable);
		text.setToolTipText(name);

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		// gridData.widthHint = TEXT_WIDTH;
		text.setLayoutData(gridData);

		text.setFont(fontRegistry.get("text-text"));

		return text;
	}

	public static void main(String[] args) throws FileNotFoundException {
		Display display = Display.getDefault();
		Shell shell = new Shell(Display.getDefault());
		shell.setLayout(new GridLayout());
		shell.setLayoutData(new GridData(GridData.FILL_BOTH));
		shell.setSize(200, 100);

		Composite parent = new Composite(shell, SWT.BORDER);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));

		createSourceText(parent);

		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private static void createSourceText(final Composite parent) {
		final Text text = GUIFactory.INSTANCE.createText(parent, "", true, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.RESIZE | SWT.ICON);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 50;
		text.setLayoutData(gd);

		Menu menu = new Menu(parent.getShell(), SWT.POP_UP);
		final MenuItem editItem = new MenuItem(menu, SWT.PUSH);

		editItem.setText("Edit");
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createSourceTextOnShell(text);
			}
		});
		text.setMenu(menu);
	}

	public static void createSourceText(final Text text) {
		text.setData("Edit", "Edit");

		// the text has edit button already
		if (text.getData("Edit") == null) {

			Button button = GUIFactory.INSTANCE.createButton(text.getParent(), "Edit");
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
			gridData.widthHint = FUIConstants.BUTTON_WIDTH;
			gridData.heightHint = FUIConstants.BUTTON_HEIGHT;
			button.setLayoutData(gridData);

			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					createSourceTextOnShell(text);
				}
			});
		}
	}

	private static void createSourceTextOnShell(final Text text) {
		// text.setData("Edit", "Edit");

		// the text has edit button already
		if (text.getData("Edit") == null) {
			final Image image = new Image(Display.getDefault(), "icons/actions/page-white-magnify-icon.png");

			Shell shell = new Shell(Display.getDefault(), SWT.APPLICATION_MODAL | SWT.CLOSE);
			shell.setLayout(new GridLayout());
			shell.setLayoutData(new GridData(GridData.FILL_BOTH));
			shell.setSize(400, 200);
			shell.setImage(image);

			final StyledText sourceText = new StyledText(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			sourceText.setLayoutData(new GridData(GridData.FILL_BOTH));
			sourceText.setText(text.getText());

			sourceText.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					text.setText(sourceText.getText());
				}
			});
			shell.open();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param editable the editable
	 * @param style the style
	 * @param width the width
	 * @return the text
	 */
	public Text createText(Composite composite, String name, boolean editable, int style, int width) {
		Text text = createText(composite, name, editable, style);
		((GridData) text.getLayoutData()).widthHint = width;
		text.pack(true);
		return text;
	}

	// ---------------------------------------------------------------

	public Button createButton(Composite composite, String name) {
		return createButton(composite, name, SWT.PUSH);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style) {
		Button button = new Button(composite, style);
		button.setText(name);
		button.setToolTipText(name);

		GridData gridData = new GridData(GridData.FILL);
		gridData.widthHint = FUIConstants.BUTTON_WIDTH;
		gridData.heightHint = FUIConstants.BUTTON_HEIGHT;
		button.setLayoutData(gridData);

		button.setForeground(new Color(Display.getDefault(), 50, 150, 50));
		button.setFont(fontRegistry.get("button-text"));

		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param listener the listener
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, Listener listener) {
		Button button = createButton(composite, name, style);
		button.addListener(SWT.Selection, listener);
		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param width the width
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, int width) {
		Button button = createButton(composite, name, style);
		((GridData) button.getLayoutData()).widthHint = width;
		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param checked the checked
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, boolean checked) {
		return createButton(composite, name, style, FUIConstants.BUTTON_WIDTH, checked);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param width the width
	 * @param checked the checked
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, int width, boolean checked) {
		Button button = createButton(composite, name, style, width);
		button.setSelection(checked);
		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param checked the checked
	 * @param listener the listener
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, boolean checked, Listener listener) {
		return createButton(composite, name, style, FUIConstants.BUTTON_WIDTH, checked, listener);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param width the width
	 * @param checked the checked
	 * @param listener the listener
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, int width, boolean checked, Listener listener) {
		Button button = createButton(composite, name, style, width, checked);
		if (listener != null) {
			button.addListener(SWT.Selection, listener);
		}
		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param width the width
	 * @param checked the checked
	 * @param enabled the enabled
	 * @param listener the listener
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, int width, boolean checked, boolean enabled, Listener listener) {
		Button button = createButton(composite, name, style, width, checked, listener);
		button.setEnabled(enabled);
		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param width the width
	 * @param backImage the back image
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, int width, Image backImage) {
		Button button = createButton(composite, name, style, width);
		button.setImage(backImage);
		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param width the width
	 * @param height the height
	 * @param backImage the back image
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, int width, int height, Image backImage) {
		Button button = createButton(composite, name, style, width);
		((GridData) button.getLayoutData()).heightHint = height;
		button.setImage(backImage);
		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @param span the span
	 * @return the button
	 */
	public Button createButton(Composite composite, String name, int style, byte span) {
		Button button = createButton(composite, name, style);
		if (span > 0) {
			((GridData) button.getLayoutData()).horizontalSpan = span;
		} else {
			((GridData) button.getLayoutData()).verticalSpan = span;
		}
		return button;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param style the style
	 * @return the combo
	 */
	public Combo createCombo(Composite parent, String name, int style) {
		final Combo combo = new Combo(parent, style);
		combo.setToolTipText(name);

		gridData = new GridData();
		gridData.widthHint = FUIConstants.BUTTON_WIDTH;
		combo.setLayoutData(gridData);

		combo.setForeground(new Color(Display.getDefault(), 50, 150, 50));
		combo.setFont(fontRegistry.get("button-text"));
		return combo;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param comboData the combo data
	 * @return the combo
	 */
	public Combo createCombo(Composite parent, String name, ComboData comboData) {
		return createCombo(parent, name, SWT.READ_ONLY, comboData);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param style the style
	 * @param comboData the combo data
	 * @return the combo
	 */
	public Combo createCombo(Composite parent, String name, int style, ComboData comboData) {
		Combo combo = createCombo(parent, name, style);
		combo.setData("comboData", comboData);
		combo.setItems(comboData.getItems());
		combo.select(comboData.defaultSelection);
		return combo;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param comboData the combo data
	 * @param listener the listener
	 * @return the combo
	 */
	public Combo createCombo(Composite parent, String name, ComboData comboData, Listener listener) {
		Combo combo = createCombo(parent, name, comboData);
		combo.addListener(SWT.Selection, listener);
		return combo;
	}

	// ---------------------------------------------------------------

	public ImageCombo createImageCombo(Composite parent, String name, List<Object[]> items) {
		final ImageCombo combo = new ImageCombo(parent, SWT.READ_ONLY);
		combo.setToolTipText(name);

		gridData = new GridData();
		gridData.widthHint = FUIConstants.BUTTON_WIDTH;
		combo.setLayoutData(gridData);

		combo.setForeground(new Color(Display.getDefault(), 50, 150, 50));
		combo.setFont(fontRegistry.get("button-text"));

		for (Object[] objects : items) {
			combo.add((String) objects[0], (Image) objects[1]);
		}
		return combo;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param items the items
	 * @return the combo
	 */
	public Combo createCombo(Composite parent, String name, String... items) {
		return createCombo(parent, name, SWT.READ_ONLY, items);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param name the name
	 * @param style the style
	 * @param items the items
	 * @return the combo
	 */
	public Combo createCombo(Composite parent, String name, int style, String... items) {
		Combo combo = createCombo(parent, name, style);
		for (int i = 0; i < items.length; i++) {
			combo.add(items[i]);
		}
		return combo;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @return the table
	 */
	public Table createTable(Composite composite, String name) {
		return createTable(composite, name, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
	}

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param name the name
	 * @param style the style
	 * @return the table
	 */
	public Table createTable(Composite composite, String name, int style) {
		return createTable(composite, style, name, false, false);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param style the style
	 * @param name the name
	 * @param headerVisible the header visible
	 * @param linesVisible the lines visible
	 * @return the table
	 */
	public Table createTable(Composite composite, int style, String name, boolean headerVisible, boolean linesVisible) {
		return createTable(composite, style, name, name, headerVisible, linesVisible);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param style the style
	 * @param data the data
	 * @param name the name
	 * @param headerVisible the header visible
	 * @param linesVisible the lines visible
	 * @return the table
	 */
	public Table createTable(Composite composite, int style, Object data, String name, boolean headerVisible, boolean linesVisible) {
		Table table = new Table(composite, style);
		table.setData(data);
		table.setData(NAME, name);
		table.setToolTipText(name);

		gridData = new GridData(GridData.FILL_BOTH);
		// gridData.heightHint = BTN_WIDTH;
		table.setLayoutData(gridData);

		table.setHeaderVisible(headerVisible);
		table.setLinesVisible(linesVisible);
		// table.s

		table.setForeground(new Color(Display.getDefault(), 50, 150, 50));
		table.setFont(fontRegistry.get("button-text"));

		return table;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param table the table
	 * @param style the style
	 * @param name the name
	 * @return the table column
	 */
	public TableColumn createTableColumn(Table table, int style, String name) {
		return createTableColumn(table, style, name, 10);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param table the table
	 * @param style the style
	 * @param name the name
	 * @param width the width
	 * @return the table column
	 */
	public TableColumn createTableColumn(Table table, int style, String name, int width) {
		TableColumn tableColumn = new TableColumn(table, style);
		tableColumn.setToolTipText(name);
		tableColumn.setText(name);
		tableColumn.setWidth(width);
		tableColumn.setResizable(true);
		return tableColumn;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param table the table
	 * @param checked the checked
	 * @param values the values
	 * @return the table item
	 */
	public TableItem createTableItem(Table table, boolean checked, String... values) {
		return createTableItem(table, values[0], checked, values);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param table the table
	 * @param data the data
	 * @param checked the checked
	 * @param values the values
	 * @return the table item
	 */
	public TableItem createTableItem(Table table, Object data, boolean checked, String... values) {
		TableItem tableItem = new TableItem(table, SWT.LEFT);
		tableItem.setChecked(checked);
		tableItem.setData(data);

		for (int i = 0; i < values.length; i++) {
			tableItem.setText(i, values[i]);
		}
		return tableItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param composite the composite
	 * @param style the style
	 * @param name the name
	 * @param headerVisible the header visible
	 * @param linesVisible the lines visible
	 * @return the tree
	 */
	public Tree createTree(Composite composite, int style, String name, boolean headerVisible, boolean linesVisible) {
		Tree tree = new Tree(composite, style);
		tree.setData(NAME, name);
		tree.setToolTipText(name);

		gridData = new GridData(GridData.FILL_BOTH);
		// gridData.heightHint = BTN_WIDTH;
		tree.setLayoutData(gridData);

		tree.setHeaderVisible(headerVisible);
		tree.setLinesVisible(linesVisible);
		// table.s

		tree.setForeground(new Color(Display.getDefault(), 50, 150, 50));
		tree.setFont(fontRegistry.get("button-text"));

		return tree;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param tree the tree
	 * @param style the style
	 * @param name the name
	 * @param width the width
	 * @return the tree column
	 */
	public TreeColumn createTreeColumn(Tree tree, int style, String name, int width) {
		TreeColumn treeColumn = new TreeColumn(tree, style);
		treeColumn.setToolTipText(name);
		treeColumn.setText(name);
		treeColumn.setWidth(width);
		return treeColumn;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param tree the tree
	 * @param style the style
	 * @param data the data
	 * @param text the text
	 * @return the tree item
	 */
	public TreeItem createTreeItem(Tree tree, int style, Object data, String... text) {
		TreeItem treeItem = new TreeItem(tree, style);
		// treeItem.setToolTipText(data.);
		treeItem.setData(data);
		treeItem.setText(text);

		return treeItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param item the item
	 * @param style the style
	 * @param data the data
	 * @param text the text
	 * @return the tree item
	 */
	public TreeItem createTreeItem(TreeItem item, int style, Object data, String... text) {

		TreeItem treeItem = new TreeItem(item, style);
		// treeItem.setToolTipText(name);
		treeItem.setData(data);
		treeItem.setText(text);

		return treeItem;

	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param widgetFactory the widget factory
	 * @param parent the parent
	 * @param name the name
	 * @return the text
	 */
	public Text createPropertyText(TabbedPropertySheetWidgetFactory widgetFactory, Composite parent, String name) {

		Composite composite = new Composite(parent, SWT.SHADOW_OUT);

		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginTop = 0;
		gridLayout.marginBottom = 0;
		gridLayout.marginHeight = 0;
		composite.setLayout(gridLayout);

		gridData = new GridData();
		gridData.verticalSpan = 0;
		gridData.verticalIndent = 0;

		composite.setLayoutData(gridData);

		gridData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gridData);

		CLabel label = widgetFactory.createCLabel(composite, name);
		gridData = new GridData();
		gridData.widthHint = 70;
		gridData.verticalSpan = 0;
		gridData.verticalIndent = 0;
		label.setLayoutData(gridData);

		Text text = widgetFactory.createText(composite, name);

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.verticalSpan = 0;
		gridData.verticalIndent = 0;
		text.setLayoutData(gridData);

		return text;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param rect the rect
	 * @param span the span
	 * @return the slider
	 */
	public Slider createSlider(Composite parent, Rectangle rect, byte span) {
		final Slider slider = new Slider(parent, SWT.HORIZONTAL);

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		if (span > 0) {
			gridData.horizontalSpan = span;
		} else {
			gridData.verticalSpan = span;
		}
		slider.setLayoutData(gridData);

		slider.setMinimum(0);
		slider.setMaximum(100);
		slider.setIncrement(1);
		slider.setPageIncrement(5);

		slider.setBounds(rect.x, rect.y, rect.width, rect.height);
		return slider;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param parent the parent
	 * @param rect the rect
	 * @param span the span
	 * @return the scale
	 */
	public Scale createScale(Composite parent, Rectangle rect, byte span) {
		final Scale scale = new Scale(parent, SWT.HORIZONTAL);

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		if (span > 0) {
			gridData.horizontalSpan = span;
		} else {
			gridData.verticalSpan = span;
		}
		scale.setLayoutData(gridData);

		scale.setMinimum(0);
		scale.setMaximum(100);
		scale.setIncrement(1);
		scale.setPageIncrement(5);

		scale.setBounds(rect.x, rect.y, rect.width, rect.height);

		scale.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				int perspectiveValue = scale.getMaximum() - scale.getSelection() + scale.getMinimum();
				System.err.println("Vol: " + perspectiveValue);

				System.err.println("Vol X : " + scale.getSelection());
			}
		});
		return scale;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param widget the widget
	 */
	public void createCalendar(final Widget widget) {
		final Shell dialog = new Shell(Display.getDefault(), SWT.NO_TRIM | SWT.ON_TOP | SWT.APPLICATION_MODAL);
		final DateTime dateTime = new DateTime(dialog, SWT.CALENDAR | SWT.BORDER);
		dialog.setLayout(new FillLayout());

		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());

		dateTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Long time = DateUtils.dateTimeToTime(dateTime);
				widget.setData("time", time);
				String formattedTime = DateUtils.decodeTime(time, EDateFormat.NICE_1);
				if (time != null) {
					if (widget instanceof Text) {
						((Text) widget).setText(formattedTime);
					} else if (widget instanceof Label) {
						((Label) widget).setText(formattedTime);
					} else if (widget instanceof TreeItem) {
						((TreeItem) widget).setText(1, formattedTime);
					}
				}
				dialog.dispose();
			}
		});

		Point location = Display.getCurrent().getCursorLocation();
		dialog.setLocation(location.x, location.y + 20);
		dialog.pack();
		dialog.open();
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new GUI object.
	 * @param coreImagePath the core image path
	 * @return the image
	 */
	public Image createImage(String coreImagePath) {
		return Activator.getImageDescriptor(coreImagePath).createImage();

	}

	// ---------------------------------------------------------------

	/**
	 * Gets the shell.
	 * @return the shell
	 */
	public static Shell getShell() {
		if (shell == null) {
			shell = new Shell(Display.getDefault());
		}
		return shell;
	}

	// ---------------------------------------------------------------

	public FormToolkit getToolkit() {
		return toolkit;
	}

	public void decorate(Control control, boolean show) {
		ControlDecoration controlDecoration = createControlDecoration(control);
		decorate(control, controlDecoration.getDescriptionText(), show);
	}

	/**
	 * Show decoration.
	 * @param control the control
	 * @param newDescriptionText the new description text
 	 * @param show the flags which indicates whether or not set decoration visible
	 */
	public static void decorate(Control control, String newDescriptionText, final boolean show) {
		// lazy decorator check/create
		final ControlDecoration controlDecoration = createControlDecoration(control, newDescriptionText);
		// description text can be different
		controlDecoration.setDescriptionText(newDescriptionText);

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (show) {
					controlDecoration.show();
				} else {
					controlDecoration.hide();
				}
			}
		});
	}

	public static ControlDecoration createControlDecoration(Control control) {
		return createControlDecoration(control, FTextConstants.WRONG_INPUT);
	}

	/**
	 * Creates a new GUI object.
	 * @param control the control to decorate
	 * @param descriptionText the description text
	 * @return the newly created control decoration
	 */
	public static ControlDecoration createControlDecoration(Control control, String descriptionText) {
		return createControlDecoration(control, descriptionText, FieldDecorationRegistry.DEC_ERROR);
	}

	/**
	 * Creates a new GUI object.
	 * @param control the control to decorate
	 * @param descriptionText the description text
	 * @param id the FieldDecoration id
	 * @return the newly created control decoration
	 */
	public static ControlDecoration createControlDecoration(Control control, String descriptionText, String id) {
		return createControlDecoration(control, SWT.LEFT | SWT.TOP, descriptionText, id);
	}

	/**
	 * Creates a new GUI object.
	 * @param control the control to decorate
	 * @param style the composite style flag to set
	 * @param descriptionText the description text
	 * @param id the FieldDecoration id
	 * @return the newly created control decoration
	 */
	public static ControlDecoration createControlDecoration(Control control, int style, String descriptionText, String id) {
		if (control.getData(FTextConstants.DECORATION) == null) {
			final ControlDecoration controlDecoration = new ControlDecoration(control, style);
			controlDecoration.setDescriptionText(descriptionText);
			FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(id);
			controlDecoration.setImage(fieldDecoration.getImage());
			controlDecoration.hide();
			control.setData(FTextConstants.DECORATION, controlDecoration);
			// dispose resources
			control.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					if (controlDecoration != null) {
						controlDecoration.dispose();
					}
				}
			});
		}
		return (ControlDecoration) control.getData(FTextConstants.DECORATION);
	}

}
