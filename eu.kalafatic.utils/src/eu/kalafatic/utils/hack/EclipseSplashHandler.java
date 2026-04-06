package eu.kalafatic.utils.hack;
import org.eclipse.ui.splash.AbstractSplashHandler;
public class EclipseSplashHandler extends AbstractSplashHandler {
    public void startSubTask(String msg, int flag) {}
    public void startTask(String msg, int total) {}
    public void runPending() {}
    public void setEndSplash(boolean b) {}
    public org.eclipse.swt.widgets.Shell createUI(org.eclipse.swt.widgets.Display d) { return null; }
    public void init(org.eclipse.swt.widgets.Shell s) { super.init(s); }
    public void setMonitor() {}
    public org.eclipse.core.runtime.IProgressMonitor getBundleProgressMonitor() { return null; }
    public void update() {}
    public void done() {}
    public int getAlpha() { return 255; }
    public void setAlpha(int alpha) {}
    public static class GSHf {
        public static int FLAG = 0;
        public static final int VISIBLE = 1;
        public static final int DONE = 2;
        public static final int TASK_END = 4;
    }
}
