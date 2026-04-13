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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.constants.APreferencePage;
import eu.kalafatic.utils.constants.FConstants;
import eu.kalafatic.utils.dialogs.DialogUtils;
import eu.kalafatic.utils.factories.GUIFactory;
import eu.kalafatic.utils.factories.SectionFactory;
import eu.kalafatic.utils.model.NetInterface;
import eu.kalafatic.utils.net.NetUtils;

/**
 * The Class class InterfacesPreferencePage.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */

public class InterfacesPreferencePage extends APreferencePage {

	/** The first section. */
	private boolean firstSection = true;

	/**
	 * Instantiates a new interfaces preference page.
	 */
	public InterfacesPreferencePage() {
		super.noDefaultAndApplyButton();
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
			Composite container = GUIFactory.INSTANCE.createComposite(parent, 1);
			Group group = GUIFactory.INSTANCE.createGroup(container, "Interfaces", 1, SWT.H_SCROLL | SWT.V_SCROLL);

			TableWrapLayout layout = new TableWrapLayout();
			layout.numColumns = 1;
			layout.horizontalSpacing = 0;
			layout.verticalSpacing = 0;
			layout.makeColumnsEqualWidth = true;

			ScrolledForm form = GUIFactory.INSTANCE.getToolkit().createScrolledForm(group);

			form.getBody().setLayout(layout);
			// form.getBody().setLayoutData(new TableWrapData(0, 0, 0, 0));
			form.getBody().setLayoutData(new TableWrapData(0, 0));

			List<NetInterface> interfaces = FConstants.INTERFACES;

			for (NetInterface netInterface : interfaces) {
				Composite sectionClient = SectionFactory.INSTANCE.createSectionClient(form, netInterface.getName());
				sectionClient.setLayout(new GridLayout(1, true));
				sectionClient.setLayoutData(new GridData(GridData.FILL_BOTH));

				createInterfaceBox(sectionClient, netInterface);
			}
			return container;
		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return parent;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the interface box.
	 * @param sectionClient the section client
	 * @param factory the factory
	 * @param netInterface the net interface
	 * @throws Exception the exception
	 */
	private void createInterfaceBox(final Composite sectionClient, NetInterface netInterface) throws Exception {

		GUIFactory.INSTANCE.createLabels(sectionClient, "Display name", netInterface.getDisplayName());

		if (netInterface.getMac() != null) {
			GUIFactory.INSTANCE.createLabels(sectionClient, "Mac address", netInterface.getMac());
		}

		GUIFactory.INSTANCE.createLabels(sectionClient, "Running", netInterface.isUp() ? "true" : "false");
		GUIFactory.INSTANCE.createLabels(sectionClient, "Virtual", netInterface.isVirtual() ? "true" : "false");
		GUIFactory.INSTANCE.createLabels(sectionClient, "Multicast", netInterface.isMulticast() ? "true" : "false");

		List<String> address = netInterface.getAddress();

		for (String stringIp : address) {

			String version = "IP version ";

			if (stringIp.startsWith("/")) {
				stringIp = stringIp.substring(1);
			} else {
				version += NetUtils.isIPv4(stringIp) ? "4" : "6";
			}
			GUIFactory.INSTANCE.createLabels(sectionClient, version, stringIp);
		}
	}

}