package eu.kalafatic.utils.builders;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.application.ActionBarAdvisor;
public class WorkbenchActionBuilder extends org.eclipse.ui.application.ActionBarAdvisor {
    public WorkbenchActionBuilder(org.eclipse.ui.application.IActionBarConfigurer configurer) { super(configurer); }
    public void fillTrayItem(MenuManager trayMenu) {}
    protected void fillStatusLine(IStatusLineManager statusLineManager) {}
    protected void fillMenuBar(IMenuManager menuBar) {}
    public org.eclipse.jface.action.IAction getAction(String id) { return null; }
}
