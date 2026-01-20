package eu.kalafatic.evolution.view;

import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

    private IWorkbenchAction newAction;
    private IWorkbenchAction saveAction;
    private IWorkbenchAction saveAsAction;
    private IWorkbenchAction closeAction;
    private IWorkbenchAction closeAllAction;
    private IWorkbenchAction exitAction;

    private IWorkbenchAction cutAction;
    private IWorkbenchAction copyAction;
    private IWorkbenchAction pasteAction;
    private IWorkbenchAction deleteAction;
    private IWorkbenchAction selectAllAction;

    private IWorkbenchAction findAction;

    private IWorkbenchAction preferencesAction;

    private IWorkbenchAction helpContentsAction;
    private IWorkbenchAction aboutAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}
	
	@Override
	protected void makeActions(IWorkbenchWindow window) {
        newAction = ActionFactory.NEW.create(window);
        register(newAction);

        saveAction = ActionFactory.SAVE.create(window);
        register(saveAction);

        saveAsAction = ActionFactory.SAVE_AS.create(window);
        register(saveAsAction);

        closeAction = ActionFactory.CLOSE.create(window);
        register(closeAction);

        closeAllAction = ActionFactory.CLOSE_ALL.create(window);
        register(closeAllAction);

        exitAction = ActionFactory.QUIT.create(window);
        register(exitAction);

        cutAction = ActionFactory.CUT.create(window);
        register(cutAction);

        copyAction = ActionFactory.COPY.create(window);
        register(copyAction);

        pasteAction = ActionFactory.PASTE.create(window);
        register(pasteAction);

        deleteAction = ActionFactory.DELETE.create(window);
        register(deleteAction);

        selectAllAction = ActionFactory.SELECT_ALL.create(window);
        register(selectAllAction);

        findAction = ActionFactory.FIND.create(window);
        register(findAction);

        preferencesAction = ActionFactory.PREFERENCES.create(window);
        register(preferencesAction);

        helpContentsAction = ActionFactory.HELP_CONTENTS.create(window);
        register(helpContentsAction);

        aboutAction = ActionFactory.ABOUT.create(window);
        register(aboutAction);
	}
	
	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
        // File menu
        MenuManager fileMenu = new MenuManager("&File", "file");
        fileMenu.add(newAction);
        fileMenu.add(saveAction);
        fileMenu.add(saveAsAction);
        fileMenu.add(closeAction);
        fileMenu.add(closeAllAction);
        fileMenu.add(exitAction);
        menuBar.add(fileMenu);

        // Edit menu
        MenuManager editMenu = new MenuManager("&Edit", "edit");
        editMenu.add(cutAction);
        editMenu.add(copyAction);
        editMenu.add(pasteAction);
        editMenu.add(deleteAction);
        editMenu.add(selectAllAction);
        menuBar.add(editMenu);

        // Search menu
        MenuManager searchMenu = new MenuManager("&Search", "search");
        searchMenu.add(findAction);
        menuBar.add(searchMenu);

        // Project menu
        MenuManager projectMenu = new MenuManager("&Project", "project");
        menuBar.add(projectMenu);

        // Run menu
        MenuManager runMenu = new MenuManager("&Run", "run");
        menuBar.add(runMenu);

        // Window menu
        MenuManager windowMenu = new MenuManager("&Window", "window");
        windowMenu.add(preferencesAction);
        menuBar.add(windowMenu);

        // Help menu
        MenuManager helpMenu = new MenuManager("&Help", "help");
        helpMenu.add(helpContentsAction);
        helpMenu.add(aboutAction);
        menuBar.add(helpMenu);
	}
	
	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		super.fillCoolBar(coolBar);
	}

}
