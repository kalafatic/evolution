package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;

import eu.kalafatic.evolution.view.menu.EvoGlobalContextMenuManager;

public class EvoGlobalContextMenuTest {

    @Test
    public void testGlobalContextMenuInstallationAndPopulation() {
        Display display = Display.getDefault();
        if (display == null) {
            return;
        }

        display.syncExec(() -> {
            Shell shell = new Shell(display);
            Text textControl = new Text(shell, SWT.MULTI);
            textControl.setText("Testing EVO Global Context Menu");
            textControl.selectAll();

            EvoGlobalContextMenuManager manager = EvoGlobalContextMenuManager.getInstance();
            manager.install();

            MenuManager menuMgr = new MenuManager();
            Event event = new Event();
            event.widget = textControl;

            manager.fillGlobalContextMenu(menuMgr, textControl, event);

            assertTrue("Context menu should contain items", menuMgr.getItems().length > 0);

            boolean hasEditSection = false;
            boolean hasProcessSection = false;
            boolean hasAiSection = false;
            boolean hasNavSection = false;

            for (org.eclipse.jface.action.IContributionItem item : menuMgr.getItems()) {
                if (item instanceof org.eclipse.jface.action.Separator) {
                    String id = item.getId();
                    if ("EDITING".equals(id)) hasEditSection = true;
                    if ("PROCESS".equals(id)) hasProcessSection = true;
                    if ("AI_ASSIST".equals(id)) hasAiSection = true;
                    if ("NAVIGATION".equals(id)) hasNavSection = true;
                }
            }

            assertTrue("Should contain EDITING section", hasEditSection);
            assertTrue("Should contain PROCESS section", hasProcessSection);
            assertTrue("Should contain AI_ASSIST section", hasAiSection);
            assertTrue("Should contain NAVIGATION section", hasNavSection);

            shell.dispose();
        });
    }

    @Test
    public void testEvoContextMenuSectionAppendedToExistingMenuManager() {
        Display display = Display.getDefault();
        if (display == null) {
            return;
        }

        display.syncExec(() -> {
            Shell shell = new Shell(display);
            Text textControl = new Text(shell, SWT.MULTI);

            EvoGlobalContextMenuManager manager = EvoGlobalContextMenuManager.getInstance();

            // Create a mock default context menu with standard Git actions
            MenuManager defaultGitMenuMgr = new MenuManager();
            org.eclipse.jface.action.Action gitCommitAction = new org.eclipse.jface.action.Action("Git Commit...") {};
            gitCommitAction.setId("git.commit");
            org.eclipse.jface.action.Action gitPushAction = new org.eclipse.jface.action.Action("Git Push...") {};
            gitPushAction.setId("git.push");

            defaultGitMenuMgr.add(gitCommitAction);
            defaultGitMenuMgr.add(gitPushAction);

            // Verify initial state: standard Git actions present
            assertNotNull("Git Commit action present", defaultGitMenuMgr.find("git.commit"));
            assertNotNull("Git Push action present", defaultGitMenuMgr.find("git.push"));

            // Append EVO context menu section
            manager.fillEvoContextMenuSection(defaultGitMenuMgr, textControl);

            // Verify both standard Git actions AND EVO context actions are present
            assertNotNull("Git Commit action preserved", defaultGitMenuMgr.find("git.commit"));
            assertNotNull("Git Push action preserved", defaultGitMenuMgr.find("git.push"));
            assertNotNull("EVO section separator present", defaultGitMenuMgr.find("EVO_SECTION"));
            assertNotNull("AI Smart Assistance submenu present", defaultGitMenuMgr.find("ai_smart_assistance"));
            assertNotNull("Quick Navigation submenu present", defaultGitMenuMgr.find("quick_navigation"));

            shell.dispose();
        });
    }

    @Test
    public void testFindMenuManagerFromExistingMenu() {
        Display display = Display.getDefault();
        if (display == null) {
            return;
        }

        display.syncExec(() -> {
            Shell shell = new Shell(display);
            Text textControl = new Text(shell, SWT.MULTI);

            EvoGlobalContextMenuManager manager = EvoGlobalContextMenuManager.getInstance();

            MenuManager menuMgr = new MenuManager("#TestMenu");
            org.eclipse.swt.widgets.Menu contextMenu = menuMgr.createContextMenu(textControl);
            textControl.setMenu(contextMenu);

            org.eclipse.jface.action.IMenuManager found = manager.findMenuManager(textControl, contextMenu);
            assertNotNull("Should resolve IMenuManager from context menu", found);

            shell.dispose();
        });
    }
}
