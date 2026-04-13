package eu.kalafatic.evolution.controller.splashHandlers;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.widgets.Label;
public class AbsolutePositionProgressMonitorPart implements org.eclipse.core.runtime.IProgressMonitor {
    public void beginTask(String name, int totalWork) {}
    public void done() {}
    public void internalWorked(double work) {}
    public boolean isCanceled() { return false; }
    public void setCanceled(boolean value) {}
    public void setTaskName(String name) {}
    public void subTask(String name) {}
    public void worked(int work) {}
    public void setVisible(boolean visible) {}
    public ProgressIndicator getProgressIndicator() { return null; }
    public Label getProgressText() { return null; }
    public void update() {}
}
