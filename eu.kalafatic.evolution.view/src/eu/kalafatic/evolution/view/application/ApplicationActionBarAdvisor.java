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
		Image flag = getFlag(locale);
		if (flag != null) {
			langItem.setImage(flag);
		}
		langItem.setText("NL: " + (locale != null ? locale : "en"));

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
		menuBar.add(createEvolutionMenu(window));
		menuBar.add(createFileMenu(window));
		menuBar.add(createEditMenu(window));
		menuBar.add(createServerMenu(window));
		menuBar.add(createExplorerMenu(window));
		menuBar.add(createToolsMenu(window));
		menuBar.add(createWindowMenu(window));
		menuBar.add(createHelpMenu(window));
	}

	// ---------------------------------------------------------------

	private IMenuManager createEvolutionMenu(final IWorkbenchWindow window) {
		MenuManager menuManager = new MenuManager("&Evolution", "eu.kalafatic.evolution.menu.evolution");
		menuManager.add(new GroupMarker("evoStart"));

		IAction memoryContextAction = builder.createAction(window, "eu.kalafatic.evolution.view.openMemoryContext");
		if (memoryContextAction != null) {
			menuManager.add(memoryContextAction);
		}

		IAction newSessionAction = builder.createAction(window, "eu.kalafatic.evolution.view.newAiSession");
		if (newSessionAction != null) {
			menuManager.add(newSessionAction);
		}

		IAction orchestrateAction = builder.createAction(window, "eu.kalafatic.evolution.controller.orchestrationCommand");
		if (orchestrateAction != null) {
			menuManager.add(orchestrateAction);
		}

		menuManager.add(new Separator());

		IAction setupOllamaAction = builder.createAction(window, "eu.kalafatic.evolution.view.setupOllama");
		if (setupOllamaAction != null) {
			menuManager.add(setupOllamaAction);
		}

		IAction setupLLMAction = builder.createAction(window, "eu.kalafatic.evolution.view.setupLLM");
		if (setupLLMAction != null) {
			menuManager.add(setupLLMAction);
		}

		IAction setupWorkspaceAction = builder.createAction(window, "eu.kalafatic.evolution.view.setupWorkspace");
		if (setupWorkspaceAction != null) {
			menuManager.add(setupWorkspaceAction);
		}

		menuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuManager.add(new GroupMarker("evoEnd"));
		return menuManager;
	}

	private IMenuManager createEditMenu(final IWorkbenchWindow window) {
		MenuManager menuManager = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
		menuManager.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));

		IAction undoAction = ActionFactory.UNDO.create(window);
		register(undoAction);
		menuManager.add(undoAction);

		IAction redoAction = ActionFactory.REDO.create(window);
		register(redoAction);
		menuManager.add(redoAction);

		menuManager.add(new Separator());

		IAction cutAction = ActionFactory.CUT.create(window);
		register(cutAction);
		menuManager.add(cutAction);

		IAction copyAction = ActionFactory.COPY.create(window);
		register(copyAction);
		menuManager.add(copyAction);

		IAction pasteAction = ActionFactory.PASTE.create(window);
		register(pasteAction);
		menuManager.add(pasteAction);

		menuManager.add(new Separator());

		IAction selectAllAction = ActionFactory.SELECT_ALL.create(window);
		register(selectAllAction);
		menuManager.add(selectAllAction);

		menuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuManager.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
		return menuManager;
	}

	private IMenuManager createServerMenu(final IWorkbenchWindow window) {
		MenuManager menuManager = new MenuManager("&Server", "Server");
		menuManager.add(new GroupMarker("serverStart"));

		IAction serverMemoryAction = builder.createAction(window, "eu.kalafatic.evolution.view.openMemoryContext");
		if (serverMemoryAction != null) {
			menuManager.add(serverMemoryAction);
		}

		// Web Interface lifecycle controls: rebuilt on every menu open so the
		// items always reflect the actual server state. Every control routes
		// through the single authoritative ServerManager.
		MenuManager webInterfaceMenu = new MenuManager("&Web Interface", "webInterface");
		webInterfaceMenu.add(new WebInterfaceContributionItem(window));
		menuManager.add(webInterfaceMenu);

		menuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuManager.add(new GroupMarker("serverEnd"));
		return menuManager;
	}

	/**
	 * Dynamic Web Interface menu content (Open / Start / Stop / Restart /
	 * Status). The state is read from the authoritative {@code ServerManager}
	 * at menu-open time, so enablement never goes stale.
	 */
	private static final class WebInterfaceContributionItem
			extends org.eclipse.jface.action.ContributionItem {

		private final IWorkbenchWindow window;

		WebInterfaceContributionItem(IWorkbenchWindow window) {
			super("eu.kalafatic.evolution.view.webInterfaceControls");
			this.window = window;
		}

		@Override
		public boolean isDynamic() {
			return true;
		}

		@Override
		public void fill(org.eclipse.swt.widgets.Menu parent, int index) {
			eu.kalafatic.evolution.controller.orchestration.ServerManager sm =
					eu.kalafatic.evolution.controller.orchestration.ServerManager.getInstance();
			eu.kalafatic.evolution.controller.orchestration.ServerEntry entry =
					sm.getServerEntryByServiceId("EVO_HTTP_PRIMARY");
			int port = sm.getPort();
			eu.kalafatic.evolution.controller.orchestration.ServerState state =
					entry != null ? entry.getState()
							: eu.kalafatic.evolution.controller.orchestration.ServerState.STOPPED;
			boolean running = state == eu.kalafatic.evolution.controller.orchestration.ServerState.RUNNING;
			String url = entry != null ? entry.getEndpointUrl() : null;

			org.eclipse.swt.widgets.MenuItem openItem =
					new org.eclipse.swt.widgets.MenuItem(parent, org.eclipse.swt.SWT.PUSH, index++);
			openItem.setText("Open Web Interface");
			openItem.setEnabled(running);
			openItem.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
				@Override
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					eu.kalafatic.evolution.view.workbench.EvoDesktopLauncher.openWebInterface();
				}
			});

			org.eclipse.swt.widgets.MenuItem openUrlItem =
					new org.eclipse.swt.widgets.MenuItem(parent, org.eclipse.swt.SWT.PUSH, index++);
			openUrlItem.setText("Open Configured Web URL");
			openUrlItem.setEnabled(running);
			openUrlItem.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
				@Override
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					if (url != null) {
						eu.kalafatic.evolution.view.workbench.EvoDesktopLauncher.launchBrowser(url);
					}
				}
			});

			new org.eclipse.swt.widgets.MenuItem(parent, org.eclipse.swt.SWT.SEPARATOR, index++);

			org.eclipse.swt.widgets.MenuItem startItem =
					new org.eclipse.swt.widgets.MenuItem(parent, org.eclipse.swt.SWT.PUSH, index++);
			startItem.setText("Start Web Server");
			startItem.setEnabled(state.canStart());
			startItem.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
				@Override
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					runServerOperation(window, "start", () -> sm.start(port));
				}
			});

			org.eclipse.swt.widgets.MenuItem stopItem =
					new org.eclipse.swt.widgets.MenuItem(parent, org.eclipse.swt.SWT.PUSH, index++);
			stopItem.setText("Stop Web Server");
			stopItem.setEnabled(state.canStop());
			stopItem.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
				@Override
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					runServerOperation(window, "stop", () -> sm.stop(port));
				}
			});

			org.eclipse.swt.widgets.MenuItem restartItem =
					new org.eclipse.swt.widgets.MenuItem(parent, org.eclipse.swt.SWT.PUSH, index++);
			restartItem.setText("Restart Web Server");
			restartItem.setEnabled(state.canRestart());
			restartItem.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
				@Override
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					runServerOperation(window, "restart", () -> sm.restart(port));
				}
			});

			new org.eclipse.swt.widgets.MenuItem(parent, org.eclipse.swt.SWT.SEPARATOR, index++);

			org.eclipse.swt.widgets.MenuItem statusItem =
					new org.eclipse.swt.widgets.MenuItem(parent, org.eclipse.swt.SWT.PUSH, index++);
			statusItem.setText("Check Status");
			statusItem.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
				@Override
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					String message = "State: " + state + (state.isRunning() ? "\nURL: " + url : "");
					if (entry != null && entry.getLastError() != null
							&& !entry.getLastError().isEmpty()) {
						message += "\nLast error: " + entry.getLastError();
					}
					org.eclipse.jface.dialogs.MessageDialog.openInformation(window.getShell(),
							"Web Server Status", message);
				}
			});
		}

		private void runServerOperation(IWorkbenchWindow window, String operation,
				ServerOperationBody body) {
			try {
				body.run();
			} catch (Exception e) {
				org.eclipse.jface.dialogs.MessageDialog.openError(window.getShell(),
						"Web Server " + operation + " failed", e.getMessage());
			}
		}

		private interface ServerOperationBody {
			void run() throws Exception;
		}
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

		// Family UX Standard v1.0: terminal + web interface entry points
		IAction openTerminalAction = new org.eclipse.jface.action.Action("Open &Terminal") {
			@Override
			public void run() {
				eu.kalafatic.evolution.view.workbench.EvoDesktopLauncher.openTerminal();
			}
		};
		openTerminalAction.setId("eu.kalafatic.evolution.view.openTerminal");
		register(openTerminalAction);
		menuManager.add(openTerminalAction);

		IAction openWebAction = new org.eclipse.jface.action.Action("Open &Web Interface") {
			@Override
			public void run() {
				eu.kalafatic.evolution.view.workbench.EvoDesktopLauncher.openWebInterface();
			}
		};
		openWebAction.setId("eu.kalafatic.evolution.view.openWebInterface");
		register(openWebAction);
		menuManager.add(openWebAction);

		menuManager.add(new Separator());

		menuManager.add(new org.eclipse.jface.action.Action("Start Background Discovery (Git / Workspace)") {
			@Override
			public void run() {
				eu.kalafatic.evolution.controller.manager.BackgroundDiscoveryService.getInstance().startAllScans();
			}
		});

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

		// Family UX Standard v1.0: EVO help page sections served by the primary
		// EvolutionServer (http://127.0.0.1:<port>/help.html#<anchor>).
		menu.add(helpAction("&Help Contents",
				eu.kalafatic.evolution.view.workbench.HelpLauncher.TOPIC_TOP));
		menu.add(helpAction("&Getting Started",
				eu.kalafatic.evolution.view.workbench.HelpLauncher.TOPIC_GETTING_STARTED));
		menu.add(helpAction("Context &Help",
				eu.kalafatic.evolution.view.workbench.HelpLauncher.TOPIC_CONTEXT));
		menu.add(helpAction("&Keyboard Shortcuts",
				eu.kalafatic.evolution.view.workbench.HelpLauncher.TOPIC_SHORTCUTS));
		menu.add(helpAction("&CLI Commands",
				eu.kalafatic.evolution.view.workbench.HelpLauncher.TOPIC_CLI));
		menu.add(helpAction("&Diagnostics",
				eu.kalafatic.evolution.view.workbench.HelpLauncher.TOPIC_DIAGNOSTICS));

		menu.add(new Separator());

		// Check for Updates / Install New Software... are contributed by the
		// standard Eclipse p2 UI (org.eclipse.equinox.p2.ui.sdk) into the
		// additions group below.

		menu.add(new Separator());

		IAction sitesAction = new org.eclipse.jface.action.Action("Manage Software Sites...") {
			@Override
			public void run() {
				org.eclipse.ui.dialogs.PreferencesUtil.createPreferenceDialogOn(window.getShell(),
						"org.eclipse.equinox.internal.p2.ui.sdk.SitesPreferencePage", null, null)
						.open();
			}
		};
		menu.add(sitesAction);

		IAction installDetailsAction = builder.createAction(window,
				"org.eclipse.equinox.p2.ui.sdk.installationDetails");
		if (installDetailsAction != null) {
			menu.add(installDetailsAction);
		}

		menu.add(new Separator());

		menu.add(ActionFactory.ABOUT.create(window));

		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
		return menu;
	}

	/**
	 * Creates a help menu action that opens the EVO help page at the given
	 * anchor via the shared {@code HelpLauncher}.
	 * @param label the action label
	 * @param anchor the help page anchor (without '#')
	 * @return the registered action
	 */
	private IAction helpAction(String label, final String anchor) {
		IAction action = new org.eclipse.jface.action.Action(label) {
			@Override
			public void run() {
				eu.kalafatic.evolution.view.workbench.HelpLauncher.openHelpTopic(anchor);
			}
		};
		action.setId("eu.kalafatic.evolution.view.help." + anchor);
		register(action);
		return action;
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
		if (locale == null) {
			return EN_IMG;
		}
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
