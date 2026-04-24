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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import eu.kalafatic.utils.builders.WorkbenchActionBuilder;
import eu.kalafatic.utils.hack.StatusLineContributionItem;
import eu.kalafatic.utils.lib.AppData;
import eu.kalafatic.utils.lib.ELang;

import static eu.kalafatic.utils.constants.FCoreConstants.FILTERED_ACTIONSETS;
import static eu.kalafatic.utils.constants.FCoreImageConstants.ARROW_DOWN_IMG;
import static eu.kalafatic.utils.constants.FCoreImageConstants.ARROW_UP_IMG;
import static eu.kalafatic.utils.constants.FCoreImageConstants.CS_IMG;
import static eu.kalafatic.utils.constants.FCoreImageConstants.DE_IMG;
import static eu.kalafatic.utils.constants.FCoreImageConstants.EN_IMG;
import static eu.kalafatic.utils.constants.FCoreImageConstants.ES_IMG;
import static eu.kalafatic.utils.constants.FCoreImageConstants.RU_IMG;
import static eu.kalafatic.utils.constants.FCoreImageConstants.ZH_IMG;

/**
 * The Class class ApplicationActionBarAdvisor.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private WorkbenchActionBuilder builder;

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
		builder = new WorkbenchActionBuilder(configurer);
	}

	@Override
	protected void fillStatusLine(IStatusLineManager statusLineManager) {
		// super.fillStatusLine(statusLineManager);

		statusLineManager.add(langItem);
		statusLineManager.add(cpuItem);

		cpuItem.setText("CPU: 100 %");

		String locale = System.getProperty("osgi.nl");
		langItem.setImage(getFlag(locale));
		langItem.setText("NL: " + locale);

		langItem.setVisible(true);
		cpuItem.setVisible(true);

		AppData.getInstance().setStatusLineManager(statusLineManager);

		//AppData.getInstance().setCpuItem(cpuItem);

		statusLineManager.setErrorMessage("fhn");
		statusLineManager.update(true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.application.ActionBarAdvisor#fillMenuBar(org.eclipse.jface .action.IMenuManager)
	 */
	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		// super.fillMenuBar(menuBar);

		IWorkbenchWindow window = getActionBarConfigurer().getWindowConfigurer().getWindow();
		menuBar.add(createFileMenu(window));
		menuBar.add(createExplorerMenu(window));
		menuBar.add(createToolsMenu(window));
		menuBar.add(createWindowMenu(window));
		menuBar.add(createHelpMenu(window));
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the file menu.
	 * @param window the window
	 * @return the i menu manager
	 */
	private IMenuManager createFileMenu(final IWorkbenchWindow window) {
		MenuManager menuManager = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
		menuManager.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));

		menuManager.add(ActionFactory.NEW.create(window));
		menuManager.add(new Separator());

		IAction saveAction = ActionFactory.SAVE.create(window);
		register(saveAction);
		menuManager.add(saveAction);

		IAction saveAllAction = ActionFactory.SAVE_ALL.create(window);
		register(saveAllAction);
		menuManager.add(saveAllAction);

		menuManager.add(new Separator());
		menuManager.add(ActionFactory.IMPORT.create(window));
		menuManager.add(ActionFactory.EXPORT.create(window));
		menuManager.add(new Separator());

		menuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuManager.add(new Separator());

		IAction quitAction = ActionFactory.QUIT.create(window);
		register(quitAction);
		menuManager.add(quitAction);

		menuManager.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
		return menuManager;
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

	private IMenuManager createWindowMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
		menu.add(ActionFactory.OPEN_NEW_WINDOW.create(window));
		menu.add(new Separator());
		MenuManager showViewMenuMgr = new MenuManager("Show &View");
		showViewMenuMgr.add(org.eclipse.ui.actions.ContributionItemFactory.VIEWS_SHORTLIST.create(window));
		menu.add(showViewMenuMgr);

		MenuManager perspectiveMenuMgr = new MenuManager("Open &Perspective");
		perspectiveMenuMgr.add(org.eclipse.ui.actions.ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(window));
		menu.add(perspectiveMenuMgr);

		menu.add(new Separator());
		IAction preferencesAction = ActionFactory.PREFERENCES.create(window);
		register(preferencesAction);
		menu.add(preferencesAction);

		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		return menu;
	}

	private IMenuManager createHelpMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
		menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));

		// For RCPs, these often need specific bundles, but we add the markers and standard actions

		menu.add(new Separator());

		IAction updateAction = builder.createAction(window, "org.eclipse.equinox.p2.ui.sdk.update");
		if (updateAction != null) {
			menu.add(updateAction);
		}

		IAction installAction = builder.createAction(window, "org.eclipse.equinox.p2.ui.sdk.install");
		if (installAction != null) {
			menu.add(installAction);
		}

		menu.add(new Separator());

		menu.add(ActionFactory.ABOUT.create(window));

		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
		return menu;
	}

	/**
	 * Fill tray item.
	 * @param trayMenu the tray menu
	 */
	public void fillTrayItem(MenuManager trayMenu) {
		 trayMenu.add(getAction(ActionFactory.ABOUT.getId()));
		 trayMenu.add(getAction(ActionFactory.CLOSE.getId()));
	}

	/**
	 * Gets the flag.
	 * @param locale the locale
	 * @return the flag
	 */
	private Image getFlag(String locale) {
		if (locale.toUpperCase().startsWith(ELang.CS.literal)) {
			return CS_IMG;
		} else if (locale.toUpperCase().startsWith(ELang.EN.literal)) {
			return EN_IMG;
		} else if (locale.toUpperCase().startsWith(ELang.DE.literal)) {
			return DE_IMG;
		} else if (locale.toUpperCase().startsWith(ELang.ZH.literal)) {
			return ZH_IMG;
		} else if (locale.toUpperCase().startsWith(ELang.RU.literal)) {
			return RU_IMG;
		} else if (locale.toUpperCase().startsWith(ELang.ES.literal)) {
			return ES_IMG;
		}
		return EN_IMG;
	}
}
