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

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.constants.APreferencePage;
import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.utils.dialogs.DialogUtils;
import eu.kalafatic.utils.factories.GUIFactory;
import eu.kalafatic.utils.hack.ImageCombo;

/**
 * @author Petr Kalafatic
 * @project Gemini 2.0
 */
public class LanguagePreferencePage extends APreferencePage {

	private GridData gridData;
	private ImageCombo langCombo;
	private Text langText;

	private String currentLanguage;
	private String locale;

	/**
	 *
	 */
	public LanguagePreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("");
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		try {

			gridData = new GridData(GridData.FILL_BOTH);
			gridData.widthHint = 600;
			gridData.heightHint = 400;
			parent.setLayoutData(gridData);

			Composite container = GUIFactory.INSTANCE.createComposite(parent, 1);
			container.setLayoutData(gridData);

			Group group = GUIFactory.INSTANCE.createGroup(container, "Application language", 3);
			createLanguageBox(group);
			return container;
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return parent;
	}

	// ---------------------------------------------------------------

	@Override
	public void handleEvent(Event event) {
		if (event.widget.equals(langCombo)) {
			TableItem item = langCombo.getItem(langCombo.getSelectionIndex());
			String language = item.getText();

			langText.setText(language);
			PREFERENCES.put(ECorePreferences.LANGUAGE.getName(), language);

			System.setProperty("osgi.nl", language);
			System.getProperties().put("osgi.nl", language);
			Locale.setDefault(Locale.forLanguageTag(language));
		}
	}

	// ---------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private void createLanguageBox(final Group group) throws BackingStoreException {

		locale = PREFERENCES.get(ECorePreferences.LANGUAGE.getName(), (String) ECorePreferences.LANGUAGE.getDef());

		GUIFactory.INSTANCE.createLabel(group, ECorePreferences.LANGUAGE.getName(), SWT.NONE);

		langText = GUIFactory.INSTANCE.createText(group, ECorePreferences.LANGUAGE.getName(), false, SWT.SINGLE | SWT.BORDER, FUIConstants.LABEL_WIDTH);
		langText.setText(locale);

		langCombo = GUIFactory.INSTANCE.createImageCombo(group, ECorePreferences.LANGUAGES.getName(), (List<Object[]>) ECorePreferences.LANGUAGES.getDef());
		langCombo.select(ECorePreferences.getLocaleIndex(locale));
		langCombo.addListener(SWT.Selection, this);
	}

	// ---------------------------------------------------------------

	@Override
	protected void performDefaults() {
		langText.setText((String) ECorePreferences.LANGUAGE.getDef());
		PREFERENCES.put(ECorePreferences.LANGUAGE.getName(), (String) ECorePreferences.LANGUAGE.getDef());
		System.setProperty("osgi.nl", (String) ECorePreferences.LANGUAGE.getDef());
		System.getProperties().put("osgi.nl", ECorePreferences.LANGUAGE.getDef());
		Locale.setDefault(Locale.forLanguageTag((String) ECorePreferences.LANGUAGE.getDef()));
		super.performDefaults();
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
			// Activator.getPreferences().flush();
			PREFERENCES.flush();

			if (!langText.getText().equals(locale)) {
				int answer = DialogUtils.INSTANCE.question("Changes needs restart\nDo you want to restart now?");
				if (answer == SWT.YES) {
					PlatformUI.getWorkbench().restart();
				}
			}

		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return super.performOk();
	}
}
