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
package eu.kalafatic.utils.p2;

import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.kalafatic.utils.Activator;

/**
 * The Class class CloudPolicy.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class CloudPolicy extends Policy {

	/**
	 * Update for preferences.
	 */
	@SuppressWarnings("unused")
	public void updateForPreferences() {
		setRepositoriesVisible(false);

		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		setRepositoriesVisible(prefs.getBoolean(PreferenceConstants.REPOSITORIES_VISIBLE));
		setRestartPolicy(prefs.getInt(PreferenceConstants.RESTART_POLICY));
		setShowLatestVersionsOnly(prefs.getBoolean(PreferenceConstants.SHOW_LATEST_VERSION_ONLY));
		setGroupByCategory(prefs.getBoolean(PreferenceConstants.AVAILABLE_GROUP_BY_CATEGORY));
		setShowDrilldownRequirements(prefs.getBoolean(PreferenceConstants.SHOW_DRILLDOWN_REQUIREMENTS));
		// setFilterOnEnv(prefs.getBoolean(PreferenceConstants.FILTER_ON_ENV));
		// setUpdateWizardStyle(prefs
		// .getInt(PreferenceConstants.UPDATE_WIZARD_STYLE));
		int preferredWidth = prefs.getInt(PreferenceConstants.UPDATE_DETAILS_WIDTH);
		int preferredHeight = prefs.getInt(PreferenceConstants.UPDATE_DETAILS_HEIGHT);
		// setUpdateDetailsPreferredSize(new Point(preferredWidth,
		// preferredHeight));

		if (prefs.getBoolean(PreferenceConstants.AVAILABLE_SHOW_ALL_BUNDLES)) {
			setVisibleAvailableIUQuery(QueryUtil.ALL_UNITS);
		} else {
			setVisibleAvailableIUQuery(QueryUtil.createIUGroupQuery());
		}
		if (prefs.getBoolean(PreferenceConstants.INSTALLED_SHOW_ALL_BUNDLES)) {
			setVisibleAvailableIUQuery(QueryUtil.ALL_UNITS);
		} else {
			setVisibleAvailableIUQuery(new UserVisibleRootQuery());
		}

	}
}