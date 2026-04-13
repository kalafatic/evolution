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
package eu.kalafatic.utils.preferences;

import static eu.kalafatic.utils.constants.FConstants.PREFERENCES;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.constants.APreferencePage;
import eu.kalafatic.utils.constants.FTextConstants;
import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.utils.db.DBUtils;
import eu.kalafatic.utils.db.DBUtils.DB;
import eu.kalafatic.utils.dialogs.DialogUtils;
import eu.kalafatic.utils.dialogs.SetupDialog;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * The Class class DatabasesPreferencePage.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings({ "unchecked" })
public class DatabasesPreferencePage extends APreferencePage {

	/** The databases. */
	private Map<String, DB> databases = new HashMap<String, DB>();

	/** The edit db btn. */
	private Button addDBBtn, editDBBtn;

	/** The db table. */
	private Table dbTable;

	/**
	 * Instantiates a new databases preference page.
	 */
	public DatabasesPreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("");
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.lib.constants.APreferencePage#handleEvent(org .eclipse.swt.widgets.Event)
	 */
	@Override
	public void handleEvent(Event event) {
		if (event.widget.equals(dbTable)) {
			editDBBtn.setEnabled(true);

		} else if (event.widget.equals(editDBBtn)) {

			DB db = (DB) dbTable.getSelection()[0].getData();
			if (db != null) {
				editDB(db);
			}
			editDBBtn.setEnabled(false);

		} else if (event.widget.equals(addDBBtn)) {
			editDB(null);
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		try {

			Composite container = GUIFactory.INSTANCE.createComposite(parent, 1);

			Group group = GUIFactory.INSTANCE.createGroup(container, "Databases", 2);
			createDatabasesBox(group);

			return container;
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return parent;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the databases box.
	 * @param group the group
	 * @throws Exception the exception
	 */
	private void createDatabasesBox(Group group) throws Exception {
		dbTable = GUIFactory.INSTANCE.createTable(group, FUIConstants.TAB_STYLE_2, "Databases", true, true);
		for (int i = 0; i < DBUtils.DB_URL_PARAMETERS.length; i++) {
			final TableColumn tc = GUIFactory.INSTANCE.createTableColumn(dbTable, SWT.BEGINNING, DBUtils.DB_URL_PARAMETERS[i]);
			tc.pack();
		}

		for (DB db : databases.values()) {
			TableItem tableItem = GUIFactory.INSTANCE.createTableItem(dbTable, db.active, db.settingsAsArray());
			tableItem.setData(db);
		}

		Composite composite = new Composite(group, SWT.SHADOW_IN);
		composite.setLayout(new GridLayout());

		GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING);
		gridData.widthHint = FUIConstants.BUTTON_WIDTH + 10;
		composite.setLayoutData(gridData);

		editDBBtn = GUIFactory.INSTANCE.createButton(composite, FTextConstants.EDIT, SWT.PUSH, this);
		editDBBtn.setEnabled(dbTable.getSelection().length > 0);

		addDBBtn = GUIFactory.INSTANCE.createButton(composite, FTextConstants.ADD, SWT.PUSH, this);

		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING);
		gridData.widthHint = FUIConstants.BUTTON_WIDTH;
		gridData.heightHint = FUIConstants.BUTTON_HEIGHT;

		editDBBtn.setLayoutData(gridData);
		addDBBtn.setLayoutData(gridData);

		dbTable.addListener(SWT.Selection, this);
	}

	// ---------------------------------------------------------------

	/**
	 * Edits the db.
	 * @param db the db
	 */
	private void editDB(DB db) {
		try {
			if (db == null) {
				db = DBUtils.getInstance().new DB();
			}
			SetupDialog.setup.put(FTextConstants.TITLE, "Edit Database");

			SetupDialog editDBDialog = new SetupDialog(db.settings);

			if (editDBDialog.open() == Window.OK) {
				db.parseURL();

				databases.put(db.url, db);

				TableItem tableItem = GUIFactory.INSTANCE.createTableItem(dbTable, db.active, db.settingsAsArray());
				tableItem.setData(db);
			}
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		try {
			// workspaceLocText.setText((String) ECorePreferences.WORKSPACE_LOC
			// .getDef());
			// PREFERENCES.put(ECorePreferences.WORKSPACE_LOC.getName(),
			// (String) ECorePreferences.WORKSPACE_LOC.getDef());
			//
			// modelsLocText
			// .setText((String) ECorePreferences.MODELS_LOC.getDef());
			// PREFERENCES.put(ECorePreferences.MODELS_LOC.getName(),
			// (String) ECorePreferences.MODELS_LOC.getDef());
			//
			// logsLocText.setText((String) ECorePreferences.LOGS_LOC.getDef());
			// PREFERENCES.put(ECorePreferences.LOGS_LOC.getName(),
			// (String) ECorePreferences.LOGS_LOC.getDef());
			//
			// certLocText.setText((String) ECorePreferences.CERT_LOC.getDef());
			// PREFERENCES.put(ECorePreferences.CERT_LOC.getName(),
			// (String) ECorePreferences.CERT_LOC.getDef());

			super.performDefaults();
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	protected void performApply() {
		try {

		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		try {
			// Forces the application to save the CORE preferences
			PREFERENCES.flush();
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return super.performOk();
	}
}
