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

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.osgi.service.prefs.Preferences;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.PreferenceInitializer;
import eu.kalafatic.utils.constants.APreferencePage;
import eu.kalafatic.utils.dialogs.DialogUtils;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * The Class class CorePreferencePage.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class CorePreferencePage extends APreferencePage {

	/** The reset btn. */
	private Button setBtn, resetBtn;

	/** The preferences. */
	private Preferences preferences;

	/**
	 * Instantiates a new core preference page.
	 */
	public CorePreferencePage() {
		super.noDefaultAndApplyButton();
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Activator.getPluginProperties().getString("CorePreferencePageDescription"));
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.lib.constants.APreferencePage#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void handleEvent(Event event) {
		try {
			if (event.widget == setBtn) {
				boolean set = setBtn.getSelection();
				preferences.putBoolean(ECorePreferences.SET.getName(), set);
				preferences.flush();
				resetBtn.setEnabled(!set);
			}
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
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

			Group group = GUIFactory.INSTANCE.createGroup(container, "Reset settings", 2);
			createReloadBox(group);

			return container;
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return parent;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the reload box.
	 * @param group the group
	 */
	private void createReloadBox(final Group group) {
		preferences = Platform.getPreferencesService().getRootNode().node(Activator.PLUGIN_ID);
		boolean set = preferences.getBoolean(ECorePreferences.SET.getName(), (Boolean) ECorePreferences.SET.getDef());

		setBtn = GUIFactory.INSTANCE.createButton(group, ECorePreferences.SET.getName(), SWT.CHECK, set, this);

		resetBtn = GUIFactory.INSTANCE.createButton(group, "Reset", SWT.PUSH);
		resetBtn.setEnabled(!set);

		resetBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferenceInitializer.setUp();
				setBtn.setSelection(true);
				setBtn.notifyListeners(SWT.Selection, new Event());
			}
		});
	}
}
