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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.constants.APreferencePage;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * The Class class GeminiPreferencePage.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */

public class GeminiPreferencePage extends APreferencePage {

	/**
	 * Instantiates a new gemini preference page.
	 */
	public GeminiPreferencePage() {
		super.noDefaultAndApplyButton();
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Activator.getPluginProperties().getString("aboutText"));
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {

		Composite container = GUIFactory.INSTANCE.createComposite(parent, 1);
		return container;
	}
}