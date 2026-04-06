package eu.kalafatic.utils.lib;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.utils.dialogs.GeminiSplashHandler;
import eu.kalafatic.utils.interfaces.ISplashUser;
import org.eclipse.swt.widgets.TrayItem;

public class AppData {
    private static AppData instance = new AppData();
    private TrayItem trayItem;
    private List<ISplashUser> splashUsers = new ArrayList<>();
    private Object splashHandler;

    public static AppData getInstance() { return instance; }
    public Object getSplashHandler() { return splashHandler; }
    public List<ISplashUser> getSplashUsersUsers() { return splashUsers; }
    public void setTrayItem(TrayItem item) { this.trayItem = item; }
    public void setSplashHandler(Object handler) { this.splashHandler = handler; }
    public void setStatusLineManager(org.eclipse.jface.action.IStatusLineManager manager) {}
}
