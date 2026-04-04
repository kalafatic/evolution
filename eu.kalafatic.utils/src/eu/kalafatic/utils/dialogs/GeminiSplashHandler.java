package eu.kalafatic.utils.dialogs;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.splash.AbstractSplashHandler;

public class GeminiSplashHandler extends AbstractSplashHandler {
    public void startSubTask(String msg, int flag) {}
    public void startTask(String msg, int total) {}
    public void runPending() {}
    public void setEndSplash(boolean b) {}
    public Shell createUI(Display d) { return null; }
    public void init(Shell s) {}
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
