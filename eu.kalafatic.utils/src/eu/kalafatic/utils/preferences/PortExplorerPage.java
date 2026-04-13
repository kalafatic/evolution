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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.application.ValidationUtils;
import eu.kalafatic.utils.constants.APreferencePage;
import eu.kalafatic.utils.constants.FConstants;
import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.utils.dialogs.DialogUtils;
import eu.kalafatic.utils.factories.GUIFactory;
import eu.kalafatic.utils.net.NetstatUtils;

/**
 * The Class class PortExplorerPage.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
public class PortExplorerPage extends APreferencePage {

	/** The discover port. */
	private String discoverPort = "0";

	/** The table columns. */
	private String[] tableColumns = new String[] { "Row", "Protocol", "From", "Where", "State", "Pid" };

	/** The discover port btn. */
	private Button discoverPortBtn;

	/** The discover port binding. */
	private Binding discoverPortBinding;

	/** The discover port table. */
	private Table discoverPortTable;

	/** The discover port text. */
	private Text singletonPortText, rcPortText, btPortText, trackerPortText, trackerUpldPortText, updatePortText, webUpldPortText, discoverPortText;

	/** The discover port value. */

	private IObservableValue discoverPortValue = BeanProperties.value("discoverPort").observe(this);
//	DataBindingContext dbc = new DataBindingContext();
//
//	IObservableValue<Integer> model =
//	    BeanProperties.value("discoverPort").observe(this);
//
//	IObservableValue<String> widget =
//	    WidgetProperties.text(SWT.Modify).observe(portText);
//
//	dbc.bindValue(widget, model,
//	    new UpdateValueStrategy<>(),
//	    new UpdateValueStrategy<>());

	/** The data binding context. */
	private DataBindingContext dataBindingContext = new DataBindingContext();

	/**
	 * Instantiates a new port explorer page.
	 */
	public PortExplorerPage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("A demonstration of a port preferences page");
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.lib.constants.APreferencePage#handleEvent(org .eclipse.swt.widgets.Event)
	 */
	@Override
	public void handleEvent(Event event) {
		if (event.widget == discoverPortBtn) {
			if (!discoverPortBinding.getValidationStatus().equals(IStatus.ERROR)) {
				String port = (String) discoverPortValue.getValue();
				String result = NetstatUtils.getInstance().getUsersOfPort(port);

				if (!result.trim().isEmpty()) {
					createPortUsersTable(discoverPortTable, result.split("\\s+"));
				}
			}
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

			Group group = GUIFactory.INSTANCE.createGroup(container, "Gemini ports", 2);
			createGEPortsBox(group);

			group = GUIFactory.INSTANCE.createGroup(container, "Discover ports", 2);
			createDiscoverPortsBox(group);

			return container;
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return parent;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the discover ports box.
	 * @param group the group
	 * @throws Exception the exception
	 */
	private void createDiscoverPortsBox(final Group group) throws Exception {
		discoverPortText = GUIFactory.INSTANCE.createText(group, "Set port number", true);

		discoverPortBtn = GUIFactory.INSTANCE.createButton(group, "Discover", SWT.PUSH, this);

		discoverPortTable = GUIFactory.INSTANCE.createTable(group, FUIConstants.TAB_STYLE_2, "", true, true);
		((GridData) discoverPortTable.getLayoutData()).heightHint = 100;

		for (int i = 0; i < tableColumns.length; i++) {
			GUIFactory.INSTANCE.createTableColumn(discoverPortTable, SWT.LEFT, tableColumns[i]).pack();
		}
		
		discoverPortBinding = dataBindingContext.bindValue(WidgetProperties.text(SWT.Modify).observe(discoverPortText), discoverPortValue,
				new UpdateValueStrategy().setAfterConvertValidator(ValidationUtils.INSTANCE.getDiscoverPortValidator(discoverPortText)), null);
//		discoverPortBinding = dataBindingContext.bindValue(SWTObservables.observeText(discoverPortText, SWT.Modify), discoverPortValue,
//				new UpdateValueStrategy().setAfterConvertValidator(ValidationUtils.INSTANCE.getDiscoverPortValidator(discoverPortText)), null);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the port users table.
	 * @param table the table
	 * @param split the split
	 */
	private void createPortUsersTable(Table table, String[] split) {
		table.removeAll();

		for (int i = 1, row = 0; i < split.length / 4;) {
			GUIFactory.INSTANCE.createTableItem(table, false, Integer.toString(row++), split[i++], split[i++], split[i++], split[i++], split[i++]);
			// TableItem item = new TableItem(table, SWT.NO);
			// item.setText(0, Integer.toString(row++));
			// item.setText(1, split[i++]);
			// item.setText(2, split[i++]);
			// item.setText(3, split[i++]);
			// item.setText(4, split[i++]);
			// item.setText(5, split[i++]);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the ge ports box.
	 * @param group the group
	 */
	private void createGEPortsBox(Group group) {
		createSingletonArgumentsPortBox(group);
		createRCPortBox(group);
		createBTPortBox(group);
		createTrackerPortBox(group);
		createTrackerUpldPortBox(group);
		createUpdatePortBox(group);
		createWebUpldPortBox(group);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the web upld port box.
	 * @param group the group
	 */
	private void createWebUpldPortBox(Group group) {
		GUIFactory.INSTANCE.createLabel(group, ECorePreferences.WEB_UPLOAD_PORT.getName(), SWT.NONE);

		webUpldPortText = GUIFactory.INSTANCE.createText(group, ECorePreferences.WEB_UPLOAD_PORT.getName(), true);

		int value = FConstants.PREFERENCES.getInt(ECorePreferences.WEB_UPLOAD_PORT.getName(), (Integer) ECorePreferences.WEB_UPLOAD_PORT.getDef());

		webUpldPortText.setText(Integer.toString(value));
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the update port box.
	 * @param group the group
	 */
	private void createUpdatePortBox(Group group) {
		GUIFactory.INSTANCE.createLabel(group, ECorePreferences.UPDATE_PORT.getName(), SWT.NONE);

		updatePortText = GUIFactory.INSTANCE.createText(group, ECorePreferences.UPDATE_PORT.getName(), true);

		int value = FConstants.PREFERENCES.getInt(ECorePreferences.UPDATE_PORT.getName(), (Integer) ECorePreferences.UPDATE_PORT.getDef());

		updatePortText.setText(Integer.toString(value));
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the tracker upld port box.
	 * @param group the group
	 */
	private void createTrackerUpldPortBox(Group group) {
		GUIFactory.INSTANCE.createLabel(group, ECorePreferences.TRACKER_UPLOAD_PORT.getName(), SWT.NONE);

		trackerUpldPortText = GUIFactory.INSTANCE.createText(group, ECorePreferences.TRACKER_UPLOAD_PORT.getName(), true);

		int value = FConstants.PREFERENCES.getInt(ECorePreferences.TRACKER_UPLOAD_PORT.getName(), (Integer) ECorePreferences.TRACKER_UPLOAD_PORT.getDef());

		trackerUpldPortText.setText(Integer.toString(value));
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the tracker port box.
	 * @param group the group
	 */
	private void createTrackerPortBox(Group group) {
		GUIFactory.INSTANCE.createLabel(group, ECorePreferences.TRACKER_PORT.getName(), SWT.NONE);

		trackerPortText = GUIFactory.INSTANCE.createText(group, ECorePreferences.TRACKER_PORT.getName(), true);

		int value = FConstants.PREFERENCES.getInt(ECorePreferences.TRACKER_PORT.getName(), (Integer) ECorePreferences.TRACKER_PORT.getDef());

		trackerPortText.setText(Integer.toString(value));
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the bt port box.
	 * @param group the group
	 */
	private void createBTPortBox(Group group) {
		GUIFactory.INSTANCE.createLabel(group, ECorePreferences.BT_CLIENT_PORT.getName(), SWT.NONE);

		btPortText = GUIFactory.INSTANCE.createText(group, ECorePreferences.BT_CLIENT_PORT.getName(), true);

		int value = FConstants.PREFERENCES.getInt(ECorePreferences.BT_CLIENT_PORT.getName(), (Integer) ECorePreferences.BT_CLIENT_PORT.getDef());

		btPortText.setText(Integer.toString(value));
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the rc port box.
	 * @param group the group
	 */
	private void createRCPortBox(Group group) {
		GUIFactory.INSTANCE.createLabel(group, ECorePreferences.RC_PORT.getName(), SWT.NONE);

		rcPortText = GUIFactory.INSTANCE.createText(group, ECorePreferences.RC_PORT.getName(), true);

		int value = FConstants.PREFERENCES.getInt(ECorePreferences.RC_PORT.getName(), (Integer) ECorePreferences.RC_PORT.getDef());

		rcPortText.setText(Integer.toString(value));
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the singleton arguments port box.
	 * @param group the group
	 */
	private void createSingletonArgumentsPortBox(final Group group) {
		GUIFactory.INSTANCE.createLabel(group, ECorePreferences.SINGLETON_ARGS_PORT.getName());

		singletonPortText = GUIFactory.INSTANCE.createText(group, ECorePreferences.SINGLETON_ARGS_PORT.getName(), true);

		int value = FConstants.PREFERENCES.getInt(ECorePreferences.SINGLETON_ARGS_PORT.getName(), (Integer) ECorePreferences.SINGLETON_ARGS_PORT.getDef());

		singletonPortText.setText(Integer.toString(value));
		setDecoration(singletonPortText, "bzdcb");
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the decoration.
	 * @param control the control
	 * @param hoverText the hover text
	 */
	public void setDecoration(Control control, String hoverText) {
		ControlDecoration controlDecoration = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		controlDecoration.setDescriptionText(hoverText);
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING);
		controlDecoration.setImage(fieldDecoration.getImage());
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		try {
			singletonPortText.setText(Integer.toString((Integer) ECorePreferences.SINGLETON_ARGS_PORT.getDef()));
			FConstants.PREFERENCES.putInt(ECorePreferences.SINGLETON_ARGS_PORT.getName(), (Integer) ECorePreferences.SINGLETON_ARGS_PORT.getDef());

			rcPortText.setText(Integer.toString((Integer) ECorePreferences.RC_PORT.getDef()));
			FConstants.PREFERENCES.putInt(ECorePreferences.RC_PORT.getName(), (Integer) ECorePreferences.RC_PORT.getDef());

			btPortText.setText(Integer.toString((Integer) ECorePreferences.BT_CLIENT_PORT.getDef()));
			FConstants.PREFERENCES.putInt(ECorePreferences.BT_CLIENT_PORT.getName(), (Integer) ECorePreferences.BT_CLIENT_PORT.getDef());

			trackerPortText.setText(Integer.toString((Integer) ECorePreferences.TRACKER_PORT.getDef()));
			FConstants.PREFERENCES.putInt(ECorePreferences.TRACKER_PORT.getName(), (Integer) ECorePreferences.TRACKER_PORT.getDef());

			trackerUpldPortText.setText(Integer.toString((Integer) ECorePreferences.TRACKER_UPLOAD_PORT.getDef()));
			FConstants.PREFERENCES.putInt(ECorePreferences.TRACKER_UPLOAD_PORT.getName(), (Integer) ECorePreferences.TRACKER_UPLOAD_PORT.getDef());

			updatePortText.setText(Integer.toString((Integer) ECorePreferences.UPDATE_PORT.getDef()));
			FConstants.PREFERENCES.putInt(ECorePreferences.UPDATE_PORT.getName(), (Integer) ECorePreferences.UPDATE_PORT.getDef());

			webUpldPortText.setText(Integer.toString((Integer) ECorePreferences.WEB_UPLOAD_PORT.getDef()));
			FConstants.PREFERENCES.putInt(ECorePreferences.WEB_UPLOAD_PORT.getName(), (Integer) ECorePreferences.WEB_UPLOAD_PORT.getDef());

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
			FConstants.PREFERENCES.putInt(ECorePreferences.SINGLETON_ARGS_PORT.getName(), Integer.parseInt(singletonPortText.getText()));
			FConstants.PREFERENCES.putInt(ECorePreferences.RC_PORT.getName(), Integer.parseInt(rcPortText.getText()));
			FConstants.PREFERENCES.putInt(ECorePreferences.BT_CLIENT_PORT.getName(), Integer.parseInt(btPortText.getText()));
			FConstants.PREFERENCES.putInt(ECorePreferences.TRACKER_PORT.getName(), Integer.parseInt(trackerPortText.getText()));
			FConstants.PREFERENCES.putInt(ECorePreferences.UPDATE_PORT.getName(), Integer.parseInt(updatePortText.getText()));
			FConstants.PREFERENCES.putInt(ECorePreferences.WEB_UPLOAD_PORT.getName(), Integer.parseInt(webUpldPortText.getText()));
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
			FConstants.PREFERENCES.flush();
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return super.performOk();
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the discover port.
	 * @return the discover port
	 */
	public String getDiscoverPort() {
		return discoverPort;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the discover port.
	 * @param discoverPort the new discover port
	 */
	public void setDiscoverPort(String discoverPort) {
		this.discoverPort = discoverPort;
	}
}
