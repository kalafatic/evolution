package eu.kalafatic.evolution.view.application;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "eu.kalafatic.evolution.view.evoPerspective"; //$NON-NLS-1$

	@Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }
    
    @Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

    @Override
    public boolean preShutdown() {
        try {
            eu.kalafatic.evolution.controller.orchestration.ServerManager.getInstance().stopAll();
        } catch (Throwable t) {
            System.err.println("[ApplicationWorkbenchAdvisor] Error stopping servers on workbench shutdown: " + t.getMessage());
        }
        return super.preShutdown();
    }
}
