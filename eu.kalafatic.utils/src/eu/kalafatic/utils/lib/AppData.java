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

    public static AppData getInstance() { return instance; }
    public GeminiSplashHandler getSplashHandler() { return null; }
    public List<ISplashUser> getSplashUsersUsers() { return splashUsers; }
    public void setTrayItem(TrayItem item) { this.trayItem = item; }
    public void setSplashHandler(eu.kalafatic.utils.dialogs.GeminiSplashHandler handler) {}
    public void setStatusLineManager(org.eclipse.jface.action.IStatusLineManager manager) {}
}
