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
package eu.kalafatic.evolution.view.application;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.IActionBarConfigurer;


import eu.kalafatic.utils.builders.WorkbenchActionBuilder;
import eu.kalafatic.utils.lib.AppData;

/**
 * The Class class ApplicationActionBarAdvisor.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ApplicationActionBarAdvisor extends WorkbenchActionBuilder {

	/** The width. */
	private final int width = 70;

	/** The cpu item. */
	private final StatusLineContributionItem cpuItem = new StatusLineContributionItem("CPU", width);

	/** The lang item. */
	private final StatusLineContributionItem langItem = new StatusLineContributionItem("NL", width);

	/**
	 * Instantiates a new application action bar advisor.
	 * @param configurer the configurer
	 */
	ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	protected void fillStatusLine(IStatusLineManager statusLineManager) {
		super.fillStatusLine(statusLineManager);

		statusLineManager.add(langItem);
		statusLineManager.add(cpuItem);

		cpuItem.setText("CPU: 100 %");

		String locale = System.getProperty("osgi.nl");
		langItem.setImage(getFlag(locale));
		langItem.setText("NL: " + locale);

		langItem.setVisible(true);
		cpuItem.setVisible(true);

		AppData.getInstance().setStatusLineManager(statusLineManager);

		// AppData.getInstance().setCpuItem(cpuItem);

		// statusLineManager.setErrorMessage("fhn");
		statusLineManager.update(true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.application.ActionBarAdvisor#fillMenuBar(org.eclipse.jface .action.IMenuManager)
	 */
	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		super.fillMenuBar(menuBar);

		IWorkbenchWindow window = getActionBarConfigurer().getWindowConfigurer().getWindow();
		menuBar.add(createExplorerMenu(window));
		menuBar.add(createToolsMenu(window));
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the tools menu.
	 * @param window the window
	 * @return the i menu manager
	 */
	private IMenuManager createToolsMenu(final IWorkbenchWindow window) {
		MenuManager menuManager = new MenuManager("&Tools", "Tools");
		menuManager.add(new GroupMarker("languageMarker"));
		menuManager.add(new GroupMarker("toolsMarker"));
		menuManager.add(new GroupMarker("actionMarker"));
		return menuManager;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the tools menu.
	 * @param window the window
	 * @return the i menu manager
	 */
	private IMenuManager createExplorerMenu(final IWorkbenchWindow window) {
		MenuManager menuManager = new MenuManager("&Explorer", "Explorer");

		menuManager.add(new GroupMarker("explorerMarker"));
		menuManager.add(new Separator());

		menuManager.add(new GroupMarker("settingsMarker"));
		menuManager.add(new Separator());

		menuManager.add(new GroupMarker("actionMarker"));

		return menuManager;
	}

	// ---------------------------------------------------------------

	/**
	 * Fill tray item.
	 * @param trayMenu the tray menu
	 */
	public void fillTrayItem(MenuManager trayMenu) {
		trayMenu.add(getAction(ActionFactory.ABOUT.getId()));
		trayMenu.add(getAction(ActionFactory.CLOSE.getId()));
	}
}
