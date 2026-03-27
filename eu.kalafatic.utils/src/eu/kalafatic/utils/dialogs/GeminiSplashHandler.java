package eu.kalafatic.utils.dialogs;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.AbstractSplashHandler;
public class GeminiSplashHandler extends AbstractSplashHandler {
    public static class GSHf {
        public static final int FLAG = 0;
        public static final int VISIBLE = 1;
        public static final int DONE = 2;
        public static final int TASK_END = 4;
    }
    public void startSubTask(String name, int flag) {}
    public void startTask(String name, int total) {}
    public int getAlpha() { return 0; }
    public void setAlpha(int alpha) {}
    public void runPending() {}
    public void setEndSplash(boolean end) {}
    public Shell createUI(Display display) { return null; }
    public void setMonitor() {}
    public org.eclipse.core.runtime.IProgressMonitor getBundleProgressMonitor() { return null; }
    public void update() {}
    public void done() {}
}
